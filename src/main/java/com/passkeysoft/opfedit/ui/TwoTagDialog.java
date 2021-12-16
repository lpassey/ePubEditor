package com.passkeysoft.opfedit.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.passkeysoft.opfedit.datamodels.*;

public abstract class TwoTagDialog extends JDialog implements ActionListener, FocusListener
{
    private static final String CANCEL = "cancel", ACTION = "action";
    private static final long serialVersionUID = 1L;
    private ElementInput findElement, actionElement = null;
    private Dimension buttonSize = new Dimension( 80, 24 );
    private TwoElementAction _action;

    protected Document dom;     // Not the same as C++ protected, but used here 
                                // to remind me of why I'm declaring it like this
    protected EPubEditor owner;
    protected JButton actionButton;
    protected SpringLayout layout;
    
    TwoTagDialog( EPubEditor owner )
    {
        super( owner, true );
        this.owner = owner;
    }
    
    void init( TwoElementAction action, Document doc )
    {
        _action = action;
        dom = doc;
        layout = new SpringLayout();
        setLayout( layout );
        
        findElement = new ElementInput( this, doc, new JLabel( action.getPatternLabel() ));
        add( findElement );
        layout.putConstraint( SpringLayout.NORTH, findElement, 0, SpringLayout.NORTH, getContentPane() );
        layout.putConstraint( SpringLayout.WEST, findElement, 10, SpringLayout.WEST, getContentPane() );
        layout.putConstraint( SpringLayout.EAST, findElement, -10, SpringLayout.EAST, getContentPane() );
        
        String targetLabel = action.getTargetLabel();
        if (null != targetLabel)
        {
            actionElement = new ElementInput( this, doc, new JLabel( action.getTargetLabel() ));
            add( actionElement );
            layout.putConstraint( SpringLayout.NORTH, actionElement, 0, SpringLayout.SOUTH, findElement );
            layout.putConstraint( SpringLayout.WEST, actionElement, 10, SpringLayout.WEST, getContentPane() );
            layout.putConstraint( SpringLayout.EAST, actionElement, -10, SpringLayout.EAST, getContentPane() );
        }
        actionButton = new JButton( action.getActionLabel() );
        actionButton.setMinimumSize( buttonSize );
        actionButton.setMaximumSize( buttonSize );
        actionButton.setActionCommand( ACTION );
        actionButton.addActionListener( this );
        add( actionButton );
        layout.putConstraint( SpringLayout.SOUTH, actionButton, -8, SpringLayout.SOUTH, getContentPane() );
        layout.putConstraint( SpringLayout.HORIZONTAL_CENTER, actionButton, -56, SpringLayout.HORIZONTAL_CENTER, this.getContentPane() );
        
        JButton cancel = new JButton( "Close" );
        cancel.setMinimumSize( buttonSize );
        cancel.setMaximumSize( buttonSize );
        cancel.setActionCommand( CANCEL );
        cancel.addActionListener( this );
        add( cancel );
        layout.putConstraint( SpringLayout.SOUTH, cancel, -8, SpringLayout.SOUTH, getContentPane() );
        layout.putConstraint( SpringLayout.HORIZONTAL_CENTER, cancel, 56, SpringLayout.HORIZONTAL_CENTER, this.getContentPane() );

        setMinimumSize( getMinimumSize());
        setPreferredSize( getPreferredSize());
        pack();
    }


    @Override
    public Dimension getMinimumSize()
    {
        int height = buttonSize.height + 56, width = 320;
        Dimension size;
        if (null != findElement)
        {
            size = findElement.getMinimumSize();
            height += size.height;
            if (size.width > width)
                width = size.width;
        }
        if ( null != actionElement)
        {
            size = actionElement.getMinimumSize();
            height += size.height;
            if (size.width > width)
                width = size.width;
        }
        
        return new Dimension( width, height );
        
    }
    
    
    @Override
    public Dimension getPreferredSize()
    {
        Dimension size = getMinimumSize();
        size.width *= 3;
        size.width /= 2;
        return size;
    }

    
    @Override
    public void actionPerformed( ActionEvent event )
    {
        String actionCommand = event.getActionCommand();
        
        if (actionCommand.equals( CANCEL )) 
        {
            setVisible(false);
        }
        else if (actionCommand.equals( ACTION ))
        {
            Cursor cursor = getContentPane().getCursor();
            try
            {
                getContentPane().setCursor(new Cursor( Cursor.WAIT_CURSOR ));
                Element aElement = null, pattern = findElement.mockElement();
                if (null != actionElement)
                {
                    aElement = actionElement.mockElement();
                    String message = _action.validate( pattern, aElement );
                    if (null != message && 0 < message.length())
                    {
                        // Show an error dialog if either element does not exist
                        JOptionPane.showMessageDialog( getOwner(), message, "Error", JOptionPane.ERROR_MESSAGE );
                        return;
                    }
                }
                int count = _action.act( pattern, aElement );
                getContentPane().setCursor( cursor );
                JOptionPane.showMessageDialog( getOwner(), count + " " + _action.getActionReport(), 
                                                       "Operation complete", JOptionPane.OK_OPTION );
//                setVisible( false );
            }
            finally
            {
                getContentPane().setCursor( cursor );
            }
        }
        else if (actionCommand.equals( "add" ))
        {
            Dimension size = getSize();
            Dimension minsize = getMinimumSize();
            if (size.height < minsize.height)
                size.height = minsize.height;
            setSize( size );
            validate();
        }
    }
    
    
    @Override
    public void focusGained( FocusEvent ignore )
    {
        System.out.println( ignore );
    }


    @Override
    public void focusLost( FocusEvent event )
    {
        System.out.println( event );
        /*
        if (event.getSource().equals( tagName ))
        {
            attrList.setEnabled( 0 < tagName.getText().length() );
            if (attrList.isEnabled())
            {
                attrList.changeSelection( 0, 0, false, false );
                attrList.requestFocusInWindow();
            }
        }
        */
    }
}
