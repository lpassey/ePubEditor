/*-*
   Copyright-Only Dedication (based on United States law)
  
  The person or persons who have associated their work with this
  document (the "Dedicator") hereby dedicate whatever copyright they
  may have in the work of authorship herein (the "Work") to the
  public domain.
  
  Dedicator makes this dedication for the benefit of the public at
  large and to the detriment of Dedicator's heirs and successors.
  Dedicator intends this dedication to be an overt act of
  relinquishment in perpetuity of all present and future rights
  under copyright law, whether vested or contingent, in the Work.
  Dedicator understands that such relinquishment of all rights
  includes the relinquishment of all rights to enforce (by lawsuit
  or otherwise) those copyrights in the Work.
  
  Dedicator recognizes that, once placed in the public domain, the
  Work may be freely reproduced, distributed, transmitted, used,
  modified, built upon, or otherwise exploited by anyone for any
  purpose, commercial or non-commercial, and in any way, including
  by methods that have not yet been invented or conceived.
  
  $Log: XFormFiles.java,v $
  Revision 1.4  2013/07/03 22:11:38  lpassey
  Improve error handling when .opf file has not yet been created.
*/


package com.passkeysoft.opfedit.business;

import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.datamodels.OPFFileModel;
import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.ui.EPubEditor;

public class XFormFiles extends MonitoredWorker<String, Void>
{
    private StringBuilder _sb = new StringBuilder();
    private EPubModel _epubModel;
    private OPFFileModel _opfData;
    private Frame _parent;
    private JTable _tableData;
    private MonitorThread _stdoutMonitor, _stderrMonitor;
    private int[] rows;
    
    class MonitorThread extends Thread
    {
        private BufferedReader _reader;
        private StringBuilder _sb;
        
        MonitorThread( InputStream in, String header )
        {
            _reader = new BufferedReader( new InputStreamReader( in ));
            _sb = new StringBuilder( header );
        }
        
        public void run()
        {
            String line = "";
            
            while (null != line) try
            {
                line = _reader.readLine();
                if (null != line && 0 < line.length())
                    _sb.append( line ).append( "\n" );
            }
            catch( IOException ignore ) { }
        }
        
        public String toString()
        {
            return _sb.toString();
        }
    }
    
    
    public XFormFiles(Frame parent, EPubModel ePub, JTable td )
    {
        _parent = parent;
        _epubModel = ePub;
        _tableData = td;
        _opfData = ePub.getOpfData();
        rows = _tableData.getSelectedRows();
    }
    
    public int getNumRows()
    {
        return rows.length;
    }
    
    
    @Override
    protected void complete()
    {
        JTextArea textArea = new JTextArea( 32, 80 );
        textArea.setEditable( false );
        textArea.setLineWrap( true );
        textArea.setWrapStyleWord( true );
        textArea.setText( _sb.toString() );
        JScrollPane scroller = new JScrollPane( textArea );
        JDialog reportDialog = new JDialog( _parent, "Transformer results", false );
        reportDialog.add( scroller );
        reportDialog.setSize( textArea.getPreferredScrollableViewportSize() );
        reportDialog.setVisible( true );
    }

    @Override
    protected String doInBackground()
    {
        String fileName;
        
        for (int i = 0; i < rows.length; i++)
        {
            String id = (String) _tableData.getValueAt( rows[i], 0 );
            String href = _opfData.getManifest().getHrefById( id );
            if (new File( href ).isAbsolute())
                fileName = href;
            else
                fileName = _epubModel.getOpfFolder() + File.separator + href;
            if (_opfData.getManifest().fileData.fileExistsRelativeToOPF( href ))
            {
                String mediaType = _opfData.getManifest().getMediaTypeById( id );
                File xformFile = new File( fileName );
                Preferences prefNode = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ).
                        node( mediaType );
                // this preference should include the full path of the command to be executed,
                // a new mime_type, a new file extension, and ...
                String transformer = prefNode.get( EPubEditor.PREFS_TRANSFORMER, null );
                if (null == transformer)
                {
                    _sb.append( "No transformer is set for files of media-type " )
                        .append( mediaType )
                        .append( "\n\n" );
                }
                else if (new File(transformer).exists()) try
                {
                    // ... the command line arguments, in a format that String.format can understand
                    String commandLine = String.format( prefNode.get( EPubEditor.PREFS_XFORM_CL, "%s" ), 
                            xformFile.getAbsolutePath() );
                    _sb.append( "executing " )
                        .append( transformer )
                        .append( " " )
                        .append( commandLine )
                        .append( "\n" );
                    Process proc = java.lang.Runtime.getRuntime().exec(
                            transformer + " " + commandLine  );

                    // I need to start two threads here, one for each of the outputs,
                    // stdout and stderr. When both threads have completed, I can proceed.
                    
                    _stdoutMonitor = new MonitorThread( proc.getInputStream(), "stdout:\n\n");
                    _stderrMonitor = new MonitorThread( proc.getErrorStream(), "stderr:\n\n");
                    
                    _stdoutMonitor.start();
                    _stderrMonitor.start();
                    
                    proc.waitFor();
                    int retVal = proc.exitValue(); 
                    
                    _sb.append( "\n" )
                        .append( transformer )
                        .append( " completed with return code: " )
                        .append( retVal )
                        .append( "\n\n" )
                        .append( _stderrMonitor )
                        .append( "\n" )
                        .append( _stdoutMonitor );
                    
                    // Rename the file with the new file extension
                    commandLine = FileUtil.getExt( xformFile );
                    String newExt = prefNode.get( EPubEditor.PREFS_XFORM_NEWEXT, commandLine );
                    // if newExt is empty, or if it matches the old extension, don't try to rename.
                    if (0 < newExt.length() && !newExt.equalsIgnoreCase( commandLine ))
                    {
                        fileName = FileUtil.getFileName( xformFile ) 
                                + "." + newExt;
                        File newFile = new File( xformFile.getParentFile(), fileName );
                        if (newFile.exists())
                        {
                            File bakFile = new File( newFile.getAbsolutePath() + ".bak" );
                            while (bakFile.exists())
                            {
                                // won't be able to rename, as the target file exists.
                                // rename the target to *.bak, before trying to rename.
                                bakFile = new File( bakFile.getAbsolutePath() + ".bak" );
                            }
                            newFile.renameTo( bakFile );
                        }
                        if (xformFile.renameTo( newFile ))
                            xformFile = newFile;
                    }
                    // change the media-type field on the manifest, and re-write it.
                    mediaType = prefNode.get( EPubEditor.PREFS_XFORM_NEWMT, mediaType );
                    commandLine = _epubModel.getPathRelativeToOpf( xformFile );
//                    commandLine = FileUtil.getPathRelativeToBase( xformFile, _epubModel.getOpfFile() );
                    _opfData.getManifest().setItemById( id, commandLine, mediaType );
                    update( null, i );
                    if (isCancelled())
                        break;
                }
                catch( IOException | InterruptedException ignore )
                {
                    ignore.printStackTrace();
                }
                else
                {
                    _sb.append( transformer )
                        .append( " cannot be found; \nno transformation was performed.\n" );
                }
            }
            else
                _sb.append( href )
                    .append( " cannot be found, \nno transformation was performed.\n" );
            _sb.append( "\n------------------------------------------\n" );  
        }
        return _sb.toString();
    }

}
