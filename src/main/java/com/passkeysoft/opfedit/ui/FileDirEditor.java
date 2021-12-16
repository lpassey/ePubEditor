package com.passkeysoft.opfedit.ui;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;

import com.passkeysoft.opfedit.business.FileTypeSwitcher;


public class FileDirEditor extends AbstractFileCellEditor
{
    private static final long serialVersionUID = 1L;
    private FileTypeSwitcher _switcher;
    
    FileDirEditor( FileTypeSwitcher switcher )
    {
        super( JFileChooser.DIRECTORIES_ONLY );
        _switcher = switcher;
    }

    @Override
    protected String getBaseFolder( File baseFile )
    {
        return baseFile.getAbsolutePath();
    }

    @Override
    protected String getFilePath( File f )
    {
        return f.getAbsolutePath();
    }

    @Override
    public void actionPerformed( ActionEvent ev )
    {
        super.fileSelectionMode = _switcher.getFileChooserSelectionMode();
        super.actionPerformed( ev );
    }
}
