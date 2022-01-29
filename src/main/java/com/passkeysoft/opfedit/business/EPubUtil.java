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
  
  $Log: EPubUtil.java,v $
  Revision 1.10  2014/07/29 22:09:09  lpassey
  Add ability to insert an element as a step-parent (all existing children become children
   of the new inserted element, and the new element become the sole child of the existing
   parent).

  Revision 1.9  2013/07/03 22:09:21  lpassey
  1. Catch FileNotFoundException from getManifestedDocument.
  2. Improve error handling when .opf file has not yet been created.
  3. Implement requested functionality to add id attributes to header elements when
   building a Table of Contents if the header does not already possess one.

  Revision 1.8  2013/06/26 17:56:38  lpassey
  1. Do not add <h1> headers to the generated Table of Contents.
  2. Add Public Domain "licence"


*/


package com.passkeysoft.opfedit.business;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.passkeysoft.DOMIterator;
import com.passkeysoft.opfedit.TempFileIOException;
import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.datamodels.GuideModel;
import com.passkeysoft.opfedit.datamodels.OPFFileModel;

import com.passkeysoft.opfedit.datamodels.SpineModel;
import com.passkeysoft.XHTMLDocument;
import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.staticutil.XMLUtil;
import com.passkeysoft.opfedit.ui.InsertTagDialog;
import com.passkeysoft.opfedit.ui.LogAndShowError;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;

/**
 * @author W. Lee Passey
 *
 * This class implements functions which may be considered to be part of the EPubModel class.
 * Therefore, the first parameter of virtually every public method will be the equivalent of "this".
 */
public class EPubUtil
{
    private static final String tocSkeleton
        = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
        + "   \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
        + "<html lang=\"en\">\n"
        + "  <head>\n"
        + "    <title>Table of Contents</title>"
        + "    <meta content=\"text/html; charset=utf-8\" http-equiv=\"content-type\" />\n"
        + "  </head>\n"
        + "  <body>\n"
        + "    <nav class=\"toc\" epub:type=\"toc\">\n"
        + "      <h3>Table of Contents</h3>\n"
        + "    </nav>"
        + "  </body>"
        + "</html>";


    /**
     * Builds an XHTML formatted Table of Contents for the publication as a whole using 
     * ordered lists (<ol>). Each header becomes a list item, and each time a header level
     * changes a new, embedded ordered list is started. When the entire list is successfully
     * created, it is saved as an XHTML file and added to the manifest.
     *
     * TODO: The new TOC is not added to the guide or the spine. Should it be?
     *
     * @param ePubData "this"
     * @param _userCssFile The user's specified css file. May be null.
     * @return An XML Document using the XHTML vocabulary which encodes a new Table of Contents
     *      or null if the process failed for any reason.
     */
    public static Document buildTOC( EPubModel ePubData, File _userCssFile )
    {
        if (null == ePubData.getOpfData().getSpine())
            return null;

        String id = "";
        // get the toc name first, so we can exclude it from the build.
        // First choice is whatever file name is given as the in the guide section.
        GuideModel guide = ePubData.getOpfData().getGuide();
        String href = guide.getHrefByType( "toc" );
        if (0 == href.length())
        {
            // Nothing there. Pick a file with the same name as the .opf file
            // + "_toc.html". If it already exists, it will be backed up before
            // being over written when the xhtml file is written.
            href = ((null == ePubData.getOpfFile()) 
                    ? "" : FileUtil.getFileName( ePubData.getOpfFile() )) + "_toc.html";

            ePubData.getOpfData().getGuide().addGuideReference( "toc", "Table of Contents", href );
        }
        File tocFile = new File( ePubData.getOpfFolder(), href );
        try
        {
            Document toc;
            try
            {
                toc = EPubModel.db.parse( new ByteArrayInputStream( tocSkeleton.getBytes() ));
                Element docEl = toc.getDocumentElement();
                toc.renameNode( docEl,"http://www.w3.org/1999/xhtml", "html" );
                docEl.setAttribute( "xmlns:epub", "http://www.idpf.org/2007/ops" );
            }
            catch( IOException e )
            {
                // Unable to parse the static TOC Skeleton from this file. This is a fatal
                // error. Catch any IOException here, so it will not mask other IOException
                // that may be thrown hereafter.
                e.printStackTrace();
                return null;
            }
            if (null != _userCssFile)
            {
                String relativePathToUserCss =
                        FileUtil.getPathRelativeToBase( _userCssFile, tocFile );
                NodeList elements = toc.getElementsByTagName( "head" );
                if (0 < elements.getLength())
                {
                    Element head = (Element) elements.item( 0 );
                    // add the user's stylesheet as the last stylesheet in the head
                    Element link = toc.createElement( "link" );
                    link.setAttribute( "href", relativePathToUserCss );
                    link.setAttribute( "rel", "stylesheet" );
                    link.setAttribute( "type", "text/css" );
                    head.insertBefore( link, null );
                }
            }
            XPathFactory fac = XPathFactory.newInstance();
            XPath xpath = fac.newXPath();
            XPathExpression exp = xpath.compile( "//nav" );
            Element lastLi, tocList, tocNode = (Element) exp.evaluate( toc, XPathConstants.NODE );
            lastLi = tocList = null;
            SpineModel.SpineHTMLIterator iter = ePubData.getOpfData().getSpine().new SpineHTMLIterator();
            // Integer progress = new Integer( 0 );
            // observer.update( this, progress );
            while (iter.hasNext())
            {
                id = iter.next();
                File htmlFile = new File( ePubData.getOpfFolder(), iter.href );
                // Don't consider the table of contents itself when building a table of contents
                try
                {
                    if (tocFile.getCanonicalPath().equalsIgnoreCase( htmlFile.getCanonicalPath() ))
                        continue;
                }
                catch( IOException ignore )
                {
                    continue;
                }
                Document doc = ePubData.getManifestedDocument( id );
                if (null != doc)
                {
                    int generatedIdCounter = 0;
                    // Do an inorder traversal of the tree elements. If a node
                    // is a header, create a list item to represent it, and add
                    // that to the tocList. If the list class does not match, the
                    // element name, create a new OL to hold it if it is more
                    // granular, or move to the parent if it is not.
                    NodeList nodelist =
                            doc.getDocumentElement().getElementsByTagName( "body" );
                    DOMIterator domIter = new DOMIterator( nodelist.item( 0 ) );
                    while (domIter.hasNext())
                    {
                        Node node = domIter.next();
                        if (Node.ELEMENT_NODE == node.getNodeType())
                        {
                            String name = node.getNodeName();
                            if (   name.toLowerCase().charAt( 0 ) == 'h'
                                && Character.isDigit( name.charAt( 1 ) )
                                && name.charAt(  1 ) != '1')
                            {
                                Element a = toc.createElement( "a" );
                                String attr = ((Element) node).getAttribute( "id" );
                                if (0 == attr.length())
                                {
                                    // No id attribute. If my previous non-white
                                    // sibling has an id, use that one.
                                    for (Node sib = node.getPreviousSibling(); 
                                         null != sib; 
                                         sib = sib.getPreviousSibling())
                                    {
                                        if (Node.TEXT_NODE == sib.getNodeType())
                                        {
                                            if (!XHTMLDocument._isWhitespace( sib, true ))
                                                break;
                                        }
                                        else if (Node.ELEMENT_NODE != sib.getNodeType())
                                            break;
                                        else
                                        {
                                            attr = ((Element) sib).getAttribute( "id" );
                                            if (   0 != attr.length() 
                                                || XHTMLDocument.htmlIsBlock( sib.getNodeName() ))
                                                break;
                                        }
                                    }
                                }
//                                if (0 == attr.length())
                                {
                                    // If the header has an anchor child,
                                    // adopt that id, otherwise create a temporary id.
                                    DOMIterator hiter = new DOMIterator( node );
                                    while (hiter.hasNext())
                                    {
                                        Node child = hiter.next();
                                        if (child.getNodeName().toLowerCase().equals( "a" ))
                                        {
                                            // if the anchor node is the first child, and the <a> tag is empty,
                                            // or if the header has no id value
                                            // use the anchor id.
                                            Node firstChild = node.getFirstChild();
                                            if (   (   child.equals( firstChild )
                                                    && !child.hasChildNodes())
                                                // or node has no id.
                                                || ((Element) node).getAttribute( "id" ).isEmpty()
                                            )
                                            {
                                                attr = ((Element) child).getAttribute( "id" );
//                                                if (0 < attr.length())
//                                                {
//                                                    ((Element) child).removeAttribute( "id" );
//                                                }
//                                                if (!child.hasAttributes())
//                                                {
//                                                    // No remaining attributes, remove the whole node.
//                                                    Node n = child.getFirstChild();
//                                                    while (null != n)
//                                                    {
//
//                                                        a.appendChild( toc.adoptNode( child.removeChild( n ) ) );
//                                                        n = child.getFirstChild();
//                                                    }
//                                                    Node parent = child.getParentNode();
//                                                    parent.removeChild( child );
//                                                }
                                                break;
                                            }
                                        }
                                    }
                                    if (0 == attr.length())
                                    {
                                        // create an id for this header
                                        attr = String.format( "%s_%02d", name, generatedIdCounter++ );
                                        ((Element) node).setAttribute( "id", attr );
                                    }
                                }
                                Element li = toc.createElement( "li" );
                                String idref = FileUtil.getPathRelativeToBase( htmlFile, tocFile ) + "#" + attr;
                                a.setAttribute( "href",  idref );
//                                a.setAttribute( "href", htmlFile.getName() + "#" + attr );
                                for (Node child = node.getFirstChild(); null != child; child =
                                        child.getNextSibling())
                                {
                                    // do not copy empty anchor nodes.
                                    if (   !"a".equalsIgnoreCase( child.getNodeName() )
                                        || child.hasChildNodes() )
                                    {
                                        a.appendChild( toc.importNode( child, true ) );
                                    }
                                }
                                li.appendChild( a );
                                if (null == tocList)
                                {
                                    tocList = toc.createElement( "ol" );
                                    tocList.setAttribute( "class", name );
                                    tocNode.appendChild( tocList );
                                }
                                else
                                {
                                    attr = tocList.getAttribute( "class" );
                                    int relative = name.compareToIgnoreCase( attr );
                                    if (0 < relative)
                                    {
                                        // need to create new ol, attached to the previous li
                                        Element ol = toc.createElement( "ol" );
                                        ol.setAttribute( "class", name );
                                        lastLi.appendChild( ol );
                                        tocList = ol;
                                    }
                                    else
                                        while (0 > relative)
                                        {
                                            // need to back up
                                            tocList =
                                                    (Element) tocList.getParentNode()
                                                            .getParentNode();
                                            if (!tocList.getNodeName().toLowerCase()
                                                    .equals( "ol" ))
                                            {
                                                // We've backed up past the list owner; start over.
                                                tocList = toc.createElement( "ol" );
                                                tocList.setAttribute( "class", name );
                                                tocNode.appendChild( tocList );
                                            }
                                            attr = tocList.getAttribute( "class" );
                                            relative = name.compareToIgnoreCase( attr );
                                        }
                                }
                                tocList.appendChild( li );
                                lastLi = li;
                            }
                        }
                    }
                    // TODO: SFID: 3426971 -- if identifiers have been added to
                    // header elements, the file needs to be saved again.
                    if (0 < generatedIdCounter) try
                    {
                        FileUtil.saveXHTMLDocumentWithBak( new XHTMLDocument( doc, htmlFile.getPath() ), htmlFile );

                    }
                    catch( FileNotFoundException ex )
                    {
                        LogAndShowError.logAndShowEx( "Unable to write to file: "
                                + tocFile + "\nPermission denied", ex );
                    }
                    catch( TempFileIOException tempFileFailure )
                    {
                        LogAndShowError.logAndShowEx( "Unable to create temporary file for: "
                                + tocFile.getAbsolutePath(), tempFileFailure );
                    }
                    catch( IOException e )
                    {
                         e.printStackTrace();
                    }

                }
            }
            // all files have been examined, write out the new TOC
            try
            {
                FileUtil.saveXHTMLDocumentWithBak( new XHTMLDocument( toc, tocFile.getPath() ), tocFile );
    
                // If the new TOC does not appear in the manifest, add it now.
                if (!ePubData.getOpfData().getManifest().isManifested( href ))
                    ePubData.getOpfData().getManifest().addManifestItem( "toc", href,
                            "application/xhtml+xml" );
                return toc;
            }
            catch( FileNotFoundException ex )
            {
                LogAndShowError.logAndShowEx( "Unable to write to file: "
                        + tocFile + "\nPermission denied", ex );
            }
            catch( TempFileIOException tempFileFailure )
            {
                LogAndShowError.logAndShowEx( "Unable to create temporary file for: "
                        + tocFile.getAbsolutePath(), tempFileFailure );
            }
            catch( IOException e )
            {
                e.printStackTrace();
            }
        }
        // Theoretically, since the DOM is a static string in this file, these
        // exceptions should never be thrown. No need to alert the end user.
        catch( DOMException | XPathExpressionException | SAXException e )
        {
            e.printStackTrace();
        }
        catch( FileNotFoundException ex )
        {
            // A manifest file does not exist; we can't create a table of contents
            LogAndShowError.logAndShowEx(
                    "Unable to find the manifested file: \""
                  + ePubData.getOpfData().getManifest().getHrefById( id ) + "\";\n"
                  + "No Table of Contents can be created.\n"
                  + "Restore the manifested file, or remove the reference from the content table."
                  /* + ex.getLocalizedMessage() */, ex );
        }
        return null;
    }

    
    static final String daisyNS = "http://www.daisy.org/z3986/2005/ncx/";
    
    private static int createNavPoint( Document ncx, Element ncxEl, DOMIterator listIter, 
                                       String tocHref, String classs, int playOrder, int level )
    {
        if (ncxEl == null)
            return playOrder;
        int po = playOrder, count = 0;
        NodeList contents = ncx.getElementsByTagName( "content" );
        while (listIter.hasNext())
        {
            Node next, child = listIter.next();
            if (child.getNodeName().equalsIgnoreCase("li"))
            {
                Element navPoint = null;
                DOMSubIterator liIter = new DOMSubIterator( listIter );
                while (liIter.hasNext())
                {
                    next = liIter.next();
                    
                    if (Node.ELEMENT_NODE == next.getNodeType())
                    {
                        if (next.getNodeName().equalsIgnoreCase( "a" ))
                        {
                            String href = ((Element) next ).getAttribute( "href" );
                            // Only include anchors that point to something.
                            if (0 < href.length())
                            {
                                if (href.startsWith( "#" ))
                                    href = tocHref + href;
                                int newPlay = playOrder;
                                for (int i = 0; i < contents.getLength(); i++)
                                {
                                    Element content = (Element) contents.item( i );
                                    String src = content.getAttribute( "src" );
                                    if (href.equalsIgnoreCase( src ))
                                    {
                                        // use this play order.
                                        navPoint = (Element) content.getParentNode();
                                        src = navPoint.getAttribute( "playOrder" );
                                        newPlay = Integer.parseInt( src );
                                        break;
                                    }
                                }
                                navPoint = ncx.createElement( "navPoint" );
                                ncxEl.appendChild( navPoint );
                                navPoint.setAttribute( "id", "po_" + po + "_" + level + "_" + count++ );
                                if (null != classs && 0 < classs.length())
                                    navPoint.setAttribute( "class", classs );
                                navPoint.setAttribute( "playOrder", "" + newPlay );
                                if (newPlay == playOrder)
                                    playOrder++;
                                Element label = ncx.createElement( "navLabel" );
                                navPoint.appendChild( label );
                                
                                Element text = ncx.createElement( "text" );
                                text.appendChild( ncx.createTextNode( next.getTextContent().trim() ));
                                label.appendChild( text );
                                
                                label = ncx.createElement( "content" );
                                navPoint.appendChild( label );
                                label.setAttribute( "src", href );
                            }
                        }
                        else if (   next.getNodeName().equalsIgnoreCase( "ol" )
                                 || next.getNodeName().equalsIgnoreCase( "ul" ))
                        {
                            playOrder = createNavPoint( ncx, navPoint, liIter, tocHref, 
                                    ((Element)next).getAttribute( "class" ), playOrder, level + 1 );
                        }
                    }
                }
            }
        }
        return playOrder;
    }


    /**
     * Builds a Daisy Navigation control file for the publication as a whole.
     * 
     * The <docTitle> and <docAuthor> values, as well as the <meta dtb:uid> value will be derived
     * from the opf data. The <navMap> element will be built from the documents TOC, which must be
     * nested lists. If a toc does not exist (as recorded in the "guide" element) one will be
     * generated before NCX generation occurs.
     * 
     * Every "tour" included in the option "tours" element of the .opf file will be added to the
     * .ncx file as part of a <navList> element.
     * 
     * This method will ensure that the .ncx file is included in the manifest, and referenced as an
     * attribute on the opf <spine> element.
     * 
     * @param ePubData
     *            The model containing all the metadata for this ePub document.
     * @param userCss 
     *            The user's specified css file. Used to create the Table of Contents. May be null.
     * @param createPageList
     *            if true, create a page list in the .ncx file according to the existence of
     *            anchors having an id and a class of "page-break." 
     *            a template that can be used to build a page list. If this parameter is not null, a
     *            reference to each node that loosely matches this node, and which has an 'id'
     *            attribute, will be included in the <pageList> node.
     */
    public static void buildNCX( EPubModel ePubData, File userCss, boolean createPageList )
    {
        OPFFileModel _opfData = ePubData.getOpfData();
        
        if (null == _opfData.getSpine())
            return;     // no spine, no TOC, no NCX.
        Document toc = null;
        // If the TOC has not yet been built (check the guide) build it first.
        String tocHref = _opfData.getGuide().getHrefByType( "toc" );
        if (tocHref != null) try
        {
            // the file name in the guide is relative to the .opf file, not the epub root path...
            toc = EPubModel.db.parse( new File( ePubData.getOpfFolder(), tocHref ));
        }
        // If I can't parse the existing TOC, I'll just build a new one.
        catch( SAXException | IOException e ) { e.printStackTrace();  }
        if (null == toc)
        {
           toc = buildTOC( ePubData, userCss );
        }
        if (null == toc)
            return;     // I can't get or build a TOC, so I can't construct the NCX.
        Document ncx;
        String ncxHref = _opfData.getManifest().getHrefById( _opfData.getSpine().getNCXId() );
        if (null == ncxHref)
        {
            tocHref = _opfData.getGuide().getHrefByType( "toc" );  // This had better succeed now!
            ncxHref = (((null == ePubData.getOpfFile()) 
                    ? FileUtil.getFileName( new File( tocHref ) ) 
                    : FileUtil.getFileName( ePubData.getOpfFile() ))) + ".ncx";
        }
        
        // create a new ncx document
        ncx = EPubModel.db.newDocument();
        Element top = ncx.createElement( "ncx" );
        top.setAttribute( "xmlns", "http://www.daisy.org/z3986/2005/ncx/" );
        top.setAttribute( "version", "2005-1" );
        top.setAttribute( "xml:lang", "en-US" );
        ncx.appendChild( top );
        
        // add optional ncx metadata, derived from the .opf metadata
        Element el = ncx.createElement( "head" );
        top.appendChild( el );
        
        Element child = ncx.createElement( "meta" );
        child.setAttribute( "name", "dtb:uid" );
        child.setAttribute( "content", _opfData.getUID() );
        el.appendChild( child );
        
        el = ncx.createElement( "docTitle" );
        top.appendChild( el );
        child = ncx.createElement( "text" );
        el.appendChild( child );
        child.appendChild( ncx.createTextNode( _opfData.getMetadata().getProperty( "title" )));
        
        ArrayList<String> properties = _opfData.getMetadata().getAllProperties( "creator" );
        for (String property : properties)
        {
            el = ncx.createElement( "docAuthor" );
            top.appendChild( el );
            child = ncx.createElement( "text" );
            el.appendChild( child );
            child.appendChild( ncx.createTextNode( property ) );
        }
        
        int playOrder = 1;      // For inexplicable reasons, the play order must be sequential, must
                                // begin at one, must not have gaps, and must differ for the page list
                                // and chapter list. To avoid collisions, build the page list first.
        Element pageList = null;
        if (createPageList)
        {
            pageList = ncx.createElement( "pageList" );
            Element template = ncx.createElement( "a" );
            template.setAttribute( "class", "page-break" );
            top.appendChild( pageList );
            SpineModel.SpineHTMLIterator spineIter =
                    ePubData.getOpfData().getSpine().new SpineHTMLIterator();
            while (spineIter.hasNext())
            {
                String id = spineIter.next();
                try
                {
                    Document doc = ePubData.getManifestedDocument( id );
                    if (null != doc)
                    {
                        NodeList nodelist =
                                doc.getDocumentElement().getElementsByTagName( "body" );
                        DOMIterator domIter = new DOMIterator( nodelist.item( 0 ) );
                        while (domIter.hasNext())
                        {
                            Element anchor = domIter.nextElement();
                            if (null == anchor)
                                break;
                            if (!XMLUtil.nodesMatch( template, anchor, true ))
                                continue;
    
                            // if we got to this point, we have a page-break anchor.
                            el = ncx.createElement( "pageTarget" );
                            el.setAttribute( "type", "normal" );
                            el.setAttribute( "id", anchor.getAttribute( "id" ) );
                            el.setAttribute( "playOrder", "" + playOrder++ );
                            pageList.appendChild( el );
                            Element label = ncx.createElement( "navLabel" );
                            el.appendChild( label );
    
                            Element text = ncx.createElement( "text" );
                            text.appendChild( ncx.createTextNode( anchor.getAttribute( "id" )
                                    .trim() ) );
                            label.appendChild( text );
    
                            label = ncx.createElement( "content" );
                            el.appendChild( label );
    
                            label.setAttribute( "src", ePubData.getOpfData().getManifest()
                                    .getHrefById( id )
                                    + "#" + anchor.getAttribute( "id" ) );
                        }
                    }
                }
                catch( FileNotFoundException ignore ) { }
            }
            if (!pageList.hasChildNodes())
            {
                top.removeChild( pageList );
                pageList = null;
            }
        }
        // build a navMap based on the document's TOC. If a TOC entry matches a page list 
        // entry, set the playOrder to match the page list entry. The new navMap must
        // appear before the page list in the .ncx file.
        el = ncx.createElement( "navMap" );
        top.insertBefore( el, pageList );
        
        // find the first list element in the toc.
        Element list;
        DOMIterator iter = new DOMIterator( toc.getDocumentElement() );
        while (iter.hasNext())
        {
            list = iter.nextElement();
            if (null != list)
            if (   list.getNodeName().equalsIgnoreCase( "ol" )
                || list.getNodeName().equalsIgnoreCase( "ul" )
               )
            {
                playOrder = createNavPoint( ncx, el, iter, tocHref, 
                        list.getAttribute( "class" ), playOrder, 0 );
            }
        }
        
        // Just like the TOC, save the NCX file and add it to the manifest.
        FileOutputStream fos = null;
        try
        {
            StreamSource xslStream = new StreamSource( new ByteArrayInputStream( ncxXsl.getBytes() ));
            Transformer t = TransformerFactory.newInstance().newTransformer( xslStream );
            t.setOutputProperty( OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");
            t.setOutputProperty( OutputKeys.STANDALONE, "yes" );
            t.setOutputProperty( OutputKeys.INDENT, "yes" );
            DOMSource xmlSource = new DOMSource( ncx );
            StreamResult output;
            fos =  new FileOutputStream( new File( ePubData.getEpubRootPath(), ncxHref ));
            output = new StreamResult( fos );
            t.transform( xmlSource, output );

            // add the NCX attribute to the spine element if it's not there.
            String ncxid = _opfData.getSpine().getNCXId();
            if (0 == ncxid.length())
            {
                ncxid = "ncx";
                _opfData.getSpine().setNCXId( ncxid );
            }
            
            // If the NCX file does not appear in the manifest, add it now.
            if (!_opfData.getManifest().isManifested( ncxHref ))
                _opfData.getManifest().addManifestItem( ncxid, ncxHref, "application/x-dtbncx+xml" );
        }
        catch( FileNotFoundException ex )
        {
            // the specified .ncx file exists but is a directory rather than a regular file,
            // does not exist but cannot be created, or cannot be opened for any other reason
            LogAndShowError.logAndShowEx( "Unable to create the NCX file " + ncxHref 
                    + "\nIt does not exist but cannot be created, it is a directory rather than a "
                    + "regular file, or you have insufficient permissions to create the file.", ex );
        }
        catch( TransformerFactoryConfigurationError | TransformerConfigurationException e )
        {
            // The implementation is not available or cannot be instantiated.
            LogAndShowError.logAndShowEx( "Unable to create a Transformer instance to save the new NCX file.", e );
        }
        // It is not possible to create a Transformer instance.
        catch( TransformerException e )
        {
            // An unrecoverable error occurred during the course of the transformation.
            LogAndShowError.logAndShowEx( "A fatal error occured while saving the new NCX file", e );
        }
        finally
        {
            if (null != fos)
                try
                {
                    fos.close();
                }
                catch( IOException ignore )
                {
                }
        }
    }


    static final String coverHTML
    = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n"
    + "<head>\n"
    + "<title>%s - Cover Image</title>\n"
    + "</head>\n"
    + "<body>\n"
    + "  <div style=\"text-align:center\">\n"
    + "      <img src=\"%s\" alt=\"Cover Image\" style=\"max-width: 99vw; max-height: 99vh\"/>\n"
    + "  </div>\n"
    + "</body>\n"
    + "</html>\n"
      ;


    public static boolean buildCover( EPubModel ePubData )
    {
        OPFFileModel _opfData = ePubData.getOpfData();
        // Get the cover image href from the guide. If there's no image, I can't build
        // an HTML wrapper for it, so return failure.
        String imageHref = _opfData.getGuide().getHrefByType( "cover" );
        if (null == imageHref || 0 == imageHref.length())
        {
            LogAndShowError.logAndShowNoEx( "A cover image must be specified in the guide,\n" +
            		"with the type of \"cover\"." );
            return false;
        }
        
        // Cover file name should be the same as the .opf file + "_cover".
        String fileName = ((null == ePubData.getOpfFile()) 
                ? "" : FileUtil.getFileName( ePubData.getOpfFile() )) + "_cover.html";
        File coverFile = new File( ePubData.getOpfFolder(), fileName );
        int count = 0;
        while (coverFile.exists())
        {
            fileName = String.format( "%s_%02d.html", FileUtil.getFileName( coverFile ), count++ );
            coverFile = new File( ePubData.getEpubRootPath(), fileName );
        }
        try
        {
            FileOutputStream cover = new FileOutputStream( coverFile );
            String output = String.format( coverHTML, _opfData.getMetadata().getProperty( "title" ), 
                    imageHref );
            cover.write( output.getBytes(), 0, output.getBytes().length );
            cover.close();
            
            // Now add this file to the manifest, and also as the first entry in the spine.
            String idFirst, id = _opfData.getManifest().addManifestItem( null, 
                                 ePubData.getPathRelativeToOpf( coverFile ), "application/xhtml+xml" );
            SpineModel spine =  _opfData.getSpine();
            idFirst = (String) spine.getValueAt( 0, 0 );
            Element refChild = spine.getItemNodeByIdref( idFirst );
            spine.addRow( id, refChild );
            return true;
        }
        catch( FileNotFoundException e )
        {
            // Thrown if the file cannot be opened for writing -
            // permissions problem or already opened.
            LogAndShowError.logAndShowEx( "Unable to open " + coverFile + ". Do you have\n" +
                    "\"write\" permission to the OPF directory?", e );
        }
        catch( IOException e )
        {
            LogAndShowError.logAndShowEx( "An unspecified IO error occured while attempting to write to "
                                         + coverFile + ".", e );
        }
        return false;
    }

    
    public static int replaceTags( EPubModel ePubData, Element findTag, Element replaceTag ) 
            throws IOException
    {
        // Traverse the entire spine looking at html files. Find nodes matching
        // the requested node and change them. Return a count of the number of
        // replacements made.
        int count = 0;
        // watch out for exact replacements, because they can cause endless loops
        if (null == replaceTag || !XMLUtil.nodesMatch( findTag, replaceTag, false ))
        {
        
        SpineModel.SpineHTMLIterator iter = ePubData.getOpfData().getSpine().new SpineHTMLIterator();
        while (iter.hasNext())
        {
            String id = iter.next();
            try
            {
                Document doc = ePubData.getManifestedDocument( id );
                if (null != doc)
                {
                    int docCount = 0;
                    DOMIterator domIter = new DOMIterator( doc.getDocumentElement()); // nodelist.item( 0 ) );
                    while (domIter.hasNext())
                    {
                        Element el = domIter.nextElement();
                        if (null == el)
                            break;
                        if (!XMLUtil.nodesMatch( findTag, el, true ))
                            continue;
                        
                        // watch out for exact replacements, because they can cause endless loops
                        if (null != replaceTag && XMLUtil.nodesMatch( findTag, replaceTag, false ))
                            continue;
                        
                        // if we got to this point, all attributes are present and match
                        ++docCount;
                        if (null == replaceTag)
                        {
                            domIter.previous();
                            // Just delete the findTag, but be sure to preserve the children.
                            // This is accomplished by moving all of my children in front of
                            // the discovered tag, then removing it.
                            Node parent = el.getParentNode();
                            while (el.hasChildNodes())
                            {
                                Node child = el.getFirstChild();
                                el.removeChild( child );
                                parent.insertBefore( child, el );
                            }
                            parent.removeChild( el );
                        }
                        else
                        {
                            int j;
                            domIter.previous();
                            NamedNodeMap attrs = findTag.getAttributes();
                            for (j = 0; j < attrs.getLength(); j++)
                            {
                                // remove all the attributes from the element to be replaced
                                // which match the attributes in the template element
                                el.removeAttribute( attrs.item( j ).getNodeName() );
                            }
                            attrs = replaceTag.getAttributes();
                            el = XMLUtil.renameElement( el, replaceTag.getNodeName() );
    //                        doc.renameNode( el, replaceTag.getNamespaceURI(),
    //                                replaceTag.getNodeName() );
                            for (j = 0; j < attrs.getLength(); j++)
                            {
                                // set the elements from the replacement element template
                                el.setAttribute( attrs.item( j ).getNodeName(), 
                                                 attrs.item( j ).getNodeValue() );
                            }
                        }
                    }
                    if (0 < docCount)
                    {
                        try( FileOutputStream f = new FileOutputStream( new File( ePubData.getOpfFolder(), iter.href )))
                        {
                            // Pretty print the html document
                            XHTMLDocument xhtmlFile = new XHTMLDocument( doc, iter.href );
                            xhtmlFile.print( f, 2, false );
                        }
                    }
                    count += docCount;
                }
            }
            catch( FileNotFoundException ignore ) 
            {
                // if an allegedly manifested file doesn't exist, just skip it.
            }
        }
        }
        return count;
    }

    public static int insertAt(EPubModel ePubData, Element newChild, Element refChild, String actionCommand )
            throws IOException
    {
        // Traverse the entire spine looking at html files. Find nodes matching
        // the referenced node and insert a copy of the new child before them. 
        // Return a count of the number of insertions made.
        int count = 0;
        SpineModel.SpineHTMLIterator iter = ePubData.getOpfData().getSpine().new SpineHTMLIterator();
        while (iter.hasNext())
        {
            String id = iter.next();
            Document doc = ePubData.getManifestedDocument( id );
            if (null != doc)
            {
                NodeList nodes = doc.getElementsByTagName( refChild.getNodeName() );
                int docCount = 0;
                for (int i = 0; i < nodes.getLength(); i++)
                {
                    // We have a list of all matching nodes in the document, but
                    // we only care about those which also match the target attributes.
                    // Because we're inserting, a partial match should be good enough
                    Element el = (Element) nodes.item( i );
                    if (!XMLUtil.nodesMatch( refChild, el, true ))
                        continue;
                   
                    // if we got to this point, all attributes are present and match
                    ++docCount;

                    // The rest is simple. Just clone the new child and insert the clone.
                    Node newNode = doc.importNode( newChild, true );
                    switch (actionCommand)
                    {
                        case InsertTagDialog.PARENT:
                        {
                            // newNode will become the parent of refChild
                            Node parent = el.getParentNode();
                            Node child = parent.replaceChild( newNode, el );
                            newNode.insertBefore( child, null );
                            // Apparently the list of nodes by name is dynamic, so by adding a parent, the not list just got longer. Skip ahead.
                            if (newNode.getNodeName().equalsIgnoreCase( refChild.getNodeName() ))
                                i++;
                            break;
                        }
                        case InsertTagDialog.STEP:
                            // move all of the children from the refChild to the newChild,
                            // then add the newChild as the sole child of refChild
                            while (el.hasChildNodes())
                            {
                                newNode.insertBefore( el.getFirstChild(), null );
                            }
                            el.insertBefore( newNode, null );
                            break;
                        case InsertTagDialog.FIRST:
                        case InsertTagDialog.LAST:
                            // newNode will become a child of refChild
                            if (actionCommand.equals( InsertTagDialog.FIRST ))
                                el.insertBefore( newNode, el.getFirstChild() );
                            else
                                el.insertBefore( newNode, null );
                            break;
                        case InsertTagDialog.AFTER:
                        case InsertTagDialog.BEFORE:
                        {
                            // newNode will become a peer of refChild
                            Node parent = el.getParentNode();
                            if (actionCommand.equals( InsertTagDialog.BEFORE ))
                                parent.insertBefore( newNode, el );
                            else
                                parent.insertBefore( newNode, el.getNextSibling() );
                            break;
                        }
                    }
                }
                if (0 < docCount)
                {
                    // Pretty print the html document
                    XHTMLDocument xhtmlFile = new XHTMLDocument( doc, iter.href );
                    File f = new File( ePubData.getOpfFolder(), iter.href );
                    FileOutputStream fos = new FileOutputStream( f );
                    try
                    {
                        xhtmlFile.print( fos, 2, false );
                        // only add to the count if the save was successful.
                        fos.close();
                        count += docCount;
                    }
                    catch( DOMException ex )
                    {
                        LogAndShowError.logAndShowEx(
                                "DOMException while parsing DOM in Pretty Print method\n",
                                ex );
                        fos.close();
                    }
                    catch( FileNotFoundException ex )
                    {
                        LogAndShowError.logAndShowEx( "Unable to write to file\n", ex );
                    }
                    catch( IOException ex )
                    {
                        LogAndShowError.logAndShowEx(
                                "Non-specific io error while writing to file\n", ex );
                    }
                }
                count += docCount;
            }
        }
        return count;
    }

    static final String ncxXsl = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
    "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" " + 
    "                                xmlns:ncx=\"http://www.daisy.org/z3986/2005/ncx/\"" +
    ">\n" + 
    
    "  <!-- standard copy template should be first because in cases of ambiguity, last wins -->\n" + 
    "  <xsl:template match=\"node()\">\n" + 
    "    <xsl:text>  </xsl:text>\n" +
    "    <xsl:copy>\n" + 
    "      <xsl:apply-templates select=\"@*\"/>" + 
    "      <xsl:apply-templates/>\n" + 
    "    </xsl:copy>\n" + 
    "  </xsl:template>\n" + 
    
    "   <xsl:template match=\"@*\" >\n" + 
    "     <xsl:copy>\n" + 
    "        <!-- xsl:apply-templates select=\"@*\"/ -->\n" + 
    "     </xsl:copy>\n" + 
    "  </xsl:template>\n" + 

    "<!--  ncx xmlns='http://www.daisy.org/z3986/2005/ncx/' -->\n" + 
    "  <xsl:template match=\"ncx|ncx:ncx\">\n" + 
    "    <xsl:text>\n</xsl:text>\n" +
    "    <ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">\n" + 
    "      <xsl:apply-templates select=\"@*\"/>\n" + 
    "      <xsl:apply-templates/><xsl:text>\n</xsl:text>\n" +
    "    </ncx><xsl:text>\n</xsl:text>\n" + 
    "  </xsl:template>\n" + 
    
    "<xsl:template match=\"text|ncx:text\">\n" +
    "  <text xmlns=\"http://www.daisy.org/z3986/2005/ncx/\"><xsl:apply-templates/></text>\n" +
    "</xsl:template>\n"+
    
    "<xsl:template match=\"content|ncx:content\">\n" +
    "  <content xmlns=\"http://www.daisy.org/z3986/2005/ncx/\">" +
    "<xsl:apply-templates select=\"@*\"/>" +
    "</content>\n" +
    "</xsl:template>\n"+

    
    ""+
    ""+
    ""+
    "  <xsl:template match=\"text()\" priority=\"10\">\n" + 
    "    <xsl:value-of select='normalize-space(.)'/>" + 
    "  </xsl:template>\n" + 
    
    "</xsl:stylesheet>";
    

}
