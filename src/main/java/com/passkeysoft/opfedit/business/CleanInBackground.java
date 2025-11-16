/*-*
   Copyright-Only Dedication (based on United States law)

  The person or persons who have associated their work with this
  document (the "Dedicator") hereby dedicate whatever copyright they
  may have in the work of authorship herein (the "Work") to the
  public domain.

  Dedicator makes this dedication for the benefit of the public at
  large and to the detriment of Dedicator's heirs and successors.
  Dedicator intends this dedication to be an overt act of
  relinquishment in perpetuity of all present and future rights
  under copyright law, whether vested or contingent, in the Work.
  Dedicator understands that such relinquishment of all rights
  includes the relinquishment of all rights to enforce (by lawsuit
  or otherwise) those copyrights in the Work.

  Dedicator recognizes that, once placed in the public domain, the
  Work may be freely reproduced, distributed, transmitted, used,
  modified, built upon, or otherwise exploited by anyone for any
  purpose, commercial or non-commercial, and in any way, including
  by methods that have not yet been invented or conceived.

  $Log: CleanInBackground.java,v $

 If a span has an id, move it to it's parent node if it's the first child
 and the parent doesn't already have an id. If the text style is italic,
 remove it and add an <i>. If it has a font-weight of bold, remove it and add
 a <b>. If it is now empty, remove it.

  Revision 1.10  2014/07/29 22:11:43  lpassey
  Use alternate File constructor to ensure parent/child relationship.

  Revision 1.9  2013/07/03 22:03:22  lpassey
  1. Catch FileNotFoundException from getManifestedDocument.
  2. Improve error handling when .opf file has not yet been created.

*/

package com.passkeysoft.opfedit.business;

import com.passkeysoft.opfedit.ui.swing.controller.MonitoredWorker;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.ui.swing.model.OPFFileModel;
import com.passkeysoft.opfedit.ui.swing.model.SpineModel;
import com.passkeysoft.XHTMLDocument;
import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.staticutil.XMLUtil;
import com.passkeysoft.opfedit.ui.swing.controller.LogAndShowError;
import com.passkeysoft.opfedit.ui.swing.controller.EPubEditor;

public class CleanInBackground extends MonitoredWorker<Void,Void>
{
    private File _userCssFile;
    private OPFFileModel _opfData;
    private EPubModel _ePub;
    private DocumentBuilder _db;

    public CleanInBackground( EPubModel ePub, File userCss, DocumentBuilder db )
    {
         _userCssFile = userCss;
         _ePub = ePub;
         _opfData = ePub.getOpfData();
         _db = db;
    }

    @Override
    protected Void doInBackground() // throws Exception
    {
        // Remove the adobe page-template from the manifest
        NodeList items = _opfData.getManifest().getManifestedItems();
        for (int i = 0; i < items.getLength(); i++)
        {
            String mediaType = ((Element) items.item( i )).getAttribute( "media-type" );
            if (mediaType.toLowerCase().contains( "application/vnd.adobe" ))
            {
                Node parent = items.item( i ).getParentNode();
                parent.removeChild( items.item( i ) );
            }
        }
        cleanFiles( _userCssFile );
        return null;
    }


    protected void complete() { /* No completion activity for this worker */ }


    /**
     * Cleans up all the XHTML documents in a publication,
     * and adds a user defined CSS file to the manifest and archive.
     */
    private void cleanFiles( File userCssFile )
    {
        if (null == _opfData.getSpine())
            return;

        String xslURL, xslPath, publisherXSLT = null;
        URL defaultXSLT = null;

        // Look for the default file first in the user's path, if any. If this fails it
        // is a non-fatal error; just continue with empty (default) properties
        xslPath = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS ).get( EPubEditor.PREFS_PATHS_XSLT, "xslt" ); // xslPath will never be null

        // load up the default XSLT script which will be run first.
        xslURL = xslPath + "/"  + "default.xsl";

        // first look for the default script in the directory of the user's choice
        if (new File(xslURL).exists()) try
        {
            defaultXSLT = new URL( "file", "localhost", xslURL );
        }
        catch( MalformedURLException ignore )
        {
        }
        if (null == defaultXSLT && !xslPath.equals( "xslt" )) try
        {
            // no default.xsl file in the users path, is there one in the default path?
            if (new File( "xslt/default.xsl").exists())
                defaultXSLT = new URL( "file", "localhost", "xslt/default.xsl" );
        }
        catch( MalformedURLException ignore )
        {
        }
        if (null == defaultXSLT)
        {
            // no default xsl file in the file system, use the default one in the jar.
            defaultXSLT = getClass().getClassLoader().getResource( xslURL );
        }

        // find the publisher-specific XSLT script to clean this document.
        String publisher = _opfData.getMetadata().getProperty( "publisher" );
        if (publisher == null || 0 == publisher.length())
        {
            // No declared publisher; use default .xsl file,
            // and tell the user what we're doing.
            LogAndShowError
                    .logAndShowNoEx( "No publisher was identified for this publication"
                            + ";\ndefault cleaning only will be performed." );
        }
        else
        {
            String[] parts = publisher.split( "[\\t \\r\\n]" );

            xslURL = xslPath + "/" + parts[0] + ".xsl";

            // first look for the publisher script in the directory of the user's choice
            if (new File(xslURL).exists())
            {
                publisherXSLT = xslURL;
            }
            if (null == publisherXSLT && !xslPath.equals( "xslt" ))
            {
                xslURL = "xslt" + "/" + parts[0] + ".xsl";
                if (new File( xslURL).exists())
                    publisherXSLT = xslURL;
            }
            // I don't store any xsl file in the jar except the default, so if
            // I can't find a publisher .xsl file in the FS, it ain't there.
            if (null == publisherXSLT)
            {
                // No publisher specific xsl files anywhere; use the default xsl
                // and tell the user what we're doing. If the publisher is "default"
                // we've already checked for those files; not need to do it again.
                LogAndShowError
                        .logAndShowNoEx( "No XML transformation file found for the publisher "
                                + publisher + ";\ndefault cleaning only will be performed." );
            }
        }
        if (null == publisherXSLT && null == defaultXSLT)
        {
            // Can't find any kind of xsl file: this is a fatal error.
            LogAndShowError
                    .logAndShowNoEx( "Unable to find any XML stylesheet to perform cleaning;\n"
                            + "No cleaning will be performed." );
        }
        else
        {
            SpineModel.SpineHTMLIterator iter = _opfData.getSpine().new SpineHTMLIterator();

            Integer progress = 0;
            update( null, progress );
            while (iter.hasNext())
            {
                if (isCancelled())
                    break;
                String id = iter.next();
                File htmlFile =
                        new File( _ePub.getOpfFolder(), iter.href );
//                LogAndShowError.logException( "Cleaning " + htmlFile.getAbsolutePath(), null );
                try
                {
                    Document doc = _ePub.getManifestedDocument( id );
                    if (null != doc)
                    {
                        // Check the input encoding as specified in the meta element.
                        // If it's not utf-8 or ASCII, change it.
                        Element adobe = null, equiv = null;
                        NodeList metas = doc.getElementsByTagName( "meta" );
                        for (int i = 0; i < metas.getLength(); i++)
                        {
                            Element meta = (Element) metas.item( i );
                            String content = meta.getAttribute( "http-equiv" );
                            // int pos = content.indexOf( "" );
                            if (0 < content.length())
                            {
                                // this is the http-equiv node, be sure it
                                // indicates that all content is utf-8 encoded.
                                meta.setAttribute( "content", "text/html; charset=utf-8" );
                                meta.setAttribute( "http-equiv", "content-type" );
                                equiv = meta;
                            }
                            content = meta.getAttribute( "name" );
                            if (content.startsWith( "Adept." ))
                            {
                                // Adobe garbage, just get rid of it (later)
                                adobe = meta;
                            }
                        }
                        if (null != adobe)
                        {
                            Node parent = adobe.getParentNode();
                            parent.removeChild( adobe );
                        }
                        if (null == equiv)
                        {
                            // there was not http-equiv element, add one now.
                            equiv = doc.createElement( "meta" );
                            equiv.setAttribute( "content", "text/html; charset=utf-8" );
                            equiv.setAttribute( "http-equiv", "content-type" );
                            metas = doc.getElementsByTagName( "head" );
                            if (0 < metas.getLength())
                                metas.item( 0 ).insertBefore( equiv, metas.item( 0 ).getFirstChild() );
                        }
                        metas = doc.getElementsByTagName( "html" );
                        if (0 < metas.getLength())
                        {
                            adobe = (Element) metas.item( 0 );
                            adobe.removeAttribute( "xml:lang" );    // confuses my XSL script
                            adobe.setAttribute( "xmlns:epub", "http://www.idpf.org/2007/ops" );

                        }
                        // Transform then pretty print the html document.
                        if (null != defaultXSLT)
                        {
                            try
                            {
                            // First run default transformation.
                            doc = XMLUtil.xFormFile( _db, doc, defaultXSLT.openStream() );
                            }
                            catch( TransformerConfigurationException e )
                            {
                                LogAndShowError.logException( defaultXSLT.getPath(), e );
                            }

                            if (null != userCssFile)
                            {
                                String relativePathToUserCss =
                                        FileUtil.getPathRelativeToBase( userCssFile, htmlFile );
                                insertUserCss( doc, relativePathToUserCss );
                            }
                        }
                        if (null != publisherXSLT) try
                        {
                            doc = XMLUtil.xFormFile( _db, doc, new FileInputStream( publisherXSLT ) );
                        }
                        catch( TransformerConfigurationException e )
                        {
                            LogAndShowError.logException( xslURL + "  " + publisherXSLT, e );
                        }
                        XHTMLDocument htmlDoc = new XHTMLDocument( doc, htmlFile.getName() );
                        htmlDoc.tidy();
                        FileUtil.saveXHTMLDocumentWithBak( htmlDoc, htmlFile );
                    }
                }
                catch( FileNotFoundException ex )
                {
                    // Couldn't find the requested file. Log an error, then continue.
                    LogAndShowError.logAndShowEx(
                            "Unable to find the manifested file: "
                                    + _opfData.getManifest().getHrefById( id ) + "\n"
                                    + ex.getLocalizedMessage(), ex );
                }
                catch( IOException ex )
                {
                    // Unexpected io error. Log an error, then continue.
                    LogAndShowError.logAndShowEx(
                            "Unspecified IO error while reading or parsing the file:  \n"
                                    + _opfData.getManifest().getHrefById( id ) + "\n"
                                    + ex.getLocalizedMessage(), ex );
                }
                catch( TransformerException ex )
                {
                    LogAndShowError.logAndShowEx(
                        "A serious error occured while transforming "
                            + _opfData.getManifest().getHrefById( id )
                            + "\nConsult the error log for more details."
                            + ex.getLocalizedMessage(), ex );
                }
                catch( TransformerFactoryConfigurationError | Exception e )
                {
                    LogAndShowError.logException( null, e );
                }
                finally
                {
                    progress++;
                    update( null, progress );
                }
            }
        }
    }

    private static void insertUserCss( Document doc, String relativePathToUserCss )
    {
        NodeList elements = doc.getElementsByTagName( "link" );
        for (int i = 0; i < elements.getLength(); i++)
        {
            // remove the user css link if it exists.
            String href =
                    ((Element) elements.item( i ))
                            .getAttribute( "href" );
            if (relativePathToUserCss.equalsIgnoreCase( href ))
            {
                Node parent = ( elements.item( i )).getParentNode();
                parent.removeChild( (elements.item( i )) );
                break;
            }
        }
        elements = doc.getElementsByTagName( "head" );
        if (0 < elements.getLength())
        {
            // Check to see if the user's style sheet is already present
            Element head = (Element) elements.item( 0 );
            elements = head.getElementsByTagName( "link" );
            Element link;
            int i;
            for (i = 0; i < elements.getLength(); i++)
            {
                link = (Element) elements.item( i );
                String cssHref = link.getAttribute( "href" );
                if (cssHref.equalsIgnoreCase( relativePathToUserCss ))
                    break;
            }
            if (elements.getLength() == i)
            {
                // The user's css was not found in the <head>; add
                // it as the last stylesheet in the head
                link = doc.createElement( "link" );
                link.setAttribute( "href", relativePathToUserCss );
                link.setAttribute( "rel", "stylesheet" );
                link.setAttribute( "type", "text/css" );
                head.insertBefore( link, null );
            }
            // TODO: save the opf file with the new manifested usercss
        }
    }
}
