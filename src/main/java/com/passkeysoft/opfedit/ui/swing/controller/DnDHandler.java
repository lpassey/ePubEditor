package com.passkeysoft.opfedit.ui.swing.controller;

import com.passkeysoft.opfedit.ui.swing.view.ManifestPanel;
import com.passkeysoft.opfedit.ui.swing.model.ManifestModel;
import com.passkeysoft.opfedit.ui.swing.model.MediaTypeModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
// import javax.swing.JOptionPane;
import javax.swing.TransferHandler;

/**
 * $Log: DnDHandler.java,v $
 * Revision 1.1  2012/08/14 21:22:56  lpassey
 * Add drag and drop handler for manifested items from code originally
 * contributed by cstavas at sourceforge
 *
 *
 * @author Ctsavas at sourceforge and lpassey
 */
public class DnDHandler extends TransferHandler
{
    private static final long serialVersionUID = 1L;
    private ManifestPanel _manifest;

    public DnDHandler( ManifestPanel manifest )
    {
        _manifest = manifest;
    }

    /**
     * Constantly checks if the "drop" hovering is a valid one.
     * @param support Helps with data management.
     * @return True if the data is valid, false otherwise.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean canImport( TransferSupport support )
    {
        Transferable t = support.getTransferable();
        try
        {
            List<File> _files =
                (java.util.List<File>) t.getTransferData( DataFlavor.javaFileListFlavor );

            for (File file : _files)
            {
                // if any one file can be dropped, then the group is acceptable.
                String newType =
                        new MediaTypeModel().resolveMediaType( null, file.getName() );
                if (!newType.equals( "other" ))
                    return true;
            }
        }
        catch( InvalidDnDOperationException ex )
        {
            // There is a bug in the API that calls this method before calling importData(),
            // but the data cannot be examined until acceptDrop has been called, resulting in
            // this exception. I work around the bug by returning true if the exception is thrown;
            // importData() itself will not import bad files, so this check is unnecessary.
            return true;
        }
        catch( UnsupportedFlavorException | IOException ex )
        {
            // I can't accept anything that's not a list.
            return false;
        }
        return false;

    }

    /**
     * The method that occurs with/and receives the drop. It gets the file path,
     * adds it to ManifestModel, and refreshes the ManifestPanel.
     * @param support Helps with data management.
     * @return True if everything went ok, false if an exception happened.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean importData( TransferSupport support )
    {
        try
        {
            Transferable t = support.getTransferable();
            List<File> _files =
                (java.util.List<File>)t.getTransferData(DataFlavor.javaFileListFlavor);

            // Add every file with a recognizable media type to the manifest
            // (NOTE: this will not add those files to the spine...)
            for (File file:_files)
            {
                String newType = new MediaTypeModel().resolveMediaType( null, file.getName() );
                if (newType.equals( "other" ))
                    continue;
                ManifestModel manifest = _manifest.getModel();
                manifest.addManifestItem( null, manifest.fileData. getPathRelativeToOpf( file ), null );
            }
            _manifest.refresh();
        }
        catch( UnsupportedFlavorException | IOException e )
        {
            return false;
        }
        return true;
    }

}
