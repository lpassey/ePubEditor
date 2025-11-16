package com.passkeysoft.opfedit.ui.swing.view;


import com.passkeysoft.opfedit.ui.swing.controller.EPubEditor;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


public class TableEditButtons extends JPanel
{
	public final static String addCommand = "Add";
    public final static String delCommand = "Del";
    public final static String upCommand = "Up";
    public final static String dnCommand = "Down";
    public final static String edCommand = "Edit";
    public final static String burstCmd = "Burst";
    public final static String xformCmd = "xform";
    public final static String closeCmd = "close";

	private static final long serialVersionUID = 5721391714244784569L;
	private JButton addButton = null;
	private JButton delButton = null;
	private JButton moveUpBut = null;
	private JButton moveDnBut = null;
	private JButton editButton = null;
	private JButton burstBut = null;
	private JButton xformBut = null;
	private JButton closeBut = null;

	static Insets margins = new Insets(0, 0, 0, 0 );

	public TableEditButtons( ActionListener panel, boolean move, boolean edit, boolean close )
    {

        setLayout(new BoxLayout(this,BoxLayout.X_AXIS));

		add( Box.createHorizontalGlue());

        addButton = new JButton();
        addButton.setText("Add");
        addButton.setMargin( margins );
        addButton.setActionCommand(addCommand);
        addButton.setPreferredSize( EPubEditor.buttonSize );
        addButton.addActionListener( panel );
        add(addButton);

		add( Box.createHorizontalStrut( 8 ));

	    delButton = new JButton();
        delButton.setText("Remove");
        delButton.setMargin( margins );
		delButton.setActionCommand(delCommand);
        delButton.setPreferredSize( EPubEditor.buttonSize );
        delButton.addActionListener( panel );
        add(delButton);

        if (move)
        {
    		add( Box.createHorizontalStrut( 8 ));

    	    moveUpBut = new JButton();
            moveUpBut.setText("Move Up");
            moveUpBut.setMargin( margins );
    		moveUpBut.setActionCommand(upCommand);
            moveUpBut.setPreferredSize( EPubEditor.buttonSize );
    		moveUpBut.addActionListener( panel );
            add(moveUpBut);

    		add( Box.createHorizontalStrut( 8 ));

    	    moveDnBut = new JButton();
            moveDnBut.setText("Move Down");
            moveDnBut.setMargin( margins );
    		moveDnBut.setActionCommand(dnCommand);
            moveDnBut.setPreferredSize( EPubEditor.buttonSize );
    		moveDnBut.addActionListener( panel );
            add(moveDnBut);
        }
        if (edit)
        {
            add( Box.createHorizontalStrut( 8 ));

            editButton = new JButton();
            editButton.setText("Edit");
            editButton.setMargin( margins );
            editButton.setActionCommand(edCommand);
            editButton.setPreferredSize( EPubEditor.buttonSize );
            editButton.addActionListener( panel );
            add(editButton);

            add( Box.createHorizontalStrut( 8 ));

            burstBut = new JButton();
            burstBut.setText("Split HTML");
            burstBut.setMargin( margins );
            burstBut.setActionCommand( burstCmd );
            burstBut.setPreferredSize( EPubEditor.buttonSize );
            burstBut.addActionListener( panel );
            add(burstBut);

            add( Box.createHorizontalStrut( 8 ));

            xformBut = new JButton();
            xformBut.setText("Transform");
            xformBut.setMargin( margins );
            xformBut.setActionCommand( xformCmd );
            xformBut.setPreferredSize( EPubEditor.buttonSize );
            xformBut.addActionListener( panel );
            add(xformBut);
        }
        if (close)
        {
            add( Box.createHorizontalStrut( 8 ));

            closeBut = new JButton();
            closeBut.setText("Done");
            closeBut.setMargin( margins );
            closeBut.setActionCommand(closeCmd);
            closeBut.setPreferredSize( EPubEditor.buttonSize );
            closeBut.addActionListener( panel );
            add(closeBut);
        }
  		add( Box.createHorizontalGlue());
        setMaximumSize( EPubEditor.buttonPanelSize );
    }

	public void setEnabled( String command, boolean enabled )
	{
	    if (command.equals( addCommand ))
	        addButton.setEnabled( enabled );
	    else if (command.equals( delCommand ))
	        delButton.setEnabled( enabled );
	    else if (command.equals( upCommand ))
	        moveUpBut.setEnabled( enabled );
	    else if (command.equals( dnCommand ))
	        moveDnBut.setEnabled( enabled );
        else if (command.equals( edCommand ))
            editButton.setEnabled( enabled );
        else if (command.equals( burstCmd ))
            burstBut.setEnabled( enabled );
        else if (command.equals( xformCmd ))
            xformBut.setEnabled( enabled );
        else if (command.equals( closeCmd ))
            closeBut.setEnabled( enabled );
	}
}