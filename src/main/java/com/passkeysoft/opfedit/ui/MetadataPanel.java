package com.passkeysoft.opfedit.ui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

import com.passkeysoft.opfedit.datamodels.MetadataModel;

public class MetadataPanel extends JPanel implements java.awt.event.ActionListener
{
    private static final long serialVersionUID = 4835607457577053944L;
    
	JScrollPane tablePane = new JScrollPane();
	JTable propertyTable = new JTable();

    private TableEditButtons _buttons;
	
	public MetadataPanel( MetadataModel properties )
	{
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		add(tablePane);
		tablePane.getViewport().add(propertyTable);
        add(Box.createVerticalStrut( 4 ));
        _buttons = new TableEditButtons( this, true, false, false );
		add( _buttons );
		
	    setModelData( properties );
	}


	/**
	 * 
	 * @param properties
	 */
	public void setModelData( MetadataModel properties )
	{
	    boolean enable = false;
	    if (null != properties)
	    {
	        enable = true;
            propertyTable.setRowHeight( 20 );
    		propertyTable.setModel( properties );
    		JComboBox metaNames = new JComboBox( MetadataModel.propNames );
    		metaNames.setEditable( true );
            DefaultCellEditor cellEditor = new DefaultCellEditor( metaNames );
            TableColumnModel cm = propertyTable.getColumnModel();
            TableColumn col = cm.getColumn(0);
            col.setCellEditor( cellEditor );
            col.setPreferredWidth( 150 );
            
            col = cm.getColumn( 1 );
            col.setPreferredWidth( 300 );
          
//            col.setMaxWidth( 14 );
//            col.setMinWidth( 14 );
            
            col = cm.getColumn( 2 );
            col.setPreferredWidth( 100 );
            
            col = cm.getColumn( 3 );
            col.setPreferredWidth( 200 );
        }
        _buttons.setEnabled( TableEditButtons.delCommand, enable );
        _buttons.setEnabled( TableEditButtons.addCommand, enable );
        _buttons.setEnabled( TableEditButtons.upCommand, enable );
        _buttons.setEnabled( TableEditButtons.dnCommand, enable );

	}
	
	
	/*
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
    public void actionPerformed(ActionEvent event)
    {
        // This method is derived from interface java.awt.event.ActionListener
		MetadataModel m = (MetadataModel) propertyTable.getModel();
		int rowIndex = propertyTable.getSelectedRow();
		int colIndex = propertyTable.getEditingColumn();
		if (colIndex >=0 && colIndex < 3)
		{
			TableCellEditor c = propertyTable.getColumnModel().getColumn( colIndex ).getCellEditor();
			if (c == null)
				c = propertyTable.getDefaultEditor( String.class );
			if (c != null)
				c.stopCellEditing();
		}
		String command = event.getActionCommand();
		if (command.equals( TableEditButtons.addCommand ))
		{
		    m.addRow();
		}
		else if (command.equals( TableEditButtons.delCommand ))
		{
			m.removeRow( rowIndex );
		}
		else if (command.equals( TableEditButtons.upCommand ))
		{
			int index = m.moveRowUp( rowIndex );
			propertyTable.setRowSelectionInterval( index, index );
		}
		else if (command.equals( TableEditButtons.dnCommand ))
		{
			int index = m.moveRowDn( rowIndex );
			propertyTable.setRowSelectionInterval( index, index );
		}
		propertyTable.setSize( tablePane.getSize() );
	}


    public void stopEditing()
    {
        TableCellEditor ed = propertyTable.getCellEditor();
        if (null != ed)
            ed.stopCellEditing();
    }

}