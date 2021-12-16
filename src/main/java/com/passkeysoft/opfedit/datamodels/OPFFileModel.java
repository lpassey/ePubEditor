/**
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
  
  $Log: OPFFileModel.java,v $
  Revision 1.8  2014/07/29 21:40:00  lpassey
  Resolve discrepancy in variable names between class variable and stack variable.

  Revision 1.7  2013/07/03 21:53:33  lpassey
  1. Improve error handling when .opf file has not yet been created.

*/


package com.passkeysoft.opfedit.datamodels;

import java.io.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.xml.sax.*;
import org.apache.log4j.Logger;
import org.w3c.dom.*;

import com.passkeysoft.opfedit.staticutil.XMLUtil;

public class OPFFileModel
{
    Logger logger = Logger.getRootLogger(); // where to log errors.

    private Document _opfDom = null; // The DOM document backing this data
    private MetadataModel _metadata = null;
    private Element _manifestEl = null;
    private ManifestModel _manifest = null;
    private SpineModel _spine = null;
    private GuideModel _guide = null;


    public String getUID()
    {
        String uid = "";
        if (null != _opfDom)
        {
            uid = _opfDom.getDocumentElement().getAttribute( "unique-identifier" );
            // uid is now the id of a "dc:identifier" element
            if (0 < uid.length())
            {
                NodeList ids = _opfDom.getDocumentElement().getElementsByTagName( "dc:identifier" );
                for (int i = 0; i < ids.getLength(); i++)
                {
                    String id = ((Element) ids.item( i )).getAttribute( "id" );
                    if (id.equals( uid ))
                    {
                        uid = ids.item( i ).getTextContent();
                        break;
                    }
                }
            }
        }
        return uid;
    }
    
    // getters for the four major divisions of the opf file.
    public MetadataModel getMetadata()
    {
        return _metadata;
    }


    public SpineModel getSpine()
    {
        return _spine;
    }


    public ManifestModel getManifest()
    {
        return _manifest;
    }


    public GuideModel getGuide()
    {
        return _guide;
    }


    /**
     * Creates a DOM expression of an OPF file and attaches it to an ePub package.
     * 
     * @throws DOMException
     *             if there is any error building a DOM
     * @throws SAXException
     *             if the OPF file cannot be parsed into a DOM
     * @throws FileNotFoundException
     *             if the specified OPF file cannot be found
     * @throws IOException
     *             when any non-specific I/O error is encountered.
     */
    
    public OPFFileModel( EPubModel pckg, Document opfDom ) 
            throws DOMException, SAXException, FileNotFoundException, IOException
    {
        this._opfDom = opfDom;
        if (null != _opfDom)
        {
            Element metadata, opfItem = _opfDom.getDocumentElement();

            // remove any namespace attribute, we'll add it back later.
            opfItem.removeAttribute( "xmlns" );
            
            // If it doesn't have a version number, add one.
            if (0 == opfItem.getAttribute( "version" ).length())
                opfItem.setAttribute( "version", "2.0" );  // required attribute.

            // find the <metadata> element and create a new metadata model
            metadata = XMLUtil.findFirstElement( opfItem, "metadata" );
            if (null == metadata)
            {
                metadata = XMLUtil.findFirstElement( opfItem, "opf:metadata" );
                if (null == metadata)
                {
                    metadata = _opfDom.createElement( "metadata" );
                    _opfDom.insertBefore( opfItem, _opfDom.getFirstChild() );
                }
            }
            _metadata = new MetadataModel( pckg, metadata );

            // find the manifest element and create a new manifest model
            _manifestEl = XMLUtil.findFirstElement( opfItem, "manifest" );
            if (null == _manifestEl)
            {
                _manifestEl = XMLUtil.findFirstElement( opfItem, "opf:manifest" );
                if (null == _manifestEl)
                {
                    _manifestEl = _opfDom.createElement( "manifest" );
                    _opfDom.getDocumentElement().insertBefore( _manifestEl, opfItem.getNextSibling() );
                }
            }
            this._manifest = new ManifestModel( pckg, _manifestEl );

            // find the spine element and create a new spine model
            Element spine = null;
            spine = XMLUtil.findFirstElement( opfItem, "spine" );

            if (null == spine)
            {
                spine = XMLUtil.findFirstElement( opfItem, "opf:spine" );

                if (null == spine)
                {
                    spine = _opfDom.createElement(  "spine" );
                    _opfDom.getDocumentElement().insertBefore( spine, _manifestEl.getNextSibling() );
                }
            }
            this._spine = new SpineModel( pckg, spine );

            // find the guide element and create a new guide model
            Element guide = null;
            guide = XMLUtil.findFirstElement( opfItem, "guide" );
            if (null == guide)
            {
                NodeList nodes = _opfDom.getDocumentElement().getElementsByTagName( "guide" );
                if (null != nodes && 0 < nodes.getLength())
                {
                    guide = (Element) nodes.item( 0 );
                    _opfDom.renameNode( guide, null, guide.getNodeName() );
                }
            }
            if (null != guide)
            {
                // Go through the guide and compare the entries to the
                // manifest. If there are any guide references which do not
                // have manifest entries, and the file can be found, add it
                // to the manifest now.
                NodeList refs = guide.getElementsByTagName( "reference" );
                for (int i = 0; i < refs.getLength(); i++)
                {
                    String href = ((Element) refs.item( i )).getAttribute( "href" );
                    int endIndex = href.lastIndexOf( '#' );
                    if (0 > endIndex)
                        endIndex = href.length();
                    href = href.substring( 0, endIndex );
                    File relative = new File( pckg.getPathRelativeToOpf( new File( href )));
                    if (relative.isAbsolute() && relative.exists())
                        _manifest.addManifestItem( null, href, null );
                    else
                    {
                        relative = new File( pckg.getOpfFolder(), relative.getPath() );
                        if (relative.exists())
                            _manifest.addManifestItem( null, href, null );
                    }
                }
            }
            this._guide = new GuideModel( pckg, guide );
        }
    }


//    @SuppressWarnings("unused")
//    private void openNode( Writer out, String prefix, Element el ) throws IOException
//    {
//        out.write( prefix + "<" + el.getNodeName() );
//        NamedNodeMap attrs = el.getAttributes();
//        for (int i = 0; i < attrs.getLength(); i++)
//        {
//            Node n = attrs.item( i );
//            out.write( " " + n.getNodeName() + "=\"" + n.getNodeValue() + "\"" );
//        }
//    }
    
    /**
     * Saves the opf file which is part of a publication
     * 
     * @param saveTo
     *            the abstract representation of an .opf file to be saved.
     * @throws TransformerException
     *             when an XSL transformer fails
     * @throws IOException 
     * @throws FileNotFoundException
     *             when the .opf file cannot be found in the file system.
     **/
    public boolean saveOPF( OutputStream saveTo ) 
            throws TransformerException, IOException
    {
        // Remember our current ePubRoot
        getMetadata().setProperty( EPubModel.metaEpubRoot, getMetadata().fileData.getEpubRootPath() );
        
        StreamSource xslStream = new StreamSource( new ByteArrayInputStream( opfXsl.getBytes() ) );
        Transformer t = TransformerFactory.newInstance().newTransformer( xslStream );

        t.setOutputProperty( OutputKeys.STANDALONE, "yes" );
        t.setOutputProperty( OutputKeys.INDENT, "no" );
        DOMSource xmlSource = new DOMSource( _opfDom );
        StreamResult output = new StreamResult( saveTo );
        t.transform( xmlSource, output );
        
//        OutputStreamWriter out = new OutputStreamWriter( saveTo );
//        if (null != out && null != _opfDom)
//        {
//            Node node; 
//            out.write( "<?xml version=\"1.0\" encoding=\"utf-8\">\n" );
//            Element section, opfItem = _opfDom.getDocumentElement();
//            opfItem.setAttribute( "xmlns", "http://www.idpf.org/2007/opf" );
//            openNode( out, "", opfItem );
//            out.write( ">\n" );
//            section = XMLUtil.findFirstElement( opfItem, "metadata" );
//            if (null != section)
//            {
//                out.write( "  <metadata xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" );
//                for (node = section.getFirstChild(); null != node; node = node.getNextSibling())
//                {
//                    if (Node.ELEMENT_NODE == node.getNodeType())
//                    {
//                        openNode( out, "    ", (Element) node );
//                        Element el = (Element) node;
//                        out.write( ">" + el.getTextContent() );
//                        out.write( "</" + el.getNodeName() + ">\n");
//                    }
//                }
//                out.flush();
//                out.write( "  </metadata>\n" );
//            }
//            section = XMLUtil.findFirstElement( opfItem, "manifest" );
//            if (null != section)
//            {
//                out.write( "  <manifest>\n" );
//                for (node = section.getFirstChild(); null != node; node = node.getNextSibling())
//                {
//                    if (Node.ELEMENT_NODE == node.getNodeType())
//                    {
//                        openNode( out, "    ", (Element) node );
//                        out.write("/>\n");
//                    }
//                }
//                out.flush();
//                out.write( "  </manifest>\n" );
//            }
//            section = XMLUtil.findFirstElement( opfItem, "spine" );
//            if (null != section)
//            {
//                openNode(out, "  ", section );
//                out.write( ">\n" );
//                for (node = section.getFirstChild(); null != node; node = node.getNextSibling())
//                {
//                    if (Node.ELEMENT_NODE == node.getNodeType())
//                    {
//                        openNode( out, "    ", (Element) node );
//                        out.write("/>\n");
//                    }
//                }
//                out.flush();
//                out.write( "  </spine>\n" );
//            }
//
//            section = XMLUtil.findFirstElement( opfItem, "guide" );
//            if (null != section && 0 < section.getChildNodes().getLength())
//            {
//                openNode(out, "  ", section );
//                out.write( ">\n" );
//                for (node = section.getFirstChild(); null != node; node = node.getNextSibling())
//                {
//                    if (Node.ELEMENT_NODE == node.getNodeType())
//                    {
//                        openNode( out, "    ", (Element) node );
//                        out.write("/>\n");
//                    }
//                }
//                out.flush();
//                out.write( "  </guide>\n" );
//            }
//            // package closing element
//            out.write( ("</" + opfItem.getNodeName() + ">\n") );
//            out.close();
//        }
        
        return true;
    }


    static Element getChildElementById( Element parent, String idref )
    {
        Element el = null;

        for (Node node = parent.getFirstChild(); null != node; node = node.getNextSibling())
        {
            if (node.getNodeType() == (Node.ELEMENT_NODE))
            {
                el = (Element) node;

                // is it me?
                String id = el.getAttribute( "id" );
                if (id.equals( idref ))
                    break;

                // is it one of my children
                el = getChildElementById( (Element) node, idref );
                if (null != el)
                    break;
            }
        }
        return el;
    }
    

    static final String opfXsl = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" " +
            "                                xmlns:opf=\"http://www.idpf.org/2007/opf\">\n" + 
            
            "  <!-- standard copy template should be first because in cases of ambiguity, last wins -->\n" + 
            "  <xsl:template match=\"node()\">\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <xsl:copy>\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text>\n" + 
            "    </xsl:copy>\n" + 
            "  </xsl:template>\n" + 
            
            "   <xsl:template match=\"@*\" >\n" + 
            "     <xsl:copy>\n" + 
            "        <!-- xsl:apply-templates select=\"@*\"/ -->\n" + 
            "     </xsl:copy>\n" + 
            "  </xsl:template>\n" + 

            "<!--  package xmlns='http://www.idpf.org/2007/opf' xmlns:opf='http://www.idpf.org/2007/opf' -->\n" + 
            "  <xsl:template match=\"package|opf:package\">\n" + 
            "    <xsl:text>\n</xsl:text>\n" +
            "    <package xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n</xsl:text>\n" +
            "    </package><xsl:text>\n</xsl:text>\n" + 
            "  </xsl:template>\n" + 
            
            "  <xsl:template match=\"metadata|opf:metadata\" priority=\"1\">\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <metadata xmlns=\"http://www.idpf.org/2007/opf\" xmlns:opf=\"http://www.idpf.org/2007/opf\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text>\n" +
            "    </metadata>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"manifest|opf:manifest\" >\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <manifest xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text>\n" +
            "    </manifest>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"spine|opf:spine\" >\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <spine xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text>\n" +
            "    </spine>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"tours|opf:tours\" priority=\"1\">\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <tours xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text></tours>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"guide|opf:guide\" priority=\"1\">\n" + 
            "    <xsl:text>\n  </xsl:text>\n" +
            "    <guide xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "      <xsl:apply-templates select=\"@*\"/>\n" + 
            "      <xsl:apply-templates/><xsl:text>\n  </xsl:text>\n" +
            "    </guide>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"*[starts-with(name(),'dc:')]\" priority=\"1\">\n" + 
            "    <xsl:text>\n    </xsl:text><xsl:copy>\n" + 
            "        <xsl:for-each select=\"@*\">\n" + 
            "            <xsl:copy/>\n" + 
            "        </xsl:for-each>\n" + 
            "        <xsl:apply-templates/>\n" + 
            "      </xsl:copy>\n" + 
            "  </xsl:template>\n" + 
            
            "  <xsl:template match=\"meta|opf:meta\">\n" + 
            "    <xsl:text>\n    </xsl:text><meta xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "        <xsl:for-each select=\"@*\">\n" + 
            "            <xsl:copy/>\n" + 
            "        </xsl:for-each>\n" + 
            "        <xsl:apply-templates/>\n" + 
            "      </meta>\n" + 
            "  </xsl:template>\n" + 
            
            "  <xsl:template match=\"item|opf:item\" priority=\"1\">\n" + 
            "    <xsl:text>\n    </xsl:text><item xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "        <xsl:apply-templates select=\"@*\"/>\n" + 
            "        <xsl:apply-templates/>\n" + 
            "      </item>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"itemref|opf:itemref\" priority=\"1\">\n" + 
            "    <xsl:text>\n    </xsl:text><itemref xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "        <xsl:apply-templates select=\"@*\"/>\n" + 
            "        <xsl:apply-templates/>\n" + 
            "      </itemref>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"reference|opf:reference\" priority=\"1\">\n" + 
            "    <xsl:text>\n    </xsl:text><reference xmlns=\"http://www.idpf.org/2007/opf\">\n" + 
            "        <xsl:apply-templates select=\"@*\"/>\n" + 
            "        <xsl:apply-templates/>\n" + 
            "      </reference>\n" + 
            "  </xsl:template>\n" + 
              
            "  <xsl:template match=\"text()\" priority=\"10\">\n" + 
            "    <xsl:value-of select='normalize-space()'/>\n" + 
            "  </xsl:template>\n" + 
            
            "</xsl:stylesheet>\n";
}
