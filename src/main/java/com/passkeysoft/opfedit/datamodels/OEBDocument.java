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
  
  $Log: OEBDocument.java,v $
  Revision 1.10  2014/07/29 21:16:19  lpassey
  Make bursting more robust by checking for an existing id before generating a new one
   (avoid identifier collisions).

  Revision 1.9  2013/07/03 21:55:27  lpassey
  Fix typo in comment

  Revision 1.8  2013/06/26 17:55:09  lpassey
  1. If header elements do not have 'id' attributes, generate one.
  2. Add Public Domain "licence"

*/

package com.passkeysoft.opfedit.datamodels;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import com.passkeysoft.DOMIterator;
import com.passkeysoft.XHTMLDocument;
import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.staticutil.XMLUtil;

public class OEBDocument extends XHTMLDocument implements Comparator<OEBDocument.HTMLAnchor>
{

    class HTMLAnchor
    {
        String htmlFile;
        public String href;
        public String id;
    }
    
    // The anchor list is ordered first by file name and then by ID, so I can fix 
    // all the pointers in a single file before moving on to another.
    @Override
    public int compare( HTMLAnchor o1, HTMLAnchor o2 )
    {
        int retVal = o1.htmlFile.compareTo( o2.htmlFile );
        if (0 == retVal)
            retVal = o1.id.compareTo( o2.id );
        if (0 == retVal)
            retVal = o1.href.compareTo( o2.href );
        return retVal;
    }
    
    private TreeSet<HTMLAnchor> anchorList = new TreeSet<>( this );
    
    private HTMLAnchor findAnchorByID( String id )
    {
        Iterator<HTMLAnchor> iter = anchorList.iterator();
        while (iter.hasNext())
        {
            HTMLAnchor a = iter.next();
            if (a.id.equals( id ))
                return a;
        }
        return null;
        
    }
    
    public OEBDocument( Document doc, String name )
    {
        super( doc, name );
    }

    private File publish( Node outNode, File f) throws DOMException, IOException
    {
        f.getCanonicalFile().getParentFile().mkdirs();
        FileOutputStream fout = new FileOutputStream( f );
        _out = new BufferedWriter( new OutputStreamWriter( fout, StandardCharsets.UTF_8 ));
//        _out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" );
        _step = 2;
        _asciiOnly = false;
        printNode( outNode, _step, 0 );
        fout.close();
        return f;
    }
    
    
    public ArrayList<File> burst(String path, DocumentBuilder db, Element delim, File href )
            throws DOMException, IOException
    {
        ArrayList<File> result = new ArrayList<>();
        Node outNode, currNode, bodyNode = null;
        
        String name = FileUtil.getFileName( href );
        String ext = FileUtil.getExt( href );
//        ext = "xhtml";
        int count = 0, counter = 0;
        
        // Set up the new document
        Document docOut = db.newDocument();

        currNode = _baseDoc.getDocumentElement();
        outNode = docOut.importNode( currNode, false );
        docOut.appendChild( outNode );
        
        // iterate the old document, copying nodes until the burst point is encountered.
        DOMIterator i = new DOMIterator(currNode);
        String newFile = String.format( "%s_%02d.%s", name, count, ext );
        while (i.hasNext())
        {
            currNode = i.next();
            Node copyNode;
            if (   XMLUtil.nodesMatch( delim, currNode, true )
                && null != bodyNode)
            {
                // burst point encountered
                // if outNode has children, spit them out before continuing.
                if (XMLUtil.hasRealContent( bodyNode ))
                {
                    result.add( publish( docOut, new File( path, newFile )));
                    count++;
                }
                
                // remove everything from the body as the document has been saved.
                while (bodyNode.hasChildNodes())
                {
                    Node child = bodyNode.getFirstChild();
                    bodyNode.removeChild( child );
                }
                
                // climb up from the current Node, and add the entire chain to the body.
                Node parentNode = currNode.getParentNode();
                if (!parentNode.getNodeName().equalsIgnoreCase( "body" ))
                {
                    outNode = copyNode = docOut.importNode( parentNode, false );
                    for (parentNode = parentNode.getParentNode();
                         !parentNode.getNodeName().equalsIgnoreCase( "body" );
                         parentNode = parentNode.getParentNode()) 
                    {
                         Node tempNode = docOut.importNode( parentNode, false );
                         tempNode.appendChild( copyNode );
                         copyNode = tempNode;
                    }
                    bodyNode.appendChild( copyNode );
                }
                
                // get ready to publish the new file.
                newFile = String.format( "%s_%02d.%s", name, count, ext );
                counter = 0;    // counter for header ids
            }
            
            // copy this node to the output.
            copyNode = docOut.importNode( currNode, false );
            if (Node.ELEMENT_NODE == copyNode.getNodeType())
            {
                if (currNode.getNodeName().equalsIgnoreCase( "body" ))
                    bodyNode = copyNode;
                if (copyNode.getNodeName().equalsIgnoreCase( "a" ))
                {
                    // If this is an <a>nchor node, remember it, because we're 
                    // going to have to fix it up later.
                    HTMLAnchor a = new HTMLAnchor();
                    a.htmlFile = String.format( "%s_%02d.%s", name, count, ext );
                    a.href = ((Element) copyNode).getAttribute( "href" );
                    if (a.href.startsWith( "#" ) || !a.href.contains( "#" ))
                    {
                        String id = ((Element) copyNode).getAttribute( "id" );
                        if (0 == id.length())
                            id = ((Element) copyNode).getAttribute( "name" );
                        
                        a.id = id;
                        anchorList.add( a );
                    }
                }
                else
                {
                    String id = ((Element)copyNode).getAttribute( "id" );
                    String nodeName = copyNode.getNodeName();
                    if (   nodeName.toLowerCase().charAt( 0 ) == 'h'
                        && Character.isDigit( nodeName.charAt( 1 ) ))
                    {
                        // the node is a header
                        String nextId = String.format( "%s_%02d", nodeName, counter );
                        if (0 >= id.length())
                        {
                            // the node has no ID. Give it one.
                            id = nextId;
                            counter++;
                            ((Element) copyNode).setAttribute( "id", id );
                        }
                        else while ( nextId.equals( id ))
                        {
                            // if the id matches the calculated id, increment the counter until
                            // it wont match.
                            nextId = String.format( "%s_%02d", nodeName, ++counter );
                        }
                    }
                    if (0 < id.length())
                    {
                        // This is an element with an id, which may be referred to elsewhere.
                        // remember it as though it were an anchor.
                        HTMLAnchor a = new HTMLAnchor();
                        a.htmlFile = String.format( "%s_%02d.%s", name, count, ext );
                        a.id = id;
                        a.href = "";
                        anchorList.add( a );
                    }
                }
            }
            outNode.appendChild( copyNode );

            if (currNode.hasChildNodes())
            {
                currNode = currNode.getFirstChild();
                outNode = outNode.getLastChild();
            }
            else if (null != currNode.getNextSibling())
            {
                currNode = currNode.getNextSibling();
            }
            else
            {
                // end of the road, must go back to the parent. If my next sibling is null,
                // that means I'm the last of my parent's children, and I need to move back
                // to my parent's next sibling ... although that may take me back several levels.
                currNode = currNode.getParentNode();
                while (   null == currNode.getNextSibling() 
                       && currNode != _baseDoc.getDocumentElement())
                {
                    currNode = currNode.getParentNode();
                    outNode = outNode.getParentNode();
                }
                if (null != currNode)
                {
                    outNode = outNode.getParentNode();
                    currNode = currNode.getNextSibling();
                }
            }
        }
        if (null != bodyNode && XMLUtil.hasRealContent( bodyNode ))
        {
            result.add( publish( docOut, new File( path, newFile )));
        }
        
        docOut = null;
        newFile = "";
        NodeList fileAnchors = null;
        
        // I now have a list of all the files I've created, and all the anchors in those files.
        // I need to look up each of those anchors and resolve them. If the anchor has an href,
        // I need to find the file where that href occurs, and update the anchor element in that
        // file. 
        Iterator<HTMLAnchor> iter = anchorList.iterator();
        while (iter.hasNext())
        {
            HTMLAnchor a = iter.next();
            if (null != a && 0 < a.href.length())
            {
                // We're pointing somewhere else. Where?
                HTMLAnchor aref = findAnchorByID( a.href.substring( 1 ));
                if (null != aref)
                {
                    // aref.fileName is the name of the new file containing this anchor,
                    // and a.fileName is the name of the file containing the pointer.
                    // If that does not represent the current DOM, save the current DOM,
                    // load that DOM, and get all the anchors.
                    if (!newFile.equalsIgnoreCase( a.htmlFile ))
                    {
                        if (0 < newFile.length())
                        {
                            // save the old DOM before loading the new one.
                            publish( docOut, new File( path, newFile ));
                        }
                        try
                        {
//                            db.setValidating( false );
//                            db.setExpandEntityReferences( true );
//                            db.setNamespaceAware( false );
                            
                            db.setEntityResolver( new EntityResolver()
                            {
                                public InputSource resolveEntity( String arg0, String arg1 )
                                {
                                    return new InputSource( new StringReader( entities ) );
                                }
                            } );

                            docOut = db.parse( new FileInputStream( new File( path, a.htmlFile )));
                            fileAnchors = docOut.getElementsByTagName( "a" );
                            newFile = a.htmlFile;
                        }
                        catch( Exception e )
                        {
                            docOut = null;
                            newFile = "";
                            e.printStackTrace();
                        }
                    }

                    // Then, find the anchor in the DOM that that matches the id, and
                    // change the href to point to both the fileName and the id.
                    if (null != docOut && null != fileAnchors)
                    {
                        for (int j = 0; j < fileAnchors.getLength(); j++)
                        {
                            Element el = (Element) fileAnchors.item( j );
                            if (a.href.equals( el.getAttribute( "href" )))
                            {
                                // This is the element that needs to be updated.
                                el.setAttribute( "href", aref.htmlFile + "#" + aref.id );
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (null != docOut && 0 < newFile.length())
            publish( docOut, new File( path, newFile ));

        return result;
    }
    
    
    public int addDivisions( Element delim, Element container, String href ) 
            throws DOMException, IOException
    {
        int count = 0;
        String id = container.getAttribute( "class" ).split( " " )[0];
        if (0 == id.length())
            id = "id";
        // Walk the tree. Every time the matching delimiter is found, build a new container,
        // and move everything to the container until another delimiter is found.
        NodeList nodeList = _baseDoc.getDocumentElement().getElementsByTagName( "body" );
        if (0 < nodeList.getLength())
        {
            Node currNode = nodeList.item( 0 ).getFirstChild();
            while (null != currNode)
            {
                if (XMLUtil.nodesMatch( delim, currNode, true ))
                {
                    // create a new container element. 
                    Element holder = (Element) container.cloneNode( false );
                    holder.setAttribute( "id", id + String.format( "%03d", ++count ));
                    Node parent = currNode.getParentNode();
                    if (XMLUtil.nodesMatch( container, parent, true ))
                    {
                        Node div = parent.getParentNode();
                        div.insertBefore( holder, parent.getNextSibling() );
                    }
                    else
                        parent.insertBefore( holder, currNode );
                    
                    // move the current node and all of its siblings to the new container.
                    Node iter = currNode;
                    while (null != iter)
                    {
                        Node next = iter.getNextSibling();
                        holder.appendChild( parent.removeChild( iter ) );
                        iter = next;
                    }
                }
                if (currNode.hasChildNodes())
                    currNode = currNode.getFirstChild();
                else if (null != currNode.getNextSibling())
                    currNode = currNode.getNextSibling();
                else
                {
                    // end of the line, must back up
                    currNode = currNode.getParentNode();
                    while (   null == currNode.getNextSibling()
                            && currNode != _baseDoc.getDocumentElement())
                        currNode = currNode.getParentNode();
                    if (null != currNode)
                        currNode = currNode.getNextSibling();
                }
            }
            if (null != href)
                publish ( _baseDoc, new File( href ));
        }
        return count;
    }

}
