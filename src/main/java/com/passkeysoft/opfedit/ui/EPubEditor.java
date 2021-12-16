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
  
  $Log: EPubEditor.java,v $
  Revision 1.6  2014/07/29 22:42:32  lpassey
  "Save as epub" becomes "Compile to ePub"
  Open ePub becomes "Import from ePub"
  Create significant MRU list for Projects (*.opf)

  Revision 1.5  2013/07/03 22:26:02  lpassey
  Improve error handling when .opf file has not yet been created.

  Revision 1.4  2013/03/12 16:58:10  lpassey
  1. Bug 3451789 - move focus to top of window when displaying style report
  2. Bug 3600161 - improve setting of publication base folder

  Revision 1.3  2012/08/14 22:10:12  lpassey
  Remember last "SaveAs" path, but use the currently opened file name as the default name.

*/

package com.passkeysoft.opfedit.ui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.*;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.apache.log4j.*;
import com.passkeysoft.opfedit.Prefs;
import com.passkeysoft.opfedit.business.CleanInBackground;
import com.passkeysoft.opfedit.business.EPubChecker;
import com.passkeysoft.opfedit.business.EPubUtil;
import com.passkeysoft.opfedit.business.GenerateStyleReport;
import com.passkeysoft.opfedit.business.XFormFiles;
import com.passkeysoft.opfedit.datamodels.*;
import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.validate.EPubFileCheck;
import com.steadystate.css.parser.CSSOMParser;

public class EPubEditor extends JFrame implements Observer
{
    private static final long serialVersionUID = 1L;

    // persistent properties
    static public Preferences prefs;
    
    private static final String 
        defaultEditor = "C:\\Program Files\\Windows NT\\Accessories\\wordpad.exe";
        
    public static final String PREFS_MEDIA_TYPES = "media-types", 
                               PREFS_EDITOR_PATH = "editor", 
                               PREFS_EDITOR_CL = "edcl",
                               PREFS_TRANSFORMER = "transformer", 
                               PREFS_XFORM_CL = "arguments",
                               PREFS_XFORM_NEWMT = "newMimeType",
                               PREFS_XFORM_NEWEXT = "newExtension",
                               PREFS_EDITORS_DEFAULT = "other", 
                               PREFS_USERCSSS = "usercss", 
                               PREFS_PATHS = "paths", 
                               PREFS_PATHS_XSLT = "userxsl",
                               PREFS_PATHS_SAVE_FILE = "saveFile", 
                               PREFS_PATHS_SAVE_EPUB = "saveEPub", 
                               PREFS_PATHS_OPF_OPEN = "opfOpenPath", 
                               PREFS_PATHS_EPUB_OPEN = "ePubOpenPath",
                               PREFS_PATHS_CONTENT = "contentPath", 
                               PREFS_PATHS_TEMP = "temp",
                               PREFS_MRU = "file-mru-list"
                ;

//    public static final String opfNS = "http://www.idpf.org/2007/opf";

    public static final Dimension buttonSize = new Dimension( 96, 32 );
    public static final Dimension buttonPanelSize = new Dimension( 32767, 32 );
    public static final Dimension toolButtonSize = new Dimension( 24, 32 );

    // action commands for toolbar buttons and menus
    static final String newOPF = "new", openOPF = "open", cleanOPF = "clean",
            saveOPF = "save", saveOPFas = "saveas", exitOPF = "exit",
            aboutOPF = "about", replaceTags = "replace",
            insertBefore = "insert", styleReport = "report", buildTOC = "toc", 
            buildNCX = "ncx", validate = "validate", tools = "tools",
            editors = "editors", options = "options", xformer="transformers",
            buildCover = "cover", importEPub = "import", compileEPub = "compile"
                ;

    static final String oxygenIconPath = "images/oxygen/16x16/";

    private static final int MAX_DISPLAY_PATH = 64;

     
    ImageIcon newIcon = new ImageIcon( getClass().getClassLoader().getResource(
            "images/book-add-icon16a.png" ) );
    ImageIcon openIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( "images/book-open-icon16a.png" ) );
    ImageIcon saveIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "actions/document-save.png" ) );
    ImageIcon saveAsIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "actions/document-save-as.png" ) );
    ImageIcon aboutIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "status/dialog-information.png" ) );
    ImageIcon exitIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "actions/dialog-close.png" ) );
    ImageIcon cleanIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( "images/broom16.png" ));
    ImageIcon replaceIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( "images/text-replace-icon16.png" ));
    ImageIcon insertIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "actions/document-import.png" ));
    ImageIcon cssIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "apps/preferences-web-browser-stylesheets.png" ));
    ImageIcon reportIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "apps/keduca.png" ));
    ImageIcon editorsIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "actions/document-edit.png" ));
    ImageIcon xformersIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "apps/plasmagik.png" ));
    ImageIcon prefsIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "filesystems/folder_bookmarks.png" ));
    ImageIcon tocIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "apps/alacarte.png" ));
    ImageIcon ncxIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "mimetypes/text-xml.png" ));
    ImageIcon imageIcon = new ImageIcon( getClass().getClassLoader()
            .getResource( oxygenIconPath + "mimetypes/image-x-generic.png" ));
    // XXX: load more icons from resources?
    
    // class global variables

    private MenuActionListener menuActionListener;

    // private boolean isDirty;

    // visual elements, initialized in the constructor
    // These first variables are menu items declared here 
    // so they can be enabled and disabled according to context
    private JMenuItem exportItem;
    private JMenuItem saveAsItem;
    private JMenuItem saveItem;
    private JMenuItem validateItem;
    private JMenuItem styleItem;
    private JMenu editMenu;
    private JMenu reportMenu;
    private JMenu recentFiles;
   
    private ContribPanel contribPanel;
    private SpinePanel contentPanel;
    private MetadataPanel propPanel;
    private ManifestPanel manifestPanel;
    private GuidePanel guidePanel;
    
    // Classes dependent on external jars
    boolean ePubCheckPresent = false, cssParserPresent = false;
    
     // a read only field
    private EPubModel _epubModel;

    public EPubModel getEPubModel()
    {
        return _epubModel;
    }
    
    public OPFFileModel getOpfData()
    {
        if (null != _epubModel)
            return _epubModel.getOpfData();
        return null;
    }
    
    /**
     * The entry point for this application. Sets the Look and Feel to the
     * System Look and Feel. Creates a new JFrame and makes it visible.
     */
    static public void main( String args[] )
    {
        try
        {
//            File f = new File ( prefFile );
//            if (f.exists())
//                Preferences.importPreferences( new FileInputStream( f ));
            prefs = Preferences.userNodeForPackage( Prefs.class );
//            prefs.node( "ui").removeNode();
            // Add the following code if you want the Look and Feel
            // to be set to the Look and Feel of the native system.
            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

            // Create a new instance of our application's frame, and make it visible.
            JFrame programFrame = new EPubEditor( args );

            programFrame.setVisible( true );
        }
        catch( Throwable t )
        {
            t.printStackTrace();
            // Ensure the application exits with an error condition.
            System.exit( 1 );
        }
    }

    class MenuActionListener implements ActionListener
    {
        public void actionPerformed( ActionEvent event )
        {
            if (event.getActionCommand().equals( newOPF ))
            {
                // TODO: if (isDirty) ask if it should be saved.
                try
                {
                    newOEBFile();
                }
                catch( BackingStoreException ignore ) { }
                finally {}
            }
            else if (event.getActionCommand().equals( openOPF ))
            {
                openOEBFile();
            }
            else if (event.getActionCommand().equals( importEPub ))
            {
                openEPubFile();
            }
            else if (event.getActionCommand().equals( cleanOPF ))
            {
                cleanOEB();
            }
            else if (event.getActionCommand().equals( replaceTags ))
            {
                replaceTags();
            }
            else if (event.getActionCommand().equals( insertBefore ))
            {
                insertBefore();
            }
            else if (event.getActionCommand().equals( styleReport ))
            {
                generateStyleReport();
            }
            else if (event.getActionCommand().equals( validate ))
            {
                if (null != _epubModel)
                {
                    ePubCheck();
                }
            }
            else if (event.getActionCommand().equals( buildCover ))
            {
                Cursor cursor = getContentPane().getCursor();
                getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
                EPubUtil.buildCover( _epubModel );
                getContentPane().setCursor( cursor );
            }
            else if (event.getActionCommand().equals( buildTOC ))
            {
                Cursor cursor = getContentPane().getCursor();
                getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
                EPubUtil.buildTOC( _epubModel, getUserCSSFile() );
                getContentPane().setCursor( cursor );
            }
            else if (event.getActionCommand().equals( buildNCX ))
            {
                Cursor cursor = getContentPane().getCursor();
                getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
                buildNCX();
                getContentPane().setCursor( cursor );
            }
            else if (event.getActionCommand().equals( saveOPF ))
            {
                saveOPFFile();
            }
            else if (event.getActionCommand().equals( saveOPFas ))
            {
                saveOPFFileAs();
            }
            else if (event.getActionCommand().equals( compileEPub ))
            {
                exportEPub();
            }
            else if (event.getActionCommand().equals( exitOPF ))
            {
                exitItem_actionPerformed( event );
            }
            else if (event.getActionCommand().equals( aboutOPF ))
            {
                aboutItem_actionPerformed( event );
            }
            else if (event.getActionCommand().equals( editors ))
            {
                editors_actionPerformed( event );
            }
            else if (event.getActionCommand().equals( xformer ))
            {
                xformers_actionPerformed( event );
            }
            else if (event.getActionCommand().equals( options ))
            {
                pathPrefs();
            }
            else if (event.getActionCommand().startsWith( "file" ))
            {
                // open the file specified by this preference.
                Preferences mru = prefs.node( PREFS_PATHS ).node( PREFS_MRU );
                File f = new File( mru.get( event.getActionCommand(), "" ));
                if (f.exists())
                    openFile( f  );
                else
                    openOEBFile();
            }
        }
    }


    private void populateMRUFiles( String numberOne )
    {
        try
        {
            // Get the MRU list.
            Preferences mru = prefs.node( PREFS_PATHS ).node( PREFS_MRU );
            // put all the key/value pairs into a sorted list
            TreeMap<String, String> paths = new TreeMap<String, String>();
            for (String key : mru.keys())
            {
                String path = mru.get( key, null );
                if (null != path && !path.equals( numberOne ))
                    paths.put( key, path );
            }
            // I now have a sorted list of all the mru items /except/ the file I just opened.
            // put the new file at the beginning of the list, then re-add the remainder
            mru.clear();

            int count = 0;
            if (null != numberOne)
            {
                mru.put( "file00", numberOne );
                count++;
            }
            Collection<String> theRest = paths.values();
            for (String path : theRest )
            {
                if (null != path && 0 < path.length())
                    mru.put( String.format( "file%02d", count++), path );
                if (count > 15)
                    break;
            }
            mru.flush();
            
            recentFiles.removeAll();
            
            // resort the preferences list
            paths.clear();
            for (String key : mru.keys())
            {
                String path = mru.get( key, null );
                if (null != path)
                    paths.put( key, path );
            }
            Set<Entry<String, String>> entries = paths.entrySet();
            for (Entry<String, String> entry : entries )
            {
                String path = entry.getValue();
                if (null != path)
                {
                    // if the path is too long, abbreviate it.
                    if (MAX_DISPLAY_PATH < path.length())
                    {
                        int second = path.length();
                        // find the second slash in the path
                        int cursor = path.indexOf( '/' ) + 1;
                        if (0 < cursor)
                        {
                            second = path.indexOf( '/', cursor ) + 1;
                            if (0 == second)
                                second = cursor;
                        }
                        // first part is path to second.
                        int last = -1;
                        // Move backwards through path to the last slash which leaves us at an abbreviated string of less than 50 chars.
                        for (cursor = path.length() - 1; cursor > second; cursor--)
                        {
                            if (path.charAt( cursor ) == '/' )
                            {
                                if (last == -1)
                                    // the last slash in the string, use it even if it makes the string too long
                                    last = cursor;
                                else if (MAX_DISPLAY_PATH > second + (path.length() - cursor))
                                    last = cursor;
                                else break; // last is what we'll use.
                            }
                        }
                        if (cursor > second)
                        {
                            // Only abbreviate when it can be done.
                            path = path.substring( 0, second ) + "..." + path.substring( last );
                        }
                    }
                    // Go through the MRU file list, and add a sub menu for each file listed.
                    JMenuItem recentFileItem = new JMenuItem();
                    recentFileItem.setActionCommand( entry.getKey() );
                    recentFileItem.addActionListener( menuActionListener );
                    recentFileItem.setHorizontalTextPosition( SwingConstants.RIGHT );
                    recentFileItem.setHorizontalAlignment( SwingConstants.LEFT );
                    recentFileItem.setText( path );
                    recentFiles.add( recentFileItem );
                }
            }
        }
        catch( BackingStoreException e )
        {
            LogAndShowError.logException( "Unable to update MRU file list.", e );
        }

    }
    
    
    private JMenuBar createMainMenu( ActionListener lSymAction )
    {
        JMenuBar mainMenuBar = new JMenuBar();

        JMenu fileMenu = new JMenu();
        fileMenu.setRequestFocusEnabled( false );
        fileMenu.setHorizontalTextPosition( SwingConstants.RIGHT );
        fileMenu.setHorizontalAlignment( SwingConstants.LEFT );
        fileMenu.setText( "File" );
        // fileMenu.setActionCommand("File");
        fileMenu.setMnemonic( (int) 'F' );
        mainMenuBar.add( fileMenu );

        JMenuItem newItem = new JMenuItem();
        newItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        newItem.setHorizontalAlignment( SwingConstants.LEFT );
        newItem.setText( "New" );
        newItem.setActionCommand( newOPF );
        newItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK ) );
        newItem.setMnemonic( (int) 'N' );
        newItem.setIcon( newIcon );
        newItem.addActionListener( lSymAction );
        fileMenu.add( newItem );

        JMenuItem openItem = new JMenuItem();
        openItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        openItem.setHorizontalAlignment( SwingConstants.LEFT );
        openItem.setText( "Open..." );
        openItem.setActionCommand( openOPF );
        openItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_O, java.awt.Event.CTRL_MASK ) );
        openItem.setMnemonic( (int) 'O' );
        openItem.setIcon( openIcon );
        openItem.addActionListener( lSymAction );
        fileMenu.add( openItem );

        saveItem = new JMenuItem();
        saveItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        saveItem.setHorizontalAlignment( SwingConstants.LEFT );
        saveItem.setText( "Save" );
        saveItem.setActionCommand( saveOPF );
        saveItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK ) );
        saveItem.setMnemonic( (int) 'S' );
        saveItem.setIcon( saveIcon );
        saveItem.addActionListener( lSymAction );
        saveItem.setEnabled( false );
        fileMenu.add( saveItem );

        saveAsItem = new JMenuItem();
        saveAsItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        saveAsItem.setHorizontalAlignment( SwingConstants.LEFT );
        saveAsItem.setText( "Save As..." );
        saveAsItem.setActionCommand( saveOPFas );
        saveAsItem.setMnemonic( (int) 'A' );
        saveAsItem.setIcon( saveAsIcon );
        saveAsItem.addActionListener( lSymAction );
        saveAsItem.setEnabled( false );
        fileMenu.add( saveAsItem );

        JMenuItem importItem = new JMenuItem();
        importItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        importItem.setHorizontalAlignment( SwingConstants.LEFT );
        importItem.setText( "Import from ePub" );
        importItem.setActionCommand( importEPub );
//      exportItem.setMnemonic( (int) 'A' );
        importItem.setIcon( openIcon );
        importItem.addActionListener( lSymAction );
        fileMenu.add( importItem );
        
        exportItem = new JMenuItem();
        exportItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        exportItem.setHorizontalAlignment( SwingConstants.LEFT );
        exportItem.setText( "Compile to ePub" );
        exportItem.setActionCommand( compileEPub );
//        exportItem.setMnemonic( (int) 'A' );
        exportItem.setIcon( saveAsIcon );
        exportItem.addActionListener( lSymAction );
        exportItem.setEnabled( false );
        fileMenu.add( exportItem );
        
        fileMenu.add( new JSeparator() );

        recentFiles = new JMenu();
        recentFiles.setRequestFocusEnabled( false );
        recentFiles.setHorizontalTextPosition( SwingConstants.RIGHT );
        recentFiles.setHorizontalAlignment( SwingConstants.LEFT );
        recentFiles.setText( "Recent Projects" );
        populateMRUFiles( null );
        fileMenu.add( recentFiles );

        fileMenu.add( new JSeparator() );

        JMenuItem exitItem = new JMenuItem();
        exitItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        exitItem.setHorizontalAlignment( SwingConstants.LEFT );
        exitItem.setText( "Exit" );
        exitItem.setActionCommand( exitOPF );
        exitItem.setIcon( exitIcon );
        exitItem.setMnemonic( (int) 'X' );
        exitItem.addActionListener( lSymAction );
        fileMenu.add( exitItem );

        editMenu = new JMenu();
        editMenu.setRequestFocusEnabled( false );
        editMenu.setHorizontalTextPosition( SwingConstants.RIGHT );
        editMenu.setHorizontalAlignment( SwingConstants.LEFT );
        editMenu.setText( "Edit" );
        // fileMenu.setActionCommand("File");
        editMenu.setMnemonic( (int) 'E' );
        editMenu.setEnabled( false );
        mainMenuBar.add( editMenu );

        JMenuItem cleanItem = new JMenuItem();
        cleanItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        cleanItem.setHorizontalAlignment( SwingConstants.LEFT );
        cleanItem.setText( "Clean" );
        cleanItem.setActionCommand( cleanOPF );
        cleanItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_C, java.awt.Event.CTRL_MASK ) );
        cleanItem.setMnemonic( (int) 'C' );
        cleanItem.setIcon( cleanIcon  );
        cleanItem.addActionListener( lSymAction );
        editMenu.add( cleanItem );

        JMenuItem subItem = new JMenuItem();
        subItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        subItem.setHorizontalAlignment( SwingConstants.LEFT );
        subItem.setText( "Replace" );
        subItem.setActionCommand( replaceTags );
        subItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_R, java.awt.Event.CTRL_MASK ) );
        subItem.setMnemonic( (int) 'R' );
        subItem.setIcon( replaceIcon );
        subItem.addActionListener( lSymAction );
        editMenu.add( subItem );

        JMenuItem insItem = new JMenuItem();
        insItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        insItem.setHorizontalAlignment( SwingConstants.LEFT );
        insItem.setText( "Insert" );
        insItem.setActionCommand( insertBefore );
        insItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_I, java.awt.Event.CTRL_MASK ) );
        insItem.setMnemonic( (int) 'I' );
        insItem.setIcon( insertIcon );
        insItem.addActionListener( lSymAction );
        editMenu.add( insItem );

        editMenu.add( new JSeparator() );

        JMenuItem tocItem = new JMenuItem();
        tocItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        tocItem.setHorizontalAlignment( SwingConstants.LEFT );
        tocItem.setText( "Build TOC" );
        tocItem.setActionCommand( buildTOC );
        tocItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_T, java.awt.Event.CTRL_MASK ) );
        tocItem.setMnemonic( (int) 'T' );
        tocItem.setIcon( tocIcon );
        tocItem.addActionListener( lSymAction );
        editMenu.add( tocItem );

        JMenuItem ncxItem = new JMenuItem();
        ncxItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        ncxItem.setHorizontalAlignment( SwingConstants.LEFT );
        ncxItem.setText( "Build NCX" );
        ncxItem.setActionCommand( buildNCX );
        ncxItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK ) );
        ncxItem.setMnemonic( (int) 'T' );
        ncxItem.setIcon( ncxIcon );
        ncxItem.addActionListener( lSymAction );
        editMenu.add( ncxItem );

        JMenuItem coverItem = new JMenuItem();
        coverItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        coverItem.setHorizontalAlignment( SwingConstants.LEFT );
        coverItem.setText( "Build Cover" );
        coverItem.setActionCommand( buildCover );
        coverItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK ) );
        coverItem.setMnemonic( (int) 'C' );
        coverItem.setIcon( imageIcon );
        coverItem.addActionListener( lSymAction );
        editMenu.add( coverItem );

        reportMenu = new JMenu();
        reportMenu.setRequestFocusEnabled( false );
        reportMenu.setHorizontalTextPosition( SwingConstants.RIGHT );
        reportMenu.setHorizontalAlignment( SwingConstants.LEFT );
        reportMenu.setText( "Reports" );
        // helpMenu.setActionCommand( "Help" );
        reportMenu.setMnemonic( (int) 'R' );
        reportMenu.setEnabled( false );
        mainMenuBar.add( reportMenu );
        
        styleItem = new JMenuItem();
        styleItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        styleItem.setHorizontalAlignment( SwingConstants.LEFT );
        styleItem.setText( "Style Report" );
        styleItem.setActionCommand( styleReport );
        styleItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_S, java.awt.Event.CTRL_MASK ) );
        styleItem.setMnemonic( (int) 'S' );
        styleItem.setIcon( cssIcon );
        styleItem.addActionListener( lSymAction );
        reportMenu.add( styleItem );

        validateItem = new JMenuItem();
        validateItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        validateItem.setHorizontalAlignment( SwingConstants.LEFT );
        validateItem.setText( "ePub Checker" );
        validateItem.setActionCommand( validate );
        validateItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_V, java.awt.Event.CTRL_MASK ) );
        validateItem.setMnemonic( (int) 'V' );
        validateItem.setIcon( reportIcon );
        validateItem.addActionListener( lSymAction );
        reportMenu.add( validateItem );

        JMenu toolsMenu = new JMenu();
        toolsMenu.setRequestFocusEnabled( false );
        toolsMenu.setHorizontalTextPosition( SwingConstants.RIGHT );
        toolsMenu.setHorizontalAlignment( SwingConstants.LEFT );
        toolsMenu.setText( "Tools" );
        toolsMenu.setMnemonic( (int) 'T' );
        mainMenuBar.add( toolsMenu );
        
        JMenuItem editorsItem = new JMenuItem();
        editorsItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        editorsItem.setHorizontalAlignment( SwingConstants.LEFT );
        editorsItem.setText( "Editors" );
        editorsItem.setActionCommand( editors );
        editorsItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_E, java.awt.Event.CTRL_MASK ) );
        editorsItem.setMnemonic( (int) 'E' );
        editorsItem.setIcon( editorsIcon );
        editorsItem.addActionListener( lSymAction );
        toolsMenu.add( editorsItem );

        JMenuItem xformersItem = new JMenuItem();
        xformersItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        xformersItem.setHorizontalAlignment( SwingConstants.LEFT );
        xformersItem.setText( "Transformers" );
        xformersItem.setActionCommand( xformer );
        xformersItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_T, java.awt.Event.CTRL_MASK ) );
        xformersItem.setMnemonic( (int) 'T' );
        xformersItem.setIcon( xformersIcon );
        xformersItem.addActionListener( lSymAction );
        toolsMenu.add( xformersItem );

        JMenuItem optionsItem = new JMenuItem();
        optionsItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        optionsItem.setHorizontalAlignment( SwingConstants.LEFT );
        optionsItem.setText( "Preferences" );
        optionsItem.setActionCommand( options );
        optionsItem.setAccelerator( KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_P, java.awt.Event.CTRL_MASK ) );
        optionsItem.setMnemonic( (int) 'P' );
        optionsItem.setIcon( prefsIcon );
        optionsItem.addActionListener( lSymAction );
        toolsMenu.add( optionsItem );

        JMenu helpMenu = new JMenu();
        helpMenu.setRequestFocusEnabled( false );
        helpMenu.setHorizontalTextPosition( SwingConstants.RIGHT );
        helpMenu.setHorizontalAlignment( SwingConstants.LEFT );
        helpMenu.setText( "Help" );
        // helpMenu.setActionCommand( "Help" );
        helpMenu.setMnemonic( (int) 'H' );
        mainMenuBar.add( helpMenu );

        JMenuItem aboutItem = new JMenuItem();
        aboutItem.setHorizontalTextPosition( SwingConstants.RIGHT );
        aboutItem.setHorizontalAlignment( SwingConstants.LEFT );
        aboutItem.setText( "About..." );
        aboutItem.setActionCommand( aboutOPF );
        aboutItem.setMnemonic( (int) 'A' );
        aboutItem.setIcon( aboutIcon );
        aboutItem.addActionListener( lSymAction );
        helpMenu.add( aboutItem );

        return mainMenuBar;
    }

    @SuppressWarnings("unused")
    private JToolBar createToolBar( ActionListener lSymAction )
    {
        JToolBar toolBar = new JToolBar();
        // JPanel toolBarPanel = new JPanel();
        // JButton newButton = new JButton();
        // JButton openButton = new JButton();
        // JButton saveButton = new JButton();
        // JButton aboutButton = new JButton();

        // toolBarPanel.setAlignmentY(0.0F);
        // toolBarPanel.setAlignmentX(0.0F);
        // toolBarPanel.setLayout(new BoxLayout(toolBarPanel,BoxLayout.X_AXIS));
        // getContentPane().add(toolBarPanel);
        // toolBarPanel.setBounds(0,0,232,33);
        // toolBar.setBorder( BorderFactory.createEmptyBorder());
        // toolBarPanel.add(JToolBar1);
        // getContentPane().add(toolBar);
        // LayoutManager lo = getLayout();
        // layout.putConstraint( SpringLayout.NORTH,
        // toolBar, 0, SpringLayout.NORTH, getContentPane());
        // JToolBar1.setBounds(0,0,232,33);

        // newButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        // newButton.setAlignmentY(0.0F);
        // newButton.setDefaultCapable(false);
        // newButton.setToolTipText("Create a new document");
        // newButton.setMnemonic((int)'N');
        // newButton.setActionCommand( newOPF );
        // toolBar.add(newButton);

        // newButton.setBounds(16,4,51,27);
        // newButton.setPreferredSize(toolButtonSize);
        // newButton.setIcon(newIcon);
        // openButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        // openButton.setAlignmentY(0.0F);
        // openButton.setDefaultCapable(false);
        // openButton.setToolTipText("Open an existing document");
        // openButton.setMnemonic((int)'O');
        // toolBar.add(openButton);

        // openButton.setBounds(67,4,51,27);
        // openButton.setPreferredSize(toolButtonSize);
        // openButton.setIcon(openIcon);
        // saveButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        // saveButton.setAlignmentY(0.0F);
        // saveButton.setDefaultCapable(false);
        // saveButton.setToolTipText("Save the active document");
        // saveButton.setMnemonic((int)'S');
        // toolBar.add(saveButton);

        // saveButton.setBounds(118,4,51,27);
        // saveButton.setPreferredSize(toolButtonSize);
        // saveButton.setIcon(saveIcon);
        // toolBar.add( Box.createHorizontalStrut( 16 ));
        // aboutButton.setHorizontalTextPosition(SwingConstants.RIGHT);
        // aboutButton.setAlignmentY(0.0F);
        // aboutButton.setDefaultCapable(false);
        // aboutButton.setToolTipText("Display program information, version number and copyright");
        // aboutButton.setMnemonic((int)'A');
        // aboutButton.setIcon(aboutIcon);
        // toolBar.add(aboutButton);

        // aboutButton.setBounds(179,4,51,27);
        // aboutButton.setPreferredSize(toolButtonSize);
        // toolBar.add( Box.createHorizontalGlue() );
        // newButton.addActionListener(lSymAction);
        // openButton.addActionListener(lSymAction);
        // saveButton.addActionListener(lSymAction);
        // aboutButton.addActionListener(lSymAction);
        return toolBar;
    }

    /**
     * Constructor for a new ePubEditor instance.
     * 
     * @param args
     *            TBD
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public EPubEditor( String args[] ) throws ParserConfigurationException,
            IOException, SAXException
    {
        String classPath = System.getProperty( "java.class.path" );
        int semiColon = classPath.indexOf( ';' );
        if (0 < semiColon)
            classPath = classPath.substring( 0,  semiColon );
        File logPath = new File( classPath );
        if (!logPath.isDirectory())
            logPath = logPath.getCanonicalFile().getParentFile();
        
        // Set up logging first, so we can log errors from the start.
        RollingFileAppender logConfig = new RollingFileAppender();
        logConfig.setFile( logPath + File.separator + "ePubEditor.log" );
        logConfig.setName( "RollingLog" );
        logConfig.setMaxFileSize( "1MB" );
        logConfig.setLayout( new PatternLayout( "%-5p - %d{DATE}: %m%n") );
        logConfig.activateOptions();
        BasicConfigurator.configure( logConfig );

        Logger logger = Logger.getRootLogger();
        logger.setLevel( Level.WARN );
        logger.debug( classPath + " | " + logPath + File.separator + "ePubEditor.log" );

        int option;
        
        for (option = 0; option < args.length; option++)
        { 
            if (args[option].equals( "-d" ) && option + 1 < args.length) 
            { 
                //  Someone wants to change the logging level 
                ++option; 
                if (args[option].equalsIgnoreCase( "debug" )) 
                    logger.setLevel( Level.DEBUG ); 
                else if (args[option].equalsIgnoreCase( "info" ))
                    logger.setLevel( Level.INFO ); 
                else if (args[option].equalsIgnoreCase( "off" )) 
                    logger.setLevel( Level.OFF ); 
                else if (args[option].equalsIgnoreCase( "error" )) 
                    logger.setLevel( Level.ERROR ); 
                else if (args[option].equalsIgnoreCase( "fatal" )) 
                    logger.setLevel( Level.FATAL ); 
                else if (args[option].equalsIgnoreCase( "all" )) 
                    logger.setLevel( Level.ALL ); 
                else if (args[option].equalsIgnoreCase( "trace" )) 
                    logger.setLevel( Level.TRACE ); 
                else 
                    logger.setLevel( Level.WARN ); 
            } 
            else 
            {
                logger.info( args[option] ); 
                break;
            }
        } 

        logger.info( "\nePubEditor starting with log level " + logger.getLevel() );

        try
        {
            new EPubFileCheck();
            ePubCheckPresent = true;
        }
        catch (NoClassDefFoundError ex)
        {
            LogAndShowError.logException( "epubcheck.jar, or one of its dependencies, was not found\n" +
                    "in the class path. ePub checking has been disabled.", ex );
        }

        try
        {
            new CSSOMParser();
            cssParserPresent = true;
        }
        catch (NoClassDefFoundError ex)
        {
            LogAndShowError.logException( "cssparser.jar, or one of its dependencies, was not found\n" +
                    "in the class path. Style reporting has been disabled.", ex );
        }

        // Set up the main window.
        JTabbedPane tabbedPane = new JTabbedPane();
        setVisible( false );
        setTitle( "Open eBook Package File Editor" );
        setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        setLayout( new BoxLayout( getContentPane(), BoxLayout.PAGE_AXIS ) );
        setSize( 800, 480 );

        menuActionListener = new MenuActionListener(); // This class handles all action events

        // Set up the toolbar - maybe

        // Setup the menu
        setJMenuBar( createMainMenu( menuActionListener ) );

        // Setup the tabbed pane which holds the real content
        getContentPane().add( tabbedPane );

        manifestPanel = new ManifestPanel( this );
        manifestPanel.addObserver( this );
        
        tabbedPane.addChangeListener( manifestPanel );
        tabbedPane.addTab( "Manifest", manifestPanel );

        contentPanel = new SpinePanel( null );
        tabbedPane.addTab( "Content", contentPanel );

        contribPanel = new ContribPanel( _epubModel );
        tabbedPane.addTab( "Authors and Contributors", contribPanel );

        propPanel = new MetadataPanel( null );
        tabbedPane.addTab( "Properties", propPanel );

        guidePanel = new GuidePanel( null );
        tabbedPane.addTab( "Guides", guidePanel );

        SymWindow aSymWindow = new SymWindow();
        this.addWindowListener( aSymWindow );
        
        // If there was a file specified on the command line, try to open it
        // and load the file. If no file is specified, or if the open fails
        // create a default model
        if (args.length > 0 && option < args.length)
        {
            openFile( new File( args[option] ));
        }
    }

    private void openFile( File f )
    {
        try
        {
            Logger.getLogger( "OpenOEBFile" ).info( "Opening " + f.getCanonicalPath() );

            _epubModel = new EPubModel( f, null );
            contribPanel.setModelData( _epubModel );
            contentPanel.setModelData( getOpfData() );
            propPanel.setModelData( getOpfData().getMetadata() );
            manifestPanel.setModelData( getOpfData().getManifest() );
            guidePanel.setModelData( getOpfData() );
            saveItem.setEnabled( true );
            saveAsItem.setEnabled( true );
            exportItem.setEnabled( true );
            editMenu.setEnabled( true );
            reportMenu.setEnabled( true );
            if (!ePubCheckPresent)
                validateItem.setEnabled( false );
            if (!cssParserPresent)
                styleItem.setEnabled( false );
            // Don't wait to save the properties file if we had a successful open
            // Add the .opf file to the MRU list.
            String newPath = _epubModel.getOpfFile().getCanonicalPath().replace( '\\', '/' );
            prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_OPF_OPEN, newPath );
            populateMRUFiles( newPath );
            // Save the root of the ePub document. If this was an import or if we have specific
            // metadata we know the answer, otherwise it's a guess
            prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_CONTENT, 
                    _epubModel.getEpubRootPath().replace( '\\', '/' )
                    + "/"  );
            if (!_epubModel.getOpfFile().equals( _epubModel.baseFile ))
            {
                // the .opf file is not the same as the file we opened, we must have opened
                // an archive file. Set this as the MRU ePub file.
                newPath = _epubModel.baseFile.getCanonicalPath().replace( '\\', '/' );
                prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_EPUB_OPEN, newPath );
            }
            prefs.flush();
        }
        catch( SAXParseException sex )
        {
            LogAndShowError.logAndShowEx(
                    "Unable to parse " + f.getName() + ", at line "
                            + sex.getLineNumber() + ", column " + sex.getColumnNumber()
                            + "\n" + sex.getLocalizedMessage(), sex );
        }
        catch( SAXException ex )
        {
            LogAndShowError.logAndShowEx( "Unable to parse the indicated .opf file.\n"
                    + ex.getLocalizedMessage(), ex );
        }
        catch( FileNotFoundException ex )
        {
            IOError( ex, "creating a temporary directory");
        }
        catch( IOException ex )
        {
            IOError( ex, "opening an OEB file" );
        }
        catch( BackingStoreException ignore ) {}
    }

    /**
     * Creates a new instance of JFrame1 with the given title.
     * 
     * @param sTitle
     *            the title for the new frame.
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @see #JFrame1()
     */
    public EPubEditor( String args[], String sTitle )
            throws ParserConfigurationException, IOException, SAXException
    {
        this( args );
        setTitle( sTitle );
    }

    void exitApplication()
    {
        try
        {
            /*
             * // Beep Toolkit.getDefaultToolkit().beep();
             * // Show a confirmation dialog 
             * int reply = JOptionPane.showConfirmDialog( this, "Do you really want to exit?", 
             * "JFC Application - Exit" , JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
             * // If the confirmation was affirmative, handle exiting.
             *  if (reply == JOptionPane.YES_OPTION)
             */
            {
                prefs.flush();
//                File f = new File( prefFile );
//                prefs.exportSubtree( new FileOutputStream( f ));

                this.setVisible( false );   // hide the Frame
                this.dispose();             // free the system resources
                System.exit( 0 );           // close the application
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    class SymWindow extends java.awt.event.WindowAdapter
    {
        public void windowClosing( java.awt.event.WindowEvent event )
        {
            Object object = event.getSource();
            if (object == EPubEditor.this)
                OPFEditor_windowClosing( event );
        }
    }

    void OPFEditor_windowClosing( java.awt.event.WindowEvent event )
    {
        // TODO: if isDirty, do you want to save??
        try
        {
            this.exitApplication();
        }
        catch( Exception e )
        {
        }
    }

    public void IOError( IOException t, String when )
    {
        String message = "An unknown IO error has occured while " + when;
        Logger.getLogger( "IOError" ).error( message, t );
        JOptionPane.showMessageDialog( this, message, "I/O Error",
                JOptionPane.ERROR_MESSAGE );
    }

    public void transformerError( Exception t )
    {
        String message = "A fatal error has occured while trying to save the OPF file.\nThe file was not saved.";
        Logger.getLogger( "TransformerError" ).error( message, t );
        JOptionPane.showMessageDialog( this, message, "Transformer Error",
                JOptionPane.ERROR_MESSAGE );
    }

    void openOEBFile()
    {
        File startPath = null;
    
        String openDir = prefs.node( PREFS_PATHS ).get( PREFS_PATHS_OPF_OPEN, null );
        if (null != openDir)
        {
            startPath = new File( openDir );
            if (startPath.isDirectory())
                startPath = new File( openDir + File.separator + "*.opf" );
        }
        else if (null != _epubModel)
            startPath = _epubModel.baseFile;
        JFileChooser fc = new JFileChooser( startPath );
        fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
        fc.setSelectedFile( startPath );
        fc.setAcceptAllFileFilterUsed( false );
        fc.addChoosableFileFilter( new OpfFileFilter() );
        Cursor cursor = getContentPane().getCursor();
        if (fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION) try
        {
            getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
            File f = fc.getSelectedFile();
            openFile( f );
        }
        finally
        {
            getContentPane().setCursor( cursor );
        }
    }

    
    void openEPubFile()
    {
        File startPath = null;
        
        String openDir = prefs.node( PREFS_PATHS ).get( PREFS_PATHS_EPUB_OPEN, null );
        if (null != openDir)
        {
            startPath = new File( openDir );
            if (startPath.isDirectory())
                startPath = new File( openDir + File.separator + "*.epub" );
        }
        else if (null != _epubModel)
            startPath = _epubModel.baseFile;
        JFileChooser fc = new JFileChooser( startPath );
        fc.setFileSelectionMode( JFileChooser.FILES_ONLY );
        fc.setSelectedFile( startPath );
        fc.setAcceptAllFileFilterUsed( false );
        fc.addChoosableFileFilter( new EpubFileFilter() );
        Cursor cursor = getContentPane().getCursor();
        if (fc.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION) try
        {
            getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
            File f = fc.getSelectedFile();
            openFile( f );
        }
        finally
        {
            getContentPane().setCursor( cursor );
        }
      
    }
    
    
    private File selectPublicationRoot()
    {
        File openPath;
        if (null != _epubModel && 0 < _epubModel.getEpubRootPath().length())
            openPath = new File( _epubModel.getEpubRootPath() );
        else
            openPath = new File( prefs.node( PREFS_PATHS ).get( PREFS_PATHS_OPF_OPEN, "." ) );
        JFileChooser fc = new JFileChooser( openPath + File.separator + "*.opf" );
        fc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        fc.setSelectedFile( openPath );
        fc.setAcceptAllFileFilterUsed( false );
        fc.setDialogTitle( "Select the publication base folder" );
        if (fc.showDialog( this, "Select" ) == JFileChooser.APPROVE_OPTION)
        {
            return fc.getSelectedFile();
        }
        return null;
    }

    public void newOEBFile() throws BackingStoreException
    {
        // TODO: if opfData is not null, we are already editing a file which may or may not
        // have been saved. Open a dialog to ask if the existing project should be saved.

        _epubModel = null;

        // Open browser dialog to set base path
        File base = selectPublicationRoot();
        if (null != base)
        {
            Cursor cursor = getContentPane().getCursor();
            getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
            try
            {
                _epubModel = new EPubModel( null, base );

                contribPanel.setModelData( _epubModel );
                contentPanel.setModelData( getOpfData() );
                propPanel.setModelData( getOpfData().getMetadata() );
                manifestPanel.setModelData( getOpfData().getManifest() );
                guidePanel.setModelData( getOpfData() );
                saveItem.setEnabled( true );
                exportItem.setEnabled( true );
                saveAsItem.setEnabled( true );
                editMenu.setEnabled( true );
                reportMenu.setEnabled( true );
                getUserCSSFile();
                prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_OPF_OPEN, base.getAbsolutePath().replace( '\\', '/' )
                        + "/" );
                prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_CONTENT, base.getAbsolutePath().replace( '\\', '/' )
                        + "/" );
                prefs.flush();
            }
            catch( SAXException ex )
            {
                LogAndShowError.logAndShowEx(
                        "Unable to parse the indicated .opf file.\n"
                                + ex.getLocalizedMessage(), ex );
            }
            catch( IOException ex )
            {
                IOError( ex, "opening an OEB file" );
            }
            finally
            {
                getContentPane().setCursor( cursor );
            }
        }
    }

    /**
     * Save the .opf file according to its current name - no overwrite warning will be given
     **/
    private void saveOPFFile()
    {
        if (null == _epubModel.getOpfFile())
        {
            // no file was opened, we must ask which file to save.
            saveOPFFileAs();
        }
        else
        {
            manifestPanel.stopEditing();
            contentPanel.stopEditing();
            contribPanel.stopEditing();
            propPanel.stopEditing();
            guidePanel.stopEditing();

            Cursor cursor = getContentPane().getCursor();
            // If there is a baseFile, we will have an .opf file even if the file opened was .epub
            try
            {
                _epubModel.saveOPFFile();
            }
            catch( FileNotFoundException e )
            {
                // This can only happen when the file cannot be written
                // because it is opened or locked by another program
                JOptionPane
                        .showMessageDialog(
                                this,
                                _epubModel.baseFile.getAbsoluteFile()
                                        + " could not be saved. Check file system permissions and try again\n"
                                        + "or save this file to a different location.",
                                "Error", JOptionPane.ERROR_MESSAGE );
            }
            catch( TransformerException ex )
            {
                transformerError( ex );
            }
            catch( IOException ex )
            {
                IOError( ex, "saving the project file" );
            }
            finally
            {
                getContentPane().setCursor( cursor );
            }
        }
    }
    
//    private void saveEPubFile()
//    {
//        if (null == _epubModel.baseFile)
//        {
//            // no file was opened, we must ask which file to save.
//            saveOPFFileAs();
//        }
//        else
//        {
//            manifestPanel.stopEditing();
//            contentPanel.stopEditing();
//            contribPanel.stopEditing();
//            propPanel.stopEditing();
//            guidePanel.stopEditing();
//
//            Cursor cursor = getContentPane().getCursor();
//
//            try
//            {
//                getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
//                if (_epubModel.baseFile.getName().toLowerCase().endsWith( ".epub" ))
//                {
//                    _epubModel.saveEpub( _epubModel.baseFile );
//                    try
//                    {
//                        prefs.node( PREFS_PATHS ).put( PREFS_PATHS_SAVE_FILE,
//                                _epubModel.baseFile.getCanonicalPath() );
//                    }
//                    catch( IOException ignore )
//                    {
//                    }
//                }
//                else
//                {
//                    if (!_epubModel.baseFile.getName().toLowerCase().endsWith( ".opf" ))
//                        _epubModel.baseFile = new File( _epubModel.baseFile.getAbsolutePath() + ".opf" );
//                    if (_epubModel.setOPFFile( _epubModel.baseFile ))
//                        _epubModel.saveOPFFile();
//                }
//            }
//            catch( TransformerException ex )
//            {
//                transformerError( ex );
//            }
//            catch( FileNotFoundException ignore )
//            {
//                // This can only happen when the file cannot be written
//                // because it is opened or locked by another program
//                JOptionPane
//                        .showMessageDialog(
//                                this,
//                                _epubModel.baseFile.getAbsoluteFile()
//                                        + " could not be saved. Check file system permissions and try again\n"
//                                        + "or save this file to a different location.",
//                                "Error", JOptionPane.ERROR_MESSAGE );
//            }
//            catch( IOException ex )
//            {
//                IOError( ex, "saving the OEB file" );
//            }
//            finally
//            {
//                getContentPane().setCursor( cursor );
//            }
//        }
//    }

    private void saveOPFFileAs()
    {
        // Stop all editing in all panels before saving the project
        manifestPanel.stopEditing();
        contentPanel.stopEditing();
        contribPanel.stopEditing();
        propPanel.stopEditing();
        guidePanel.stopEditing();

        // What is the default save path? 1. the same as the opf file we
        // opened; 2. the parent of the epub root folder; or 3. the system 
        // documents folder (indicated by null path).

        String savePath = null;
        File saveFile = _epubModel.getOpfFile();
        if (null != saveFile)
            savePath = _epubModel.getOpfFolder().getPath();
        else if (null != _epubModel.getEpubRootPath())
            savePath = new File( _epubModel.getEpubRootPath() ).getAbsolutePath();
        
        // Get the last save path, but use the .opf parent path as a default
//        String saveAsPath = prefs.node( PREFS_PATHS ).get( PREFS_PATHS_SAVE_FILE, savePath );
        
        // Select the file to save. Insist that the new .opf be a descendant of the epub root path.
        JFileChooser fc = new JFileChooser( savePath );
        fc.setAcceptAllFileFilterUsed( false );
        fc.addChoosableFileFilter( new OpfFileFilter() );
        if (null == saveFile)
            saveFile = new File( savePath, new File( savePath ).getName());
        if (saveFile.isDirectory())
        {
            saveFile = new File( saveFile.getName() + ".opf" );
        }
        else
        {
            savePath = saveFile.getName().toLowerCase();
            if (!savePath.endsWith( ".opf" ))
            {
                saveFile = new File( savePath + ".opf" );
            }
        }
        fc.setSelectedFile( saveFile );
        int selected = JOptionPane.NO_OPTION;
        saveFile = null;
        while (JOptionPane.NO_OPTION == selected)
        {
            if (fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION)
            {
                saveFile = fc.getSelectedFile();

                // check to see if the path is a descendant of the epub root. If not,
                // ask again.
                // TODO: change so if the opf is being saved somewhere other than the
                // epub root the entire project gets copied. Might be best to implement
                // this in _epubModel.saveOPFFile(), or maybe have saveOPFFile call 
                // setEpubRoot if the root changed.
                String relativity = FileUtil.getPathRelativeToBase( saveFile,
                        new File( _epubModel.getEpubRootPath() ));
                if (relativity.startsWith( ".." ))
                {
                    fc.setCurrentDirectory( new File( _epubModel.getEpubRootPath() ));
                    continue;
                }

                // file must end with the .opf extension.
                if (!saveFile.getName().toLowerCase().endsWith( ".opf" ))
                    saveFile = new File( saveFile.getAbsolutePath() + ".opf" );
                
                // Check to see if the file exists, and if so ask if the
                // user wants to replace it.
                if (saveFile.exists())
                {
                    selected = JOptionPane
                            .showConfirmDialog(
                                    this,
                                    saveFile.getAbsolutePath()
                                            + " already exists\nDo you want to replace it?",
                                    "Save As", JOptionPane.YES_NO_OPTION );
                    if (JOptionPane.CLOSED_OPTION == selected)
                        selected = JOptionPane.NO_OPTION;
                }
                else
                    selected = JOptionPane.OK_OPTION;
            }
            else
                selected = JOptionPane.CANCEL_OPTION;
        }
        if (saveFile != null && JOptionPane.OK_OPTION == selected)
        {
            try
            {
                if (_epubModel.setOPFFile( saveFile ))
                {
                    _epubModel.saveOPFFile();
                    
                    // Once saved, put this on top of the project MRU list.
                    prefs.node( PREFS_PATHS ).put(  PREFS_PATHS_OPF_OPEN, saveFile.getCanonicalPath() );
                    populateMRUFiles( saveFile.getCanonicalPath() );
                }
            }
            catch( FileNotFoundException ignore )
            {
                // This can only happen when the file cannot be written
                // because it is opened or locked by another program
                JOptionPane.showMessageDialog(
                        this,
                        _epubModel.getOpfFile().getAbsolutePath()
                                + " could not be saved. Check file system permissions and try again\n"
                                + "or save this file to a different location.",
                        "Error", JOptionPane.ERROR_MESSAGE );
            }
            catch( TransformerException ex )
            {
                transformerError( ex );
            }
            catch( IOException e )
            {
                // TODO Auto-generated catch block
                IOError(e, "saving " + _epubModel.getOpfFile().getAbsolutePath() );
            }
        }
    }
    
    private void exportEPub()
    {
        // FIXED: SF BUG ID: 3426975 - Save as Epub should set document base folder
        while (!_epubModel.isRooted())
        {
            File newRoot = selectPublicationRoot();
            // If null, selection was canceled 
            if (null == newRoot)
                return;
            _epubModel.setEpubRoot( newRoot );
        }

        // start by saving the .opf file. This will cause editing on all pages to stop
        saveOPFFile();
        
        if (null == _epubModel.getOpfFile())
            return; // if there is no .opf file then the previous save failed. We can't proceed
        
        // What is the default export path? 1. The last place we saved an .epub file to, 
        // or 2. the parent of the epub root folder. We wouldn't have gotten this far if
        // we weren't rooted, so one of these two things must be present.
        String savePath = new File( _epubModel.getEpubRootPath() ).getParent();
        
        // Get the last save path, but use the .opf parent path as a default
        String saveAsPath = prefs.node( PREFS_PATHS ).get( PREFS_PATHS_SAVE_EPUB, savePath );
        String rootName =  FileUtil.getFileName( new File( _epubModel.getEpubRootPath() ));
        
        ContributorModel cm = contribPanel.getModelData();
        // Find the "file-as" value for the first author;
        for (int i = 0; i < cm.getRowCount(); i++)
        {
            String role = (String) cm.getValueAt( i, 1 );
            if (role.equalsIgnoreCase( ContributorModel.roles[0][1] ))
            {
                String fileAs = (String) cm.getValueAt( i, 2 );
                if (0 < fileAs.length())
                    rootName = fileAs;
                else
                    rootName = (String) cm.getValueAt( i, 0 );
                break;
            }
        }
        
        int titleIdx = getOpfData().getMetadata().getPropIndex( MetadataModel.propNames[1] );
        String title = getOpfData().getMetadata().getProperty( MetadataModel.propNames[1] );
        
        File saveFile = new File( new File( saveAsPath ).getParentFile(), rootName + " - " + title );
        
        JFileChooser fc = new JFileChooser( savePath );
        fc.setAcceptAllFileFilterUsed( false );
        fc.addChoosableFileFilter( new EpubFileFilter() );
        
        if (saveFile.isDirectory())
        {
            // Select the file to save. Default epub name is the folder name, 
            // minus any extension if it exists.
            saveFile = new File(saveFile, rootName );
        }
        savePath = saveFile.getName().toLowerCase();
        if (   !savePath.endsWith( ".epub" )
            && !savePath.endsWith( ".zip" ))
        {
            saveFile = new File( saveFile + ".epub" );
        }

        fc.setSelectedFile( saveFile );
        int selected = JOptionPane.NO_OPTION;
        while (JOptionPane.NO_OPTION == selected)
        {
            if (fc.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION)
            {
                saveFile = fc.getSelectedFile();

                // Check to see if the file exists, and if so ask if the
                // user wants to replace it.
                if (saveFile.exists())
                {
                    selected = JOptionPane
                            .showConfirmDialog(
                                    this,
                                    saveFile.getAbsolutePath()
                                            + " already exists\nDo you want to replace it?",
                                    "Save As", JOptionPane.YES_NO_OPTION );
                    if (JOptionPane.CLOSED_OPTION == selected)
                        selected = JOptionPane.NO_OPTION;
                }
                else
                    selected = JOptionPane.OK_OPTION;
            }
            else
                selected = JOptionPane.CANCEL_OPTION;
        }
        if (saveFile != null && 0 == selected)
        {
            if (   !saveFile.getName().toLowerCase().endsWith( ".epub" )
                && !saveFile.getName().toLowerCase().endsWith( ".zip" ))
            {
                saveFile = new File( saveFile + ".epub" );
            }
            if (   null != getOpfData().getManifest() 
                && !getOpfData().getManifest().isValid())
            {
                LogAndShowError.logAndShowNoEx( 
                        "One or more files listed in the manifest do not exist.\n" +
                		"The file was not saved" );
            }
            else
            {
                Cursor cursor = getContentPane().getCursor();
                try
                {
                    getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
                    _epubModel.saveEpub( saveFile );
                    prefs.node( PREFS_PATHS ).put( PREFS_PATHS_SAVE_EPUB, saveFile.getCanonicalPath() );
                }
                catch( FileNotFoundException ex )
                {
                    String message = "The manifested file \n\""
                            + ex.getMessage()
                            + "\"\n could not be found in the file system.";
                    JOptionPane.showMessageDialog( this, message,
                            "File Not Found", JOptionPane.ERROR_MESSAGE );
                    Logger.getLogger( "FileNotFound" ).error( message, ex );

                    ex.printStackTrace();
                }
                catch( ZipException ex )
                {
                    JOptionPane.showMessageDialog( this, ex.getMessage(),
                            "Zip error", JOptionPane.ERROR_MESSAGE );
                    Logger.getLogger( "FileNotFound" ).error(
                            ex.getMessage(), ex );

                    ex.printStackTrace();

                }
                catch( TransformerException ex )
                {
                    transformerError( ex );
                }
                catch( IOException ex )
                {
                    IOError( ex, "saving the OEB file" );
                }
                finally
                {
                    getContentPane().setCursor( cursor );
                }
            }
        }
    }

    
    private void buildNCX()
    {
        Cursor cursor = getContentPane().getCursor();
        getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        EPubUtil.buildNCX( _epubModel, getUserCSSFile(), true );
        getContentPane().setCursor( cursor );
    }


    private File getUserCSSFile()
    {
        String cssFileName = prefs.node( PREFS_PATHS ).get( PREFS_USERCSSS, "ebook.css" );
        File newCssFile = null, usercss = new File( cssFileName );
        if (usercss.exists())
            try
            {
                // copy this file to the same folder as the .opf file.
                File opfRoot = _epubModel.getOpfFolder();
                cssFileName = usercss.getName();
                newCssFile = new File( opfRoot, cssFileName );
                if (!newCssFile.exists())
                {
                    FileUtil.copyFile( usercss, newCssFile );
                    // Add the user's style sheet to the manifest. If it already
                    // exists, we'll get back it's index in the manifest list.
                }
                manifestPanel.getModel().addManifestItem( "usercss", cssFileName, null );
            }
            catch( FileNotFoundException ex )
            {
                // if the file does not exist, is a directory rather than a regular file,
                // or for some other reason cannot be opened for reading.
                ex.printStackTrace();
            }
            catch( IOException ex )
            {
                // If some other I/O error occurs
                ex.printStackTrace();
            }
        else
            cssFileName = null;
        return newCssFile;
    }

    
    public void cleanOEB()
    {
        if (0 < getOpfData().getSpine().getRowCount())
        {
            // if there is a usercss file specified in the properties file,
            // copy that to the same directory as the .opf file, and
            // then pass that file to the clean routine.
            File newCssFile = getUserCSSFile();
    
            CleanInBackground cleaner = new CleanInBackground(  _epubModel, newCssFile, EPubModel.db );
            new ObservingProgressMonitor( this, cleaner,
                    "Cleaning OEB Publication files", null, 0, 
                    getOpfData().getSpine().getRowCount() );

            cleaner.execute();
            manifestPanel.refresh();
        }
    }
    
    
    private void replaceTags()
    {
        ReplaceTagDialog rtd = new ReplaceTagDialog( this, _epubModel );
        rtd.setVisible( true );
    }

    
    private void insertBefore()
    {
        InsertTagDialog itd = new InsertTagDialog( this, _epubModel );
        itd.setVisible( true );

    }

    
    private void generateStyleReport()
    {
        Cursor cursor = getContentPane().getCursor();
        getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        try
        {
            // if there is a usercss file specified in the properties file,
            // copy that to the same directory as the .opf file, and
            // then pass that file to the clean routine.
            String cssFileName = prefs.node( PREFS_PATHS ).get( PREFS_USERCSSS, "ebook.css" );
            File usercss = null;
            if (null != cssFileName)
            {
                usercss = new File( cssFileName );
                if (!usercss.exists())
                    usercss = null;
            }
            GenerateStyleReport styles = new GenerateStyleReport( usercss, _epubModel );
            new ObservingProgressMonitor( this, styles, "Generating Style Report", null, 0, 
                        _epubModel.getOpfData().getSpine().getRowCount() );
            styles.execute();
            // return cursor to standard state.
            DefaultTableModel styleReport = styles.get();
            getContentPane().setCursor( cursor );
            if (null != styleReport)
            {
                JScrollPane scroller;
                int numRows = styleReport.getRowCount();
                if (numRows == 0)
                {
                    JTextArea textArea = new JTextArea( 45, 120 );
                    textArea.setEditable( false );
                    textArea.setLineWrap( true );
                    textArea.setWrapStyleWord( true );
                    textArea.setText( "No unanticipated document styles discovered" );
                    textArea.setCaretPosition( 0 );
                    scroller = new JScrollPane( textArea );
                }
                else
                {
                    // open the report, and show it in a non-modal dialog
                    JTable table = new JTable();
                    table.setAutoCreateRowSorter( true );
                    table.setModel( styleReport );
                    TableColumnModel cm = table.getColumnModel();
                    cm.getColumn( 0 ).setPreferredWidth( 150 );
                    cm.getColumn( 1 ).setPreferredWidth( 150 );
                    cm.getColumn( 2 ).setPreferredWidth( 450 );
                    scroller = new JScrollPane( table );
                }
                JDialog report = new JDialog( this, "Used styles report", false );
                report.add( scroller );
                report.setSize( 800, numRows > 30 ? 600 : 80 + (numRows * 16) );

                report.setVisible( true );

            }
        }
        catch( InterruptedException e )
        {
            e.printStackTrace();
        }
        catch( ExecutionException e )
        {
            LogAndShowError.logAndShowEx( "Could not find the Steady State CSS Parser "
                    + "\nNo user defined exceptions will be applied.", e );
        }
        finally
        {
            getContentPane().setCursor( cursor );
        }
    }

    public void ePubCheck()
    {
        Cursor cursor = getContentPane().getCursor();
        getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        try
        {
            getContentPane().setCursor( cursor );

            EPubChecker ePubChecker = new EPubChecker( this, _epubModel );
            new ObservingProgressMonitor( this, ePubChecker, "Checking OEB Publication", null, 0, 300 );
            ePubChecker.execute();
        }
        catch (Exception e)
        {
            LogAndShowError.logException( "Could not run epubcheck", e );
        }
        finally
        {
            getContentPane().setCursor( cursor );
        }

    }


    private void exitItem_actionPerformed( ActionEvent event )
    {
        exitItem_actionPerformed_Interaction1( event );
    }

    
    private void exitItem_actionPerformed_Interaction1(
            java.awt.event.ActionEvent event )
    {
        try
        {
            this.exitApplication();
        }
        catch( Exception e )
        {
        }
    }

    
    private void aboutItem_actionPerformed( ActionEvent event )
    {
        JOptionPane.showMessageDialog( this, "ePubEditor ver .01" );
    }

    
    private void editors_actionPerformed( ActionEvent event )
    {
        SetEditorsDialog editor = new SetEditorsDialog( this );
        editor.setVisible( true );
    }

    
    public void xformers_actionPerformed( ActionEvent event )
    {
        SetTransformersDialog xform = new SetTransformersDialog( this );
        xform.setVisible( true );
    }

    public void pathPrefs()
    {
        SetPathsDialog paths = new SetPathsDialog( this );
        paths.setVisible( true );
    }

    @Override
    public void update( Observable actor, Object arg1 )
    {
        if (actor.getClass().getSimpleName().equals( "Watched" ))
        {
            String fileName;
            if (((ManifestPanel.Watched) actor).command.equals( TableEditButtons.edCommand ))
            {
                String id = (String) arg1;
                String href = getOpfData().getManifest().getHrefById( id );
                if (new File( href ).isAbsolute())
                    fileName = href;
                else
                {
                    fileName = _epubModel.getOpfFolder() + File.separator + href;
                }
                String mediaType = getOpfData().getManifest().getMediaTypeById( id );
                Preferences editPrefs = prefs.node( PREFS_MEDIA_TYPES ).node( mediaType );
                String editor = editPrefs.get( PREFS_EDITOR_PATH, null );
                String cl = editPrefs.get( PREFS_EDITOR_CL, "\"%s\"" );
                if (null == editor || !new File( editor ).exists())
                {
                    editPrefs = prefs.node( PREFS_MEDIA_TYPES ).node( PREFS_EDITORS_DEFAULT );
                    editor = editPrefs.get( PREFS_EDITOR_PATH, defaultEditor );
                }
    
                if (new File( editor ).exists()) try
                {
                    if (!"/".equals( File.separator ))
                    {
                        fileName = fileName.replace( "/", File.separator );
                    }
                    java.lang.Runtime.getRuntime().exec(
                            editor + " " + String.format( cl, fileName ) );
                }
                catch( IOException ignore )
                {
                    ignore.printStackTrace();
                }
            }
            else if (((ManifestPanel.Watched) actor).command.equals( TableEditButtons.xformCmd ))
            {
                Cursor cursor = getContentPane().getCursor();
                getContentPane().setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
                try
                {
                    getContentPane().setCursor( cursor );

                    XFormFiles xformer = new XFormFiles( this, _epubModel, (JTable) arg1 );
                    new ObservingProgressMonitor( this, xformer, "Transforming file(s)", 
                                                  null, 0, xformer.getNumRows() );
                    xformer.execute();
                }
                finally
                {
                    getContentPane().setCursor( cursor );
                }
            }
        }
    }
}
