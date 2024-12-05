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
  
  $Log: XMLUtil.java,v $
  Revision 1.7  2014/07/29 20:56:10  lpassey
  1. Add getElementById method
  2. Fix method to parse style attribute

  Revision 1.6  2013/06/26 17:58:53  lpassey
  1. Add getNextElement and getPrevElement methods. Like getNextSibling and getPrevSibling,
   but only returns elements, not nodes.
  2. Add Public Domain "licence"

*/

package com.passkeysoft.opfedit.staticutil;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
// import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class XMLUtil
{
    public static Element getNextElement( Node node )
    {
        for( Node temp = node.getNextSibling(); temp != null; temp = temp.getNextSibling())
        {
            if (Node.ELEMENT_NODE == temp.getNodeType())
                return (Element) temp;
        }
        return null;
    }
    
    public static Element getPrevElement( Node node )
    {
        for (Node temp = node.getPreviousSibling(); null != temp; temp = temp.getPreviousSibling())
        {
            if (Node.ELEMENT_NODE == temp.getNodeType())
                return (Element) temp;
        }
        return null;
    }
    
    /**
     * Starting with the node indicated by <code>parent</code> perform an in-order traversal
     * of the DOM tree, find the first element which has a name matching <code>elementName</code>
     * @param parent The starting node; this node will not be tested for equivalence
     * @param elementName The name of the node sought
     * @return The first descendent node with a name that matches <code>elementName</code>, 
     * null otherwise.
     */
    public static Element findFirstElement( Node parent, String elementName )
    {
        for (Node child = parent.getFirstChild(); null != child; child = child.getNextSibling())
        {
            if (Node.ELEMENT_NODE == child.getNodeType())
            {
                if (elementName.equals( child.getNodeName() ))
                {
                    return (Element) child;
                }
                Node grandChild = findFirstElement( child, elementName );
                if (null != grandChild)
                    return (Element) grandChild;
            }
        }
        return null;
    }
    
    
    public static Element getElementById( Node parent, String id)
    {
        // find the first element
        for (Node child = parent.getFirstChild(); null != child; child = child.getNextSibling())
        {
            if (Node.ELEMENT_NODE == child.getNodeType())
            {
                String attr = ((Element) child).getAttribute( "id" );
                if (null != attr && attr.equals( id ))
                    return (Element) child;
                Element grandChild = getElementById( child, id );
                if (null != grandChild)
                    return grandChild;
            }
        }
        return null;
    }
    /**
     * The only way to rename a node in a non-namespaced DOM is to create a new node, move
     * everything to the new node then replace the old one.
     * @param oldNode The Element being renamed
     * @param nodeName The new name of the element
     * @return The new Element which replaces the old Element -- effectively the renamed Node
     */
    public static Element renameElement( Element oldNode, String nodeName )
    {
        Document owner = oldNode.getOwnerDocument();
        Element element = owner.createElement( nodeName );
        NamedNodeMap attrs = oldNode.getAttributes();
        for (int i = 0; i < attrs.getLength();i++)
        {
            element.setAttributeNode( (Attr) attrs.item( i ).cloneNode( true ) );
        }
        while (oldNode.hasChildNodes())
        {
            Node child = oldNode.getFirstChild();
            element.appendChild( oldNode.removeChild( child ) );
        }
        Node parent = oldNode.getParentNode();
        parent.replaceChild( element, oldNode );
        return element;
    }
    
    
    /**
     * Transforms a DOM document according to the rules of the specified XSLT script.
     * @param db The DocumentBuilder which was used to build the input document.
     * @param domDoc The DOM document to be transformed.
     * @param xsl An input stream wrapper of the XSLT script containing the transformation rules.
     * @return The result of the transformation as a new DOM document.
     * @throws TransformerConfigurationException
     * @throws TransformerFactoryConfigurationError
     * @throws TransformerException
     */
    public static Document xFormFile( DocumentBuilder db, Document domDoc, InputStream xsl ) 
        throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException
    {
        Transformer transformer;

        StreamSource xslStreamSrc = new StreamSource( xsl );
        transformer = TransformerFactory.newInstance().newTransformer( xslStreamSrc );
        /* 
         * Commented code serves to write the document to a temporary file for debugging purposes.
         */
        {
            transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
            // transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
            // "\"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\""
            // );
            transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
            transformer.setOutputProperty( OutputKeys.CDATA_SECTION_ELEMENTS, "style" );
            transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
            Document inter = db.newDocument();
            DOMResult xhtmlout = new DOMResult( inter );
            DOMSource ins = new DOMSource( domDoc );
            try
            {
                transformer.transform( ins, xhtmlout );

//                FileOutputStream out = new FileOutputStream( "test" + ".htm" );
//                if (null != out)
//                {
//                    StreamResult output = new StreamResult( out );
//                    transformer.transform( ins, output );
//                    out.close();
//                }
            }
            catch( TransformerException ex )
            {
                ex.printStackTrace();
                throw ex;
            }
//            catch( IOException e )
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
            return inter;
        }
//
//        return null;
    }

    
    
    /**
     * Compares the XML nodes thing1 and thing2 and returns true if they are functionally
     * equivalent. The actual behavior depends on the node type of the two nodes. If the
     * two nodes are Elements, all the attributes of thing1 must match the attributes of 
     * thing2, and if 'partial' is false, thing2 may not contain any attribute which is not
     * possessed by thing1.
     * 
     * @param thing1 the Node to test for equivalence
     * @param thing2 the Node against which to test thing1
     * @param partial if thing1 and thing2 are Element nodes, allow partial map
     * @return true if the two nodes are functionally equivalent, false otherwise
     */
    static public boolean nodesMatch( Node thing1, Node thing2, boolean partial )
    {
        if (thing1.getNodeType() == thing2.getNodeType())
        {
            if (   Node.ELEMENT_NODE == thing1.getNodeType() 
                    // special case: elements match is not case sensitive.
                    // TODO: SFID:3427006 add regex matching on element names and attribute values
                && thing1.getNodeName().equalsIgnoreCase( thing2.getNodeName() ))
            {
                // matching elements. check to see that all attributes
                // of thing1 match thing2
                NamedNodeMap attrs = thing1.getAttributes();
                if (0 == attrs.getLength() && partial)
                    return true;        // No attributes for thing1, partial match is OK.
                int i;
                // Either thing1 has attributes, or we need a full match
                for (i = 0; i < attrs.getLength(); i++)
                {
                    String attrName = attrs.item( i ).getNodeName();
                    // if the attribute is "id" then this is a match, even if thing2 doesn't have the same id.
                    if (attrName.equalsIgnoreCase( "id" ))
                        continue;
                    String attrVal = ((Element) thing2).getAttribute( attrName ).toLowerCase();
                    // if the attribute is "style", then different parsing rules apply.
                    // split the string on semi-colons, then remove all the spaces before
                    // doing the comparison.
                    if (attrName.equalsIgnoreCase( "style" ))
                    {
                        String[] oldAttrVals = attrs.item( i ).getNodeValue().toLowerCase().split( ";" );
                        String[] newAttrVals = attrVal.toLowerCase().split( ";" );
                        int j;
                        for (j = 0; j < oldAttrVals.length; j++ )
                        {
                             String oldAttrVal =  oldAttrVals[j].replaceAll( "\\s+", "" );
                             if ( 0 == oldAttrVal.length())
                             {
                                break;  // no more attributes to look at.
                             }
                             //  See if the style sought is among the styles provided
                             int k;
                             for (k = 0; k < newAttrVals.length; k++)
                             {
                                 String newAttrVal = newAttrVals[k].replaceAll( "\\s+", "" );
                                 if (newAttrVal.equalsIgnoreCase( oldAttrVal ))
                                     break; // found a match
                             }
                             if (k == newAttrVals.length)
                                 break;     // no match was found
                        }
                        if (j < oldAttrVals.length)   // thing 1 has an attribute that thing 2 does not.
                            break;
                    }
                    else
                    {
                        String[] attrVals = attrs.item( i ).getNodeValue().toLowerCase().split( "\\s+" );
                        int j;
                        for (j = 0; j < attrVals.length; j++ )
                        {
                             if ( 0 == attrVals[j].length())
                             {
                                continue;
                             }
                             else if (attrVal.equals( attrVals[j] ) )
                                 break; // found a match
                        }
                        if (j >= attrVals.length)   // thing 1 has an attribute that thing 2 does not.
                            break;
                    }
                }
                if (i >= attrs.getLength())
                {
                    // partial test was satisfied
                    if (partial)
                        return true;
                    else
                    {
                        // Same as above, only reversed
                        attrs = thing2.getAttributes();
                        for (i = 0; i < attrs.getLength(); i++)
                        {
                            String attrVal = ((Element) thing1).getAttribute( attrs.item( i ).getNodeName() ).toLowerCase();
                            String[] attrVals = attrs.item( i ).getNodeValue().toLowerCase().split( "\\s+" );
                            int j;
                            for (j = 0; j < attrVals.length; j++ )
                            {
                                 if ( 0 == attrVals[j].length() || !attrVal.contains( attrVals[j] ) )
                                    break;
                            }
                            if (j < attrVals.length)
                                break;
                        }
                        if (i >= attrs.getLength())
                            return true;
                    }
                }
            }
            else if (thing1.getNodeName().equals( thing2.getNodeName() ))
            {
                if (null == thing1.getNodeValue() && null == thing2.getNodeValue())
                    return true;
                else if (   null != thing1.getNodeValue()
                          && null != thing2.getNodeValue()
                          && thing1.getNodeValue().equals( thing2.getNodeValue() ))
                    return true;
            }
        }
        return false;
    }

    
    /**
     * Test if a DOM Node contains any text content at any level which is not whitespace.
     * @param node The parent node which we are testing
     * @return <code>true</code> if the node contains some non-white content, 
     *      <code>false</code> otherwise.
     */
    static public boolean hasRealContent( Node node )
    {
        if (Node.TEXT_NODE == node.getNodeType())
            if (0 < node.getNodeValue().trim().length())
                return true;    // this node is a non-white text node.
        // I'm not a non-white text node. Does any one of my children have one?
        for (Node child = node.getFirstChild(); null != child; child = child.getNextSibling())
        {
            if (hasRealContent( child ))
                return true;
        }
        return false;
    }
}
