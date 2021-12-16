package com.passkeysoft.opfedit.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.passkeysoft.opfedit.datamodels.GuideModel;
import com.passkeysoft.opfedit.datamodels.ManifestHrefComboBoxModel;
import com.passkeysoft.opfedit.datamodels.OPFFileModel;


public class GuidePanel extends JPanel implements ActionListener

{
    private static final long serialVersionUID = 1L;

    private JTable _guideTable = new JTable();

    private TableEditButtons _buttons;
    
    public GuidePanel( OPFFileModel opfData )
    {
        _guideTable.setRowHeight( 20 );
        JScrollPane contentPane = new JScrollPane( _guideTable );

        //  Add all my components
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        add( contentPane );
        add( Box.createVerticalStrut( 4 ));
        _buttons = new TableEditButtons( this, false, false, false );
        add( _buttons );
        setModelData( opfData );
    }

    
    public void setModelData( OPFFileModel opfData )
    {
        boolean enable = false;
        if (null != opfData)
        {
            enable = true;
            _guideTable.setModel( opfData.getGuide() );
            TableColumnModel cm = _guideTable.getColumnModel();
            
            TableColumn col = cm.getColumn( 0 );
            col.setCellEditor( new DefaultCellEditor( new JComboBox( GuideModel.types )));
            col.setPreferredWidth( 100 );
            
            col = cm.getColumn( 1 );
            col.setPreferredWidth( 200 );
            
            col = cm.getColumn( 2 );
            col.setPreferredWidth( 200 );
            ManifestHrefComboBoxModel manifest = new ManifestHrefComboBoxModel( opfData.getManifest(), true );
            col.setCellEditor( new DefaultCellEditor( new JComboBox( manifest )));
        }
        _buttons.setEnabled( TableEditButtons.delCommand, enable );
        _buttons.setEnabled( TableEditButtons.addCommand, enable );
    }
    
    
    @Override
    public void actionPerformed( ActionEvent event )
    {
        GuideModel m = (GuideModel) _guideTable.getModel();
        int rowIndex = _guideTable.getSelectedRow();
        String command = event.getActionCommand();
        if (command.equals( TableEditButtons.addCommand ))
        {
            int r = _guideTable.getRowCount();
            m.setValueAt( null, r, 0 );
        }
        else if (command.equals( TableEditButtons.delCommand ))
        {
            m.removeRow( rowIndex );
        }
    }


    public void stopEditing()
    {
        TableCellEditor ed = _guideTable.getCellEditor();
        if (null != ed)
            ed.stopCellEditing();
    }

}
