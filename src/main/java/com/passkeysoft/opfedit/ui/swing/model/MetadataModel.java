package com.passkeysoft.opfedit.ui.swing.model;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.ListModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataListener;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.passkeysoft.opfedit.staticutil.XMLUtil;

public class MetadataModel extends MonitoringTableModel implements ListModel
{
    private static final String DublinCoreNS = "http://purl.org/dc/elements/1.1/";

    private static final long serialVersionUID = 1L;

    static public String[] propNames =
    { "identifier",
      "title",
      "language", // required
      "date",     // in the format defined by "Date and Time Formats" at
                  // http://www.w3.org/TR/NOTE-datetime; possesses the optional "event" attribute
      "type", "publisher", "rights", "description", "subject", "source", "format",
      "creator", "relation", "coverage" };


    static public int getPropIndex( String propName )
    {
       int i;
       for (i = 0; i < propNames.length; i++)
       {
           if (propName.equalsIgnoreCase( propNames[i] ))
               return i;
       }
       return -1;
    }

    private Element _metadata = null;
    private EventListenerList _listenerList = new EventListenerList();

    MetadataModel( EPubModel fileData, Element metadataNode )
    {
        super( fileData );
        if (null != metadataNode)
        {
            _metadata = XMLUtil.renameElement( metadataNode, "metadata" );

            // be sure that this node has the dc: and opf: namespaces defined
            if (!_metadata.hasAttribute( "xmlns:dc" ))
                _metadata.setAttribute( "xmlns:dc", DublinCoreNS );
            if (!_metadata.hasAttribute( "xmlns:opf" ))
                _metadata.setAttribute( "xmlns:opf", "http://www.idpf.org/2007/opf" );

            // Walk this tree's node, and anything that is in a <dc-metadata> node, or an
            // <x-metadata> node get moved to the parent
            NodeList deprecated = _metadata.getElementsByTagName( "dc-metadata" );
            for (int i = 0; i < deprecated.getLength(); i++)
            {
                Node container = deprecated.item( i );
                while (container.hasChildNodes())
                {
                    Node meta = container.getFirstChild();
                    _metadata.appendChild( meta );
                }
                _metadata.removeChild( container );
            }
            deprecated = _metadata.getElementsByTagName( "x-metadata" );
            for (int i = 0; i < deprecated.getLength(); i++)
            {
                Node container = deprecated.item( i );
                while (container.hasChildNodes())
                    _metadata.appendChild( container.getFirstChild() );
                _metadata.removeChild( container );
            }
            Node meta;
            for (meta = _metadata.getFirstChild(); null != meta; meta = meta.getNextSibling())
            {
                if (Node.ELEMENT_NODE == meta.getNodeType())
                {
                    metadataNode = (Element) meta;
                    if (metadataNode.hasAttribute( "xmlns:xsi" ))
                        metadataNode.removeAttribute( "xmlns:xsi" );
                    if (metadataNode.hasAttribute( "xmlns:calibre" ))
                        metadataNode.removeAttribute( "xmlns:calibre" );
                    if (metadataNode.hasAttribute( "xmlns" ))
                        metadataNode.removeAttribute( "xmlns" );
                    if (metadataNode.hasAttribute( "scheme" ))
                    {
                        metadataNode.setAttribute( "opf:scheme", metadataNode.getAttribute( "scheme" ) );
                        metadataNode.removeAttribute( "scheme" );
                    }
                    String nodeName = metadataNode.getNodeName().toLowerCase();
                    if (nodeName.startsWith( "dc:" ))
                    {
                        // make sure the node name is all lower case.
                        meta = metadataNode = XMLUtil.renameElement( metadataNode, nodeName );
                    }
                    else if (!nodeName.equals("meta") && -1 < getPropIndex( nodeName ))
                    {
                        meta = metadataNode = XMLUtil.renameElement( metadataNode, "dc:" + nodeName );
                    }
                    else if (nodeName.equals( "meta" ))
                    {
                        // if the node has a "property" attribute and a "refines" attribute,
                        // add the metadata to the element with the "refines" id, then
                        // remove the node.
                        String property = metadataNode.getAttribute( "property" );
                        if (null != property && 0 < property.length())
                        {
                            String refines = metadataNode.getAttribute( "refines" );
                            if (null != refines && 0 < refines.length())
                            {
                                while (refines.charAt( 0 ) == '#')
                                    refines = refines.substring( 1 );
                                // find the node that has the id of refines
                                Element target = XMLUtil.getElementById( _metadata, refines );
                                if (null != target)
                                {
                                    target.setAttribute( "opf:" + property, metadataNode.getTextContent() );
                                }
                            }
                            meta = metadataNode.getPreviousSibling();
                            metadataNode.getParentNode().removeChild( metadataNode );
                            if (null == meta)
                                meta = metadataNode.getParentNode().getFirstChild();
                        }
                    }
                    if ("date".equals( nodeName ) || "dc:date".equals( nodeName ))
                    {
                        // Check for proper format per http://www.w3.org/TR/NOTE-datetime
                        // if the date cannot be interpreted, replace it with today.
                        /*
                         *     Year:
                                  YYYY (eg 1997)
                               Year and month:
                                  YYYY-MM (eg 1997-07)
                               Complete date:
                                  YYYY-MM-DD (eg 1997-07-16)
                               Complete date plus hours and minutes:
                                  YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
                               Complete date plus hours, minutes and seconds:
                                  YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
                               Complete date plus hours, minutes, seconds and a decimal fraction of a second
                                  YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
                         */
                        String date = metadataNode.getTextContent();
                        SimpleDateFormat df = new SimpleDateFormat( "yyyy" ); // DateFormat.getDateInstance();
                        try
                        {
                            // if no exception, this is OK
                            df.parse( date );
                        }
                        catch( ParseException e )
                        {
                            df.applyPattern( "yyyy-MM" );
                            try
                            {
                                df.parse( date );
                            }
                            catch( ParseException e1 )
                            {
                                df.applyPattern( "yyyy-MM-dd" );
                                try
                                {
                                    df.parse( date );
                                }
                                catch( ParseException e2 )
                                {
                                    df.applyPattern( "yyyy-MM-dd'T'HH:mmZ" );
                                    try
                                    {
                                        df.parse( date );
                                    }
                                    catch( ParseException e3 )
                                    {
                                        df.applyPattern( "yyyy-MM-dd'T'HH:mm:ssZ" );
                                        try
                                        {
                                            df.parse( date );
                                        }
                                        catch( ParseException e4 )
                                        {
                                            df.applyPattern( "yyyy-MM-dd'T'HH:mm:ss.SZ" );
                                            try
                                            {
                                                df.parse( date );
                                            }
                                            catch( ParseException e5 )
                                            {
                                                df.applyPattern( "yyyy-MM-dd" );
                                                metadataNode.setTextContent( df.format( new Date() ) );
                                            }
                                         }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    public String getProperty( String prop )
    {
        String propName;
        for (int i = 0; i < getRowCount(); i++)
        {
            propName = (String) getValueAt( i, 0 );
            if (propName.equalsIgnoreCase( prop ))
                return (String) getValueAt( i, 1 );
        }
        return "";
    }

    public boolean setProperty( String propName, String propValue )
    {
        String name;
        // find the row that contains this property
        int i;
        for (i = 0; i < getRowCount(); i++)
        {
            name = (String) getValueAt( i, 0 );
            if (name.equalsIgnoreCase( propName ))
                break;
        }
        // if i is equal to the row count we have a new row, otherwise we're replacing a value.
        if (i == getRowCount())
        {
            addRow();
            setValueAt( "", i, 3 );
        }
        setValueAt( propName, i, 0 );
        setValueAt( propValue, i, 1 );
        return true;
    }

    public ArrayList<String> getAllProperties( String prop )
    {
        ArrayList<String> properties = new ArrayList<>();
        Node meta;
        for (meta = _metadata.getFirstChild();null != meta; meta = meta.getNextSibling())
        {
            if (meta.getNodeType() == Node.ELEMENT_NODE)
            {
                String name = meta.getNodeName().toLowerCase();
                if (name.startsWith( "dc:" ))
                    name = name.substring( 3 );
                if (name.equals( prop ))
                {
                    properties.add( meta.getTextContent() );
                }
            }
        }
        return properties;
    }


    Element getCreatorsElement()
    {
        return _metadata;
    }


    /**
     * Returns the value for the cell at columnIndex and rowIndex.
     *
     * @see javax.swing.table.TableModel
     */
    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        if (rowIndex < getRowCount())
        {
            Element meta = (Element) getElementAt( rowIndex );
            if (meta.getNodeName().equals( "meta" ))
            {
                switch (columnIndex)
                {
                    case 0:
                        return meta.getAttribute( "name" );
                    case 1:
                        return meta.getAttribute( "content" );
                    case 3:
                        if (0 < meta.getAttribute( "opf:scheme" ).length())
                            return meta.getAttribute( "opf:scheme" );
                        return meta.getAttribute( "scheme" );
                }
            }
            else
                switch (columnIndex)
                {
                    case 0:
                        String name = meta.getNodeName().toLowerCase();
                        if (name.startsWith( "dc:" ))
                            name = name.substring( 3 );
                        return name;
                    case 1:
                        return meta.getTextContent();
                    case 2:
                        return meta.getAttribute( "id" );
                    case 3:
                    {
                        if (meta.hasAttributes())
                        {
                            NamedNodeMap attrs = meta.getAttributes();
                            StringBuilder attributes = new StringBuilder();

                            for (int i = 0; i < attrs.getLength(); i++)
                            {
                                if (!attrs.item( i ).getNodeName().equalsIgnoreCase( "id" ))
                                {
                                    if (0 < attributes.length())
                                        attributes.append( ", " );
                                    attributes.append( attrs.item( i ).getNodeName() )
                                        .append( "=\"" )
                                        .append( attrs.item( i ).getNodeValue() )
                                        .append( "\"" );
                                }
                            }
                            return attributes.toString();
                        }
                    }
                }
        }
        return null;
    }


    private void clearAltAttributes( Element meta )
    {
        NamedNodeMap attrs = meta.getAttributes();
        for (int i = 0; i < attrs.getLength();)
        {
            Attr attr = (Attr) attrs.item( i );
            if (!attr.getName().equalsIgnoreCase( "id" )
                    && !attr.getName().equalsIgnoreCase( "name" )
                    && !attr.getName().equalsIgnoreCase( "content" ))
            {
                meta.removeAttributeNode( attr );
            }
            else
                ++i;
        }
    }


    /**
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt( Object aValue, int rowIndex, int colIndex )
    {
        Element meta;
        if (null != aValue)
        {
            String value = aValue.toString();
            meta = (Element) getElementAt( rowIndex );
            switch (colIndex)
            {
                case 0:
                    // If the value is new, check it against the list of property names.
                    // If the value matches a property name, rename the element to
                    // match the property name; if it doesn't match, change the
                    // element name to "meta" and set the "name" attribute to match the
                    // new value;

                    int index = getPropIndex( value );
                    if (0 > index)
                    {
                        // Everything that is not in the DC properties list is
                        // a <meta> element where the property name is encapsulated
                        // in the name attribute
                        if (!meta.getNodeName().equals( "meta" ))
                        {
                            // if the node is not yet a <meta> element, change it.
                            XMLUtil.renameElement( meta, "meta" );
//                            _metadata.getOwnerDocument().renameNode( meta, null, "meta" );
                        }
                        meta.setAttribute( "name", value );
                    }
                    else
                    {
                        String name = meta.getNodeName().toLowerCase();
                        if (name.startsWith( "dc:" ))
                            name = name.substring( 3 );
                        if (!name.equals( propNames[index] ))
                        {
                            int current = getPropIndex( name );

                            if (0 <= current && 3 > current)    // happy to change "meta" elements
                            {
                                // There must be at least one instance of each of the first
                                // three required attributes. Check to make sure that
                                // changing this element type will not violate this rule.
                                Node child;
                                for (child = _metadata.getFirstChild(); null != child; child = child.getNextSibling())
                                {
                                    if (!meta.equals( child ) && Node.ELEMENT_NODE == child.getNodeType())
                                    {
                                        name = child.getNodeName().toLowerCase();
                                        if (name.startsWith( "dc:" ))
                                            name = name.substring( 3 );
                                        int pos = getPropIndex( name );
                                        // if there is another element of this same kind, we're OK
                                        if (pos == current)
                                            break;
                                    }
                                }
                                if (null == child)
                                    break;
                            }
                            // named element which must be renamed.
                            if (meta.getNodeName().equals( "meta" ))
                            {
                                String content = meta.getAttribute( "content" );
                                meta.setTextContent( content );
                                meta.removeAttribute( "name" );
                                meta.removeAttribute( "content" );
                                meta.removeAttribute( "opf:scheme" );
                            }
                            XMLUtil.renameElement( meta, "dc:" + propNames[index] );
//                            _metadata.getOwnerDocument().renameNode( meta, DublinCoreNS, "dc:" + propNames[index] );
                        }
                    }
                    break;
                case 1:
                    if (!meta.getNodeName().equals( "meta" ))
                        meta.setTextContent( value );
                    else
                        meta.setAttribute( "content", value );
                    break;
                case 2:
                    if (0 != value.length())
                        meta.setAttribute( "id", value );
                    break;
                case 3:
                    if (0 < value.length())
                    {
                        // remove all alternate attributes then replace them.
                        clearAltAttributes( meta );

                        // deal with multiple attributes.
                        String[] newAttrs = value.split(" *, *");
                        for (String newAttr : newAttrs)
                        {
                            // TODO: deal with malformed metadata pairs
                            String[] values = newAttr.split( " *= *" );
                            if (1 < values.length)
                            {
                                String[] temp = values[1].split( "\"" );
                                meta.setAttribute( values[0], 0 < temp[0].length() ? temp[0] : temp[1] );
                            }
                        }
                    }
                    else if (!meta.getNodeName().equalsIgnoreCase( "identifier" ))
                    {
                        // remove all attributes except name, content and id
                        clearAltAttributes( meta );
                    }
                    break;
            }
        }
        fireTableRowsUpdated( rowIndex, rowIndex );
    }


    /*
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName( int column )
    {
        switch (column)
        {
            case 0:
                return "Property";
            case 1:
                return "Value";
            case 2:
                return "Id (optional)";
            case 3:
                return "Other Attributes (optional)";
        }
        return null;
    }


    /*
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int rowIndex, int columnIndext)
     */
    @Override
    public boolean isCellEditable( int rowIndex, int columnIndex )
    {
        return true;
    }


    /*
     * (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return 4;
    }


    /*
     * (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        return getSize();
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
     */
    @Override
    public void addListDataListener( ListDataListener l )
    {
        _listenerList.add( ListDataListener.class, l );
    }


    /*
     * @see javax.swing.ListModel#getElementAt(int)
     */
    @Override
    public Object getElementAt( int index )
    {
        int rowCount = 0;
        Node node;
        for (node = _metadata.getFirstChild();null != node; node = node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (!ContributorModel.isContributor( node ))
                {
                    if (index == rowCount)
                        return node;
                    ++rowCount;
                }
            }
        }
        return null;
    }


    /*
     * @see javax.swing.ListModel#getSize()
     */
    @Override
    public int getSize()
    {
        int rowCount = 0;
        Node node;
        for (node = _metadata.getFirstChild();null != node; node = node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (!ContributorModel.isContributor( node ))
                {
                    ++rowCount;
                }
            }
        }
        return rowCount;
    }


    /*
     * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
     */
    @Override
    public void removeListDataListener( ListDataListener l )
    {
        _listenerList.remove( ListDataListener.class, l );
    }


    public int addRow()
    {
        // Adding a new row.
        Element meta = _metadata.getOwnerDocument().createElement( "meta" );
        meta.setAttribute( "name", "metadata name" );
        meta.setAttribute( "content", "metadata value" );
        meta.setAttribute( "opf:scheme", "metadata scheme" );
        _metadata.appendChild( meta );
        int rowIndex = getSize();
        fireTableRowsInserted( rowIndex, rowIndex );
        return rowIndex;
    }


    public void removeRow( int index )
    {
        int rowCount = 0;
        Node node;
        for (node = _metadata.getFirstChild();null != node; node = node.getNextSibling())
        {
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                if (!ContributorModel.isContributor( node ))
                {
                    if (index == rowCount)
                    {
                        _metadata.removeChild( node );
                        fireTableRowsDeleted( index, index );
                    }
                    ++rowCount;
                }
            }
        }
    }


    public int moveRowUp( int rowIndex )
    {
        int numRows = getSize();
        if (rowIndex < numRows && rowIndex > 0) // 0 is a valid index, but one can't move the first row up
        {
            Element src = (Element) getElementAt( rowIndex );
            Element dest = (Element) getElementAt( rowIndex - 1 );
            _metadata.removeChild( src );
            _metadata.insertBefore( src, dest );
            fireTableRowsUpdated( rowIndex - 1, rowIndex );
            rowIndex--;
        }
        return rowIndex;
    }


    public int moveRowDn( int rowIndex )
    {
        int numRows = getSize();
        if (rowIndex < numRows - 1)     // Can't move the last row down
        {
            Element src = (Element) getElementAt( rowIndex );
            Element dest = (Element) getElementAt( rowIndex + 1 );
            _metadata.removeChild( src );
            _metadata.insertBefore( src, dest.getNextSibling());
            fireTableRowsUpdated( rowIndex, rowIndex + 1 );
            rowIndex++;
        }
        return rowIndex;
    }
}
