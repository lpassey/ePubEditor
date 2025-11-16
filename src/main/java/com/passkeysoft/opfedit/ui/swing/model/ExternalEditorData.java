package com.passkeysoft.opfedit.ui.swing.model;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.prefs.*;

import com.passkeysoft.opfedit.ui.swing.controller.EPubEditor;

public class ExternalEditorData extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

    public MediaTypeModel _mediaTypes;         // All currently defined media-types
    public ArrayList<Preferences> _editors;    // Subset of media-types that have an associated editor

    public ExternalEditorData()
    {
        _mediaTypes = new MediaTypeModel();
        _editors = new ArrayList<>( 16 );
        resetEditorArray();
    }

    private void resetEditorArray()
    {
        Preferences media_type;

        _editors.clear();
        _mediaTypes.resetMediaTypeList();
        for (int i = 0; i < _mediaTypes.getSize(); i++ )
        {
            media_type = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES).node( (String) _mediaTypes.getElementAt( i ));
            String editCmd = media_type.get( EPubEditor.PREFS_EDITOR_PATH, null );
            if (null != editCmd)
            {
                // there is an editor associated with this media type; add it to the list.
                _editors.add( media_type );
            }
        }
        fireTableDataChanged();
    }

    // Unused getters
//    public MediaTypeModel getAllMediaTypes()
//    {
//        return _mediaTypes;
//    }
//
//
//    public ArrayList<Preferences> getEditorPrefs()
//    {
//        return _editors;
//    }


    @Override
    public int getRowCount()
    {
        return _editors.size();
    }

    @Override
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public void setValueAt( Object aObject, int rowIndex, int columnIndex )
    {
        Preferences prefs = _editors.get( rowIndex );
        switch( columnIndex )
        {
        case 0:
            break;
        case 1:
            prefs.put( EPubEditor.PREFS_EDITOR_PATH, (String) aObject );
            break;
        case 2:
            prefs.put( EPubEditor.PREFS_EDITOR_CL, (String) aObject );
            break;
        }
    }


    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        Preferences prefs = _editors.get( rowIndex );
        switch( columnIndex )
        {
        case 0:
            return prefs.absolutePath().substring( EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ).absolutePath().length() + 1 );
        case 1:
            return prefs.get( EPubEditor.PREFS_EDITOR_PATH, "" );
        case 2:
            return prefs.get( EPubEditor.PREFS_EDITOR_CL, "" );
        }
        return null;
    }


    @Override
    public String getColumnName( int columnIndex )
    {
            switch( columnIndex )
            {
            case 0:
                return "media-type";
            case 1:
                return "Editor executable";
            case 2:
                return "Command line options";
            }
        return "";
    }

    @Override
    public boolean isCellEditable( int row, int column )
    {
        return 0 != column;
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
            Preferences pref = _editors.get( index );
            // remove 'editor' and 'edcl' values from this node.
            pref.remove( EPubEditor.PREFS_EDITOR_PATH );
            pref.remove( EPubEditor.PREFS_EDITOR_CL );
        }
        resetEditorArray();      // clear and rebuild the list.
    }


    public void addEditor( String mediaType, String program, String commandLine )
    {
        Preferences pref = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ).node( mediaType );
        pref.put( EPubEditor.PREFS_EDITOR_PATH, program );
        pref.put( EPubEditor.PREFS_EDITOR_CL, commandLine );
        resetEditorArray();      // clear and rebuild the list.
    }


    public boolean contains( String mediaType )
    {
        int i;
        for (i = 0; i < _editors.size(); i++)
        {
            Preferences pref = _editors.get( i );
            String myMediaType = pref.absolutePath().substring( EPubEditor.prefs.node(
                        EPubEditor.PREFS_MEDIA_TYPES ).absolutePath().length() + 1 );
            if (myMediaType.equals( mediaType ))
                break;
        }
        return i < _editors.size();
    }
}
