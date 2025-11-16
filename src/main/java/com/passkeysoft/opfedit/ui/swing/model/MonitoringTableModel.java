package com.passkeysoft.opfedit.ui.swing.model;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import javax.swing.table.AbstractTableModel;

public abstract class MonitoringTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1L;

    private boolean _dirty;
    public EPubModel fileData;

    MonitoringTableModel( EPubModel owner )
    {
        fileData = owner;
        _dirty = false;
    }

    public boolean isDirty()
    {
        return _dirty;
    }

    @Override
    public void fireTableDataChanged()
    {
        _dirty = true;
        super.fireTableDataChanged();
    }
}
