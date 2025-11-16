package com.passkeysoft.opfedit.ui.swing.controller;

import com.passkeysoft.opfedit.ui.swing.view.AbstractFileCellEditor;
import java.awt.Component;
import java.io.File;
// import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JTable;


public class OPFFileCellEditor extends AbstractFileCellEditor
{
    private static final long serialVersionUID = 1L;

    private EPubEditor _editor;
    private JTable manifest;
    private int activeRow;
    public OPFFileCellEditor( EPubEditor editor )
    {
        super( JFileChooser.FILES_ONLY );
        _editor = editor;

    }

    @Override
    protected String getBaseFolder( File f )
    {
        File opfFolder = _editor.getEPubModel().getOpfFolder();
        File base = new File( opfFolder, text.getText() );
        return base.getParentFile().getAbsolutePath();
    }


    @Override
    public Component getTableCellEditorComponent( JTable table, Object aValue, boolean isSelected,
            int row, int column )
    {
        if (column == 1)
        {
            manifest = table;
            activeRow = row;
            String fileName = (String) manifest.getValueAt( activeRow, 2 );

            text.setText( fileName );
        }
        return this;
    }

    /**
     * Adding or modifying the path of a file in the manifest.
     * If the file is not in the opf relative path, save it as an absolute path.
     */
    @Override
    protected String getFilePath( File f )
    {
//        try
        {
            String relative = _editor.getEPubModel().getPathRelativeToOpf( f );
            File relPath = new File( relative );
            if (relPath.isAbsolute())
            {
                if (null != _editor.getEPubModel().copyFileToOpf( new File( relative )))
                    relative = relPath.getName();
            }
            return relative;
        }
//        catch (IOException ex)
//        {
//            ex.printStackTrace();
//        }
//        return null;
    }

    @Override
    public boolean stopCellEditing()
    {
        manifest.setValueAt( text.getText(), activeRow, 2 );
        return super.stopCellEditing();
    }
}
