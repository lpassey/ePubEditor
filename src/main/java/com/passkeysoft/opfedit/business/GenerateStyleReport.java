/*-*
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
  
  $Log: GenerateStyleReport.java,v $
  Revision 1.5  2014/07/29 22:04:54  lpassey
  Look for .css files relative to the .opf file, not in the epub root.

  Revision 1.4  2013/07/03 22:10:40  lpassey
  1. Catch FileNotFoundException from getManifestedDocument.
  2. Improve error handling when .opf file has not yet been created.
*/


package com.passkeysoft.opfedit.business;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;
// import java.util.TreeSet;

import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.css.CSSStyleSheet;
import org.w3c.dom.css.CSSValue;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.datamodels.SpineModel;
import com.passkeysoft.opfedit.datamodels.StyleSheetCascade;
import com.passkeysoft.opfedit.ui.LogAndShowError;
import com.steadystate.css.parser.CSSOMParser;
import org.w3c.css.sac.InputSource;

public class GenerateStyleReport extends MonitoredWorker<DefaultTableModel, Object>
{
    private EPubModel _ePubData;
    private StyleSheetCascade _userStyles;
    private final String[] columnNames = { "Selector", "File Name", "Styles" };
    
    public GenerateStyleReport( File usercss, EPubModel epubData )
    {
        _ePubData = epubData;
        final DocumentBuilderFactory DOMfactory = DocumentBuilderFactory.newInstance();
        DOMfactory.setValidating( false );

        try
        {
            DocumentBuilder db = DOMfactory.newDocumentBuilder();
            Document _styleHolder = db.newDocument();
            _userStyles = new StyleSheetCascade();
            
            if (null != usercss)
            {
                try
                {
                    // build a style sheet from the user's sheet. If a style is detected
                    // in this cascade, it will /not/ be added to the report.
                    CSSOMParser parser = new CSSOMParser();
                    FileReader reader = new FileReader( usercss );
                    InputSource is = new InputSource( reader );
                    CSSStyleSheet ss =
                            parser.parseStyleSheet( is, _styleHolder,
                                    usercss.getName() );
                    reader.close();
                    if (null != ss && 0 < ss.getCssRules().getLength())
                        _userStyles.add( ss );
                }
                catch( FileNotFoundException fnfe )
                {
                    LogAndShowError.logAndShowEx( "Could not find user specified CSS file "
                            + usercss.getAbsolutePath()
                            + "\nNo user defined exceptions will be applied.", fnfe );
                }
                catch( IOException ex )
                {
                    LogAndShowError.logAndShowEx( "An unknown IO error has occured while parsing\n"
                    		                    + "the user defined CSS file. No user defined exceptions\n"
                    		                    + "will be applied.", ex );
                }
            }
            else
                LogAndShowError.logAndShowNoEx( ""
                        + "No user defined CSS file is specified in Tools>Preferences.\n"
                        + "No user defined exceptions will be applied." );
        }
        catch( ParserConfigurationException ex )
        {
            LogAndShowError.logAndShowEx( "Unable to create a DOM DocumentBuilder", ex );
        }
    }

    
    @Override
    protected void complete()
    { }

    
    @Override
    protected DefaultTableModel doInBackground()
    {
        // traverse every element in every html file. If a "class" attribute is
        // encountered, record it in a list set.
        DefaultTableModel classes = new DefaultTableModel( columnNames, 0 );
        SpineModel.SpineHTMLIterator iter = _ePubData.getOpfData().getSpine().new SpineHTMLIterator();
        while (iter.hasNext())
        {
            String id = iter.next();
            try
            {
                Document doc = _ePubData.getManifestedDocument( id );
                if (null != doc)
                {
                    StyleSheetCascade styles = new StyleSheetCascade();

                    // find any links, and parse StyleSheets.
                    NodeList links = doc.getElementsByTagName( "link" );
                    for (int i = 0; i < links.getLength(); i++)
                    {
                        Element link = (Element) links.item( i );
                        String pathName = link.getAttribute( "rel" );
                        if (pathName.toLowerCase().contains( "stylesheet" ))
                        {
                            pathName = link.getAttribute( "type" );
                            if (!pathName.toLowerCase().contains( "adobe-page-template" ))
                            {
                                try
                                {
                                    pathName = link.getAttribute( "href" );
                                    CSSOMParser parser = new CSSOMParser();
                                    org.w3c.css.sac.InputSource is;
                                    File html = new File( _ePubData.getOpfFolder() 
                                            + File.separator + iter.href );
                                    File css = new File( html.getParent()
                                            + File.separator + pathName);
                                    if (css.exists())
                                    {
                                        FileReader reader =
                                                new FileReader( css );
                                        is = new org.w3c.css.sac.InputSource( reader );
                                        CSSStyleSheet ss =
                                                parser.parseStyleSheet( is, links.item( i ),
                                                        pathName );
                                        reader.close();
                                        if (null != ss && 0 < ss.getCssRules().getLength())
                                            styles.add( ss );
                                    }
                                }
                                catch( IOException ex )
                                {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                    addClassValuesToReport( doc.getDocumentElement(), classes, styles,
                                            _userStyles, iter.href );
                }
            }
            catch( DOMException ex )
            {
                // only raised in "exceptional" circumstances, i.e., when an
                // operation is impossible to perform.
                ex.printStackTrace();
            }
            catch( FileNotFoundException ignore )
            {
                // File not available, just skip it
            }
        }
        return classes;
    }

    /**
     * support function for generateStyleReport()
     * @param node
     * @param classes
     * @param styles
     * @param userStyles
     * @param fileName
     */
    private void addClassValuesToReport( Node node, DefaultTableModel classes,
            StyleSheetCascade styles, StyleSheetCascade userStyles, String fileName )
    {
        if (node.getNodeType() == Node.ELEMENT_NODE)
        {
            String classs = ((Element) node).getAttribute( "class" ).trim();
            if (0 < classs.length())
            {
                // look at each selector in the whitespace limited list of classes
                String[] selectors = classs.split( "\\s" );
                for (String s : selectors)
                {
                    // look up the same style in the master style sheet (may not catch it if the
                    // master style sheet was modified after it was attached to this publication)
                    TreeMap<String, CSSValue> myStyles =
                        userStyles.matchStyles( node.getNodeName(), s );
                    if (null == myStyles || 0 >= myStyles.size())
                    {
                        // not in the master, try to find how it's defined in the publication
                        // specific style sheet
                        TreeMap<String, CSSValue> newStyles =
                            styles.matchStyles( node.getNodeName(), s );
                        String selector = node.getNodeName() + "." + s;
                        String styleString = "";        // An orphan style;
                        if (0 < newStyles.size())
                        {
                            // the class was found in the declared style sheet.
                            // Report how it is defined
                            styleString = newStyles.toString();
                        }
                        // Add this style to the table model if it doesn't already exist
                        int j;
                        for (j = 0; j < classes.getRowCount(); j++)
                        {
                            if (selector.equalsIgnoreCase( (String) classes.getValueAt( j, 0 ) )
                                && fileName.equalsIgnoreCase( (String) classes.getValueAt( j, 1 ) )
                                && styleString.equalsIgnoreCase( (String) classes.getValueAt( j, 2 ) ))
                                break;
                        }
                        if (j == classes.getRowCount())
                            classes.addRow( new String[]{selector, fileName, styleString} );
//                        classes.add(  "     \"" + node.getNodeName() + "." + selectors[i]
//                                    + "\", \"" + fileName + "\", \"" + styleString + "\"" );
                    }
                }
            }
        }
        if (node.hasChildNodes())
        {
            for (Node child = node.getFirstChild(); child != null; child =
                    child.getNextSibling())
            {
                addClassValuesToReport( child, classes, styles, userStyles, fileName );
            }
        }
    }

}
