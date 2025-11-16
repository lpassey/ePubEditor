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

  $Log: SpinePanel.java,v $
  Revision 1.5  2013/07/03 22:21:00  lpassey
  1. Move selection to newly added row.
  2. Pop up warning when there are no eligible files to add from the Manifest.


*/


package com.passkeysoft.opfedit.ui.swing.view;

import javax.swing.*;
import javax.swing.table.*;

import com.passkeysoft.opfedit.ui.swing.model.ManifestIdComboBoxModel;
import com.passkeysoft.opfedit.ui.swing.model.OPFFileModel;
import com.passkeysoft.opfedit.ui.swing.model.SpineModel;

import java.awt.event.*;

public class SpinePanel extends JPanel
        implements ActionListener
{
	private static final long serialVersionUID = -750275842156441128L;

	JTable contentTable = new JTable();
	JScrollPane contentPane;
	JComboBox types = new JComboBox();

	// TODO: more robust enabling and disabling of buttons -- all buttons disabled if nothing is
	// selected. Move up only when first item is not selected. Move down only when last item is
	// not selected. Add only when eligible, unmanifested items exist. Remove only when something
	// is selected.
    private TableEditButtons _buttons;

	public SpinePanel( OPFFileModel opfData )
	{
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

		contentTable.setRowHeight( 20 );
		contentPane = new JScrollPane( contentTable );

		//  Add all my components
		add( contentPane );
        add(Box.createVerticalStrut( 4 ));
        _buttons = new TableEditButtons( this, true, false, false );
        add( _buttons );
        setModelData( opfData );
	}


	public void setModelData( OPFFileModel opfData )
	{
	    boolean enable = false;
		if (null != opfData)
		{
		    enable = true;
		    // Note: entries in the spine which do not exist in the manifest
		    // will not be included in the displayed list.
		    opfData.getSpine().addActionListener( this );
		    // TODO: Remove items from the spine which are not manifested!!
    		contentTable.setModel( opfData.getSpine() );
    		contentTable.getSelectionModel().setSelectionMode( ListSelectionModel.SINGLE_SELECTION );

    		// Fill the combo box with the id of all manifested items.
    		ManifestIdComboBoxModel manifest =
            new ManifestIdComboBoxModel( opfData.getManifest(), false );
            DefaultCellEditor cellEditor = new DefaultCellEditor( new JComboBox( manifest));
            TableColumnModel cm = contentTable.getColumnModel();
            TableColumn col = cm.getColumn( 0 );
            col.setPreferredWidth( 150 );
            col.setCellEditor( cellEditor );
            col = cm.getColumn( 1 );
            col.setPreferredWidth( 450 );
            col = cm.getColumn( 2 );
            col.setPreferredWidth( 300 );
		}
        _buttons.setEnabled( TableEditButtons.delCommand, enable );
        _buttons.setEnabled( TableEditButtons.addCommand, enable );
        _buttons.setEnabled( TableEditButtons.upCommand, enable );
        _buttons.setEnabled( TableEditButtons.dnCommand, enable );
 	}


	/*
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(java.awt.event.ActionEvent event)
	{
		String command = event.getActionCommand();
		if (0 == event.getID())
		{
	          JOptionPane.showMessageDialog( null, command, "Error", JOptionPane.ERROR_MESSAGE );
		}
		else
		{
    		SpineModel m = (SpineModel) contentTable.getModel();
    		int rowIndex = contentTable.getSelectedRow();
    		int colIndex = contentTable.getEditingColumn();

    		if (colIndex >=0 && colIndex < 3)
    		{
    			TableCellEditor c = contentTable.getColumnModel().getColumn( colIndex ).getCellEditor();
    			if (c == null)
    				c = contentTable.getDefaultEditor( String.class );
    			if (c != null)
    				c.stopCellEditing();
    		}

    		if (command.equals( TableEditButtons.addCommand ))
    		{
    		    int index =  m.addRow( null, null );
    		    if (0 <= index)
    		    {
                        contentTable.changeSelection( index, 0, false, false );
       		    }
    		    else
    		    {
    		        // pop up a dialog saying there are no manifested items to add.
    		        JOptionPane.showMessageDialog( null,
    		                  "There are no manifested items eligible to be added to the content list.\n"
    		                + "You must add a new content document to the manifest before adding it here.",
    		                "Warning", JOptionPane.WARNING_MESSAGE );
    		    }

    		}
    		else if (command.equals(  TableEditButtons.delCommand  ))
    		{
    			m.removeRow( rowIndex );
    		}
    		else if (command.equals(  TableEditButtons.upCommand  ))
    		{
    		    contentTable.changeSelection( m.moveRowUp( rowIndex ), 0, false, false );
    		}
    		else if (command.equals(  TableEditButtons.dnCommand  ))
    		{
                    contentTable.changeSelection( m.moveRowDn( rowIndex ), 0, false, false );
    		}
            }
	}


    public void stopEditing()
    {
        TableCellEditor ed = contentTable.getCellEditor();
        if (null != ed)
            ed.stopCellEditing();
    }
}