package com.passkeysoft.opfedit.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.passkeysoft.opfedit.datamodels.ExternalXformerData;

public class SetTransformersDialog extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;
    
    protected Frame _owner;
    private JTable _xformerTable;
    private JComboBox _newMediaType;
    
    public SetTransformersDialog( Frame owner )
    {
        super( owner, "Select transformers for media-types" );
        // TODO Auto-generated constructor stub
        _xformerTable = new JTable( new ExternalXformerData() );
        _xformerTable.setRowHeight( 20 );
        _xformerTable.setFillsViewportHeight(true);
        _xformerTable.setEnabled( true );
        
        setLayout( new BoxLayout( this.getContentPane(), BoxLayout.Y_AXIS ));

        JScrollPane scrollerPane = new JScrollPane( _xformerTable );
        add( scrollerPane );

        getContentPane().add(Box.createVerticalStrut( 4 ));
        TableEditButtons _buttons = new TableEditButtons( this, false, false, true );
        add( _buttons );

        setSize( 800, 480 );
        TableColumnModel cm = _xformerTable.getColumnModel();
        
        TableColumn col = cm.getColumn( 0 );
        
        col.setPreferredWidth( 200 );

        col = cm.getColumn( 1 );
        col.setPreferredWidth( 400 );
        FileCellEditor ed = new FileCellEditor();
        col.setCellEditor( ed );
        
        col = cm.getColumn( 2 );
        col.setPreferredWidth( 200 );
        
        col = cm.getColumn( 3 );
        col.setPreferredWidth( 200 );
        _newMediaType = new JComboBox( ((ExternalXformerData) (_xformerTable.getModel()))._mediaTypes );
        DefaultCellEditor dce = new DefaultCellEditor( _newMediaType );
        col.setCellEditor( dce );
        
        col = cm.getColumn( 4 );
        col.setPreferredWidth( 200 );
    }

    
    @Override
    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand();
        ExternalXformerData m = (ExternalXformerData) _xformerTable.getModel();
        int rowIndex[] = _xformerTable.getSelectedRows();
        if (command.equals( TableEditButtons.addCommand ))
        {
            // Create a dialog with a combo box listing the media-types which are /not/
            // in the current list, a FileCellEditor to browse the system, and a 
            // free-form text input area to collect the command line.
            SelectAssociatedProgram picker = new SelectAssociatedProgram( _owner, 
                    "Set additional transformer", m._mediaTypes, m._xformers );
            int option = picker.showDialog();
            if (JOptionPane.OK_OPTION == option)
            {
                m.addEditor( picker.getMediaType(), picker.getProgramFile(), 
                             picker.getCommandLine(), "", "" );
            }
        }
        else if (command.equals( TableEditButtons.delCommand ))
        {
            // Tell the Manifest Model to remove the data.
            m.removeRows( rowIndex );
        }
        else if (command.equals( TableEditButtons.closeCmd ))
        {
            // Tell the Manifest Model to remove the data.
            this.setVisible( false );
        }

    }

}
