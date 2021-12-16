package com.passkeysoft.opfedit.datamodels;

import java.util.prefs.Preferences;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.passkeysoft.opfedit.business.FileTypeSwitcher;
import com.passkeysoft.opfedit.ui.EPubEditor;

public class PathPrefModel implements TableModel, FileTypeSwitcher
{
    Preferences prefs = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS );
    
    @Override
    public int getRowCount()
    {
        return 4;
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public String getColumnName( int columnIndex )
    {
        switch( columnIndex )
        {
        case 0:
            return "";
        case 1:
            return "File system path";
        }
        return null;
    }

    @Override
    public Class<?> getColumnClass( int columnIndex )
    {
        return String.class;
    }

    @Override
    public boolean isCellEditable( int rowIndex, int columnIndex )
    {
        if (1 == columnIndex)
            return true;
        return false;
    }

    @Override
    public Object getValueAt( int rowIndex, int columnIndex )
    {
        if (0 == columnIndex)
        {
            switch (rowIndex)
            {
            case 0:
                return "Temporary files folder";
            case 1:
                return "User defined xsl file folder";
            case 2:
                return "Default program file folder";
            case 3:
                return "User defined .css file";
            }
        }
        else switch (rowIndex)
        {
        case 0:
            return prefs.get( "temp", null );
        case 1:
            return prefs.get( "userxsl", null );
        case 2:
            return prefs.get( "programs", null );
        case 3:
            return prefs.get( "usercss", null );
            
        }
        return null;
    }

    @Override
    public void setValueAt( Object aValue, int rowIndex, int columnIndex )
    {
        if (1 == columnIndex) switch (rowIndex)
        {
        case 0:
            prefs.put( "temp", (String) aValue );
            break;
        case 1:
            prefs.put( "userxsl", (String) aValue );
            break;
        case 2:
            prefs.put( "programs", (String) aValue );
            break;
        case 3:
            prefs.put( "usercss", (String) aValue );
            break;
        }
    }

    @Override
    public void addTableModelListener( TableModelListener l )
    {
//        throw new UnsupportedOperationException();
    }

    @Override
    public void removeTableModelListener( TableModelListener l )
    {
//        throw new UnsupportedOperationException();
    }

    @Override
    public int getFileChooserSelectionMode()
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
