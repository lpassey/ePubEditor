package com.passkeysoft.opfedit.ui.swing.view;

import com.passkeysoft.opfedit.ui.swing.controller.FileDirEditor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.passkeysoft.opfedit.business.FileTypeSwitcher;
import com.passkeysoft.opfedit.ui.swing.model.PathPrefModel;

public class SetPathsDialog extends JDialog implements ActionListener, FileTypeSwitcher
{
    private static final long serialVersionUID = 1L;
    private JTable _pathsTable;

    public SetPathsDialog( Frame owner )
    {
        super( owner, "Set local paths", true );
        _pathsTable = new JTable( new PathPrefModel());
        _pathsTable.setRowHeight( 20 );
        _pathsTable.setFillsViewportHeight(true);
        _pathsTable.setEnabled( true );

        setLayout( new BoxLayout( this.getContentPane(), BoxLayout.Y_AXIS ));

        JScrollPane scrollerPane = new JScrollPane( _pathsTable );
        add( scrollerPane );

        getContentPane().add(Box.createVerticalStrut( 4 ));
        TableEditButtons _buttons = new TableEditButtons( this, false, false, true );
        add( _buttons );

        TableColumnModel cm = _pathsTable.getColumnModel();

        TableColumn col = cm.getColumn( 0 );

        col.setPreferredWidth( 200 );

        col = cm.getColumn( 1 );
        col.setPreferredWidth( 400 );
        FileDirEditor ed = new FileDirEditor( this );
        col.setCellEditor( ed );
        setSize( 800, 360 );
    }


    @Override
    public void actionPerformed( ActionEvent event )
    {
        String command = event.getActionCommand();
        if (command.equals( TableEditButtons.closeCmd ))
        {
            // Tell the Manifest Model to remove the data.
            this.setVisible( false );
        }

    }


    @Override
    public int getFileChooserSelectionMode()
    {
        if (3 == _pathsTable.getSelectedRow())
            return JFileChooser.FILES_ONLY;
        return JFileChooser.DIRECTORIES_ONLY;
    }

}
