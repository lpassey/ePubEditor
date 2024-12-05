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
 */

package com.passkeysoft;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;


import com.sun.org.apache.xerces.internal.dom.ElementImpl;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.passkeysoft.DOMIterator;

/**
 * @author W. Lee Passey
 *
 */
public class XHTMLDocument
{
    private boolean _lastWasWhite;
    private int _gnLineLen = 0, _gnWrap = 80;
    private String _name;
    protected int _step;
    protected BufferedWriter _out = null;
    protected Document _baseDoc;
    protected boolean _asciiOnly = false;


    public XHTMLDocument(Document doc, String name )
    {
        _baseDoc = doc;
        _name = name;
    }


    /**
     * @param nodeName
     *            the name of an HTML tag
     * @return true if the content model of the named tag is 'empty' (cannot have children) false
     *         otherwise
     */
    static public boolean htmlIsEmpty( String nodeName )
    {
        if (null != nodeName)
        {
            if (   "area".equalsIgnoreCase( nodeName )
                || "col".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "hr" )
                || nodeName.equalsIgnoreCase( "br" )
                || nodeName.equalsIgnoreCase( "img" )
                || nodeName.equalsIgnoreCase( "input" )
                || nodeName.equalsIgnoreCase( "link" )
                || nodeName.equalsIgnoreCase( "meta" )
                || nodeName.equalsIgnoreCase( "param" )
                || "picture".equalsIgnoreCase( nodeName )
                || "wbr".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "svg:image" )
                )
                return true;
        }
        return false;
    }


    /**
     * @param nodeName the name of an HTML tag
     * @return  true if the content model of the named tag is 'block', but which cannot
     *          contain other blocks; false otherwise
     */
    static public boolean htmlIsSemiBlock( String nodeName )
    {
        // return true if it's a block that can't contain other blocks
        if (null != nodeName)
        {
            if (   "audio".equalsIgnoreCase( nodeName )
                || "button".equalsIgnoreCase( nodeName )
                || "canvas".equalsIgnoreCase( nodeName )
                || "caption".equalsIgnoreCase( nodeName )
                || "center".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "dd" )
                || "dialog".equalsIgnoreCase( nodeName )
                || "dt".equalsIgnoreCase( nodeName )
                || "figcaption".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "h1" )
                || nodeName.equalsIgnoreCase( "h2" )
                || nodeName.equalsIgnoreCase( "h3" )
                || nodeName.equalsIgnoreCase( "h4" )
                || nodeName.equalsIgnoreCase( "h5" )
                || nodeName.equalsIgnoreCase( "h6" )
                || "legend".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "li" )
                || nodeName.equalsIgnoreCase( "meta" )
                || "option".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "p" )
                || "samp".equalsIgnoreCase( nodeName )
                || "script".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "style" )
                || "select".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "td" )
                || "textarea".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "th" )
                || nodeName.equalsIgnoreCase( "title" )

                || nodeName.equalsIgnoreCase( "link" )
                )
                return true;
        }
        return false;
    }
    
    /**
     * @param nodeName the name of an HTML tag
     * @return true if the content model of the named tag is 'block' (starts and ends on new lines)
     *         false otherwise
     */
    static public boolean htmlIsBlock( String nodeName )
    {
        // return true if it's a block that may contain other blocks
        if (null != nodeName)
        {
            if (htmlIsSemiBlock( nodeName ))
                return true;
            if (   "address".equalsIgnoreCase( nodeName )
                || "article".equalsIgnoreCase( nodeName )
                || "pre".equalsIgnoreCase( nodeName )
                || "aside".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "blockquote" )
                || nodeName.equalsIgnoreCase( "body" )
                || "colgroup".equalsIgnoreCase( nodeName )
                || "datalist".equalsIgnoreCase( nodeName )
                || "dd".equalsIgnoreCase( nodeName )
                || "dl".equalsIgnoreCase( nodeName )
                || "details".equalsIgnoreCase( nodeName )
                || "dir".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "div" )
                || "fieldset".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "figure" )
                || "footer".equalsIgnoreCase( nodeName )
                || "form".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "html" )
                || nodeName.equalsIgnoreCase( "head" )
                || nodeName.equalsIgnoreCase( "header" )
                || nodeName.equalsIgnoreCase( "iframe" )
                || "main".equalsIgnoreCase( nodeName )
                || "map".equalsIgnoreCase( nodeName )
                || "nav".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "ol" )
                || "optgroup".equalsIgnoreCase( nodeName )
                || "section".equalsIgnoreCase( nodeName )
                || "summary".equalsIgnoreCase( nodeName )
                || nodeName.startsWith( "svg" )
                || nodeName.equalsIgnoreCase( "table" )
                || nodeName.equalsIgnoreCase( "tbody" )
                || "template".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "thead" )
                || "tfoot".equalsIgnoreCase( nodeName )
                || nodeName.equalsIgnoreCase( "tr" )
                || nodeName.equalsIgnoreCase( "ul" )
                )
                return true;
        }
        return false;
    }
    

    static public boolean htmlIsInline( String nodeName )
    {
        if (htmlIsBlock( nodeName ) || htmlIsEmpty( nodeName ))
            return false;
        return true;
    }
    
    
    /**
     * @return  true if a node is a text node which consists exclusively of white space
     * @param pNode The node to be tested
     * @param includeNBSP   true if non-breaking spaces (unicode 160)
     */
    public static boolean _isWhitespace( Node pNode, boolean includeNBSP )
    {
        if (null != pNode && Node.TEXT_NODE == pNode.getNodeType() )
        {
            String textValue =  pNode.getNodeValue();
            if (null != textValue)
            {
                if (!includeNBSP)
                {
                    // if non-breaking space is not considered whitespace,
                    // just look for ordinary spaces 
                    return textValue.trim().isEmpty();
                }
                // otherwise we need to consider entities as well
                for (int i = 0; i < textValue.length(); i++)
                {
                    if (    i + 6 < textValue.length()
                        && (   textValue.substring( i, i + 6 ).equalsIgnoreCase( "&nbsp;" )
                            || textValue.substring( i, i + 6 ).equalsIgnoreCase( "&#160;" )))
                    {
                        i += 6;
                        continue;
                    }
                    char c = textValue.charAt( i );
                    if (   !Character.isWhitespace( c )
                        && 160 != textValue.codePointAt( i )
                        && 0x2007 != textValue.codePointAt( i )
                        && 0x202f != textValue.codePointAt( i ))
                    {
                        // if we find a character that is neither a whitespace entity
                        // /or/ a whitespace character, the node is not whitespace.
                        return false;
                    }
                }
                return true;
            }
            else
                return true;
        }
        // If it's not a text node, it can't be whitespace
        return false;
    }

    
    /**
     * @return The first or last child node of a parent which is a text node.
     * @param node The parent node
     * @param first true to find the first text node, false to find the last
     */
    private Node _getTextNode( Node node, boolean first )
    {
        Node pTemp = null;

        if (   null == node 
            || node.getNodeType() == Node.TEXT_NODE)
            return node;

        if (first)
        {
            for (pTemp = node.getFirstChild( ); 
                 pTemp != null; 
                 pTemp =  pTemp.getNextSibling())
            {
                if (null != (node = _getTextNode( pTemp, first )))
                {
                    return node;
                }
            }
        }
        else
        {
            for (pTemp = node.getLastChild(); 
                 pTemp != null; 
                 pTemp = pTemp.getPreviousSibling())
            {
                if (null != (node = _getTextNode( pTemp, first )))
                {
                    return node;
                }
            }
        }
        return null;
    }


    /**
     *  Write a new line to the buffered writer, and reset the line length value to zero
     *
     * @throws IOException when write could not be completed
     */
    private void _newLine() throws IOException
    {
        if (0 != _gnLineLen)
        {
            _out.write( '\n' );
            _gnLineLen = 0;
        }
        _lastWasWhite = true;
    }
    
    
    /**
     * Write a new line to the buffered writer, then indent the specified number of
     * spaces. Set the line length equal to the indent size.
     * 
     * @param indent The number of spaces to indent
     * @throws IOException when write could not be completed
     */
    private void _startLine( int indent ) throws IOException
    {
        _newLine();
        for (int i = 0; i < indent; i++)
            _out.write( ' ' );
        _gnLineLen = indent;
    }

    /**
     * Windows 1252 - convert to unicode.
     * @param ch  windows 1252 character to convert to unicode
     * @return the converted character
     */
    private char x1252toUni( char ch )
    {
        switch (ch)
        {
            case 128:
                ch = 0x20AC;
                break;
            case 129:
                ch = 0;
                break;
            case 130:
                ch = 0x201A;
                break;
            case 131:
                ch = 0x0192;
                break;
            case 132:
                ch = 0x201E;
                break;
            case 133:
                ch = 0x2026;
                break;
            case 134:
                ch = 0x2020;
                break;
            case 135:
                ch = 0x2021;
                break;
            case 136:
                ch = 0x02C6;
                break;
            case 137:
                ch = 0x2030;
                break;
            case 138:
                ch = 0x0160;
                break;
            case 139:
                ch = 0x2039;
                break;
            case 140:
                ch = 0x0152;
                break;
            case 141:
                ch = 0;
                break;
            case 142:
                ch = 0x017D;
                break;
            case 143:
                ch = 0;
                break;
            case 144:
                ch = 0;
                break;
            case 145:
                ch = 0x2018;
                break;
            case 146:
                ch = 0x2019;
                break;
            case 147:
                ch = 0x201C;
                break;
            case 148:
                ch = 0x201D;
                break;
            case 149:
                ch = 0x2022;
                break;
            case 150:
                ch = 0x2013;
                break;
            case 151:
                ch = 0x2014;
                break;
            case 152:
                ch = 0x02DC;
                break;
            case 153:
                ch = 0x2122;
                break;
            case 154:
                ch = 0x0161;
                break;
            case 155:
                ch = 0x203A;
                break;
            case 156:
                ch = 0x0153;
                break;
            case 157:
                ch = 0;
                break;
            case 158:
                ch = 0x017E;
                break;
            case 159:
                ch = 0x0178;
                break;
        }
        return ch;
    }
    
    private String _xformChar( char ch )
    {
        if (ch > 127)
        {
            if (160 > ch)
            {
                ch = x1252toUni( ch );
                if (0 == ch)
                    return "";
            }
            if (_asciiOnly || 160 == ch)
            {
                return String.format( "&#%d;", (int) ch );
            }
        }
        return String.valueOf( ch );
    }
    
    /**
     * Write some text to the buffered writer. Track the amount of text written 
     * as a line, but /do not/ wrap the text. Ampersands and angle brackets are
     * written as entities, not natively.
     * 
     * @param text A string holding the text to write.
     * @throws IOException  when write could not be completed
     */
    private void _writeTracked( String text, int preserveSpace ) throws IOException
    {
        for (int i = 0; i < text.length(); i++)
        {
            if (   0 == preserveSpace 
                && Character.isWhitespace( text.charAt( i )))
            {
                if (_lastWasWhite)
                {
                    continue;
                }
                _lastWasWhite = true;
                _out.write( ' ' );
                ++_gnLineLen;
            }
            else
            {
                _lastWasWhite = false;
                if ('&' == text.charAt(i))
                {
                    _out.write( "&amp;" );
                    _gnLineLen += 5;
                }
                else if ('<' == text.charAt( i ))
                {
                    _out.write( "&lt;" );
                    _gnLineLen += 4;
                }
                else if ('>' == text.charAt( i ))
                {
                    _out.write( "&gt;" );
                    _gnLineLen += 4;
                }
                else if (0xA0 == text.charAt( i ))
                {
                    _out.write( "&#160;" );
                    _gnLineLen += 6;
                }
                else if (173 == text.codePointAt( i ))
                {
                    _out.write( "&shy;" );
                    _gnLineLen += 5;
                }
                else if (127 < text.codePointAt( i ))
                {
                    // not an ascii character
                    String line = _xformChar( text.charAt( i ));
                    if (null != line)
                    {
                        _out.write( line );
                        _gnLineLen += (line.length());
                    }
                }
                else
                {
                    _out.write( text.charAt( i ));
                    _gnLineLen++;
                }
            }
        }
        _out.flush();
    }
    
    
    private String normalizeSpaces( String text )
    {
        StringBuilder normalized = new StringBuilder( text.length());
        int i = 0;
        while (i < text.length() && Character.isWhitespace( text.charAt( i ) ))
            i++;
        while (i < text.length())
        {
            if (!Character.isWhitespace( text.charAt( i )))
                normalized.append( text.charAt( i ));
            else
            {
                if (i + 1 < text.length() && !Character.isWhitespace( text.charAt( i + 1)))
                    normalized.append( ' ' );
            }
            i++;
        }
        return normalized.toString();
    }
    
    
    private int _expandedTextLength( String text )
    {
        int limit = text.length();
//        if (!_asciiOnly)
//            return limit;
        // the literal text length is less that the wrap point,
        // but may need to be expanded. What is it's expanded length?
        int textLen = 0;
        for (int i = 0; i < limit; i++)
        {
            textLen += _xformChar( text.charAt( i )).length();
        }
        return textLen;
    }


    /**
     * Write some text to the buffered writer, wrapping and indenting as appropriate.
     * 
     * @param text The text to write.
     * @param indent The number of spaces to indent new lines
     * @throws IOException  when write could not be completed
     */
    private void writeWrapped( String text, int indent ) 
            throws IOException
    {
        int nStart = 0;
        int wrapPoint = _gnWrap - _gnLineLen;
        if (wrapPoint < 0)
            wrapPoint = 0;
        while (nStart < text.length())
        {
            int textLen = text.length() - 1;   // last character of the string
            if (0 < wrapPoint)
            {
                String line = text.substring( nStart, 
                        textLen < wrapPoint + 1 ? textLen : wrapPoint + 1 );
                // line is the remaining text, at least as far as the global wrapping value. 
                // how long will the expanded text be?
                textLen = _expandedTextLength( line );
            }
            else
                textLen = _expandedTextLength( text.substring( nStart ));
            // textLen is the length of the expanded text, which may or may not be 
            // greater than the length of the line.
            if ( wrapPoint >= nStart + textLen)
            {
                // even expanded, the remaining line still will not need wrapping
                _writeTracked( text.substring( nStart ), 0);
                break;      // all done.
            }
            // from here on out, the line will need wrapping.
            else if (Character.isWhitespace( text.charAt( nStart ) ))
            {
                // collapse leading whitespace.
                _startLine( indent );
                ++nStart;
                wrapPoint += (_gnWrap - _gnLineLen);
                if (wrapPoint < 0)
                    wrapPoint = nStart;
            }
            else
            {
                // This line must be wrapped.
                if (wrapPoint >= text.length())
                    wrapPoint = text.length() - 1;
                
                // Back up the wrap point a word at a time until the distance between nStart and
                // wrapPoint yields an expanded text which is less than _gnLineLen - _gnWrap.
                while (0 < wrapPoint && _gnLineLen + textLen > _gnWrap)
                {
                    while (   wrapPoint > nStart 
                           && !Character.isWhitespace( text.charAt( wrapPoint ))
                           )
                    {
                        // move the wrap point back to a whitespace.
                        --wrapPoint;
                    }
                    while (   wrapPoint > nStart
                           && Character.isWhitespace( text.charAt( wrapPoint ))
                           )
                    {
                        // move further back to the first non-whitespace
                        --wrapPoint;
                    }
                    textLen = _expandedTextLength( text.substring( nStart, wrapPoint ) );
                }
                while (   wrapPoint < text.length()
                       && !Character.isWhitespace( text.charAt( wrapPoint )))
                {
                    // move back forward to the first whitespace eligible to wrap at.
                    wrapPoint++;
                }
 
                if (nStart < wrapPoint)
                {
                    _writeTracked( text.substring( nStart, wrapPoint ), 0 );
                }
                for (nStart = wrapPoint + 1; 
                     nStart < text.length() && Character.isWhitespace( text.charAt( nStart )); 
                     nStart++ ) {}
                if (nStart < text.length())
                    _startLine( indent );
                wrapPoint += (_gnWrap - _gnLineLen);
            }
        }
    }

    
    /**
     * Writes a list of a node's attributes as name/value pairs in the form
     * 'name="value"'
     * 
     * @param pNode The node containing the attributes
     * @throws IOException  when write could not be completed
     */
    private void printAttrList( Node pNode, int indent ) throws IOException
    {
        if (pNode.hasAttributes())
        {
            int nNumAttrs;
            NamedNodeMap attrs = pNode.getAttributes();
            if (null != attrs)
            {
                int i;
       
                nNumAttrs = attrs.getLength();
                for (i = 0; i < nNumAttrs; i++)
                {
                    Node pChild = attrs.item( i );
                    String nodeValue = pChild.getNodeValue();
                    if (   (   !pChild.getNodeName().toLowerCase().equals( "xmlns" )
                            && !pChild.getNodeName().toLowerCase().equals( "xmlns:html" ))
//                        || pNode.getNodeName().equalsIgnoreCase( "html" ) 
                        || (   0 < nodeValue.length() 
                            && !nodeValue.contains( "xhtml" )))
                    {
                        if (_gnLineLen >= _gnWrap)
                        {
                            _startLine( indent );
                        }
                        else
                        {
                            _out.write( ' ' );
                            ++_gnLineLen;
                        }
                        _writeTracked( pChild.getNodeName(), 0 );
                        String attrVal = pChild.getNodeValue();
                        _writeTracked( "=\"", 0 );
    
                        if (null != attrVal && !attrVal.isEmpty())
                        {
                            int nQuotePoint = attrVal.indexOf( '"' );
                            if (-1 == nQuotePoint)
                            {
                                writeWrapped( normalizeSpaces( attrVal ), indent + 2 );
                            }
                            else 
                            {
                                // The attribute value has embedded quotation marks.
                                // Replace them with single quotes
                                int nStart = 0; 
                                while (-1 != nQuotePoint)
                                {
                                    _writeTracked( attrVal.substring( nStart, nQuotePoint ) + "'", 0 );
                                    nStart = nQuotePoint + 1;
                                    nQuotePoint = attrVal.indexOf( '"', nStart );
                                }
                                _writeTracked( attrVal.substring( nStart ), 0);
                            }
                        }
                        _writeTracked( "\"", 0);
                    }
                }
            }
        }
    }
    

    /**
     * Write an opening tag to the buffered writer. If the tag is an HTML
     * empty tag, add the self-closing slash as well.
     * @param pNode the HTML node to write
     * @throws IOException when write could not be completed
     */
    private void createOpenTag( Node pNode, int indent ) throws IOException
    {
        String nodeName = pNode.getNodeName();
        _out.write( '<' );
        _gnLineLen++;
        _out.write( nodeName );
        _gnLineLen += nodeName.length();

        if (nodeName.equalsIgnoreCase( "html" ))
        {
            // Add namespace declarations here.
            _out.write( " xmlns=\"" + pNode.getNamespaceURI() + "\"" );
        }
        // print node attributes
        if ( pNode.hasAttributes())
        {
            printAttrList( pNode, indent );
        }
        if (   (   "span".equalsIgnoreCase( nodeName )
                || "a".equalsIgnoreCase( nodeName ))
            && !pNode.hasChildNodes()
                && pNode.hasAttributes())
        {
            // an empty span or anchor which has attributes.
            // close the node then add a closing tag.
            _out.write( "></" );
            _out.write( nodeName );
            _out.write( ">");
            _gnLineLen+= 4;
            _gnLineLen += nodeName.length();
        }
        else if ( htmlIsEmpty( nodeName ))
        {
            // it is a defined empty node.
            _out.write( " />" );
            _gnLineLen += 2;
        }
        else
        {
            _out.write( ">" );
            _gnLineLen++;
        }
    }


    /**
     * Pretty print a node, and all it's children, to the buffered writer.
     * 
     * Note that this code is derived from 'C' code found at http://sourceforge.net/projects/domcapi/
     * 
     * @param pNode The node to write.
     * @param indent The amount of space to indent each new line. This value is
     *               incremented as this method is called recursively.
     * @param preserveSpace true if text nodes should /not/ be trimmed of whitespace
     *                      before writing.
     * @return true if white space should be added after this node, false otherwise
     * @throws DOMException
     * @throws IOException
     */
    protected boolean printNode( Node pNode, int indent, int preserveSpace ) 
           throws IOException // , DOMException
    {
        if (null == pNode)
            return false;
        Node pChild;
        String nodeName = pNode.getNodeName();
        boolean needsSpace = false,
                isBlock = htmlIsBlock( pNode.getNodeName() ),
                isSemiBlock = htmlIsSemiBlock( pNode.getNodeName() );

        switch (pNode.getNodeType())
        {
            case Node.ELEMENT_NODE:
                // Uncomment to replace "meta" nodes for utf-8
/*                
                if (   pNode.getNodeName().equalsIgnoreCase( "meta" )
                    && null != ((Element) pNode).getAttribute( "http-equiv" )
                    && 0 != ((Element)pNode).getAttribute( "http-equiv"  ).length())
                {
                    _out.write("\n  <meta http-equiv=\"content-type\" content=\"text/html; charset=utf-8\" />" );
                    return;
                }
*/
                if (   pNode.getNodeName().equalsIgnoreCase( "style" )
                    || pNode.getNodeName().equalsIgnoreCase( "pre" ))
                {
                    preserveSpace++;
                }
                // <a> elements get put on a new line if following a block
                if (   (isBlock && _gnLineLen != indent && !pNode.getNodeName().equalsIgnoreCase( "br" ))
                    || (   pNode.getNodeName().equalsIgnoreCase( "a" )
                        && null != pNode.getPreviousSibling()
                        && (   htmlIsBlock( pNode.getPreviousSibling().getNodeName())
                            || (   _isWhitespace( pNode.getPreviousSibling(), false )) 
                                && pNode.getPreviousSibling().getPreviousSibling() != null 
                                && htmlIsBlock( pNode.getPreviousSibling().getPreviousSibling().getNodeName()))
                        ))
                {
                    _startLine( indent );
                }
                if (   !isBlock         // inline or empty element
                    && pNode.hasChildNodes()    // has children
                    && pNode.getFirstChild().getNodeType() == Node.TEXT_NODE    // child is text ...
                    && !_isWhitespace( pNode.getFirstChild(), false ))  // ... and not exclusively white
               {
                   String text = pNode.getFirstChild().getNodeValue();
                   if (Character.isWhitespace( text.charAt( 0 )))
                       writeWrapped (" ", indent );
               }
                else if (   htmlIsEmpty(pNode.getNodeName())
                         && _gnLineLen < indent )
                {
                    // an empty element that is not yet indented. Do that first
                    _startLine( indent );
                }
                createOpenTag( pNode, indent );
                if (   pNode.getNodeName().equalsIgnoreCase( "br" )
                    || pNode.getNodeName().equalsIgnoreCase( "hr"  ))
                {
                    _startLine( indent );
                }
                else if (   pNode.getNodeName().equalsIgnoreCase( "div"  )
                         && pNode.hasChildNodes()
                         && !htmlIsBlock( pNode.getFirstChild().getNodeName()))
                {
                    _startLine( indent + _step );
                }
                // If this is a block, ignore any white space which follows.
                if (isBlock)
                    _lastWasWhite = true;
                break;
            case Node.TEXT_NODE:
              if (0 != preserveSpace || !_isWhitespace( pNode, false ))
              {
                  String text = pNode.getNodeValue();
                  if (   _gnLineLen > indent 
                      && !_lastWasWhite
                      && Character.isWhitespace( text.charAt( 0 ) ))
                  {
                      writeWrapped( " ", indent );
                  }
                  if (0 != preserveSpace)
                  {
                      _writeTracked( text, preserveSpace );
                  }
                  else
                  {
                      writeWrapped( normalizeSpaces( text ), indent );
                  }
                  if (Character.isWhitespace( text.charAt( text.length() - 1 )))
                  {
                      // The node ends with whitespace, which may have been trimmed.
                      needsSpace = true;
                  }
              }
              else
              {
                  // An attempt to collapse pure whitespace nodes
                  if (   null != pNode.getPreviousSibling()
                      && !htmlIsBlock( pNode.getPreviousSibling().getNodeName())
                      && !_isWhitespace( pNode.getPreviousSibling(), false))
                  {
                      // This node /has/ a previous sibling which is not a block mode, and
                      // which is not pure space.  Check to see if maybe it /ends/ in a space
                      Node pText = _getTextNode( pNode.getPreviousSibling(), false );
                      
                      if (null != pText)
                      {
                          // if my previous sibling is not whitespace, but the text
                          // /ends/ with whitespace I want to skip the next step
                          String text = pText.getNodeValue();
                          if (   text.isEmpty()
                              || Character.isWhitespace( text.charAt( text.length() - 1 )))
                              break;        // from the case statement
                      }
                      if (_gnLineLen >= _gnWrap)
                      {
                          _newLine();
                      }
                      else if (0 < _gnLineLen)
                      {
                          _writeTracked( " ", 0 );
                      }
                  }
                  else if (   null == pNode.getPreviousSibling()
                           && (   !htmlIsBlock(  pNode.getParentNode().getNodeName())
                               || pNode !=  pNode.getParentNode().getFirstChild()))
                  {
                      // If I have no previous sibling and my parent is not a block, or
                      // I'm not the first child of my parent, output a single space.
                      if (_gnLineLen >= _gnWrap)
                      {
                          _newLine();
                      }
                      else
                      {
                          _writeTracked( " ", 0 );
                      }
                  }
              }
              break;
            case Node.DOCUMENT_NODE:
//              // print out my own DTD, and ignore the documents DTD and PIs
//              _out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" );
//              _out.write(   "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
//                         + "   \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" );
              indent -= _step;
              break;
            case Node.PROCESSING_INSTRUCTION_NODE:
            /*
              if (0 == strncmp( pNode.getNodeName(), "xml", 3))
              {
                  if (   _gnLineLen <= indent 
                      && !_lastWasWhite )
                  {
                      _writeWrapped( " ", indent );
                  }
                  _out.write( "<?" + pNode.getNodeName() + " " );
                  _out.write( pNode.getNodeValue());
                  if ( pNode.hasAttributes())
                  {
                      _printAttrList( pNode );
                  }
                  _out.write( "?>\n");
              }
            */
                break;
            case Node.DOCUMENT_TYPE_NODE:
            /*
              out.write( "<!DOCTYPE %s", pNode.getNodeName());
              if (null != (pValue = getAttribute( pNode, "SYSTEM" )))
                  out.write( " SYSTEM \"%s\"", pValue );
              if (null != (pValue = getAttribute( pNode, "PUBLIC" )))
                  out.write( " PUBLIC \"%s\"", pValue );
              out.write( " >\n" );
            */
            
                break;
            case Node.COMMENT_NODE:
                _newLine();
                _out.write( "<!-- " );
                _out.write( pNode.getNodeValue());
                _out.write( " -->\n" );
                break;
            case Node.CDATA_SECTION_NODE:
                _newLine();
                _out.write( "//<![CDATA[" );
                _out.write( pNode.getNodeValue());
                _out.write( "\n//]]>\n" );
                break;
            case Node.ENTITY_NODE:
                _writeTracked( pNode.getNodeName(), 0);
                break;
            case Node.ENTITY_REFERENCE_NODE:
                 _out.write( '&' );
                _writeTracked( pNode.getNodeName(), 0);
                _out.write( ';' );
                _gnLineLen += 2;
                break;
            default:
                _out.write( '<' );
                _writeTracked( pNode.getNodeName(), 0);
                _out.write( '>' );
                _gnLineLen += 2;
        }
        
        // Process all the child nodes.
        for (pChild = pNode.getFirstChild();
           null != pChild;
           pChild = pChild.getNextSibling())
        {
            int newIndent = indent + (isBlock ? _step : 0);
            needsSpace = printNode( pChild, newIndent, preserveSpace );
            if (   0 != indent + _step 
                || pChild.getNodeType() != Node.COMMENT_NODE)
            {
                if (needsSpace && null != pChild.getNextSibling())
                {
                    // there is unprinted whitespace. and a sibling node. 
                    // print a single space before continuing
                    if (!_lastWasWhite && !_isWhitespace( pChild.getNextSibling(), false ))
                    {
                        if (_gnLineLen < _gnWrap)
                        {
                            writeWrapped( " ", indent );
                        }
                        else
                        {
                            _startLine( newIndent );
                        }
                    }
                }
            }
        }
        if (   pNode.getNodeType() == Node.ELEMENT_NODE
            && !htmlIsEmpty( pNode.getNodeName() ))
        {
            if (   "style".equalsIgnoreCase( nodeName )
                || "pre".equalsIgnoreCase( nodeName))
                preserveSpace--;

            // Don't start a new line before end tags unless there are children
            // and we're not already at the start of a new line.
            if (pNode.hasChildNodes() && indent != _gnLineLen)
            {
                if (isBlock && !isSemiBlock)
                {
                    // if I am a true block (not a semi-block),
                    // then start a new line before the end tag
                    _startLine( indent );
                    needsSpace = false;
                }
                else if (isSemiBlock && _gnLineLen <= indent)
                {
                    // on a new line, but haven't been indented.
                    _startLine( indent );
                }
            }
            if (   !(   (   "span".equalsIgnoreCase( nodeName )
                         || "a".equalsIgnoreCase( nodeName ))
                     && !pNode.hasChildNodes()))
            {
                _out.write( "</" );
                _out.write( nodeName );
                _gnLineLen += nodeName.length();
                _out.write( '>' );
                _gnLineLen += 3;
            }
            // Be sure not to wrap after inline elements!
            if (isBlock) // includes semi-blocks
            {
                // Note: we're starting a new line, but we're not indenting;
                // whatever node follows us needs to worry about that.
                _newLine();
                needsSpace = false;
            }
        }
        _out.flush();
        return needsSpace;
    }
    
    
    /**
     * Pretty print the internal DOM document to an output stream. Let the caller
     * close the OutputStream
     * 
     * @param fout The target output stream
     * @param step The number of spaces to indent for each new block.
     * @throws IOException forward this to the caller
     */
    public void print( OutputStream fout, int step, boolean ascii ) 
            throws IOException // DOMException,
    {
        _out = new BufferedWriter( new OutputStreamWriter( fout, StandardCharsets.UTF_8 ));
//        _out.write( "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" );
        _step = step;
        _asciiOnly = ascii;
        printNode( _baseDoc, 0, 0 );
    }
    
    
    /**
     * A place to fix XML files (this object will always be valid XML) into valid HTML.
     */
    public void tidy()
    {
        DOMIterator iter = new DOMIterator( _baseDoc.getDocumentElement() );
        while (iter.hasNext())
        {
            Node node = iter.next();
            
            if (Node.ELEMENT_NODE == node.getNodeType())
            {
                // if the node is inline, move leading and trailing spaces out of the node.
                if (htmlIsInline(node.getNodeName()))
                {
                    Node child = _getTextNode( node, true );
                    if (null != child)
                    {
                        String textValue = child.getNodeValue();
                        StringBuilder normalized = new StringBuilder( textValue.length());
                        for (int i = 0; i < textValue.length(); i++)
                        {
                            char c = textValue.charAt( i );
                            if (Character.isWhitespace( c ))
                            {
                                normalized.append( c );
                            }
                            else
                                break;
                        }
                        // How many spaces before text begins?
                        int l = normalized.length();
                        if (0 < l)
                        {
                            // some. move them to a new node.
                            textValue = textValue.substring( l );
                            child.setTextContent( textValue );
                            child = _baseDoc.createTextNode( normalized.toString() );
                            node.getParentNode().insertBefore( child, node );
                        }
                    }
                }
                // If an anchor has an id, but no other attribute, change it to a span.
                if (   "span".equalsIgnoreCase( node.getNodeName() )
                    || "a".equalsIgnoreCase( node.getNodeName() ))
                {
                    Node parent;
                    NamedNodeMap attrs = node.getAttributes();
                    if (    1 == attrs.getLength()
                        || (2 == attrs.getLength() && null != attrs.getNamedItem( "xmlns" )))
                    {
                        // just one attribute. is it an id? 
                        Node id = attrs.getNamedItem( "id" );
                        if (id != null)
                        {
                            // Only one attribute, and it's an id. If the node is an anchor, change it to a span.
                            if ("a".equalsIgnoreCase( node.getNodeName() ))
                            {
                                Node xmlns = attrs.getNamedItem( "xmlns" );
                                _baseDoc.renameNode( node, node.getNamespaceURI(), "span");
                                if (null != xmlns)
                                    attrs.removeNamedItem( "xmlns" );
                            }
                            // Am I childless?
                            parent = node.getFirstChild();
                            if (null == parent)
                            {
                                parent = node.getParentNode();
                                // Yes, see if I can merge my id with the parent.
                                if (   Node.ELEMENT_NODE == parent.getNodeType()
                                    && node == parent.getFirstChild())
                                {
                                    // I am the first child of my owner node. If it is an element node,
                                    // and if it does not have an id attribute, move
                                    // my id attribute to it and delete the span node.
                                    NamedNodeMap sib_attribs = parent.getAttributes();
                                    Node parent_id = sib_attribs.getNamedItem( "id" );
                                    if (null == parent_id)
                                    {
                                        Node id_node = attrs.removeNamedItem( "id" );
                                        parent.getAttributes().setNamedItem( id_node );
                                        if (null != attrs.getNamedItem( "xmlns" ))
                                            attrs.removeNamedItem( "xmlns" );
                                        // empty span or anchor will be removed in the following clause...
                                    }
                                }
                            }
                        }
                    }
                    if (0 == attrs.getLength())
                    {
                        // No attributes, just remove the node
                        iter.previous();
                        parent = node.getParentNode();
                        // spans without attributes need to be killed.
                        while (node.hasChildNodes())
                        {
                            // move all of the nodes children to the parent
                            Node child = node.getFirstChild();
                            parent.insertBefore( child, node );
                        }
                        parent.removeChild( node );
                    }
                }
                // Sometimes an XSLT transformation will convert a block element to an <li>
                // without constructing a containing list element. If this node is an <li>,
                // and its parent is not an <ol> or <ul>, create a new <ul> node and move this
                // node and all it's siblings to the new list. Then move through the new node
                // backwards moving children up again until an <li> is encountered.
                else if (   "li".equalsIgnoreCase( node.getNodeName() )
                         && !"ol".equalsIgnoreCase( node.getParentNode().getNodeName() )
                         && !"ul".equalsIgnoreCase( node.getParentNode().getNodeName() )
                   )
                {
                    Element ul = _baseDoc.createElement( "ul" );
                    Node parent = node.getParentNode();
                    parent.insertBefore( ul, node );
                    while (null != node)
                    {
                        ul.appendChild( node );
                        node = ul.getNextSibling();
                    }
                    // node is null - use this to add back in reverse order
                    while(   null != ul.getLastChild() 
                          && !"li".equalsIgnoreCase( ul.getLastChild().getNodeName() ))
                    {
                        Node child = ul.getLastChild();
                        parent.insertBefore( child, node );
                        node = child;
                    }
                }
            }
        }
    }
    
    
    public static final String entities
    /* Latin 1 characters */
    = "<!ENTITY nbsp   \"&#160;\">" 
    + "<!ENTITY iexcl  \"&#161;\">" // <!--inverted exclamation mark, U+00A1 -->
    + "<!ENTITY cent   \"&#162;\"> "// " // <!--cent sign, U+00A2 -->
    + "<!ENTITY pound  \"&#163;\">" // <!--pound sign, U+00A3 -->
    + "<!ENTITY curren \"&#164;\">" // <!--currency sign, U+00A4 -->
    + "<!ENTITY yen    \"&#165;\">" // <!--yen sign = yuan sign, U+00A5 -->
    + "<!ENTITY brvbar \"&#166;\">" // <!--broken bar = broken vertical bar, U+00A6 -->
    + "<!ENTITY sect   \"&#167;\">" // <!--section sign, U+00A7 -->
    + "<!ENTITY uml    \"&#168;\">" // <!--diaeresis = spacing diaeresis, U+00A8 -->
    + "<!ENTITY copy   \"&#169;\">" // <!--copyright sign, U+00A9 -->
    + "<!ENTITY ordf   \"&#170;\">" // <!--feminine ordinal indicator, U+00AA -->
    + "<!ENTITY laquo  \"&#171;\">" // <!--left-pointing double angle quotation mark = left
                                    // pointing guillemet, U+00AB -->
    + "<!ENTITY not    \"&#172;\">" // <!--not sign = angled dash, U+00AC -->
    + "<!ENTITY shy    \"&#173;\">" // <!--soft hyphen = discretionary hyphen, U+00AD -->
    + "<!ENTITY reg    \"&#174;\">" // <!--registered sign = registered trade mark sign, U+00AE -->
    + "<!ENTITY macr   \"&#175;\">" // <!--macron = spacing macron = overline = APL overbar,
                                    // U+00AF ISOdia -->
    + "<!ENTITY deg    \"&#176;\">" // <!--degree sign, U+00B0 -->
    + "<!ENTITY plusmn \"&#177;\">" // <!--plus-minus sign = plus-or-minus sign, U+00B1 -->
    + "<!ENTITY sup2   \"&#178;\">" // <!--superscript two = superscript digit two =
                                    // squared, U+00B2 -->
    + "<!ENTITY sup3   \"&#179;\">" // <!--superscript three = superscript digit three =
                                    // cubed, U+00B3 -->
    + "<!ENTITY acute  \"&#180;\">" // <!--acute accent = spacing acute, U+00B4 ISOdia -->
    + "<!ENTITY micro  \"&#181;\">" // <!--micro sign, U+00B5 -->
    + "<!ENTITY para   \"&#182;\">" // <!--pilcrow sign = paragraph sign, U+00B6 -->
    + "<!ENTITY middot \"&#183;\">" // <!--middle dot = Georgian comma = Greek middle dot, U+00B7 -->
    + "<!ENTITY cedil  \"&#184;\">" // <!--cedilla = spacing cedilla, U+00B8 ISOdia -->
    + "<!ENTITY sup1   \"&#185;\">" // <!--superscript one = superscript digit one, U+00B9 -->
    + "<!ENTITY ordm   \"&#186;\">" // <!--masculine ordinal indicator, U+00BA -->
    + "<!ENTITY raquo  \"&#187;\">" // <!--right-pointing double angle quotation mark =
                                    // right pointing guillemet, U+00BB -->
    + "<!ENTITY frac14 \"&#188;\">" // <!--vulgar fraction one quarter = fraction one
                                    // quarter, U+00BC -->
    + "<!ENTITY frac12 \"&#189;\">" // <!--vulgar fraction one half = fraction one half U+00BD -->
    + "<!ENTITY frac34 \"&#190;\">" // <!--vulgar fraction three quarters = fraction three
                                    // quarters, U+00BE -->
    + "<!ENTITY iquest \"&#191;\">" // <!--inverted question mark = turned question mark U+00BF -->
    + "<!ENTITY Agrave \"&#192;\">" // <!--latin capital letter A with grave = latin capital
                                    // letter A grave, U+00C0 -->
    + "<!ENTITY Aacute \"&#193;\">" // <!--latin capital letter A with acute, U+00C1 -->
    + "<!ENTITY Acirc  \"&#194;\">" // <!--latin capital letter A with circumflex, U+00C2 -->
    + "<!ENTITY Atilde \"&#195;\">" // <!--latin capital letter A with tilde, U+00C3 -->
    + "<!ENTITY Auml   \"&#196;\">" // <!--latin capital letter A with diaeresis, U+00C4 -->
    + "<!ENTITY Aring  \"&#197;\">" // <!--latin capital letter A with ring above = latin
                                    // capital letter A ring, U+00C5 -->
    + "<!ENTITY AElig  \"&#198;\">" // <!--latin capital letter AE = latin capital ligature
                                    // AE, U+00C6 -->
    + "<!ENTITY Ccedil \"&#199;\">" // <!--latin capital letter C with cedilla, U+00C7 -->
    + "<!ENTITY Egrave \"&#200;\">" // <!--latin capital letter E with grave, U+00C8 -->
    + "<!ENTITY Eacute \"&#201;\">" // <!--latin capital letter E with acute, U+00C9 -->
    + "<!ENTITY Ecirc  \"&#202;\">" // <!--latin capital letter E with circumflex, U+00CA -->
    + "<!ENTITY Euml   \"&#203;\">" // <!--latin capital letter E with diaeresis, U+00CB -->
    + "<!ENTITY Igrave \"&#204;\">" // <!--latin capital letter I with grave, U+00CC -->
    + "<!ENTITY Iacute \"&#205;\">" // <!--latin capital letter I with acute, U+00CD -->
    + "<!ENTITY Icirc  \"&#206;\">" // <!--latin capital letter I with circumflex, U+00CE -->
    + "<!ENTITY Iuml   \"&#207;\">" // <!--latin capital letter I with diaeresis, U+00C -->
    + "<!ENTITY ETH    \"&#208;\">" // <!--latin capital letter ETH, U+00D0 -->
    + "<!ENTITY Ntilde \"&#209;\">" // <!--latin capital letter N with tilde, U+00D1 -->
    + "<!ENTITY Ograve \"&#210;\">" // <!--latin capital letter O with grave, U+00D2 -->
    + "<!ENTITY Oacute \"&#211;\">" // <!--latin capital letter O with acute, U+00D -->
    + "<!ENTITY Ocirc  \"&#212;\">" // <!--latin capital letter O with circumflex, U+00D -->
    + "<!ENTITY Otilde \"&#213;\">" // <!--latin capital letter O with tilde, U+00D5 -->
    + "<!ENTITY Ouml   \"&#214;\">" // <!--latin capital letter O with diaeresis, U+00D6 -->
    + "<!ENTITY times  \"&#215;\">" // <!--multiplication sign, U+00D7 -->
    + "<!ENTITY Oslash \"&#216;\">" // <!--latin capital letter O with stroke = latin
                                    // capital letter O slash, U+00D8 >
    + "<!ENTITY Ugrave \"&#217;\">" // <!--latin capital letter U with grave, U+00D9 -->
    + "<!ENTITY Uacute \"&#218;\">" // <!--latin capital letter U with acute, U+00DA -->
    + "<!ENTITY Ucirc  \"&#219;\">" // <!--latin capital letter U with circumflex, U+00DB -->
    + "<!ENTITY Uuml   \"&#220;\">" // <!--latin capital letter U with diaeresis, U+00DC -->
    + "<!ENTITY Yacute \"&#221;\">" // <!--latin capital letter Y with acute, U+00DD -->
    + "<!ENTITY THORN  \"&#222;\">" // <!--latin capital letter THORN, U+00DE -->
    + "<!ENTITY szlig  \"&#223;\">" // <!--latin small letter sharp s = ess-zed, U+00DF -->
    + "<!ENTITY agrave \"&#224;\">" // <!--latin small letter a with grave = latin small
                                    // letter a grave, U+00E0 -->
    + "<!ENTITY aacute \"&#225;\">" // <!--latin small letter a with acute, U+00E1 -->
    + "<!ENTITY acirc  \"&#226;\">" // <!--latin small letter a with circumflex, U+00E2 -->
    + "<!ENTITY atilde \"&#227;\">" // <!--latin small letter a with tilde, U+00E3 -->
    + "<!ENTITY auml   \"&#228;\">" // <!--latin small letter a with diaeresis, U+00E4 -->
    + "<!ENTITY aring  \"&#229;\">" // <!--latin small letter a with ring above = latin
                                    // small letter a ring, U+00E5 -->
    + "<!ENTITY aelig  \"&#230;\">" // <!--latin small letter ae = latin small ligature ae U+00E6 -->
    + "<!ENTITY ccedil \"&#231;\">" // <!--latin small letter c with cedilla, U+00E7 -->
    + "<!ENTITY egrave \"&#232;\">" // <!--latin small letter e with grave, U+00E8 -->
    + "<!ENTITY eacute \"&#233;\">" // <!--latin small letter e with acute, U+00E -->
    + "<!ENTITY ecirc  \"&#234;\">" // <!--latin small letter e with circumflex, U+00EA -->
    + "<!ENTITY euml   \"&#235;\">" // <!--latin small letter e with diaeresis, U+00EB -->
    + "<!ENTITY igrave \"&#236;\">" // <!--latin small letter i with grave, U+00EC -->
    + "<!ENTITY iacute \"&#237;\">" // <!--latin small letter i with acute, U+00ED -->
    + "<!ENTITY icirc  \"&#238;\">" // <!--latin small letter i with circumflex, U+00EE -->
    + "<!ENTITY iuml   \"&#239;\">" // <!--latin small letter i with diaeresis, U+00EF -->
    + "<!ENTITY eth    \"&#240;\">" // <!--latin small letter eth, U+00F0 -->
    + "<!ENTITY ntilde \"&#241;\">" // <!--latin small letter n with tilde, U+00F1 -->
    + "<!ENTITY ograve \"&#242;\">" // <!--latin small letter o with grave, U+00F2 -->
    + "<!ENTITY oacute \"&#243;\">" // <!--latin small letter o with acute, U+00F3 -->
    + "<!ENTITY ocirc  \"&#244;\">" // <!--latin small letter o with circumflex, U+00F4 -->
    + "<!ENTITY otilde \"&#245;\">" // <!--latin small letter o with tilde, U+00F5 -->
    + "<!ENTITY ouml   \"&#246;\">" // <!--latin small letter o with diaeresis, U+00F6 -->
    + "<!ENTITY divide \"&#247;\">" // <!--division sign, U+00F7 -->
    + "<!ENTITY oslash \"&#248;\">" // <!--latin small letter o with stroke, = latin small
                                    // letter o slash, U+00F8 -->
    + "<!ENTITY ugrave \"&#249;\">" // <!--latin small letter u with grave, U+00F9 -->
    + "<!ENTITY uacute \"&#250;\">" // <!--latin small letter u with acute, U+00FA -->
    + "<!ENTITY ucirc  \"&#251;\">" // <!--latin small letter u with circumflex, U+00FB -->
    + "<!ENTITY uuml   \"&#252;\">" // <!--latin small letter u with diaeresis, U+00FC -->
    + "<!ENTITY yacute \"&#253;\">" // <!--latin small letter y with acute, U+00FD -->
    + "<!ENTITY thorn  \"&#254;\">" // <!--latin small letter thorn, U+00FE -->
    + "<!ENTITY yuml   \"&#255;\">" // <!--latin small letter y with diaeresis, U+00FF
    // -->
    // <!-- C0 Controls and Basic Latin -->
    + "<!ENTITY quot    \"&#34;\">"     // <!-- quotation mark, U+0022 -->
    + "<!ENTITY amp     \"&#38;#38;\">" // <!-- ampersand, U+0026 -->
    + "<!ENTITY lt      \"&#38;#60;\">" // <!-- less-than sign, U+003C -->
    + "<!ENTITY gt      \"&#62;\">"     // <!-- greater-than sign, U+003E -->
    + "<!ENTITY apos     \"&#39;\">"    // <!-- apostrophe = APL quote, U+0027 -->

    // <!-- Latin Extended-A -->
    + "<!ENTITY OElig   \"&#338;\">"    // <!-- latin capital ligature OE, U+0152 -->
    + "<!ENTITY oelig   \"&#339;\">"    // <!-- latin small ligature oe, U+0153 -->
                                        // <!-- ligature is a misnomer, this is a separate 
                                        // character in some languages -->
    + "<!ENTITY Scaron  \"&#352;\">" // <!-- latin capital letter S with caron, U+0160 -->
    + "<!ENTITY scaron  \"&#353;\">" // <!-- latin small letter s with caron, U+0161 -->
    + "<!ENTITY Yuml    \"&#376;\">" // <!-- latin capital letter Y with diaeresis, U+017 -->

    // <!-- Spacing Modifier Letters -->
    + "<!ENTITY circ    \"&#710;\">" // <!-- modifier letter circumflex accent, U+02C6
    // -->
    + "<!ENTITY tilde   \"&#732;\">" // <!-- small tilde, U+02DC ISOdia -->

    // <!-- General Punctuation -->
    + "<!ENTITY ensp    \"&#8194;\">" // <!-- en space, U+2002 -->
    + "<!ENTITY emsp    \"&#8195;\">" // <!-- em space, U+2003 -->
    + "<!ENTITY thinsp  \"&#8201;\">" // <!-- thin space, U+2009 -->
    + "<!ENTITY zwnj    \"&#8204;\">" // <!-- zero width non-joiner, U+200C NEW RFC 2070 -->
    + "<!ENTITY zwj     \"&#8205;\">" // <!-- zero width joiner, U+200D NEW RFC 2070 -->
    + "<!ENTITY lrm     \"&#8206;\">" // <!-- left-to-right mark, U+200E NEW RFC 2070 -->
    + "<!ENTITY rlm     \"&#8207;\">" // <!-- right-to-left mark, U+200F NEW RFC 2070 -->
    + "<!ENTITY ndash   \"&#8211;\">" // <!-- en dash, U+2013 -->
    + "<!ENTITY mdash   \"&#8212;\">" // <!-- em dash, U+2014 -->
    + "<!ENTITY lsquo   \"&#8216;\">" // <!-- left single quotation mark, U+2018 -->
    + "<!ENTITY rsquo   \"&#8217;\">" // <!-- right single quotation mark, U+2019 -->
    + "<!ENTITY sbquo   \"&#8218;\">" // <!-- single low-9 quotation mark, U+201A NEW -->
    + "<!ENTITY ldquo   \"&#8220;\">" // <!-- left double quotation mark, U+201C -->
    + "<!ENTITY rdquo   \"&#8221;\">" // <!-- right double quotation mark, U+201D -->
    + "<!ENTITY bdquo   \"&#8222;\">" // <!-- double low-9 quotation mark, U+201E NEW -->
    + "<!ENTITY dagger  \"&#8224;\">" // <!-- dagger, U+2020 -->
    + "<!ENTITY Dagger  \"&#8225;\">" // <!-- double dagger, U+2021 -->
    + "<!ENTITY permil  \"&#8240;\">" // <!-- per mille sign, U+2030 -->
    + "<!ENTITY lsaquo  \"&#8249;\">" // <!-- single left-pointing angle quotation mark U+2039 ISO proposed -->
                                      // <!-- lsaquo is proposed but not yet ISO standardized -->
    + "<!ENTITY rsaquo  \"&#8250;\">" // <!-- single right-pointing angle quotation mark U+203A ISO proposed -->
                                      // <!-- rsaquo is proposed but not yet ISO standardized -->

    // <!-- Currency Symbols -->
    + "<!ENTITY euro   \"&#8364;\">"  // <!-- euro sign, U+20AC NEW -->
    // <!-- Latin Extended-B -->
    + "<!ENTITY fnof     \"&#402;\">" // <!-- latin small letter f with hook = 
                                      // function = florin, U+0192 -->
    // <!-- Greek -->
    + "<!ENTITY Alpha    \"&#913;\">" // <!-- greek capital letter alpha, U+0391 -->
    + "<!ENTITY Beta     \"&#914;\">" // <!-- greek capital letter beta, U+0392 -->
    + "<!ENTITY Gamma    \"&#915;\">" // <!-- greek capital letter gamma, U+0393 ISOgrk3 -->
    + "<!ENTITY Delta    \"&#916;\">" // <!-- greek capital letter delta, U+0394 ISOgrk3 -->
    + "<!ENTITY Epsilon  \"&#917;\">" // <!-- greek capital letter epsilon, U+0395 -->
    + "<!ENTITY Zeta     \"&#918;\">" // <!-- greek capital letter zeta, U+0396 -->
    + "<!ENTITY Eta      \"&#919;\">" // <!-- greek capital letter eta, U+0397 -->
    + "<!ENTITY Theta    \"&#920;\">" // <!-- greek capital letter theta, U+0398 ISOgrk3 -->
    + "<!ENTITY Iota     \"&#921;\">" // <!-- greek capital letter iota, U+0399 -->
    + "<!ENTITY Kappa    \"&#922;\">" // <!-- greek capital letter kappa, U+039A -->
    + "<!ENTITY Lambda   \"&#923;\">" // <!-- greek capital letter lamda, U+039B ISOgrk3 -->
    + "<!ENTITY Mu       \"&#924;\">" // <!-- greek capital letter mu, U+039C -->
    + "<!ENTITY Nu       \"&#925;\">" // <!-- greek capital letter nu, U+039D -->
    + "<!ENTITY Xi       \"&#926;\">" // <!-- greek capital letter xi, U+039E ISOgrk3 -->
    + "<!ENTITY Omicron  \"&#927;\">" // <!-- greek capital letter omicron, U+039F -->
    + "<!ENTITY Pi       \"&#928;\">" // <!-- greek capital letter pi, U+03A0 ISOgrk3 -->
    + "<!ENTITY Rho      \"&#929;\">" // <!-- greek capital letter rho, U+03A1 -->
                                      // <!-- there is no Sigmaf, and no// U+03A2 character either -->
    + "<!ENTITY Sigma    \"&#931;\">" // <!-- greek capital letter sigma, U+03A3 ISOgrk3 -->
    + "<!ENTITY Tau      \"&#932;\">" // <!-- greek capital letter tau, U+03A4 -->
    + "<!ENTITY Upsilon  \"&#933;\">" // <!-- greek capital letter upsilon, U+03A5 ISOgrk3 -->
    + "<!ENTITY Phi      \"&#934;\">" // <!-- greek capital letter phi, U+03A6 ISOgrk3 -->
    + "<!ENTITY Chi      \"&#935;\">" // <!-- greek capital letter chi, U+03A7 -->
    + "<!ENTITY Psi      \"&#936;\">" // <!-- greek capital letter psi, U+03A8 ISOgrk3 -->
    + "<!ENTITY Omega    \"&#937;\">" // <!-- greek capital letter omega, U+03A9 ISOgrk3 -->
    + "<!ENTITY alpha    \"&#945;\">" // <!-- greek small letter alpha, U+03B1 ISOgrk3 -->
    + "<!ENTITY beta     \"&#946;\">" // <!-- greek small letter beta, U+03B2 ISOgrk3 -->
    + "<!ENTITY gamma    \"&#947;\">" // <!-- greek small letter gamma, U+03B3 ISOgrk3 -->
    + "<!ENTITY delta    \"&#948;\">" // <!-- greek small letter delta, U+03B4 ISOgrk3 -->
    + "<!ENTITY epsilon  \"&#949;\">" // <!-- greek small letter epsilon, U+03B5 ISOgrk3 -->
    + "<!ENTITY zeta     \"&#950;\">" // <!-- greek small letter zeta, U+03B6 ISOgrk3 -->
    + "<!ENTITY eta      \"&#951;\">" // <!-- greek small letter eta, U+03B7 ISOgrk3 -->
    + "<!ENTITY theta    \"&#952;\">" // <!-- greek small letter theta, U+03B8 ISOgrk3 -->
    + "<!ENTITY iota     \"&#953;\">" // <!-- greek small letter iota, U+03B9 ISOgrk3 -->
    + "<!ENTITY kappa    \"&#954;\">" // <!-- greek small letter kappa, U+03BA ISOgrk3 -->
    + "<!ENTITY lambda   \"&#955;\">" // <!-- greek small letter lamda, U+03BB ISOgrk3 -->
    + "<!ENTITY mu       \"&#956;\">" // <!-- greek small letter mu, U+03BC ISOgrk3 -->
    + "<!ENTITY nu       \"&#957;\">" // <!-- greek small letter nu, U+03BD ISOgrk3 -->
    + "<!ENTITY xi       \"&#958;\">" // <!-- greek small letter xi, U+03BE ISOgrk3 -->
    + "<!ENTITY omicron  \"&#959;\">" // <!-- greek small letter omicron, U+03BF NEW -->
    + "<!ENTITY pi       \"&#960;\">" // <!-- greek small letter pi, U+03C0 ISOgrk3 -->
    + "<!ENTITY rho      \"&#961;\">" // <!-- greek small letter rho, U+03C1 ISOgrk3 -->
    + "<!ENTITY sigmaf   \"&#962;\">" // <!-- greek small letter final sigma, U+03C2 ISOgrk3 -->
    + "<!ENTITY sigma    \"&#963;\">" // <!-- greek small letter sigma, U+03C3 ISOgrk3 -->
    + "<!ENTITY tau      \"&#964;\">" // <!-- greek small letter tau, U+03C4 ISOgrk3 -->
    + "<!ENTITY upsilon  \"&#965;\">" // <!-- greek small letter upsilon, U+03C5 ISOgrk3 -->
    + "<!ENTITY phi      \"&#966;\">" // <!-- greek small letter phi, U+03C6 ISOgrk3 -->
    + "<!ENTITY chi      \"&#967;\">" // <!-- greek small letter chi, U+03C7 ISOgrk3 -->
    + "<!ENTITY psi      \"&#968;\">" // <!-- greek small letter psi, U+03C8 ISOgrk3 -->
    + "<!ENTITY omega    \"&#969;\">" // <!-- greek small letter omega, U+03C9 ISOgrk3 -->
    + "<!ENTITY thetasym \"&#977;\">" // <!-- greek theta symbol, U+03D1 NEW -->
    + "<!ENTITY upsih    \"&#978;\">" // <!-- greek upsilon with hook symbol, U+03D2 NEW -->
    + "<!ENTITY piv      \"&#982;\">" // <!-- greek pi symbol, U+03D6 ISOgrk3 -->

    // <!-- General Punctuation -->
    + "<!ENTITY bull     \"&#8226;\">" // <!-- bullet = black small circle, U+2022 -->
                                       // <!-- bullet is NOT the same as bullet operator, U+2219 -->
    + "<!ENTITY hellip   \"&#8230;\">" // <!-- horizontal ellipsis = three dot leader, U+2026 -->
    + "<!ENTITY prime    \"&#8242;\">" // <!-- prime = minutes = feet, U+2032 -->
    + "<!ENTITY Prime    \"&#8243;\">" // <!-- double prime = seconds = inches, U+2033
    // -->
    + "<!ENTITY oline    \"&#8254;\">" // <!-- overline = spacing overscore, U+203E NEW -->
    + "<!ENTITY frasl    \"&#8260;\">" // <!-- fraction slash, U+2044 NEW -->

    // <!-- Letterlike Symbols -->
    + "<!ENTITY weierp   \"&#8472;\">" // <!-- script capital P = power set = Weierstrass p U+2118 -->
    + "<!ENTITY image    \"&#8465;\">" // <!-- black-letter capital I = imaginary part U+2111 -->
    + "<!ENTITY real     \"&#8476;\">" // <!-- black-letter capital R = real part symbol, U+211C -->
    + "<!ENTITY trade    \"&#8482;\">" // <!-- trade mark sign, U+2122 -->
    + "<!ENTITY alefsym  \"&#8501;\">" // <!-- alef symbol = first transfinite cardinal, U+2135 NEW -->
    // <!-- alef symbol is NOT the same as hebrew letter alef,
    // U+05D0 although the same glyph could be used to depict both characters -->

    // <!-- Arrows -->
    + "<!ENTITY larr     \"&#8592;\">" // <!-- leftwards arrow, U+2190 -->
    + "<!ENTITY uarr     \"&#8593;\">" // <!-- upwards arrow, U+2191-->
    + "<!ENTITY rarr     \"&#8594;\">" // <!-- rightwards arrow, U+2192 -->
    + "<!ENTITY darr     \"&#8595;\">" // <!-- downwards arrow, U+2193 -->
    + "<!ENTITY harr     \"&#8596;\">" // <!-- left right arrow, U+2194 -->
    + "<!ENTITY crarr    \"&#8629;\">" // <!-- downwards arrow with corner leftwards =
    // carriage return, U+21B5 NEW -->
    + "<!ENTITY lArr     \"&#8656;\">" // <!-- leftwards double arrow, U+21D0 -->
    // <!-- Unicode does not say that lArr is the same as the 'is implied by' arrow
    // but also does not have any other character for that function. So lArr can
    // be used for 'is implied by' as suggests -->
    + "<!ENTITY uArr     \"&#8657;\">" // <!-- upwards double arrow, U+21D1 -->
    + "<!ENTITY rArr     \"&#8658;\">" // <!-- rightwards double arrow, U+21D2 -->
    // <!-- Unicode does not say this is the 'implies' character but does not have
    // another character with this function so rArr can be used for 'implies'
    // as suggests -->
    + "<!ENTITY dArr     \"&#8659;\">" // <!-- downwards double arrow, U+21D3 -->
    + "<!ENTITY hArr     \"&#8660;\">" // <!-- left right double arrow U+21D4 -->

    // <!-- Mathematical Operators -->
    + "<!ENTITY forall   \"&#8704;\">" // <!-- for all, U+2200 -->
    + "<!ENTITY part     \"&#8706;\">" // <!-- partial differential, U+2202 -->
    + "<!ENTITY exist    \"&#8707;\">" // <!-- there exists, U+2203 -->
    + "<!ENTITY empty    \"&#8709;\">" // <!-- empty set = null set, U+2205 -->
    + "<!ENTITY nabla    \"&#8711;\">" // <!-- nabla = backward difference, U+2207 -->
    + "<!ENTITY isin     \"&#8712;\">" // <!-- element of, U+2208 -->
    + "<!ENTITY notin    \"&#8713;\">" // <!-- not an element of, U+2209 -->
    + "<!ENTITY ni       \"&#8715;\">" // <!-- contains as member, U+220B -->
    + "<!ENTITY prod     \"&#8719;\">" // <!-- n-ary product = product sign, U+220F -->
                                       // <!-- prod is NOT the same character as U+03A0
                                       // 'greek capital letter pi' though the same glyph
                                       // might be used for both -->
    + "<!ENTITY sum      \"&#8721;\">" // <!-- n-ary summation, U+2211 -->
                                       // <!-- sum is NOT the same character as U+03A3 'greek 
                                       // capital letter sigma' though the same glyph might 
                                       // be used for both -->
    + "<!ENTITY minus    \"&#8722;\">" // <!-- minus sign, U+2212 -->
    + "<!ENTITY lowast   \"&#8727;\">" // <!-- asterisk operator, U+2217 -->
    + "<!ENTITY radic    \"&#8730;\">" // <!-- square root = radical sign, U+221A -->
    + "<!ENTITY prop     \"&#8733;\">" // <!-- proportional to, U+221D -->
    + "<!ENTITY infin    \"&#8734;\">" // <!-- infinity, U+221E -->
    + "<!ENTITY ang      \"&#8736;\">" // <!-- angle, U+2220 -->
    + "<!ENTITY and      \"&#8743;\">" // <!-- logical and = wedge, U+2227 -->
    + "<!ENTITY or       \"&#8744;\">" // <!-- logical or = vee, U+2228 -->
    + "<!ENTITY cap      \"&#8745;\">" // <!-- intersection = cap, U+2229 -->
    + "<!ENTITY cup      \"&#8746;\">" // <!-- union = cup, U+222A -->
    + "<!ENTITY int      \"&#8747;\">" // <!-- integral, U+222B -->
    + "<!ENTITY there4   \"&#8756;\">" // <!-- therefore, U+2234 -->
    + "<!ENTITY sim      \"&#8764;\">" // <!-- tilde operator = varies with = similar to U+223C -->
                                       // <!-- tilde operator is NOT the same character as 
                                       // the tilde, U+007E, although the same glyph might 
                                       // be used to represent both -->
    + "<!ENTITY cong     \"&#8773;\">" // <!-- approximately equal to, U+2245 -->
    + "<!ENTITY asymp    \"&#8776;\">" // <!-- almost equal to = asymptotic to U+2248 ISOamsr -->
    + "<!ENTITY ne       \"&#8800;\">" // <!-- not equal to, U+2260 -->
    + "<!ENTITY equiv    \"&#8801;\">" // <!-- identical to, U+2261 -->
    + "<!ENTITY le       \"&#8804;\">" // <!-- less-than or equal to, U+2264 -->
    + "<!ENTITY ge       \"&#8805;\">" // <!-- greater-than or equal to, U+2265 -->
    + "<!ENTITY sub      \"&#8834;\">" // <!-- subset of, U+2282 -->
    + "<!ENTITY sup      \"&#8835;\">" // <!-- superset of, U+2283 -->
    + "<!ENTITY nsub     \"&#8836;\">" // <!-- not a subset of, U+2284 ISOamsn -->
    + "<!ENTITY sube     \"&#8838;\">" // <!-- subset of or equal to, U+2286 -->
    + "<!ENTITY supe     \"&#8839;\">" // <!-- superset of or equal to, U+2287 -->
    + "<!ENTITY oplus    \"&#8853;\">" // <!-- circled plus = direct sum, U+2295 -->
    + "<!ENTITY otimes   \"&#8855;\">" // <!-- circled times = vector product, U+2297 -->
    + "<!ENTITY perp     \"&#8869;\">" // <!-- up tack = orthogonal to = perpendicular, U+22A5 -->
    + "<!ENTITY sdot     \"&#8901;\">" // <!-- dot operator, U+22C5 -->
    // <!-- dot operator is NOT the same character as// U+00B7 middle dot -->

    // <!-- Miscellaneous Technical -->
    + "<!ENTITY lceil    \"&#8968;\">" // <!-- left ceiling = APL upstile, U+2308 -->
    + "<!ENTITY rceil    \"&#8969;\">" // <!-- right ceiling, U+2309 -->
    + "<!ENTITY lfloor   \"&#8970;\">" // <!-- left floor = APL downstile, U+230A -->
    + "<!ENTITY rfloor   \"&#8971;\">" // <!-- right floor, U+230B -->
    + "<!ENTITY lang     \"&#9001;\">" // <!-- left-pointing angle bracket = bra U+2329 -->
    // <!-- lang is NOT the same character as// U+003C 'less than sign'
    // or U+2039 'single left-pointing angle quotation mark' -->
    + "<!ENTITY rang     \"&#9002;\">" // <!-- right-pointing angle bracket = ket, U+232A -->
                                       // <!-- rang is NOT the same character as
                                       // U+003E 'greater than sign' or U+203A
                                       // 'single right-pointing angle quotation mark' -->

    // <!-- Geometric Shapes -->
    + "<!ENTITY loz      \"&#9674;\">" // <!-- lozenge, U+25CA -->

    // <!-- Miscellaneous Symbols -->
    // <!-- black here seems to mean filled as opposed to hollow -->
    + "<!ENTITY spades   \"&#9824;\">" // <!-- black spade suit, U+2660 -->
    + "<!ENTITY clubs    \"&#9827;\">" // <!-- black club suit = shamrock, U+2663 -->
    + "<!ENTITY hearts   \"&#9829;\">" // <!-- black heart suit = valentine, U+2665 -->
    + "<!ENTITY diams    \"&#9830;\">" // <!-- black diamond suit, U+2666 -->
;
}
