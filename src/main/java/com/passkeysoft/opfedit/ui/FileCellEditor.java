package com.passkeysoft.opfedit.ui;

import java.io.File;

import javax.swing.JFileChooser;


public class FileCellEditor extends AbstractFileCellEditor
{
    public FileCellEditor()
    {
        super( JFileChooser.FILES_ONLY );
    }

    private static final long serialVersionUID = 1L;

    @Override
    protected String getBaseFolder( File baseFile )
    {
        if (null != baseFile)
            return (baseFile.getAbsoluteFile().getParent());
        return null;
    }

    @Override
    protected String getFilePath( File f )
    {
        return f.getAbsolutePath();
    }

}
