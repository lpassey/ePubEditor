package com.passkeysoft.opfedit.ui.swing.view;

import com.passkeysoft.opfedit.ui.swing.controller.FileCellEditor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.passkeysoft.opfedit.ui.swing.model.ExternalEditorData;

public class SetEditorsDialog extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;
    protected Frame _owner;
    private JTable _editorTable;

    public SetEditorsDialog( Frame owner )
    {
        super( owner, "Select editors for media-types", true );

        _editorTable = new JTable( new ExternalEditorData() );
        _editorTable.setRowHeight( 20 );
        _editorTable.setFillsViewportHeight(true);
        _editorTable.setEnabled( true );

        setLayout( new BoxLayout( this.getContentPane(), BoxLayout.Y_AXIS ));

        JScrollPane scrollerPane = new JScrollPane( _editorTable );
        add( scrollerPane );

        getContentPane().add(Box.createVerticalStrut( 4 ));
        TableEditButtons _buttons = new TableEditButtons( this, false, false, true );
        add( _buttons );

        setSize( 800, 480 );
        TableColumnModel cm = _editorTable.getColumnModel();

        TableColumn col = cm.getColumn( 0 );

        col.setPreferredWidth( 200 );

        col = cm.getColumn( 1 );
        col.setPreferredWidth( 400 );
        FileCellEditor ed = new FileCellEditor();
        col.setCellEditor( ed );

        col = cm.getColumn( 2 );
        col.setPreferredWidth( 200 );
    }


    @Override
    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand();
        ExternalEditorData m = (ExternalEditorData) _editorTable.getModel();
        int rowIndex[] = _editorTable.getSelectedRows();
        if (command.equals( TableEditButtons.addCommand ))
        {
            // Create a dialog with a combo box listing the media-types which are /not/
            // in the current list, a FileCellEditor to browse the system, and a
            // free-form text input area to collect the command line.
            SelectAssociatedProgram picker = new SelectAssociatedProgram( _owner,
                    "Set additional editor", m._mediaTypes, m._editors );
            int option = picker.showDialog();
            if (JOptionPane.OK_OPTION == option)
            {
                m.addEditor( picker.getMediaType(), picker.getProgramFile(), picker.getCommandLine() );
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
