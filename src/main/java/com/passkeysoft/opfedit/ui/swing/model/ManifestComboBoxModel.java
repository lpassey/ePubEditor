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

package com.passkeysoft.opfedit.ui.swing.model;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author W. Lee Passey
 *
 */
public class ManifestComboBoxModel extends EventListenerList implements ComboBoxModel<Object>, ListDataListener
{
    private static final long serialVersionUID = 1L;
    ManifestModel _manifest;
    private int _selected = 0;
    private boolean _all;


    ManifestComboBoxModel( ManifestModel manifest, boolean all )
    {
        _manifest = manifest;
        _manifest.addListDataListener( this );
        _all = all;
    }

    /* (non-Javadoc)
     * @see javax.swing.ListModel#addListDataListener(javax.swing.event.ListDataListener)
     */
    @Override
    public void addListDataListener( ListDataListener arg0 )
    {
        add( ListDataListener.class, arg0 );
    }


    /**
     * @return the Element at the specified index.
     * @see javax.swing.ListModel#getElementAt(int)
     */
    @Override
    public Object getElementAt( int index )
    {
        NodeList items = _manifest.getManifestedItems();

        // We only want to return references to those items which are of type
        // "application/xhtml+xml", "application/x-dtbook+xml", "text/x-oeb1-document"
        // or in my extension, "text" or "text/html"
        if (0 < items.getLength())
        {
            if (_all)
                return items.item( index );
            for (int i = 0; i < items.getLength(); i++)
            {
                Element element = (Element) items.item( i );
                String type = element.getAttribute( "media-type" );
                if ( SpineModel.isAcceptable( type ))
                {
                    if (index == 0)
                        return items.item( i );
                    index--;
                }
            }
        }
        return null;
    }


    /**
     * @return the length of the list
     * @see javax.swing.ListModel#getSize()
     */
    @Override
    public int getSize()
    {
        int length = 0;
        NodeList items = _manifest.getManifestedItems();
        if (_all)
            return items.getLength();
        for (int i = 0; i < items.getLength(); i++)
        {
            Element element = (Element) items.item( i );
            String type = element.getAttribute( "media-type" );
            if (SpineModel.isAcceptable( type ))
            {
                ++length;
            }
        }
        return length;
    }


    /**
     * @see javax.swing.ListModel#removeListDataListener(javax.swing.event.ListDataListener)
     */
    @Override
    public void removeListDataListener( ListDataListener l )
    {
        remove( ListDataListener.class, l );
    }


    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#getSelectedItem()
     */
    @Override
    public Object getSelectedItem()
    {
        return getElementAt( _selected );
    }


    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
     */
    @Override
    public void setSelectedItem( Object arg0 )
    {
        NodeList items = _manifest.getManifestedItems();
        int acceptable = 0;

        for (int i = 0; i < items.getLength(); i++)
        {
            Element element = (Element) items.item( i );
            if (arg0.equals( element ))
            {
                _selected = acceptable;
                break;
            }
            String type = element.getAttribute( "media-type" );
            if (_all || SpineModel.isAcceptable( type ))
            {
                ++acceptable;
            }
        }
    }


    private void notifyListeners( ListDataEvent e )
    {
        Object[] listeners = getListenerList();
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2)
        {
            if (listeners[i]==ListDataListener.class)
            {
                ListDataListener listener = (ListDataListener)listeners[i+1];
                switch( e.getType())
                {
                    case ListDataEvent.CONTENTS_CHANGED:
                        listener.contentsChanged( new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, e.getIndex0(), e.getIndex1()) );
                        break;
                    case ListDataEvent.INTERVAL_ADDED:
                        listener.contentsChanged( new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, e.getIndex0(), e.getIndex1()) );
                        break;
                    case ListDataEvent.INTERVAL_REMOVED:
                        listener.contentsChanged( new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, e.getIndex0(), e.getIndex1()) );
                        break;
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
     */
    @Override
    public void contentsChanged( ListDataEvent e )
    {
       notifyListeners( e );
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.event.ListDataListener#intervalAdded(javax.swing.event.ListDataEvent)
     */
    @Override
    public void intervalAdded( ListDataEvent e )
    {
        notifyListeners( e );
    }

    /*
     * (non-Javadoc)
     * @see javax.swing.event.ListDataListener#intervalRemoved(javax.swing.event.ListDataEvent)
     */
    @Override
    public void intervalRemoved( ListDataEvent e )
    {
        notifyListeners( e );
    }

}
