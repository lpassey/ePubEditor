package com.passkeysoft.opfedit.datamodels;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class OpfFileFilter extends FileFilter
{
    @Override
    public boolean accept( File file )
    {
        //  This allows us to browse.
        if (file.isDirectory())
            return true;
        if (   file.getName().toLowerCase().endsWith( ".opf" ))
            return true;
        return false;
    }

    @Override
    public String getDescription()
    {
        return "Open eBook Project Files (*.opf)";
    }

}
