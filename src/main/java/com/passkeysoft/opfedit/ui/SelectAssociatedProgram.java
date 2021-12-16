package com.passkeysoft.opfedit.ui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.passkeysoft.opfedit.datamodels.MediaTypeModel;

public class SelectAssociatedProgram extends JDialog implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private JComboBox _mediaTypes;
    private JTextField _programFile;
    private int _index = 0;
    private JTextField _commandLine;

    private String _command;

    private int _response =  JOptionPane.CANCEL_OPTION;;
    
    public SelectAssociatedProgram( Frame owner, String title, MediaTypeModel m, 
                                    ArrayList<Preferences> mediaTypes )
    {
        super( owner, title, true );
        SpringLayout layout = new SpringLayout();
        setLayout( layout );
        
        JLabel mediaTypeLabel = new JLabel( "media-type: ");
        JLabel programLabel = new JLabel( "Executable program file: " );
        JLabel commandLineLabel = new JLabel( "Command line options (use \"%s\" to represent the file in the package):" );
        layout.putConstraint( SpringLayout.NORTH, mediaTypeLabel, 10, SpringLayout.NORTH, getContentPane() );
        layout.putConstraint( SpringLayout.WEST, mediaTypeLabel, 10, SpringLayout.WEST, getContentPane() );
        add( mediaTypeLabel );
        
        _mediaTypes = new JComboBox();

        // iterate through all the media-types and add those which do /not/ appear in the dialog's list.
//        MediaTypeModel m = mediaTypes;
        for (int i = 0; i < m.getSize(); i++)
        {
            if (!mediaTypes.contains( (String) m.getElementAt( i )))
                _mediaTypes.addItem( m.getElementAt( i ));
        }
        _mediaTypes.addItem( "New media-type" );
        _mediaTypes.addActionListener( this );
        _mediaTypes.setActionCommand( "media" );
        add( _mediaTypes );
        layout.putConstraint( SpringLayout.NORTH, _mediaTypes, 8, SpringLayout.NORTH, getContentPane() );
        layout.putConstraint( SpringLayout.WEST, _mediaTypes, 8, SpringLayout.EAST, mediaTypeLabel );
        layout.putConstraint( SpringLayout.EAST, _mediaTypes, -10, SpringLayout.EAST, getContentPane() );
        
        add( programLabel );
        layout.putConstraint( SpringLayout.NORTH, programLabel, 10, SpringLayout.SOUTH, _mediaTypes );
        layout.putConstraint( SpringLayout.WEST, programLabel, 10, SpringLayout.WEST, getContentPane() );
        
        JButton browseButton = new JButton( "Browse" );
        browseButton.addActionListener( this );
        browseButton.setActionCommand( "browse" );
        add( browseButton );
        
        layout.putConstraint( SpringLayout.NORTH, browseButton, 8, SpringLayout.SOUTH, programLabel );
        layout.putConstraint( SpringLayout.EAST, browseButton, -10, SpringLayout.EAST, getContentPane() );
        
        _programFile = new JTextField();
        _programFile.setEditable( false );
        add( _programFile );
        layout.putConstraint( SpringLayout.NORTH, _programFile, 10, SpringLayout.SOUTH, programLabel );
        layout.putConstraint( SpringLayout.WEST, _programFile, 10, SpringLayout.WEST, getContentPane() );
        layout.putConstraint( SpringLayout.EAST, _programFile, -10, SpringLayout.WEST, browseButton );

        add( commandLineLabel );
        layout.putConstraint( SpringLayout.NORTH, commandLineLabel, 10, SpringLayout.SOUTH, _programFile );
        layout.putConstraint( SpringLayout.WEST, commandLineLabel, 10, SpringLayout.WEST, getContentPane() );

        _commandLine = new JTextField( "\"%s\"");
        layout.putConstraint( SpringLayout.NORTH, _commandLine, 10, SpringLayout.SOUTH, commandLineLabel );
        layout.putConstraint( SpringLayout.WEST, _commandLine, 10, SpringLayout.WEST, getContentPane() );
        layout.putConstraint( SpringLayout.EAST, _commandLine, -10, SpringLayout.EAST, getContentPane() );
        add (_commandLine );
        
        
        
        
        
        JButton ok = new JButton( "OK" );
        ok.setPreferredSize( EPubEditor.buttonSize );
        ok.setActionCommand( "ok" );
        ok.addActionListener( this );
        add( ok );
        layout.putConstraint( SpringLayout.SOUTH, ok, -10, SpringLayout.SOUTH, getContentPane() );
        layout.putConstraint( SpringLayout.EAST, ok, -10, SpringLayout.HORIZONTAL_CENTER, getContentPane() );
        
        JButton cancel = new JButton( "Cancel" );
        cancel.setPreferredSize( EPubEditor.buttonSize );
        cancel.setActionCommand( "cancel" );
        cancel.addActionListener( this );
        add( cancel );
        layout.putConstraint( SpringLayout.SOUTH, cancel, -10, SpringLayout.SOUTH, getContentPane() );
        layout.putConstraint( SpringLayout.WEST, cancel, 10, SpringLayout.HORIZONTAL_CENTER, getContentPane() );
        
        setSize( 640, 360 );
     }

    public String getCommandLine()
    {
        return _commandLine.getText();
    }
    
    public String getProgramFile()
    {
        return _programFile.getText();
    }
    
    public String getMediaType()
    {
        return (String) _mediaTypes.getSelectedItem();
    }
    
    public String getLastCommand()
    {
        return _command;
    }
    
    
    public int showDialog()
    {
        this.setVisible( true );
        return _response;
    }
    
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        _command = e.getActionCommand();
        if (_command.equals( "browse" ))
        {
            // bring up a file chooser, then set the text area to the 
            // absolute path of the file chosen
            JFileChooser fc = new JFileChooser( EPubEditor.prefs.node( 
                            EPubEditor.PREFS_PATHS ).get( "programs", null ));
            int retval = fc.showDialog( this, "Select" );
            if (JFileChooser.APPROVE_OPTION == retval)
            {
                File f = fc.getSelectedFile();
                _programFile.setText( f.getAbsolutePath() );
            }
        }
        else if (_command.equals( "media" ))
        {
            // combo box selection changed. If selection is New media-type, start JOptionPane
            // to collect the new media type.
            String selected = (String) _mediaTypes.getSelectedItem();
            if (selected.equals( "New media-type" ))
            {
                
                selected = JOptionPane.showInputDialog( this,
                        null, "Enter new media-type", JOptionPane.QUESTION_MESSAGE );
                if (null != selected)
                {
                    _mediaTypes.insertItemAt( selected, 0 );
                    _index = 0;
                }
                _mediaTypes.setSelectedIndex( _index );
            }
            else
                _index = _mediaTypes.getSelectedIndex();
        }
        else if (_command.equals( "ok" ))
        {
            this.setVisible( false );
            _response = JOptionPane.OK_OPTION;
        }
        else if (_command.equals( "cancel" ))
        {
            this.setVisible( false );
            _response = JOptionPane.CANCEL_OPTION;
        }
    }
}
