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

package com.passkeysoft.opfedit.datamodels;

import java.util.prefs.Preferences;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import com.passkeysoft.opfedit.business.FileTypeSwitcher;
import com.passkeysoft.opfedit.ui.EPubEditor;

public class PathPrefModel implements TableModel, FileTypeSwitcher
{
    private Preferences _prefs = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS );
    
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
        return 1 == columnIndex;
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
            return _prefs.get( "temp", null );
        case 1:
            return _prefs.get( "userxsl", null );
        case 2:
            return _prefs.get( "programs", null );
        case 3:
            return _prefs.get( "usercss", null );
            
        }
        return null;
    }

    @Override
    public void setValueAt( Object aValue, int rowIndex, int columnIndex )
    {
        if (1 == columnIndex) switch (rowIndex)
        {
        case 0:
            _prefs.put( "temp", (String) aValue );
            break;
        case 1:
            _prefs.put( "userxsl", (String) aValue );
            break;
        case 2:
            _prefs.put( "programs", (String) aValue );
            break;
        case 3:
            _prefs.put( "usercss", (String) aValue );
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
