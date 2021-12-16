/**
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
  
  $Log: ManifestPanel.java,v $
  Revision 1.9  2014/07/29 22:34:32  lpassey
  Select file from file system using icon, not file name.

  Revision 1.8  2013/07/03 22:24:04  lpassey
  1. Move selection to newly added row.
  2. Pop up a question dialog to ask if newly manifested files should also be added
   to the spine.

  Revision 1.7  2013/06/26 17:49:35  lpassey
  1. Implement RowSorter interface to allow Manifest view to be sorted on various columns
  2. Add Public Domain "licence"


*/
package com.passkeysoft.opfedit.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.passkeysoft.opfedit.business.DnDHandler;
import com.passkeysoft.opfedit.datamodels.ManifestModel;
import com.passkeysoft.opfedit.datamodels.MediaTypeModel;
import com.passkeysoft.opfedit.datamodels.SpineModel;
import com.passkeysoft.opfedit.staticutil.FileUtil;

/**
 * @author W. Lee Passey
 *
 */
public class ManifestPanel extends JPanel 
        implements ActionListener, ChangeListener, ListSelectionListener
{
    private static final long serialVersionUID = 1L;
    private JTable _manifestTable;
    private JComboBox _media_types = new JComboBox( new MediaTypeModel());
    private TableEditButtons _buttons;
    private EPubEditor _master;
    private boolean _isSelected = false;
    
    class Watched extends Observable
    {
        public String command;
        
        public void trigger( String cmd, Object id )
        {
            command = cmd;
            setChanged();
            notifyObservers( id );
        }
    }
    Watched agent = new Watched();
    
    public class ManifestRowSorter extends RowSorter<ManifestModel>
    {
        private com.passkeysoft.opfedit.datamodels.ManifestModel _model;
        LinkedList<RowSorter.SortKey> _sortKeys;
        
        public ManifestRowSorter( ManifestModel m )
        {
            _model = m;
            _sortKeys = new LinkedList<RowSorter.SortKey>( );
            // nothing is sorted to begin with.
            for (int i = 0; i < 4; i++ )
            {
                _sortKeys.add( new SortKey( i, SortOrder.UNSORTED ));
            }
        }
        
        @Override
        public ManifestModel getModel()
        {
            return _model;
        }
    
        @Override
        public void toggleSortOrder( int column )
        {
            // skip any non sorting columns.
            if (column == 1)
                return;
            try
            {
                SortKey key = null;
                // Find the SortKey for this column.
                for (int i = 0; i < _sortKeys.size(); i++)
                {
                    key = _sortKeys.get( i );
                    if (column == key.getColumn())
                        break;
                    key = null;
                }
                if (null != key)
                {
                    // Remove this key from the list, and add a replacement at the beginning
                    _sortKeys.remove( key );
                    if (key.getSortOrder() == SortOrder.ASCENDING)
                    {
                        key = new SortKey(column, SortOrder.DESCENDING);
                    }
                    else // current order is either descending or not sorted.
                         // in either case, switch to ascending order.
                    {
                        key = new SortKey( column, SortOrder.ASCENDING );
                    }
                    _model.sort( key );
                    _sortKeys.addFirst( key );
                }
            }
            catch (IndexOutOfBoundsException ex )
            {}
            
        }
    
        // The model and the view have identical structures.
        @Override
        public int convertRowIndexToModel( int index )
        {
            return index;
        }
    
        @Override
        public int convertRowIndexToView( int index )
        {
            return index;
        }
    
        @Override
        public void setSortKeys( List<? extends javax.swing.RowSorter.SortKey> keys )
        {
            // Nobody gets to set sort order except me.
//            _sortKeys = (List<javax.swing.RowSorter.SortKey>) keys;    
        }
    
        @Override
        public List<? extends javax.swing.RowSorter.SortKey> getSortKeys()
        {
            return _sortKeys;
        }
    
        @Override
        public int getViewRowCount()
        {
             return _model.getRowCount();
        }
    
        @Override
        public int getModelRowCount()
        {
            return _model.getRowCount();
        }
    
        @Override
        public void modelStructureChanged()
        {
//            System.out.println( "modelStructureChanged" );
        }
    
        @Override
        public void allRowsChanged()
        {
//            System.out.println( "allRowsChanged" );
        }
    
        @Override
        public void rowsInserted( int firstRow, int endRow )
        {
//            System.out.println( "rowsInserted" );    
        }
    
        @Override
        public void rowsDeleted( int firstRow, int endRow )
        {
//            System.out.println( "rowsDeleted" );    
        }
    
        @Override
        public void rowsUpdated( int firstRow, int endRow )
        {
//            System.out.println( "rowsUpdated" );    
        }
    
        @Override
        public void rowsUpdated( int firstRow, int endRow, int column )
        {
//            System.out.println( "rowsUpdated" );
        }
    
    }
    /**
     * Constructor
     */
    public ManifestPanel( EPubEditor master )
    {
        this._master = master;
        _manifestTable = new JTable();
        ListSelectionModel lsm = _manifestTable.getSelectionModel();
        lsm.addListSelectionListener( this );
        
        _media_types.addActionListener( this );
        setLayout( new BoxLayout( this,BoxLayout.Y_AXIS ));
        _manifestTable.setRowHeight( 20 );
        JScrollPane manifestPane = new JScrollPane( _manifestTable );
        add( manifestPane );
        add(Box.createVerticalStrut( 4 ));
        _buttons = new TableEditButtons( this, false, true, false );
        _buttons.setEnabled( TableEditButtons.delCommand, false );
        _buttons.setEnabled( TableEditButtons.edCommand, false );
        _buttons.setEnabled( TableEditButtons.burstCmd, false );
        _buttons.setEnabled( TableEditButtons.xformCmd, false );
        setModelData( null == master.getOpfData() 
                ? null 
                : master.getOpfData().getManifest() );
        add( _buttons );
        
        //Ctsavas initialization space start
        DnDHandler dndHandler = new DnDHandler( this );
        setTransferHandler( dndHandler );
        //Ctsavas initialization space end
        
    }

    
    public void addObserver( Observer o )
    {
        agent.addObserver( o );
    }
    

    /**
     * Sets the table model backing this table.
     * @param dataFile
     */
    public void setModelData( ManifestModel dataFile )
    {
        boolean enable = false;
        if (null != dataFile ) // && null != dataFile.getManifest())
        {
            enable = true;
            _manifestTable.setModel( dataFile );
//            _manifestTable.setRowSorter( new Sorter( dataFile ));
            _manifestTable.setRowSorter( new ManifestRowSorter( dataFile ));
            TableColumnModel cm = _manifestTable.getColumnModel();
            
            TableColumn col = cm.getColumn( 1 );
          
            col.setMaxWidth( 14 );
            col.setMinWidth( 14 );
            
            col = cm.getColumn( 0 );
            col.setPreferredWidth( 150 );
            
            col = cm.getColumn( 1 );
            OPFFileCellEditor ed = new OPFFileCellEditor( _master );
            col.setCellEditor( ed );

            col = cm.getColumn( 2 );
            col.setPreferredWidth( 400 );
            
            col = cm.getColumn( 3 );
            col.setPreferredWidth( 200 );
            col.setCellEditor( new DefaultCellEditor( _media_types ));
            
            col = cm.getColumn( 4 );
            col.setPreferredWidth( 50 );
        }
        _buttons.setEnabled( TableEditButtons.addCommand, enable );
    }
    
    
    /**
     * Refreshes the view of this table to match the underlying table model.
     */
    public void refresh()
    {
        ManifestModel m = (ManifestModel) _manifestTable.getModel();
        m.fireTableDataChanged();
//        focusGained( null );
    }

    
    @Override
    public void actionPerformed( ActionEvent event )
    {
        // This method is derived from interface java.awt.event.ActionListener
        ManifestModel m = (ManifestModel) _manifestTable.getModel();
        int rowIndex[] = _manifestTable.getSelectedRows();
        int colIndex = _manifestTable.getEditingColumn();
        if (colIndex >=0 && colIndex < 4)
        {
            TableCellEditor c = _manifestTable.getColumnModel().getColumn( colIndex ).getCellEditor();
            if (c == null)
                c = _manifestTable.getDefaultEditor( String.class );
            if (c != null)
                c.stopCellEditing();
        }
        String command = event.getActionCommand();
        if (command.equals( TableEditButtons.addCommand ))
        {
            String contentPath = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS ).get( 
                        EPubEditor.PREFS_PATHS_CONTENT, null );
            if (null == contentPath)
                contentPath = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS ).get( 
                            EPubEditor.PREFS_PATHS_OPF_OPEN, null );
            
            // Bring up file chooser with multiple selection enabled.
            JFileChooser fc = new JFileChooser( contentPath );
            fc.setMultiSelectionEnabled( true );

            if (fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION)
            {
                int index = 0;
                File[] f = fc.getSelectedFiles();

                for (int i = 0; i < f.length; i++)
                {
                    try
                    {
                        if (f[i].createNewFile())
                        {
                            //  If we created a new HTML file, fill it with some default crap
                            MediaTypeModel mt = new MediaTypeModel();
                            if (MediaTypeModel.isHTML( mt.resolveMediaType( null, f[i].getName() )))
                            {
                                FileWriter fout = new FileWriter( f[i] );
                                fout.write( "<?xml version='1.0' encoding='UTF-8'?>\n" );
                                fout.write( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
                                          + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" );
                                fout.write( 
                                        "<html xmlns='http://www.w3.org/1999/xhtml' >\n" +
                                        "<head>\n" +
                                        "  <title>Untitled Page</title>\n" +
                                        "  <meta content='text/html; charset=utf-8' http-equiv='Content-Type' />\n" +
                                        "</head>\n" +
                                        "<body>\n" +
                                        "  <div class='chapter'>\n" +
                                        "  <p>Your content goes here.</p>\n" +
                                        "  </div>\n" +
                                        "</body>\n" +
                                        "</html>\n"
                                        );
                                fout.close();
                            }
                        }
                        // Save the canonical path to where this file came from.
                        EPubEditor.prefs.node( EPubEditor.PREFS_PATHS ).put( 
                                        EPubEditor.PREFS_PATHS_CONTENT, 
                                        f[i].getParentFile().getCanonicalPath().replace( '\\', '/' ) );
                        
                        // if the file is not in the epub base, copy it there.
                        String rootPath = _master.getEPubModel().getEpubRootPath();
                        File common = FileUtil.getSharedPath( new File( rootPath ), f[i]);
                        
                        if (null == common || common.getCanonicalPath().length() < rootPath.length())
                        {
                            // The commonality is less that the ePub document base directory. 
                            // Copy the file to be a peer to the .opf file (which may not exist yet,
                            // in which case it will be the base directory). Change f[i] to match
                            // the new path.
                            f[i] = _master.getEPubModel().copyFileToOpf( f[i] );
                        }
                        if (null != f[i])
                        {
                            // Add a row for this file, and increment the count.
                            String relative = m.fileData.getPathRelativeToOpf( f[i] );
                        
                            String idref = m.addManifestItem( null, relative, null );
                            index = m.getItemIndex( idref );
                            
                            // If the file is a content document, ask if the file 
                            // should be added to the spine
                            
                            if (SpineModel.isAcceptable( m.getItemById( idref ).getAttribute( "media-type" )))
                            {
                                int reply = JOptionPane.showConfirmDialog( this, 
                                        "Do you wish to add " + relative + " to the content list?", 
                                        "Add file to content" , JOptionPane.YES_NO_OPTION, 
                                        JOptionPane.QUESTION_MESSAGE);
                                // If the confirmation was affirmative, add to spine.
                                if (reply == JOptionPane.YES_OPTION)
                                {
                                    SpineModel spine = _master.getOpfData().getSpine();
                                    spine.addRow( idref, null );
                                }
                            }
                        }
                    }
                    catch( IOException e )
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                // Move focus to the last added which is not necessarily at the end of the table.
                _manifestTable.changeSelection( index, 1, false, false );
            }
       }
        else if (command.equals( TableEditButtons.delCommand ))
        {
            // Tell the Manifest Model to remove the data.
            m.removeRow( rowIndex );
        }
        else if (command.equals( TableEditButtons.edCommand ))
        {
            // Get the absolute file name of the selected row, 
            // then launch the registered editor.
            agent.trigger( command, _manifestTable.getValueAt( _manifestTable.getSelectedRow(), 0 ) );
        }
        else if (command.equals( TableEditButtons.xformCmd ))
        {
            // Get the absolute file name of the selected row, 
            // then launch the registered editor.
            agent.trigger( command, _manifestTable );
        }
        else if (command.equals( TableEditButtons.burstCmd ))
        {
            String media_type = (String) _manifestTable.getValueAt( _manifestTable.getSelectedRow(), 3 );
            
            // this test is probably unnecessary, as the button would not have
            // been enabled if this were not a burstable document.
            if (MediaTypeModel.isHTML( media_type ))
            {
                // an HTML file that can be burst.
                String href = (String) _manifestTable.getValueAt( _manifestTable.getSelectedRow(), 2 );
                String id = (String) _manifestTable.getValueAt( _manifestTable.getSelectedRow(), 0 );
                try
                {
                    Document dom = _master.getEPubModel().getManifestedDocument( id );
                    if (null != dom)
                    {
                        // Create dialog box to identify burst point
                        BurstPointDialog where = new BurstPointDialog( _master, dom, href );
                        where.setVisible( true );
                        ArrayList<File> newItems = where.getNewContent();
                        if (null != newItems)
                        {
                            for (int i = 0; i < newItems.size(); i++)
                            {
                                // Add the files that were created to the manifest. If this manifested
                                // item appears in the spine, new spine entries should also be created for 
                                // the new items.
                                String idref = m.addManifestItem( id, m.fileData.getPathRelativeToOpf( newItems.get(i)), 
                                                                  media_type );
                                SpineModel spine = _master.getOpfData().getSpine();
                                Node ref = spine.getItemNodeByIdref( id );
                                if (null != ref)
                                {
                                    spine.addRow( idref, ref );
                                }
                            }
                            if (0 < newItems.size())
                            {
                                // Remove the source item from the manifest, and from the spine if applicable.
                                m.removeItem( id );
                            }
                        }
                    }
                }
                catch( FileNotFoundException ex )
                {
                    // Couldn't find the requested file. Log an error, then continue.
                    LogAndShowError.logAndShowEx(
                            "Unable to find the manifested file: "
                                    + m.getHrefById( id ) + "\n"
                                    + ex.getLocalizedMessage(), ex );
                }
            }
        }
        else if (command.equals( "comboBoxChanged" ))
            resetButtons();
        refresh();
    }


    public ManifestModel getModel()
    {
        return (ManifestModel) _manifestTable.getModel();
    }


    @Override
    public void stateChanged( ChangeEvent change )
    {
        Object o = change.getSource();
        if (o.getClass().equals( JTabbedPane.class ))
        {
            JTabbedPane parent = (JTabbedPane) o;
            Component c = parent.getSelectedComponent();
            if (this.equals( c ))
            {
                _isSelected = true;
            }
            else if (_isSelected)
            {
                TableCellEditor ed = _manifestTable.getCellEditor();
                if (null != ed)
                    ed.stopCellEditing();
                _isSelected = false;
            }
        }
    }


    @Override
    public void valueChanged( ListSelectionEvent arg0 )
    {
        resetButtons();
    }
    
    private void resetButtons()
    {
        // Start by disabling all the variable buttons.
        _buttons.setEnabled( TableEditButtons.delCommand, false );
        _buttons.setEnabled( TableEditButtons.edCommand, false );
        _buttons.setEnabled( TableEditButtons.burstCmd, false );
        _buttons.setEnabled( TableEditButtons.xformCmd, false );
        
        // Now re-enable those which are appropriate
        int sel = _manifestTable.getSelectedRowCount();
        int selected = _manifestTable.getSelectedRow();
        
        // if nothing is selected, nothing gets re-enabled
        // For some weird reason, sometimes a selected row can exceed the actual row count.
        if (sel >= 0 && selected < _manifestTable.getRowCount())
        {
            // enable remove button.
            _buttons.setEnabled( TableEditButtons.delCommand, true );
            ManifestModel model = (ManifestModel) _manifestTable.getModel();
            
            // Edit and burst are only enabled on single selections
            if (1 == sel)
            {
                // Only enable edit and burst if the file exists. Fix for SF BUG 3427234
                String href = (String) _manifestTable.getValueAt( selected, 2 );
                if (null != href)
                {
                    File f = new File( href );
                    if (   f.exists()
                        || model.fileData.fileExistsRelativeToOPF( href ))
                    {
                        // everything can use the default editor -- even when not a good idea.
                        _buttons.setEnabled( TableEditButtons.edCommand, true );
                        
                        // We can only burst HTML types.
                        String media_type = (String) _manifestTable.getValueAt( selected, 3 );
                        if (MediaTypeModel.isHTML( media_type ))
                        {
                            _buttons.setEnabled( TableEditButtons.burstCmd, true );
                        }
                    }
                }
            }                
            // enable xform if the selected type has a transformer enabled.
            int selections[] = _manifestTable.getSelectedRows();
            for (int i = 0; i < selections.length; i++)
            {
                if (model.fileData.fileExistsRelativeToOPF( (String) _manifestTable.getValueAt( selections[i], 2 )))
                {
                    String media_type = (String) _manifestTable.getValueAt( selections[i], 3 );
                    Preferences xformers = EPubEditor.prefs.node( EPubEditor.PREFS_MEDIA_TYPES ). 
                            node( media_type );
                    if (null != xformers.get( EPubEditor.PREFS_TRANSFORMER, null ))
                    {
                        // We only need one transformable to enable for all.
                        _buttons.setEnabled( TableEditButtons.xformCmd, true );
                        break;
                    }
                }
            }
        }
    }


    public void stopEditing()
    {
        TableCellEditor ed = _manifestTable.getCellEditor();
        if (null != ed)
            ed.stopCellEditing();
    }
}
