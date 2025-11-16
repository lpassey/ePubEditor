/* *
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

  $Log: SpineModel.java,v $
  Revision 1.7  2013/07/03 22:00:32  lpassey
  Add CVS log entries

*/


package com.passkeysoft.opfedit.ui.swing.model;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.event.EventListenerList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.passkeysoft.opfedit.ui.swing.controller.LogAndShowError;

public class SpineModel extends MonitoringTableModel
{
    private static final long serialVersionUID = 1L;

    private static String[] acceptable =
    {
         "application/xhtml+xml",
         "text/html",
         "text/x-oeb1-document",
         "application/x-dtbook+xml",
         "text"
    };


   static private TreeSet<String> htmlTypes = new TreeSet<>();
    {
        htmlTypes.add( acceptable[0] );
        htmlTypes.add( acceptable[1] );
        htmlTypes.add( acceptable[2] );
    }


	private Element _spine; // , _manifest;
	private EventListenerList _watcher;


    public static boolean isAcceptable( String test )
    {
        for (String s : acceptable)
        {
            if (test.equalsIgnoreCase( s ))
                return true;
        }
        return false;
    }

    SpineModel( EPubModel _opfData, Element spineNode )
    {
        super( _opfData );
   		_spine = spineNode;
        if (null != _spine)
        {
            // find every "itemref" element and reset the namespace.
            NodeList items = _spine.getElementsByTagName( "opf:itemref" );
            for (int i = 0; i < items.getLength(); i++)
            {
                Element item = (Element) items.item( i );
                _opfData.getOpfDom().renameNode( item, null, "itemref" );
            }
        }
        if (null != _opfData)
        {
       		_watcher = new EventListenerList();
       	}
    }

    public void addActionListener( ActionListener listener )
    {
        _watcher.add( ActionListener.class, listener );
    }


    public void removeActionListener( ActionListener listener )
    {
        _watcher.remove( ActionListener.class, listener );
    }


    class SpineDocumentIterator implements Iterator<String>
    {
        NodeList itemRefs = null;
        int limit = 0;
        int cursor = 0;
        Set<String> mediaTypes;
        String id;
        public String href;


        SpineDocumentIterator( Set<String> mt )
        {
            mediaTypes = mt;
            if (null != _spine)
            {
                itemRefs = _spine.getElementsByTagName( "itemref" );

                limit = itemRefs.getLength();
            }
        }


        private Element moveToValid() throws IllegalStateException
        {
            // if itemRefs.item( cursor ) does not reference a manifested
            // item of type "application/xhtml+xml", "text/x-oeb1-document"
            // or "text/html" move cursor to the next manifested item which
            // /does/ have one of those types
            if (cursor < limit)
                do
                {
                    String idref = ((Element) itemRefs.item( cursor )).getAttribute( "idref" );
                    Element item = fileData.getOpfData().getManifest().getItemById( idref );
                    if (null != item)
                    {
                        String mediaType = item.getAttribute( "media-type" );
                        if (mediaTypes.contains( mediaType ))
                        {
                            // We have a manifested item whose media type is
                            // in the set of types sought.
                            return item;
                        }
                    }
                    else
                    {
                        // A non-manifested item in the spine. This is a serious
                        // error, and probably merits throwing an exception. I
                        // don't want to stop processing however, so we'll just
                        // log it and bring up an error dialog.
                        LogAndShowError.logAndShowNoEx( "Unmanifested item \"" + idref
                                + "\" detected in the spine" );
                    }
                    cursor++;
                } while (cursor < limit);
            return null;
        }


        /*
         * Returns true if the iteration has more elements. (In other words, returns true
         * if next would return an element rather than throwing an exception.)
         *
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext()
        {
            moveToValid();
            return cursor < limit;
        }


        /*
         * (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        @Override
        public String next()
        {
            Element item = moveToValid();
            if (null == item)
                throw new IllegalStateException();
            id = item.getAttribute( "id" );
            href = item.getAttribute( "href" );
            cursor++;
            return id;
        }


        /*
         * (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * An iterator implementation that traverses the documents in the <spine>
     * element, returning those which claim to have an XHTML-variant mime-type
     */
    public class SpineHTMLIterator extends SpineDocumentIterator
    {
        public SpineHTMLIterator()
        {
            super( htmlTypes );
        }
    }


    // Methods derived from interface javax.swing.table.TableModel
    @Override
    public int getColumnCount()
    {
        return 3;
    }


    @Override
    public int getRowCount()
    {
        if (null != _spine)
        {
            NodeList spineList = _spine.getElementsByTagName( "itemref" );
           	return spineList.getLength();
        }
        return 0;
    }


    /*
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        NodeList elementList = _spine.getElementsByTagName( "itemref" );
    	if (rowIndex < elementList.getLength())
    	{
    	    Element rowNode = (Element) elementList.item( rowIndex );
    	    String idRef = rowNode.getAttribute( "idref" );
    	    elementList = fileData.getOpfData().getManifest().getManifestedItems();
    	    for (int i = 0; i < elementList.getLength(); i++)
    	    {
    	        if (idRef.equals( ((Element) elementList.item( i )).getAttribute( "id" )) )
    	        {
    	            rowNode = (Element) elementList.item( i );
                	if (columnIndex == 0)
                		return (rowNode.getAttribute( "id" ));
                	else if (columnIndex == 1)
                	    return (rowNode.getAttribute( "href" ));
                	else if (columnIndex == 2)
                	    return (rowNode.getAttribute( "media-type" ));
                	return null;
    	        }
    	    }
        }
        return null;
    }


    /*
     * @see javax.swing.table.AbstractTableModel#setValueAt(java.lang.Object, int, int)
     */
    @Override
    public void setValueAt( Object aValue, int rowIndex, int columnIndex )
	{
        if (getRowCount() > rowIndex && null != aValue && 0 == columnIndex)
        {
            // editing an existing row. Only the idref can be changed.
            // verify that this item is not already represented in the spine.
            Element itemRef;
            NodeList inSpine = _spine.getElementsByTagName( "itemref" );

            for (int i = 0; i < inSpine.getLength(); i++)
            {
                if ( i != rowIndex)
                {
                    itemRef = (Element) inSpine.item( i );
                    String attr = itemRef.getAttribute( "idref" );
                    if (attr.equals( (String) aValue ))
                    {
                        // We need to send a message if this occurs.
                        sendActionEvent( attr + " is already part of the content");
                        return;
                    }
                }
            }

            itemRef = (Element) inSpine.item( rowIndex );
            itemRef.removeAttribute( "idref" );
            itemRef.setAttribute( "idref", (String ) aValue );
            fireTableRowsUpdated( rowIndex, rowIndex );
        }
	}


    private void sendActionEvent( String string )
    {
        ActionEvent event = new ActionEvent(this, 0, string);
        ActionListener[] listeners = _watcher.getListeners( ActionListener.class );
        for (ActionListener listener : listeners) listener.actionPerformed( event );
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName( int column )
	{
		if (column == 0)
			return "ID";
		else if (column == 1)
			return "File Path";
		else if (column == 2)
		    return "media-type";
		return null;
	}

    /*
     * (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#isCellEditable(int, int)
     */
    @Override
	public boolean isCellEditable( int rowIndex, int columnIndex )
	{
        return 0 == columnIndex;
    }


	public int addRow( String idref, Node refChild )
	{
        Element itemRef, element;

        int i, limit;
        if (null == idref || 0 == idref.length())
        {
            NodeList manifested = fileData.getOpfData().getManifest().getManifestedItems();
            String id = "";
            limit = manifested.getLength();
            // Find the first allowable element in the manifest that is not in the spine.
            for (i = 0; i < limit; i++)
            {
                // get a manifested element
                element = (Element) manifested.item(i);
                id = element.getAttribute( "media-type" );
                if (isAcceptable( id ))
                {
                    id = element.getAttribute( "id" );

                    // is it in the spine?
                    if (null == getItemNodeByIdref( id ))
                    {
                        // no match in the spine use this manifest id as the idref;
                        break;
                    }
                }
            }
            if (i < limit && 0 < id.length())
                idref = id;
        }
        else
        {
            Element item = getItemNodeByIdref( idref );
            // If the item is already in the spine, don't add it again
            if (null != item)
                idref = null;
        }
        if (idref != null && 0 < idref.length())
        {
            if (null != refChild && !_spine.equals( refChild.getParentNode() ))
                refChild = null;
            itemRef = _spine.getOwnerDocument().createElement( "itemref" );
            _spine.insertBefore( itemRef, refChild );
            itemRef.setAttribute( "idref", idref );
            int rowIndex = getRowCount() - 1;
            fireTableRowsInserted( rowIndex, rowIndex );
            return rowIndex;
        }
        return -1;
	}


    public Element getItemNodeByIdref( String id )
    {
        NodeList spineList = _spine.getElementsByTagName( "itemref" );
        Element element;
        int i;

        for (i = 0; i < spineList.getLength(); i++)
        {
            element = (Element) spineList.item( i );
            String testid = element.getAttribute( "idref" );
            if (testid.equalsIgnoreCase( id ))
                return element;
        }
        return null;
    }


	void removeItem( Element itemref )
	{
        if (null != itemref)
        {
            Node parent = itemref.getParentNode();  // this had better be the spine!!
            if (parent.equals( _spine ))
            {
                // clean up all the whitespace following the node
                while (   null != itemref.getNextSibling()
                        && (Node.TEXT_NODE == itemref.getNextSibling().getNodeType())
                        && itemref.getNextSibling().getTextContent().trim().length() == 0)
                {
                    parent.removeChild( itemref.getNextSibling() );
                }
                parent.removeChild( itemref );
                fireTableDataChanged();
            }
        }
	}


	public void removeRow( int rowIndex )
	{
        NodeList spineList = _spine.getElementsByTagName( "itemref" );

		if (0 <= rowIndex && rowIndex < spineList.getLength())
		{
		    Node itemref = spineList.item( rowIndex );
		    removeItem( (Element) itemref );
		}
	}


    public int moveRowUp( int rowIndex )
    {
        NodeList spineList = _spine.getElementsByTagName( "itemref" );

        int limit = spineList.getLength();

        // row 0 can't be moved up
        if (rowIndex > 0 && rowIndex < limit)
        {
            if (rowIndex == limit - 1)
            {
                // special case. Moving the last row up is better
                // handled as moving the second-to-last row down.
                moveRowDn( rowIndex - 1 );
            }
            else
            {
                Node itemref = spineList.item( rowIndex );
                Node parent = itemref.getParentNode();
                Node prevItem = spineList.item(  rowIndex - 1 );

                // Move all the nodes from the itemref point to just before prevItem
                // until another itemref is found.
                do
                {
                    Node temp = itemref.getNextSibling();
                    parent.removeChild( itemref );
                    parent.insertBefore( itemref, prevItem );
                    itemref = temp;
                } while (null != itemref && !"itemref".equals( itemref.getNodeName() ));
        		fireTableRowsUpdated( rowIndex - 1, rowIndex );
            }
    		--rowIndex;
        }
    	return rowIndex;
    }


    public int moveRowDn( int rowIndex )
    {
        NodeList spineList = _spine.getElementsByTagName( "itemref" );

        // last row can't be moved down
        if (rowIndex < spineList.getLength() -1)
        {
            Node itemref = spineList.item( rowIndex );
            Node parent = itemref.getParentNode();
            Node nextItem = spineList.item(  rowIndex + 2 );    // Might be null!

            // Move all the nodes from the itemref point to just after nextItem
            // until another itemref is found.
            do
            {
                Node temp = itemref.getNextSibling();
                parent.removeChild( itemref );
                parent.insertBefore( itemref, nextItem );
                itemref = temp;
            } while (null != itemref && !"itemref".equals( itemref.getNodeName() ));
            fireTableRowsUpdated( rowIndex, rowIndex + 1 );
            ++rowIndex;
        }
    	return rowIndex;
    }


    void renameItemRef( String oldValue, String aValue )
    {
        NodeList spineList = _spine.getElementsByTagName( "itemref" );
        for (int i = 0; i < spineList.getLength(); i++)
        {
            Element rowNode = (Element) spineList.item( i );
            String idRef = rowNode.getAttribute( "idref" );
            if (idRef.equals( oldValue ))
            {
                rowNode.setAttribute( "idref", aValue );
                break;
            }
        }
    }

    public String getNCXId()
    {
        if (null != _spine)
            return _spine.getAttribute( "toc" );
        return "";
    }

    public void setNCXId( String ncx )
    {
        if (null != _spine)
            _spine.setAttribute( "toc", ncx );
    }
}