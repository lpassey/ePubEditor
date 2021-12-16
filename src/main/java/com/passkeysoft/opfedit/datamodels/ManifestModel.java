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
  
  $Log: ManifestModel.java,v $
  Revision 1.7  2014/07/29 21:33:29  lpassey
  Remove "properties" attribute from items.

  Revision 1.6  2013/06/26 17:53:14  lpassey
  1. Add "Sort" method to sort the underlying data model
  2. Add Public Domain "licence"

*/

package com.passkeysoft.opfedit.datamodels;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.ListModel;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.w3c.dom.*;

import com.passkeysoft.opfedit.staticutil.XMLUtil;

/** 
 * @author W. Lee Passey 
 */
public class ManifestModel extends MonitoringTableModel implements ListModel
{
    private static final long serialVersionUID = 1L;
    
    private Element _manifest;
    private ImageIcon _present, _missing;

    private EventListenerList _listenerList;
    
    /**
     * Constructor
     * 
     * @param dataFile a OPFFileModel object or extension that represents the
     *   file or file system containing the .opf data to be displayed in the table.
     */
    public ManifestModel( EPubModel _opfData, Element manifest )
    {
        super( _opfData );
        _manifest = manifest;
        if (null != manifest)
        {
            // find every "item" element and reset the namespace.
            NodeList items = manifest.getElementsByTagName( "opf:item" );
            for (int i = 0; i < items.getLength(); i++)
            {
                Element item = (Element) items.item( i );
                _opfData.getOpfDom().renameNode( item, null, "item" );
            }
            items = manifest.getElementsByTagName( "item" );
            for (int i = 0; i < items.getLength(); i++)
            {
                Element item = (Element) items.item( i );
                String attr = item.getAttribute( "properties" );
                if (attr.length() > 0)
                    item.removeAttribute( "properties" );
            }
        }
        URL imagepath = getClass().getClassLoader().getResource( "images/ok.gif" );
        _present = new ImageIcon( imagepath );
        imagepath = getClass().getClassLoader().getResource( "images/stop.gif" );
        _missing = new ImageIcon( imagepath );
        _listenerList = new EventListenerList();
    }

    
    public NodeList getManifestedItems()
    {
        NodeList items = _manifest.getElementsByTagName( "item" );
        return items;
    }
    

    public boolean isValid()
    {
        // Validate that all the files in the manifest exist. The href value must either
        // be relative to the .opf file, or an absolute path in the file system. If
        // the value is an absolute path, it will be moved to be a peer of the .opf
        // file when the ePub is created.
        NodeList items = getManifestedItems();
        for (int i = 0; i < items.getLength(); i++)
        {
            Element item = (Element) items.item( i );
            String href = item.getAttribute( "href" );
            if (!fileData.fileExistsRelativeToOPF( href ))
            {
                File f = new File( href );
                if (f.exists() && f.isAbsolute())
                {
                    // Copy the file next to the .opf file, and update the manifest entry.
                    try
                    {
                        f = fileData.copyFileToOpf(f);
                        if (null == f)
                        {
                            return false;
                        }
                    }
                    catch( FileNotFoundException e )
                    {
                        return false;
                    }
                    item.setAttribute( "href", fileData.getPathRelativeToOpf( f ) );
                    // if the manifest changed to copy in new files,
                    // the manifest and spine views must be refreshed.
                    fireTableDataChanged();
                }
                else
                {
                    return false;
                }
            }
        }
        return true;
    }
    
    
    private int getManifestedIndex( NodeList items, String href )
    {
        if (null != href)
        {
            for (int i = 0; i < items.getLength(); i++)
            {
                String idref = ((Element) items.item( i )).getAttribute( "href" );
                if (idref != null && idref.equals( href ))
                    return i;
            }
        }
        return -1;
    }
    
    
    public int getItemIndex( String id )
    {
        if (null != id)
        {
            NodeList items = getManifestedItems();
            for (int i = 0; i < items.getLength(); i++)
            {
                String idref = ((Element) items.item( i )).getAttribute( "id" );
                if (idref != null && idref.equals( id ))
                    return i;
            }
        }
        return -1;
        
    }
    
    public boolean isManifested( String href )
    {
        NodeList items = getManifestedItems();
        return 0 <= getManifestedIndex( items, href );
    }

    
    public Element getItemById( String id )
    {
        NodeList manifested = getManifestedItems();
        for (int i = 0; i < manifested.getLength(); i++)
        {
            Element item = (Element) manifested.item( i );
            if (id.equalsIgnoreCase( item.getAttribute( "id" ) ))
                return item;
        }
        return null;
    }
    
    /**
     * @param id
     *            an identifier which may or may not exist in the manifest
     * @return the href associated with an identifer if it exists in the manifest, null otherwise.
     */
    public String getHrefById( String id )
    {
        String href = null;
        Element item = getItemById( id );
        if (null != item)
            href = item.getAttributeNS( null, "href" );
        return href;
    }

    
    /**
     * @param id
     *            an identifier which may or may not exist in the manifest
     * @return the mimetype associated with an identifer if it exists in the manifest, null otherwise.
     */
    public String getMediaTypeById( String id )
    {
        String mediaType = null;
        Element item = getItemById( id );
        if (null != item)
            mediaType = item.getAttributeNS( null, "media-type" );
        return mediaType;
    }

    
    public void setItemById( String id, String href, String mediaType )
    {
        Element item = getItemById( id );
        if (null != item)
        {
            item.setAttribute( "href", href );
            item.setAttribute( "media-type", mediaType );
            fireTableDataChanged();
        }
    }

    
    /**
     * Insert a new element into the manifest in sorted order using a simple
     * insertion sort. Sort the name first by extension, then by path. If the list
     * is not already sorted, results will be somewhat unpredictable (but not fatal)
     * 
     * @param id The desired id for the added item. If the id already exists in the
     * manifest or this parameter is <code>null</code> a new unique id will be generated.
     * @param href The path of the file being manifested, relative to the .opf file
     * @return the id attribute of the new item in the list of manifested items. If the item
     * already exists in the manifest, then the id attribute of the existing item.
     */
    public String addManifestItem( String id, String href, String mediaType )
    {
        // Check to see if this file is already manifested.
        // If so, return the id of the manifested item.
        NodeList items = getManifestedItems();
        int index = getManifestedIndex( items, href );
        if (0 <= index)
        {
            id = ((Element) items.item( index )).getAttribute( "id" );
            return id;
        }

        Element item = _manifest.getOwnerDocument().createElement( "item" );
        
        // If id equals null, or the id already exists, we will need to generate a new unique id 
        // for this item. Start with just the file name, and check to see if it exists. If so,
        // add a counter until it works.
        String base;
        if (null == id)
        {
            int slash = href.lastIndexOf( '\\' );
            if (-1 == slash)
                slash = href.lastIndexOf( '/' );
            if (-1 == slash)
                slash = 0;
            else
                ++slash;
            int dot = href.lastIndexOf( '.' );
            if (-1 == dot)
                dot = href.length();
            base = href.substring( slash, dot );
            id = base + "_00";
            if (!Character.isLetter( id.charAt( 0 ) ))
                id = "id" + id;
        }
        else
            base = id;
        index = 0;
        // loop through the entire manifest list and see if the new id matches an old id.
        for (int i = 0; i < items.getLength(); i++)
        {
            String idref = ((Element)items.item( i )).getAttribute( "id" );
            if (idref.equalsIgnoreCase( id ))
            {
                // yes we have a match. Create a new id and see if /that/ is unique
                id = String.format( "%s_%02d", base, index);
                index++;
                i = 0;      // restart the for loop at the beginning.
                continue;
            }
        }
        item.setAttribute( "id", id );
        item.setAttribute( "href", href );
        String newType = new MediaTypeModel().resolveMediaType( mediaType, href );
        
        item.setAttribute( "media-type", newType );
        
        // Insert this item in sorted order using a simple insertion sort. Files in
        // a directory should be sorted together, first by extension, then by name
        for (index = 0; index < items.getLength(); index++)
        {
            String tempHref = ((Element) items.item( index )).getAttribute( "href" );
            String tempName = new File( tempHref ).getParent();
            if (null == tempName)
                tempName = "";
            String tempPart = new File( href ).getParent();
            if (null == tempPart)
                tempPart = "";
            int order = tempName.compareToIgnoreCase( tempPart );
            if ( 0 < order)
            {
                _manifest.insertBefore( item, items.item( index ) );
                break;
            }
            else if (0 == order)
            {
                // files are in the same path...
                tempName = tempHref.substring( tempHref.lastIndexOf( '.' ) );
                int dot = href.lastIndexOf( '.' );
                tempPart = href.substring( dot == -1 ? href.length() : dot  );
                order = tempName.compareToIgnoreCase( tempPart );
                if ( 0 < order )
                {
                    _manifest.insertBefore( item, items.item( index ));
                    break;
                }
                else if (0 == order)
                {
                    // extensions match, compare the main part.
                    tempName = new File( tempHref ).getName();
                    tempPart = new File( href ).getName();
                    if (0 < tempName.compareToIgnoreCase( tempPart ))
                    {
                        _manifest.insertBefore( item, items.item( index ));
                        break;
                    }
                }
            }
        }
        if (index == items.getLength())
            _manifest.insertBefore( item, null );
        // Process the listeners last to first, notifying those that are interested in this event
        ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index, index);
        ListDataListener[] listeners = _listenerList.getListeners( ListDataListener.class );
        for (int i = listeners.length - 1; i >= 0; --i)
        {
            listeners[i].intervalAdded( e );
        }
        fireTableRowsInserted( index, index );
        return id;
    }
    
    
    /**
     * Removes a range of items from the manifest according to their position in a table
     * @param rowIndex
     */
    public void removeRow( int rowIndex[] )
    {
        if (null != _manifest)
        {
            NodeList items = getManifestedItems();
            SpineModel spine = fileData.getOpfData().getSpine();
            for (int i = rowIndex.length - 1; i >= 0; i-- )
            {
                Node item = items.item( rowIndex[i] );
                if (null != item)
                {
                    // If this manifested item is referenced in the spine,
                    // it must be removed from there first.
                    String id = ((Element) item).getAttribute( "id" ); 
                    Element spineItem = spine.getItemNodeByIdref( id );
                    if (null != spineItem)
                        spine.removeItem( spineItem );
                    Node parent;
                    parent = item.getParentNode();
                    parent.removeChild( item );
                }
            }
            // Process the listeners last to first, notifying those that are interested in this event
            ListDataListener[] listeners = _listenerList.getListeners( ListDataListener.class );
            for (int i = listeners.length - 1; i >= 0; --i)
            {
                ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, rowIndex[0], rowIndex[ rowIndex.length - 1 ]);
                listeners[i].intervalRemoved( e );
            }
            fireTableRowsDeleted(rowIndex[0], rowIndex[ rowIndex.length - 1 ] );
            
        }
    }
    
    
    public void removeItem( String id )
    {
        if (null != _manifest && null != id)
        {
            NodeList items = getManifestedItems();
            for (int i = 0; i < items.getLength(); i++)
            {
                Element item = (Element) items.item( i );
                if (id.equals( item.getAttribute( "id" ) ))
                {
                    int rowIndex[] = new int[1];
                    rowIndex[0] = i;
                    removeRow( rowIndex );
                    return;
                }
            }
        }
    }


    public void sort( SortKey key )
    {
        // Do a bubble sort on the child list, based in the column.
        // If the values are equal, do not re-order.
        if (1 == key.getColumn())
            return;
        String attr = getColumnName( key.getColumn());
        if (_manifest.hasChildNodes())
        {
            Element first = XMLUtil.findFirstElement( _manifest, "item" );
            Element test = XMLUtil.getNextElement( first );
            while (null != test)
            {
                Element next = XMLUtil.getNextElement( test );
                while (test != first)
                {
                    Element prev = XMLUtil.getPrevElement( test );
                    String testStr = test.getAttribute( attr );
                    String prevStr = prev.getAttribute( attr );
                    int comp = testStr.compareToIgnoreCase( prevStr );
                    if (   (comp < 0 && key.getSortOrder() == SortOrder.ASCENDING)
                        || (comp > 0 && key.getSortOrder() == SortOrder.DESCENDING ))
                    {
                        _manifest.insertBefore( _manifest.removeChild( test ), prev );
                        if (prev == first)
                            first = test;
                    }
                    else
                        break;
                }
                test = next;
            }
        }
    }
    
    
    @Override
    /**
     * 
     * @param rowIndex
     * @param columnIndex
     * @return true of false depending on whether the cell is editable
     */
    public boolean isCellEditable( int rowIndex, int columnIndex)
    {
        if (2 != columnIndex) 
            return true;
        return false;
    }


    /**
     *  @return the number of columns in the model
     */
    @Override
    public int getColumnCount()
    {
        return 5;
    }

    
    @Override
    public Class<?> getColumnClass( int colIndex )
    {
        if (colIndex == 1)
            return ImageIcon.class;
        return String.class;
        
    }
    
    
    /** 
     * @return the number of columns in the model. A JTable uses this method to
     *         determine how many columns it should create and display by
     *         default.
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        if (null != _manifest)
        {
            NodeList items = getManifestedItems();
            
            int count = items.getLength();
            return count;
        }
        return 0;
    }

    
    /**
     * @param column - the column being queried 
     * @return: a string containing the name of column
     */
    @Override
    public String getColumnName( int colIndex )
    {
        switch (colIndex)
        {
            case 0:
                return "id";
            case 2:
                return "href";
            case 3:
                return "media-type";
            case 4:
                return "properties";
        }
        return "";
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
        NodeList items = getManifestedItems();
        Element item = (Element) items.item( rowIndex );
        switch (colIndex)
        {
            case 1:
                String href = item.getAttribute( "href" );
                File test = new File( href );
                if (test.isAbsolute() && test.exists())
                    return _present;
                if (fileData.fileExistsRelativeToOPF( href ))
                    return _present;
                else
                    return _missing;
            case 0:
                return item.getAttribute( "id" );
            case 2:
                return item.getAttribute( "href" );
            case 3:
                return item.getAttribute( "media-type" );
            case 4:
                return item.getAttribute( "properties" );
        }
        return null;
    }

    
    /**
     * @param aValue - value to assign to cell
     * @param rowIndex - the row of the cell
     * @param columnIndex - the column of the cell
     */
    @Override
    public void setValueAt( Object aValue, int rowIndex, int colIndex )
    {
        if (null != _manifest)
        {
            NodeList items = getManifestedItems();
            if (rowIndex >= 0 && rowIndex < items.getLength())
            {
                Element item = (Element) items.item( rowIndex );
                switch (colIndex)
                {
                    case 0: // id
                        // If the old ID matches something in the spine, change that idref
                        // to match this one.
                        String oldValue = item.getAttribute( "id" );
                        if (!oldValue.equals( (String) aValue ))
                        {
                            fileData.getOpfData().getSpine().renameItemRef( oldValue, (String) aValue);
                        }
                        item.setAttribute( "id", (String) aValue );
                        break;
                    case 2: // href
                        item.setAttribute( "href", ((String) aValue).replace( '\\', '/' ) );
                        break;
                    case 3: // media-type
                        // TODO: if media type is "other", we need to bring up a dialog box
                        // to collect the other property, and add a fallback
//                        if ("other".equals( (String) aValue))
//                        {
//                            
//                        }
//                        else
                            item.setAttribute( "media-type", (String) aValue );
                        break;
                    case 4: // other properties
                        break;
                }
                fireTableRowsUpdated( rowIndex, rowIndex );
            }
        }
    }
    
    
    @Override
    public void addListDataListener( ListDataListener l )
    {
        _listenerList.add( ListDataListener.class, l );
    }


    @Override
    public Object getElementAt( int index )
    {
        NodeList items = getManifestedItems();
        if (0 < items.getLength() && index < items.getLength())
            return items.item( index );
        return null;
    }


    @Override
    public int getSize()
    {
        return getManifestedItems().getLength();
    }


    @Override
    public void removeListDataListener( ListDataListener l )
    {
        _listenerList.remove( ListDataListener.class, l );
    }

    public void notifyListeners()
    {
        ListDataListener[] listeners = (ListDataListener[]) (listenerList.getListenerList());
        for (ListDataListener l:listeners )
        {
            l.contentsChanged( new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() ));
        }
    }
}
