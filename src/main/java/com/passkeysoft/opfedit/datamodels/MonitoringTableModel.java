package com.passkeysoft.opfedit.datamodels;

import javax.swing.table.AbstractTableModel;

public abstract class MonitoringTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;
    
    boolean dirty;
    public EPubModel fileData;

    MonitoringTableModel( EPubModel owner )
    {
        fileData = owner;
        dirty = false;
    }
    
    public boolean isDirty()
    {
        return dirty;
    }
    
    @Override
    public void fireTableDataChanged()
    {
        dirty = true;
        super.fireTableDataChanged();
    }
}
