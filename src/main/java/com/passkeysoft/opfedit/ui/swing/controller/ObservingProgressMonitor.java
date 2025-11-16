package com.passkeysoft.opfedit.ui.swing.controller;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ProgressMonitor;

public class ObservingProgressMonitor extends ProgressMonitor implements PropertyChangeListener
{
    int progress;
    private Component _parent;
    MonitoredWorker<?,?> watched;

    public ObservingProgressMonitor( Component parentComponent, MonitoredWorker<?,?> worker,
            Object message, String note, int min, int max )
    {
        // setup the parameters for the Progress Monitor
        super( parentComponent, message, note, min, max );
        _parent = parentComponent;
        watched = worker;
        worker.addPropertyChangeListener( this );
        progress = 0;
        _parent.setEnabled( false );
    }


    @Override
    public void propertyChange( PropertyChangeEvent evt )
    {
        // No other property type is anticipated, but the following
        // test is added as an abundance of caution
        if (evt.getPropertyName().equals( "progress" ))
        {
            Object arg = evt.getNewValue();
            if (null == arg)
            {
                progress++;
            }
            else if (arg instanceof Float && ((Float) arg).floatValue() < 1 )
            {
                // If the object is a floating point number and less than 1 assume a percentage
                progress = (int) (((Float) arg).floatValue() * getMaximum());
            }
            else if (arg instanceof Number)
            {
                // If the object is any other number, just set progress to this value
                progress = ((Number) arg).intValue();
            }
            else
            {
                // no clue as to what we're being told, just move one step ahead.
                progress++;
            }

            if (0 > progress)
            {
                // if progress is less than 0, we're done
                close();
                _parent.setEnabled( true );
            }
            else if (isCanceled())
            {
                // If the user asked for cancellation, pass that message on to the worker,
                // but don't stop processing in case the worker ignores the message.
                watched.cancel( false );
            }
            else
            {
                // If progress exceeds the max, return it to zero. Watch out for
                // division by zero!
                setProgress( 0 < getMaximum() ? progress  % getMaximum() : 0 );
            }
        }
    }
}
