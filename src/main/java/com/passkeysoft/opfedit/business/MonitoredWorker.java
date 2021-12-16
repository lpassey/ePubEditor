package com.passkeysoft.opfedit.business;

import java.beans.PropertyChangeListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.SwingWorker;

/**
 * An abstract class that represents a worker thread that updates it's progress periodically.
 * It implements the Observer interface so it can be passed into a method that reports
 * progress according to the Observable/Observer paradigm.
 * 
 * @author Lee Passey
 *
 */
public abstract class MonitoredWorker<T,V> extends SwingWorker<T,V> implements Observer
{
    @Override
    public void update( Observable o, Object arg )
    {
        firePropertyChange("progress", null, arg);
    }
    
    
    @Override
    protected void done()
    {
        // This implementation fires a property change with a value of -1 when work is complete
        firePropertyChange( "progress", null, new Integer( -1 ));
        
        // Let subclasses finish up their work on the Event Dispatch Thread
        complete();
        
        // turn off all listeners, to help garbage collection on the thread happen faster
        PropertyChangeListener[] listeners = getPropertyChangeSupport().getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++)
        {
            removePropertyChangeListener( listeners[i] );
        }
    }

    /**
     * Executed on the Event Dispatch Thread after the doInBackground method is finished.
     * When this method is called, the Progress Monitor will have been closed, but property
     * listeners are still active.
     */
    protected abstract void complete();
    
}
