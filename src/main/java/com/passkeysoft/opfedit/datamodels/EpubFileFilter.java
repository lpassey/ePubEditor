package com.passkeysoft.opfedit.datamodels;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class EpubFileFilter extends FileFilter
{
    @Override
    public boolean accept( File file )
    {
        //  This allows us to browse.
        if (file.isDirectory())
            return true;
        return file.getName().toLowerCase().endsWith( ".epub" )
            || file.getName().toLowerCase().endsWith( ".zip" );
    }

    @Override
    public String getDescription()
    {
        return "Open eBook Publication Files (*.epub, *.zip)";
    }

}
