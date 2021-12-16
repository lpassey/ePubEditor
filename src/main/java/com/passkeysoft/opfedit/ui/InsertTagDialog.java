/**
 * 
 */
package com.passkeysoft.opfedit.ui;

import java.awt.Dimension;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

import org.w3c.dom.Element;

import com.passkeysoft.opfedit.business.EPubUtil;
import com.passkeysoft.opfedit.datamodels.*;

/**
 * @author Lee Passey
 *
 */
public class InsertTagDialog extends TwoTagDialog implements TwoElementAction
{
    public static final String PARENT = "parent", LAST = "last", FIRST = "first", 
                                 AFTER = "after", BEFORE = "before", STEP="step";
    private static final long serialVersionUID = 1L;
    EPubModel data;
    ButtonGroup radios;

    public InsertTagDialog( EPubEditor owner, EPubModel epubModel )
    {
        super( owner );
        setTitle( "Insert new element before another" );
        this.init( this, epubModel.getOpfDom() );
        data = epubModel;
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout( new BoxLayout( radioPanel, BoxLayout.PAGE_AXIS ));
        radios = new ButtonGroup();
        JRadioButton rb = new JRadioButton( "Insert Before" );
        rb.setActionCommand( BEFORE );
        rb.setSelected( true );
        radios.add( rb );
        radioPanel.add( rb );
        rb = new JRadioButton( "Insert After" );
        rb.setActionCommand( AFTER );
        radios.add( rb );
        radioPanel.add( rb );
        rb = new JRadioButton( "Insert as First Child" );
        rb.setActionCommand( FIRST );
        radios.add( rb );
        radioPanel.add( rb );
        rb = new JRadioButton( "Insert as Last Child" );
        rb.setActionCommand( LAST );
        radios.add( rb );
        radioPanel.add( rb );
        rb = new JRadioButton( "Insert as Parent" );
        rb.setActionCommand( PARENT );
        radios.add( rb );
        radioPanel.add( rb );
        rb = new JRadioButton( "Insert as Step-Parent" );
        rb.setActionCommand( STEP );
        radios.add( rb );
        radioPanel.add( rb );
        add( radioPanel );
        layout.putConstraint( SpringLayout.SOUTH, radioPanel, -8, SpringLayout.NORTH, actionButton );
        layout.putConstraint( SpringLayout.WEST, radioPanel, 128, SpringLayout.WEST, getContentPane() );
    }

    @Override
    public String getPatternLabel()
    {
        return "New element: ";
    }

    @Override
    public String getTargetLabel()
    {
        return "Target element:";
    }

    @Override
    public String getActionLabel()
    {
        return "Insert";
    }

    @Override
    public String getActionReport()
    {
        return " elements inserted.";
    }

    @Override
    public int act( Element newElement, Element target )
    {
            ButtonModel but =  radios.getSelection();
            try
            {
                return EPubUtil.insertAt( data, newElement, target, but.getActionCommand() );
            }
            catch (IOException ex)
            {
                LogAndShowError.logAndShowEx( "Non-specific io error while writing to file\n", ex );
            }
            return 0;
    }


    @Override
    public Dimension getMinimumSize()
    {
        Dimension size = super.getMinimumSize();
        size.height += 196;
        return size;
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
    public String validate( Element pattern, Element target )
    {
        if (null == pattern || null == target)
            return "Two element definitions are required";
        return null;
    }

}
