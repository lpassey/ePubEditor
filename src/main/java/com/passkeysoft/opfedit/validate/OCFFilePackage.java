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
  
  $Log: OCFFilePackage.java,v $
  Revision 1.6  2013/07/09 22:06:04  lpassey
  Fix bug that crept in during last update.

  Revision 1.5  2013/07/03 22:29:26  lpassey
  Improve error handling when .opf file has not yet been created.

*/


package com.passkeysoft.opfedit.validate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

import javax.xml.transform.TransformerException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.adobe.epubcheck.ocf.OCFPackage;
import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.staticutil.FileUtil;

public class OCFFilePackage extends OCFPackage
{

    EPubModel _ePubData;

    public OCFFilePackage( EPubModel epub )
    {
        super();
        _ePubData = epub;
    }

    @Override
    public boolean hasEntry( String name )
    {
        // ePubCheck wants to check for a container file, which we may
        // or may not have yet, so just tell it what it wants to hear.
        if (name.equals( "META-INF/container.xml" ))
            return true;
        // ditto for mimetype
        else if (name.equals( "mimetype" ))
            return true;
        else
        {
            File opfFile = _ePubData.getOpfFile();
            if (null == opfFile && name.equals( "content.opf" ))
                return true;
            if (null != opfFile && name.equals( opfFile.getName() ))
                return true;
        }
        File entry = new File( _ePubData.getEpubRootPath(), name );
        return entry.exists();
    }

    
    @Override
    public InputStream getInputStream( String name ) throws IOException
    {
        File entry = new File( _ePubData.getEpubRootPath(), name );
        File opfFile = _ePubData.getOpfFile();
        if (name.equals( "META-INF/container.xml" ))
        {
            // return a bogus -- but valid -- container file.
            String opfRootPath = _ePubData.getEpubRootPath();
            
            opfRootPath = null == opfFile ? "content.opf" :
                FileUtil.getPathRelativeToBase( opfFile, 
                    new File( _ePubData.getEpubRootPath() ));
            ByteArrayInputStream containerxml = new ByteArrayInputStream( 
                    String.format( container, opfRootPath ).getBytes() );
            return containerxml;
        }
        else if (name.equals( "mimetype" ))
            return new ByteArrayInputStream( "application/epub+zip".getBytes() );
        else if (   (null == opfFile && name.equals( "content.opf" )) 
                 || (null != opfFile && entry.equals( opfFile )))
        {
            // generate a new .opf file on the fly into a temporary output stream.
            final ByteArrayOutputStream os = new ByteArrayOutputStream();
            try
            {
                if (_ePubData.getOpfData().saveOPF( os ))
                {
                     return new ByteArrayInputStream( os.toByteArray() );
                }
            }
            catch( TransformerException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return new ByteArrayInputStream("".getBytes());
        }
//        File entry = new File( _ePubData.getOpfFile().getParent() + File.separator + name );
        if (entry.exists())
            return new FileInputStream( entry );
        return null;
   }

    
    @Override
    public HashSet<String> getFileEntries() throws IOException
    {
        HashSet<String> entryNames = new HashSet<String>();

        NodeList entries = _ePubData.getOpfData().getManifest().getManifestedItems();
        
        for (int i = 0; i < entries.getLength(); i++)
        {
            String href = ((Element) (entries.item( i ))).getAttribute( "href");
            // href should be a path relative to the opf File. Convert it to
            // a path relative to the ePub root path.
            if (0 < href.length())
            {
                href = FileUtil.getPathRelativeToBase(
                         new File( _ePubData.getOpfFolder(), href ),
                         new File( _ePubData.getEpubRootPath() ));
                entryNames.add( href );
            }
        }
        return entryNames;
    }

    @Override
    public HashSet<String> getDirectoryEntries() throws IOException
    {
        HashSet<String> entryNames = new HashSet<String>();

        return entryNames;
    }

    static final String container = "<?xml version=\"1.0\"?> "
        + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\"> "
        + "   <rootfiles> "
        + "      <rootfile full-path=\"%s\" media-type=\"application/oebps-package+xml\"/> "
        + "   </rootfiles> "
        + "</container>"
        ;

    @Override
    public long getTimeEntry( String name )
    {
        // TODO Auto-generated method stub
        return 0;
    }

}
