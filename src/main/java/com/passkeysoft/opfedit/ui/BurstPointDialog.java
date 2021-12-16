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
  
  $Log: BurstPointDialog.java,v $
  Revision 1.6  2013/07/03 22:28:32  lpassey
  Improve error handling when .opf file has not yet been created.

  Revision 1.5  2013/06/26 17:51:56  lpassey
  Formatting only changes

  Revision 1.4  2012/08/14 21:56:10  lpassey
  Burst to the opf folder only if the target file is not already in the hierarchy. 
  May need further refinement.
*/

package com.passkeysoft.opfedit.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.datamodels.OEBDocument;
import com.passkeysoft.opfedit.datamodels.TwoElementAction;

class BurstPointDialog extends TwoTagDialog implements TwoElementAction
{
    private static final long serialVersionUID = 1242918772265117696L;
    private ArrayList<File> newItems = null;
    private String _href;

    BurstPointDialog( EPubEditor owner, Document doc, String href )
    {
        super( owner );
        _href = href;
        setTitle( "Split XHTML file on element" );
        init( this, doc );
    }


    @Override
    public String getPatternLabel()
    {
        return "Split HTML at";
    }


    @Override
    public String getTargetLabel()
    {
        return null;
    }


    @Override
    public String getActionLabel()
    {
        return "Split";
    }


    public String getActionReport()
    {
         return " new HTML files created";
    }


    @Override
    public String validate( Element delim, Element target )
    {
        return null;
    }
    
    
    @Override
    public int act( Element delim, Element target )
    {
        OEBDocument xhtml = new OEBDocument( dom, _href );
        try
        {
            // If the selected element is /not/ a container element, first
            // encapsulate the document with <div>s
            if (!delim.getNodeName().equalsIgnoreCase( "div" ))
            {
                Element container = dom.createElement( "div" );
                String classs = delim.getAttribute( "class" );
                if (0 < classs.length())
                    container.setAttribute( "class", classs );
                else
                    container.setAttribute( "class", "split" );
                
//                File tempFile;
//                tempFile = File.createTempFile( "tmp", ".html");
//                tempFile.deleteOnExit();
                xhtml.addDivisions( delim, container, null /* tempFile.getAbsolutePath() */ );
                delim = container;
            }
            String root = owner.getEPubModel().getEpubRootPath();
            
            // _href is probably already a url relative to the opf folder,
            // but if it is absolute, calculate the relativeness. 
            File busted = new File( _href );
            if (busted.isAbsolute())
            {
                _href = owner.getEPubModel().getPathRelativeToOpf( busted );
                busted = new File( _href );
            }
            // if still absolute, there is no common path; burst to the ePub root directory.
            if (!busted.isAbsolute())
            {
                root = new File( root, busted.getPath() ).getParent();
            }
            newItems = xhtml.burst( root, EPubModel.db, delim, busted);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        if (null != newItems)
            return newItems.size();
        return 0;
    }

    
    /**
     * 
     * @return a list of the new Files that were created when the old file was split up,
     * or null if the action was canceled.
     */
    public ArrayList<File> getNewContent()
    {
        return newItems;
    }

}
