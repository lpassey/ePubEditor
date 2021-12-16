package com.passkeysoft.opfedit.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ElementInput extends JPanel 
        implements ActionListener, FocusListener, MouseListener, CellEditorListener
{
    private static final long serialVersionUID = 1L;
    private JTable attrList;
    private JTableHeader header;
    private JTextField tagName;
    private JButton addButton;
    private Document doc;
    private static final String ADD = "add";
    private DefaultCellEditor nameCol, valCol;
    private ActionListener parent;
    
    ElementInput( ActionListener parent, Document doc, JLabel label )
    {
        this.doc = doc;
        this.parent = parent;
        
        SpringLayout layout = new SpringLayout();
        setLayout( layout );

        // Create all the components
        tagName = new JTextField();
        tagName.addFocusListener( this );
        tagName.addActionListener( this );
        
        JLabel attrLabel = new JLabel("Attributes:" );
        attrList = new JTable( 1, 2 );
        attrList.setRowHeight( 22 );
        attrList.addMouseListener( this );
        attrList.addFocusListener( this );
        
        nameCol = new DefaultCellEditor( new JTextField() );
        nameCol.addCellEditorListener( this );
        TableColumnModel cm = attrList.getColumnModel();
        TableColumn col = cm.getColumn( 0 );
        col.setHeaderValue( "Attribute Name" );
        col.setCellEditor( nameCol );
        
        valCol = new DefaultCellEditor( new JTextField() );
        col = cm.getColumn( 1 );
        col.setHeaderValue( "Value" );
        col.setCellEditor( valCol );

        attrList.setEnabled( false );
        
        header = attrList.getTableHeader();
        addButton = new JButton( new ImageIcon( "images/add.gif" ));
        addButton.setMaximumSize( new Dimension( 15, 15 ) );
        addButton.setPreferredSize( new Dimension( 15, 15 ));
        addButton.setActionCommand( ADD );
        addButton.setEnabled( false );
        addButton.addActionListener( this );
        
        // Add all the components to the panel
        add( label );
        add( tagName );
        add( attrLabel );
        add( header );
        add( addButton );
        add( attrList );
        
        // Glue everything together
        
        // text field is glued to top right
        layout.putConstraint( SpringLayout.NORTH, tagName, 14, SpringLayout.NORTH, this );
        layout.putConstraint( SpringLayout.WEST, tagName, 128, SpringLayout.WEST, this );
        layout.putConstraint( SpringLayout.EAST, tagName, -10, SpringLayout.EAST, this );
        
        // label is glued to top left
        layout.putConstraint( SpringLayout.NORTH, label, 16, SpringLayout.NORTH, this );
        layout.putConstraint( SpringLayout.EAST, label, -10, SpringLayout.WEST, tagName );
        
        // table header is glued to bottom of text field
        layout.putConstraint( SpringLayout.NORTH, header, 0, SpringLayout.SOUTH, tagName );
        layout.putConstraint( SpringLayout.EAST, header, 0, SpringLayout.EAST, tagName );
        layout.putConstraint( SpringLayout.WEST, header, 0, SpringLayout.WEST, tagName );
        
        // attribute label goes to the left of the header.
        layout.putConstraint( SpringLayout.EAST, attrLabel, -8, SpringLayout.WEST, header );
        layout.putConstraint( SpringLayout.VERTICAL_CENTER, attrLabel, 0, SpringLayout.VERTICAL_CENTER, header );
        
        // table is glued to table header
        layout.putConstraint( SpringLayout.NORTH, attrList, 0, SpringLayout.SOUTH, header );
        layout.putConstraint( SpringLayout.EAST, attrList, 0, SpringLayout.EAST, header );
        layout.putConstraint( SpringLayout.WEST, attrList, 0, SpringLayout.WEST, header );
        
        // add button goes to the left of the table
        layout.putConstraint( SpringLayout.EAST, addButton, -8, SpringLayout.WEST, attrList );
        layout.putConstraint( SpringLayout.NORTH, addButton, 0, SpringLayout.NORTH, attrList );
        
        addFocusListener( this );
    }

    
    @Override
    public Dimension getMinimumSize()
    {
        Dimension size, minSize = new Dimension( 480, 24 );
        size = tagName.getMinimumSize();
        minSize.height += size.height;
        size = header.getMinimumSize();
        minSize.height += size.height;
        size = attrList.getMinimumSize();
        minSize.height += size.height;
        
        return minSize;
    }
    
    
    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = getMinimumSize();
        size.width = 600;
        return size;
    }
    
    
    public Element mockElement()
    {
        Element e = null;
        
        nameCol.stopCellEditing();
        valCol.stopCellEditing();
        if (0 < tagName.getText().length()) try
        {
            e = doc.createElement( tagName.getText() );

            for (int i = 0; i < attrList.getRowCount(); i++)
            {
                Object cellValue = attrList.getValueAt( i, 0 );
                if (null != cellValue)
                {
                    String name = cellValue.toString();
                    if (0 < name.length())
                    {
                        String value = (String) attrList.getValueAt( i, 1 );
                        e.setAttribute( name, value );
                    }
                }
            }
        }
        catch( DOMException ex )
        {
            // TODO: might throw an exception if "tagName" is not valid; report it here.
        }
        return e;
    }
        
    
    public String getText()
    {
        return tagName.getText();
    }


    @Override
    public void actionPerformed( ActionEvent event )
    {   
        String actionCommand = event.getActionCommand();
        if (event.getSource().equals( tagName ))
            attrList.requestFocusInWindow();
        if (actionCommand.equals( ADD ))
        {
            addButton.setEnabled( false );
            ((DefaultTableModel) attrList.getModel()).addRow( new Object[] { "", "" });
            validate();
            parent.actionPerformed( event );
        }
    }


    @Override
    public void focusGained( FocusEvent event )
    {
        if (!event.getSource().equals( tagName ) && !event.getSource().equals( attrList ))
        {
            System.out.println( event.getSource().getClass().getName() + " gained focus" );
        }
    }


    @Override
    public void focusLost( FocusEvent event )
    {
        if (event.getSource().equals( tagName ))
        {
            attrList.setEnabled( 0 < tagName.getText().length() );
            if (attrList.isEnabled())
            {
                attrList.changeSelection( 0, 0, false, false );
                attrList.requestFocusInWindow();
            }
        }
        else if (event.getSource().equals( attrList ))
        {
            // attribute list lost focus, do appropriate cleanup
            TableModel data = attrList.getModel();
            int count = data.getRowCount();
            if ( count > 0)
            {
                String attr;
                int row = attrList.getSelectedRow();
                // if there is a selected row, see if we need to clear a value
                if (-1 < row)
                {
                    // if the first column is empty, the value must be empty as well.
                    // (the reverse is not necessarily true)
                    attr = (String) data.getValueAt( row, 0 );
                    if (null == attr || 1 > attr.length())
                    {
                        data.setValueAt( "", row, 1 );
                    }
                }
                // if there are more than 1 row, go through the table and remove any
                // empty rows, unless column one of a row currently has the focus.
                while (1 < count)
                {
                    attr = (String) data.getValueAt( count - 1, 0 );
                    String val = (String) data.getValueAt( count - 1, 1 );
                    if (   (null == attr || 1 > attr.length())
                        && (null == val || 1 > val.length()))
                    {
                        if (   (attrList.getSelectedRow() != count - 1)
                            || (0 != attrList.getSelectedColumn()))
                        {
                            ((DefaultTableModel)data).removeRow( count - 1 );
                        }
                    }
                    count--;
                }
                attrList.validate();

                // if the first column of the last row is not empty, enable the add button,
                // otherwise disable it.
                count = data.getRowCount();
                String str = (String) data.getValueAt( count - 1, 0 );
                addButton.setEnabled( str != null && 0 < str.length());
             }
        }
        else
            System.out.println( event.getSource().getClass().getName() + " lost focus" );
    }


    @Override
    public void mouseClicked( MouseEvent ignore )
    {
        attrList.requestFocusInWindow();
    }


    @Override
    public void mouseEntered( MouseEvent ignore )
    {
    }


    @Override
    public void mouseExited( MouseEvent ignore )
    {
    }


    @Override
    public void mousePressed( MouseEvent event )
    {
        attrList.requestFocusInWindow();
    }


    @Override
    public void mouseReleased( MouseEvent ignore )
    {
    }


    @Override
    public void editingCanceled( ChangeEvent ignore )
    {
    }


    @Override
    public void editingStopped( ChangeEvent arg0 )
    {
        TableModel data = attrList.getModel();
        int count = data.getRowCount();
        if ( count > 0)
        {
            String attr;
            int row = attrList.getSelectedRow();
            
            // if the first column is empty, the value must be empty as well.
            // (the reverse is not necessarily true)
            if (-1 < row)
            {
                attr = (String) data.getValueAt( row, 0 );
                if (null == attr || 1 > attr.length())
                {
                    data.setValueAt( "", row, 1 );
                }
            }
            // if the first column of the last row is not empty, enable the add button,
            // otherwise disable it.
            String str = (String) data.getValueAt( count - 1, 0 );
            addButton.setEnabled( str != null && 0 < str.length());
        }
    }

}
