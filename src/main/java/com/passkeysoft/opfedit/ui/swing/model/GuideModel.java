package com.passkeysoft.opfedit.ui.swing.model;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GuideModel extends MonitoringTableModel
{
    private static final long serialVersionUID = 2751181048004253681L;
    private Element _guideElement;

    public static String[] types =
    {
         "cover",       // the book cover(s), jacket information, etc.
         "title-page",  // page with possibly title, author, publisher, and other metadata
         "toc",         // table of contents
         "index",       // back-of-book style index
         "glossary",
         "acknowledgements",
         "bibliography",
         "colophon",
         "copyright-page",
         "dedication",
         "epigraph",
         "foreword",
         "loi",         // list of illustrations, //
         "lot",         // list of tables
         "notes",
         "preface",
         "text",        // First "real" page of content (e.g. "Chapter 1")
         "other"
    };


    GuideModel( EPubModel fileData, Element guideNode )
    {
        super(fileData );
        if (null != guideNode)
            _guideElement = guideNode;
        else
        {
            Document doc = fileData.getOpfDom();
            _guideElement = doc.createElement( "guide" );
            doc.getDocumentElement().appendChild( _guideElement );
        }
    }


    public void addGuideReference( String type, String title, String href )
    {

        Element ref = _guideElement.getOwnerDocument().createElement( "reference" );
        ref.setAttribute( "type", type );
        ref.setAttribute( "title",  title );
        ref.setAttribute( "href",  href );
        _guideElement.appendChild( ref );
        fireTableDataChanged();
    }


    public String getHrefByType( String type )
    {
        NodeList items = _guideElement.getElementsByTagName( "reference" );
        for (int i = 0; i < items.getLength(); i++)
        {
            Element item = (Element) items.item( i );
            String attr = item.getAttribute( "type" );
            if (attr.equalsIgnoreCase( type ))
            {
                return item.getAttribute( "href" );
            }
        }
        return "";
    }


    @Override
    public int getColumnCount()
    {
        return 3;
    }

    /**
     * @return the number of columns in the model. A JTable uses this method to
     *         determine how many columns it should create and display by default.
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        int rowCount = 0;
        if (null != _guideElement)
        {
            NodeList items = _guideElement.getElementsByTagName( "reference" );
            rowCount = items.getLength();
        }
        return rowCount;
    }


    /**
     *
     */
    @Override
    public boolean isCellEditable( int rowIndex, int colIndex)
    {
        // All cells are editable; only the Title is free-form
        return true;
    }


    /**
     * @return the value for the cell at columnIndex and rowIndex.
     * @param rowIndex
     *            the row whose value is to be queried
     * @param colIndex
     *            the column whose value is to be queried
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt( int rowIndex, int colIndex )
    {
        NodeList items = _guideElement.getElementsByTagName( "reference" );
        Element item = (Element) items.item( rowIndex );
        switch (colIndex)
        {
            case 0:
                return item.getAttribute( "type" );
            case 2:
                return item.getAttribute( "href" );
            case 1:
                return item.getAttribute( "title" );
        }
        return null;
    }


    /**
     *
     */
    @Override
    public void setValueAt( Object aValue, int rowIndex, int colIndex )
    {
        NodeList items = _guideElement.getElementsByTagName( "reference" );
        if (items.getLength() <= rowIndex)
        {
            // adding a new row
            addGuideReference( "other", "<Edit this title>", "select from manifest" );
        }
        else
        {
            // setting the value in an existing row.
            Element item = (Element) items.item( rowIndex );
            if (null != item)       // Probably unnecessary test
            {
                String value = (String) aValue;
                switch (colIndex)
                {
                    case 0:
                        item.setAttribute( "type", value );
                        break;
                    case 2:
                        // TODO: allow hash tags.
                        item.setAttribute( "href", value );
                        break;
                    case 1:
                        item.setAttribute( "title", value );
                        break;
                }
                fireTableRowsUpdated( rowIndex, rowIndex );
            }
        }
    }


    /**
     * @param colIndex - index of the column being queried
     * @return a string containing the name of column
     */
    @Override
    public String getColumnName( int colIndex )
    {
        switch (colIndex)
        {
            case 0:
                return "type";
            case 2:
                return "href";
            case 1:
                return "title";
        }
        return "";
    }


    public void removeRow( int rowIndex )
    {
        NodeList items = _guideElement.getElementsByTagName( "reference" );
        if (0 <= rowIndex && items.getLength() > rowIndex)
        {
            Element o = (Element) items.item( rowIndex );
            Node parent = o.getParentNode();
            parent.removeChild( o );
            fireTableRowsDeleted( rowIndex, rowIndex );
        }
    }


}
