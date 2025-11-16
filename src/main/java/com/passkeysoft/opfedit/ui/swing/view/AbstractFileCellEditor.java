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

  $Log: AbstractFileCellEditor.java,v $
  Revision 1.4  2014/07/29 23:17:29  lpassey
  Make text property available to other classes in package.

  Revision 1.3  2012/08/14 21:48:42  lpassey
  Add @Override annotations for methods from TableCellEditor



*/

package com.passkeysoft.opfedit.ui.swing.view;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;

public abstract class AbstractFileCellEditor extends JPanel
        implements ActionListener, TableCellEditor
{
    private static final long serialVersionUID = 1L;

    protected JLabel text = new JLabel();
    private EventListenerList listeners = new EventListenerList();
    protected JButton browser = new JButton( "..." );
    protected int fileSelectionMode;

    transient ChangeEvent changeEvent;

    public AbstractFileCellEditor( int fsm )
    {
        fileSelectionMode = fsm;
        setLayout( new BoxLayout( this, BoxLayout.LINE_AXIS ) );

        browser.setMargin( new Insets( 0, 0, 0, 0 ) );
        browser.setMaximumSize( new Dimension( 16, 16 ) );
        browser.setAlignmentY( 0.0F );
        text.setAlignmentY( 0.0F );
        add( browser );
        add( Box.createHorizontalStrut( 8 ) );
        add( text );

        changeEvent = new ChangeEvent( this );

        browser.addActionListener( this );

    }

    @Override
    public Component getTableCellEditorComponent( JTable table, Object aValue, boolean isSelected,
            int row, int column )
    {
        if (null != aValue)
            text.setText( ((String) aValue).trim() );
        return this;
    }

    @Override
    public boolean shouldSelectCell( EventObject anEvent )
    {
        // The return value of shouldSelectCell() is a boolean indicating
        // whether the editing cell should be selected or not.
        return true;
    }

    @Override
    public Object getCellEditorValue()
    {
        // Returns the value contained in the editor
        return text.getText().trim();
    }

    @Override
    public boolean isCellEditable( EventObject anEvent )
    {
        // Ask the editor if it can start editing using anEvent.
        text.grabFocus();
        return true;
    }

    @Override
    public void addCellEditorListener( CellEditorListener l )
    {
        // add a listener to the list that's notified when the editor
        // starts, stops, or cancels editing.
        listeners.add( CellEditorListener.class, l );
    }

    @Override
    public void removeCellEditorListener( CellEditorListener l )
    {
        // Remove a listener from the list that's notified
        listeners.remove( CellEditorListener.class, l );
    }

    // We must fire an event for the listeners so they know to
    // update the cells
    @Override
    public boolean stopCellEditing()
    {
        Object[] list = listeners.getListenerList();
        for (int i = list.length - 2; i >= 0; i -= 2)
        {
            if (list[i] == CellEditorListener.class)
            {
                ((CellEditorListener) list[i + 1]).editingStopped( changeEvent );
            }
        }
        return true;
    }

    @Override
    public void cancelCellEditing()
    {
        // Tell the editor to cancel editing and not accept any partially edited value.
        Object[] list = listeners.getListenerList();
        for (int i = list.length - 2; i >= 0; i -= 2)
        {
            if (list[i] == CellEditorListener.class)
            {
                ((CellEditorListener) list[i + 1]).editingCanceled( changeEvent );
            }
        }
    }


    abstract protected String getBaseFolder( File baseFile );

    abstract protected String getFilePath( File f );

    @Override
    public void actionPerformed( ActionEvent ev )
    {
        File selected = new File( text.getText() );
        JFileChooser fc = new JFileChooser( getBaseFolder( selected ) );

        fc.setSelectedFile( selected  );
        fc.setFileSelectionMode( fileSelectionMode );
        if (fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION)
        {
            File f = fc.getSelectedFile();

            String filePath = getFilePath( f );
            text.setText( filePath.replace( '\\', '/' ).trim());
        }
        stopCellEditing();
    }

    @Override
    public void addNotify()
    {
        super.addNotify();
        text.grabFocus();
    }

}
