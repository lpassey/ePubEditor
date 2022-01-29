package com.passkeysoft.opfedit.datamodels;

import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.table.AbstractTableModel;

import com.passkeysoft.opfedit.ui.EPubEditor;

public class ExternalXformerData extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
    
    public MediaTypeModel _mediaTypes;         // All currently defined media-types
    public ArrayList<Preferences> _xformers;    // Subset of media-types that have an associated transformer
    

    public ExternalXformerData()
    {
        _mediaTypes = new MediaTypeModel();
        _xformers = new ArrayList<>( 16 );
        resetXFormerArray();
    }

    
    private void resetXFormerArray()
    {
        Preferences media_type;
        
        _xformers.clear();
        _mediaTypes.resetMediaTypeList();
        for (int i = 0; i < _mediaTypes.getSize(); i++ )
        {
            media_type = EPubEditor.prefs.node( 
                         EPubEditor.PREFS_MEDIA_TYPES).node( (String) _mediaTypes.getElementAt( i ));
            String xformCmd = media_type.get( EPubEditor.PREFS_TRANSFORMER, null );
            if (null != xformCmd)
            {
                // there is a transformer associated with this media type; add it to the list.
                _xformers.add( media_type );
            }
        }
        fireTableDataChanged();
    }

    
    @Override
    public int getRowCount()
    {
        return _xformers.size();
    }

    @Override
    public int getColumnCount()
    {
        return 5;
    }

    @Override
    public boolean isCellEditable( int row, int column )
    {
        return 0 != column;
    }

    
    @Override
    public String getColumnName( int columnIndex )
    {
            switch( columnIndex )
            {
            case 0:
                return "media-type";
            case 1:
                return "Transformer executable";
            case 2:
                return "Command line options";
            case 3:
                return "Transformed media-type";
            case 4:
                return "Transformed file extension";
            }
        return "";
    }
    
    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        Preferences prefs = _xformers.get( rowIndex );
        switch( columnIndex )
        {
        case 0:
            return prefs.absolutePath().substring( EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ).absolutePath().length() + 1 );
        case 1:
            return prefs.get( EPubEditor.PREFS_TRANSFORMER, "" );
        case 2:
            return prefs.get( EPubEditor.PREFS_XFORM_CL, "" );
        case 3: 
            return prefs.get( EPubEditor.PREFS_XFORM_NEWMT, "" );
        case 4: 
            return prefs.get( EPubEditor.PREFS_XFORM_NEWEXT, "" );
        }
        return null;
    }


    @Override
    public void setValueAt( Object aObject, int rowIndex, int columnIndex )
    {
        Preferences prefs = _xformers.get( rowIndex );
        if (null != aObject) switch( columnIndex )
        {
        case 0:
            break;
        case 1: 
            prefs.put( EPubEditor.PREFS_TRANSFORMER, (String) aObject );
            break;
        case 2:
            prefs.put( EPubEditor.PREFS_XFORM_CL, (String) aObject );
            break;
        case 3: 
            prefs.put( EPubEditor.PREFS_XFORM_NEWMT,  (String) aObject );
            break;
        case 4: 
            prefs.put( EPubEditor.PREFS_XFORM_NEWEXT,  (String) aObject );
            break;
        }
    }
    
    
    /**
     * Removes a number of rows from this array list. 
     * @param rowIndex An array of integers representing positions in the 
     *                 data list which should be cleared
     */
    public void removeRows( int[] rowIndex )
    {
        // First remove the editor preferences from the referenced nodes in the 
        // preferences store, then recreate the list. Lastly, signal a data change.
        for (int index : rowIndex)
        {
            Preferences pref = _xformers.get( index );
            // remove 'editor' and 'edcl' values from this node.
            pref.remove( EPubEditor.PREFS_TRANSFORMER );
            pref.remove( EPubEditor.PREFS_XFORM_CL );
            pref.remove( EPubEditor.PREFS_XFORM_NEWMT );
            pref.remove( EPubEditor.PREFS_XFORM_NEWEXT );
        }
        resetXFormerArray();      // clear and rebuild the list.
    }
    
    
    public void addEditor( String mediaType, String program, String commandLine, 
                           String newMediaType, String newExt )
    {
        Preferences pref = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ).node( mediaType );
        pref.put( EPubEditor.PREFS_TRANSFORMER, program );
        pref.put( EPubEditor.PREFS_XFORM_CL, commandLine );
        pref.put( EPubEditor.PREFS_XFORM_NEWMT, newMediaType );
        pref.put( EPubEditor.PREFS_XFORM_NEWEXT, newExt );
        resetXFormerArray();      // clear and rebuild the list.
    }

    

}
