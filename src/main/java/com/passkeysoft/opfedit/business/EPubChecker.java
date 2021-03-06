package com.passkeysoft.opfedit.business;

import java.awt.Frame;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.util.EPUBVersion;
import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.validate.EPubFileCheck;

public class EPubChecker extends MonitoredWorker<String, Object>
{
    private EPubFileCheck _ePubFileChecker;
    private EPubModel _ePubModel;
    private Frame _parent;
    private EPUBVersion _version;

    public EPubChecker( Frame parent, EPubModel ePub, EPUBVersion version )
    {
        _ePubFileChecker = new EPubFileCheck();
        _ePubModel = ePub;
        _parent = parent;
        _version = version;
    }

    @Override
    protected String doInBackground() // throws Exception
    {
        try
        {
            return _ePubFileChecker.validate( _ePubModel, _version,this );
        }
        catch( Exception e )
        {
            return e.getMessage();
        }
    }

    @Override
    protected void complete()
    {
        if (!isCancelled()) try
        {
            String report;
            try
            {
                report = get();
            }
            catch( ExecutionException e1 )
            {
                // Should never happen, so this is for debugging only
                e1.printStackTrace();
                _ePubFileChecker.message( MessageId.OPF_004d, (EPUBLocation) null, e1.toString());
                report = _ePubFileChecker.toString();
            }

            if (null != report)
            {
                // open the report, and show it in a dialog
                JTextArea textArea = new JTextArea( 50, 100 );
                textArea.setEditable( false );
                textArea.setLineWrap( true );
                textArea.setWrapStyleWord( true );
                textArea.setText( report );
                textArea.setCaretPosition( 0 );
                JScrollPane scroller = new JScrollPane( textArea );
                JDialog reportDialog = new JDialog( _parent, 
                        "epubcheck results (see http://code.google.com/p/epubcheck/wiki/Errors )", false );
                reportDialog.add( scroller );
                reportDialog.setSize( textArea.getPreferredScrollableViewportSize() );
                reportDialog.setVisible( true );
            }
        }
        catch( CancellationException ignore )
        {
            // the user knew what s/he was doing, no need for more interaction.
        }
        catch( InterruptedException e1 )
        {
            // Should never happen, so this is for debugging only
            e1.printStackTrace();
        }
        
    }

}
