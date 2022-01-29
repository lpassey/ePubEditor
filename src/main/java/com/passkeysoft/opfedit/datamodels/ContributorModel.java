package com.passkeysoft.opfedit.datamodels;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.passkeysoft.opfedit.staticutil.XMLUtil;

public class ContributorModel extends MonitoringTableModel
{
	private static final long serialVersionUID = 1L;
	
    public static String[][] roles = {  //  Try to arrange these in order of commoness of use
                                        { "aut", "Author" },
                                        { "art", "Artist" },
                                        { "clb", "Collaborator" },
                                        { "com", "Compiler" },
                                        { "dsr", "Designer" }, 
                                        { "edt", "Editor" },
                                        { "ill", "Illustrator" },
                                        { "trl", "Translator"},
                                        { "spn", "Sponsor" },
                                        { "aft", "Author of Afterword" },
                                        { "aui", "Author of Introduction" },
                                        { "nrt", "Narrator" },
                                        { "pht", "Photographer" },
                                        { "prt", "Printer" },
                                        { "adp", "Adapter" }, 
                                        { "ann", "Annotator" },
                                        { "red", "Redactor" },
                                        { "rev", "Reviewer" },
                                        { "trc", "Transcriber" },
                                        { "arr", "Arranger" },
                                        { "asn", "Associated name" },
                                        { "aqt", "Author in quotations" },
                                        { "ant", "Bibliographic antecedent" },
                                        { "bkp", "Book producer" },
                                        { "cmm", "Commentator" },
                                        { "lyr", "Lyricist" },
                                        { "mdc", "Metadata contact" },
                                        { "mus", "Musician" },
                                        { "ths", "Thesis advisor" },
                                        { "oth", "Other" }, //  must be last
                                        };
    
    static boolean isContributor( Node candidate )
    {
        return candidate.getNodeName().toLowerCase().contains( "creator" )
            || candidate.getNodeName().toLowerCase().contains( "contributor" );
    }
    
    /*
     *   PRIVATE methods
     */
    
    private Element _metadata;
    
    
    private Element getRowElement( int rowIndex )
    {
        for (Node child = _metadata.getFirstChild(); null != child; child = child.getNextSibling())
        {
            if (isContributor( child ))
            {
                if (0 == rowIndex)
                    return (Element) child;
                rowIndex--;
            }
        }
        return null;
    }

    
    /*
     *   CONSTRUCTOR
     */
    public ContributorModel( EPubModel epubData )
    {
        super(epubData);
        if (   null != epubData 
            && null != epubData.getOpfData() 
            && null != epubData.getOpfData().getMetadata())
        {
            // validate the data I'm responsible for
            _metadata = epubData.getOpfData().getMetadata().getCreatorsElement();
            for (Node n = _metadata.getFirstChild(); null != n; n = n.getNextSibling())
            {
                if (isContributor( n ))
                {
                    Element contributor = (Element) n;
                    
                    // replace non-namespaced attributes with their name-spaced alternatives
                    if (contributor.hasAttribute( "role" ))
                    {
                        String role = contributor.getAttribute( "role" );
                        contributor.removeAttribute( "role" );
                        Attr attr = _metadata.getOwnerDocument().createAttribute( "opf:role" );

                        attr.setNodeValue( role );
                        contributor.setAttributeNode( attr );
                    }
                    else if (n.getNodeName().toLowerCase().contains( "creator" ))
                    {
                        if (contributor.getAttribute( "role" ).equals( "aut" ))
                            contributor.removeAttribute( "role" );

                        Attr attr = _metadata.getOwnerDocument().createAttribute( "opf:role" );

                        attr.setNodeValue( "aut" );
                        contributor.setAttributeNode( attr );
                    }
                    if (contributor.hasAttribute( "file-as" ))
                    {
                        String fileAs = contributor.getAttribute( "file-as" );
                        contributor.removeAttribute( "file-as" );
                        Attr attr = _metadata.getOwnerDocument().createAttribute( "opf:file-as" );

                        attr.setNodeValue( fileAs );
                        contributor.setAttributeNode( attr );
                    }
                }
            }
        }
    }
    
    
    /*
     *   VIRTUAL METHODS
     *   
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        // This method is derived from interface javax.swing.table.TableModel
        return 3;
    }

    
    /*
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int column)
    {
        // This method is derived from class javax.swing.table.AbstractTableModel
        switch (column)
        {
        case 0:
            return "Name";
        case 1:
            return "Role";
        case 2:
            return "File As";
        }
        return null;
    }

    

    /*
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        int rows = 0;
    
        if (null != _metadata)
        {
            for (Node n = _metadata.getFirstChild(); null != n; n = n.getNextSibling())
            {
                if (   n.getNodeName().toLowerCase().contains( "creator" )
                    || n.getNodeName().toLowerCase().contains( "contributor" ))
                {
                    ++rows;
                }
            }
        }
        return rows;
    }

    
    /*
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        int rows = getRowCount();
        if (rowIndex <= rows)   // should never happen
        {
            Element c = getRowElement( rowIndex );
            if (c != null) switch( columnIndex )
            {
            case 0:
                return c.getTextContent();
            case 1:
                String role = c.getAttributeNS( "*", "role" );
                if (null == role || 0 == role.length())
                {
                    role = c.getAttribute( "opf:role"  );
                    if (null == role || 0 == role.length())
                    {
                        role = c.getAttribute( "role"  );
                        if (null == role || 0 == role.length())
                            return "Other";
                    }
                }
                for ( String[] is : roles )
                {
                    if (role.equalsIgnoreCase( is[0] ))
                        return is[1];
                }
            case 2:
                String fileAs = c.getAttributeNS( "*", "file-as" );
                if (null == fileAs || 0 == fileAs.length())
                {
                    fileAs = c.getAttribute( "opf:file-as" );
                    if (null == fileAs || 0 == fileAs.length())
                        fileAs = c.getAttribute( "file-as" );
                }
                return fileAs;
            }
        }
        return null;
    }

    
    /**
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        // All cells in this table are editable.
        return true;
    }

    
    /**
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
        int i, rows = getRowCount();

        Element contrib;
        if (rowIndex >= rows)
        {
            // adding a new contributor
            contrib = _metadata.getOwnerDocument().createElement( "dc:creator" );
            contrib.setAttribute( "opf:role", "aut" );
            
            _metadata.insertBefore( contrib, null );
            fireTableRowsInserted( rows, rows + 1 );
            rowIndex = rows;
        }
        contrib = getRowElement( rowIndex );
        if (null != contrib)
        {
            switch( columnIndex )
            {
            case 0:         //  One of a list of names
                Node oldChild = contrib.getFirstChild();
                Text author = contrib.getOwnerDocument().createTextNode( (String) aValue );
                if (null != oldChild)
                    contrib.replaceChild( author, oldChild );
                else
                    contrib.insertBefore( author, null );
                break;
            case 1:         //  the enumerated role
                //  Unfortunately we're given a string, so we have to find it in
                //  the array of roles to get the appropriate abbreviation
                for (i = 0; i < roles.length - 1; i++)
                {
                    if (roles[i][1].equals( aValue ))
                        break;
                }
//                    Attr attr = _metadata.getOwnerDocument().createAttribute( "opf:role" );
//
//                    attr.setNodeValue( roles[i][0] );
//                    contrib.setAttributeNode( attr );
                contrib.setAttribute( "opf:role", roles[i][0] );
                // Make sure the element name corresponds to the role
                if (0 < i)
                {
                    // the node is for a creator, but the role is only contributory; change the node name
                    XMLUtil.renameElement( contrib, "dc:contributor" );
//                        _metadata.getOwnerDocument().renameNode( contrib, 
//                                "http://purl.org/dc/elements/1.1/", "dc:contributor" );
                }
                else
                    XMLUtil.renameElement( contrib, "dc:creator" );
//                        _metadata.getOwnerDocument().renameNode(  contrib, "http://purl.org/dc/elements/1.1/", "dc:creator" );
                break;
            case 2:         //  File as name - optional
//                    attr = _metadata.getOwnerDocument().createAttribute( "opf:file-as" );
//                    attr.setNodeValue( (String) aValue );
//                    
//                    contrib.setAttributeNode( attr );
                contrib.setAttribute( "opf:file-as", (String) aValue );
                break;
            }
            fireTableCellUpdated( rowIndex, columnIndex );
        }    
    }
    

    /*
     *  PUBLIC methods for ordering.
     */
    public void removeRow( int rowIndex )
    {
        Element o = getRowElement( rowIndex );
        if (null != o)
        {
            Node parent = o.getParentNode();
            parent.removeChild( o );
            fireTableRowsDeleted( rowIndex, rowIndex );
        }
    }
    
    
    public int moveRowUp( int rowIndex )
    {

        Element o = getRowElement( rowIndex );
        if (null != o)
        {
            Node sib = getPreviousContributor( o );
            if (null != sib)
            {
                Node parent = o.getParentNode();
                parent.removeChild( o );
                parent.insertBefore( o, sib );
                fireTableRowsUpdated( rowIndex - 1, rowIndex );
                rowIndex--;
            }
        }
        return rowIndex;
    }
    
    
    private Node getPreviousContributor( Element o )
    {
        for (Node n = o.getPreviousSibling(); null != n; n = n.getPreviousSibling())
            if (isContributor( n ))
                return n;
        return null;
    }


    public int moveRowDn( int rowIndex )
    {
        Element o = getRowElement( rowIndex );
        if (null != o)
        {
            Node sib = getNextContributor( o );
            if (null != sib)
            {
                Node parent = o.getParentNode();
                parent.removeChild( o );
                parent.insertBefore( o, sib.getNextSibling() );
                fireTableRowsUpdated( rowIndex, rowIndex + 1);
                rowIndex++;
            }
        }
        return rowIndex;
    }


    private Node getNextContributor( Element o )
    {
        for (Node n = o.getNextSibling(); null != n; n = n.getNextSibling())
            if (isContributor( n ))
                return n;
        return null;
    }
}