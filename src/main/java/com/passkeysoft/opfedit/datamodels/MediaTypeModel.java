package com.passkeysoft.opfedit.datamodels;

import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.ComboBoxModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.passkeysoft.opfedit.ui.EPubEditor;

public class MediaTypeModel implements ComboBoxModel
{
    /*************************************************************
     *          Static fields and methods
     *************************************************************/
    static private String[][] mediaTypes = {
        { "application/xhtml+xml", ".xhtml" }, //0
        { "text/css", ".css" },
        { "image/gif", ".gif" },
        { "image/jpeg", ".jpeg" },
        { "image/png", ".png" },
        { "image/svg+xml", ".svg" },
        { "application/xml", ".xml" },
        { "application/x-dtbncx+xml", ".ncx" },
        { "application/x-dtbook+xml", ".dtb" },
        { "text/x-oeb1-document", ".htm" }, // 9
        { "text/x-oeb1-css", ".css" },
        { "text/plain", ".txt" },
        { "text/html", ".html" },           // 12
        { "other", null },
        // Alternate file extensions for existing media types. Used only
        // to "intuit" media types for files with specific extensions.
        { "image/jpeg", ".jpg" },
       };


    public static boolean isHTML( String media_type )
    {
        return(   media_type.equalsIgnoreCase( mediaTypes[0][0] )
               || media_type.equalsIgnoreCase( mediaTypes[9][0] )
               || media_type.equalsIgnoreCase( mediaTypes[12][0] ));
    }
    
    /***********************************************************
     *            Instance fields and methods
     ***********************************************************/
    
    private int selected = -1;
    private EventListenerList eventListeners;
    
    protected ArrayList<String> _media_types = new ArrayList<String>( 24 );
    
    public MediaTypeModel()
    {
        eventListeners = new EventListenerList();
        resetMediaTypeList();
    }
    
    
    public void resetMediaTypeList()
    {
        _media_types.clear();
        
        //  Fill the media-type list with all pre-set property names
        for (int i = 0; i < mediaTypes.length; i++)
        {
            _media_types.add( mediaTypes[i][0] );
            if (mediaTypes[i][1] == null)
                break;
        }
        // Now search the preferences store and see if there are any media-types listed there
        // which are not in the list we just created. Add these to the list.
        Preferences xform = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES );
        try
        {
            String children[] = xform.childrenNames();  // application, text, image, etc.
            for (int i = 0; i < children.length; i++)
            {
                addMediaTypeToCombo( children[i] ,xform.node( children[i] ) );
            }
        }
        catch( BackingStoreException ignore ) {}
        fireListChanged();
    }
    
    
    private void addMediaTypeToCombo( String parentPath, Preferences media_type ) 
            throws BackingStoreException
    {
        String children[] = media_type.childrenNames();
        for (int i = 0; i < children.length; i++)
        {
            String mType = parentPath + "/" + children[i];

            // If this media type is not already in the list, add it.
            if (!contains( mType ))
                _media_types.add( mType );
            addMediaTypeToCombo( mType, media_type.node( children[i] ) );
        }
    }


    private void fireListChanged()
    {
        Object[] listeners = eventListeners.getListenerList();
        // Process the listeners last to first, notifying those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) 
        {
            ListDataEvent e = new ListDataEvent( this, ListDataEvent.CONTENTS_CHANGED, 0, getSize() -1 );
            if (listeners[i] == ListDataListener.class) 
            {
                ((ListDataListener)listeners[i+1]).contentsChanged( e );
            }
        }
        
    }
    
    
    public String resolveMediaType( String mediaType, String href )
    {
        if (null != mediaType)
        {
            // check to be sure it's an allowed media type
            for (String type:_media_types)
            {
                if (type.equals( mediaType ))
                {
                    return mediaType;
                }
            }
        }
        // If we got this far, either we have no media-type, or it was not specified
        // See if we can figure it out from the file extension.
        String newType = "other";
        for (String[] ext:mediaTypes)
        {
            if (null != ext[1] && href.toLowerCase().endsWith( ext[1] ))
            {
                newType = ext[0];
                break;
            }
        }
        return newType;
    }

    
    public boolean contains( String mediaType )
    {
        int i = 0;
        while( i < _media_types.size() )
        {
            if (mediaType.equals( _media_types.get( i )))
                break;
            i++;
        }
        if (_media_types.size() == i)
            return false;
        return true;
    }
    
    @Override
    public int getSize()
    {
        return _media_types.size();
    }

    @Override
    public Object getElementAt( int index )
    {
        return _media_types.get( index );
    }

    @Override
    public void addListDataListener( ListDataListener l )
    {
        eventListeners.add(ListDataListener.class, l);
    }

    @Override
    public void removeListDataListener( ListDataListener l )
    {
        eventListeners.remove(ListDataListener.class, l);
    }

    @Override
    public void setSelectedItem( Object anItem )
    {
        selected = _media_types.indexOf( anItem );
    }

    @Override
    public Object getSelectedItem()
    {
        if (0 <= selected && _media_types.size() > selected )
            return _media_types.get( selected );
        return null;
    }

}
