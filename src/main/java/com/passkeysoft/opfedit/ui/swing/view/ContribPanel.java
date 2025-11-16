package com.passkeysoft.opfedit.ui.swing.view;

import javax.swing.*;

import java.awt.event.*;
import javax.swing.table.*;

import com.passkeysoft.opfedit.ui.swing.model.ContributorModel;
import com.passkeysoft.opfedit.datamodels.EPubModel;

public class ContribPanel extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private JTable _contribTable = new JTable();;
    private JComboBox _roles = new JComboBox();
    private TableEditButtons _buttons;


    public ContribPanel( EPubModel creators )
    {
        // Load static values for contributor roles.
        for (int i = 0; i < ContributorModel.roles.length; i++)
        {
            _roles.addItem( ContributorModel.roles[i][1] );
        }
        _contribTable.setRowHeight( 20 );
        JScrollPane contribPane = new JScrollPane( _contribTable );

        setLayout( new BoxLayout( this, BoxLayout.PAGE_AXIS ));

        add(contribPane);
        add(Box.createVerticalStrut( 4 ));
        _buttons = new TableEditButtons( this, true, false, false );
        add( _buttons );
        setModelData( creators );
    }


    public void setModelData( EPubModel creators )
    {
        boolean enable = false;
        if (null != creators)
        {
            enable = true;
            _contribTable.setModel( new ContributorModel( creators ));
            TableColumnModel cm = _contribTable.getColumnModel();
            TableColumn col = cm.getColumn(1);
            col.setCellEditor( new DefaultCellEditor( _roles ));
            for (int i = 0; i < 3; i++)
            {
                col = cm.getColumn(i);
                col.setPreferredWidth( 100 * (((i + 1) % 2)) + 1);
            }
        }
        _buttons.setEnabled( TableEditButtons.delCommand, enable );
        _buttons.setEnabled( TableEditButtons.addCommand, enable );
        _buttons.setEnabled( TableEditButtons.upCommand, enable );
        _buttons.setEnabled( TableEditButtons.dnCommand, enable );
    }


    public ContributorModel getModelData()
    {
        return (ContributorModel) _contribTable.getModel();
    }


    public void actionPerformed( ActionEvent ev )
    {
        ContributorModel m = (ContributorModel) _contribTable.getModel();
        int rowIndex = _contribTable.getSelectedRow();
        int colIndex = _contribTable.getEditingColumn();
        int numRows = _contribTable.getRowCount();
        if (colIndex >=0 && colIndex < 3)
        {
            TableCellEditor c = _contribTable.getColumnModel().getColumn( colIndex ).getCellEditor();
            if (c == null)
                c = _contribTable.getDefaultEditor( String.class );
            if (c != null)
                c.stopCellEditing();
        }
        String command = ev.getActionCommand();
        if (command.equals( TableEditButtons.addCommand ))
        {
            m.setValueAt( new String(), numRows, 0);
        }
        else if (numRows > 0)
        {
            if (command.equals( TableEditButtons.delCommand ))
            {
                m.removeRow( rowIndex );
            }
            else if (command.equals( TableEditButtons.upCommand ))
            {
                int index = m.moveRowUp( rowIndex );
                _contribTable.setRowSelectionInterval( index, index );
            }
            else if (command.equals( TableEditButtons.dnCommand ))
            {
                int index = m.moveRowDn( rowIndex );
                _contribTable.setRowSelectionInterval( index, index );
            }
        }
    }


    public void stopEditing()
    {
        TableCellEditor ed = _contribTable.getCellEditor();
        if (null != ed)
            ed.stopCellEditing();
    }
}