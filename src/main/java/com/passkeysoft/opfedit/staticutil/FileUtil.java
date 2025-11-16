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
  
  $Log: FileUtil.java,v $
  Revision 1.6  2013/07/03 22:14:14  lpassey
  Fix behavior when base File object does not yet exist.

 */

package com.passkeysoft.opfedit.staticutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.w3c.dom.DOMException;

import com.passkeysoft.XHTMLDocument;

import com.passkeysoft.opfedit.TempFileIOException;
import com.passkeysoft.opfedit.ui.LogAndShowError;

public class FileUtil
{

    /**
     * Gets the path of a file relative to the a specific base file. If the base is null,
     * returns the absolute, canonical file name.
     * @param absFile the file to which we need to calculate the relative path.
     * @return the partial path of absFile relative to the root of the publication. If
     *      the root path is empty, returns the absolute, canonical file path; if the file
     *      does not exist as a child of the base path, an empty string is returned.
     * @throws IOException
     */
    public static String getPathRelativeToBase( File absFile, File base )
    {
        try
        {
            if (null == base)
                return (absFile.getCanonicalPath());
            base = base.getCanonicalFile();
            absFile = absFile.getCanonicalFile();
            if (!base.isDirectory())
                base = base.getParentFile();
            File common = getSharedPath( base, absFile );
            if (null == common)
                return "";
            if (common.getCanonicalPath().length() < base.getCanonicalPath().length())
            {
                StringBuilder relative = new StringBuilder();
                // what we have in common is greater than what is expected,
                // add "..\" to make up for it.
                while (!base.equals( common ))
                {
                    relative.append( ".." + File.separator );
                    base = base.getParentFile();
                }
                int len = common.getCanonicalPath().length();
                String file = absFile.getCanonicalPath();

                File.listRoots();
                file = file.substring(  len + File.separator.length() );
                relative.append( file );
                return relative.toString().replace( '\\', '/' );
            }
            else
            {
                // The file is rooted in the same directory as the base file. Return that
                // portion of the path which follows the base folder
                return absFile.getCanonicalPath().substring( (int) (base.getCanonicalPath().length() + 1) ).replace( '\\', '/' );
            }
        }
        catch (IOException ignore) {}
        return "";
    }

    
    public static File getSharedPath( File baseFile, File absFile ) throws IOException
    {
        if (null == baseFile)
            return absFile;
        // find the commonality
        String base = baseFile.getCanonicalPath();
        String file = absFile.getCanonicalPath();
        int cursor;
        for (cursor = 0; cursor < base.length(); cursor++)
        {
            if (cursor > file.length() || base.charAt( cursor ) != file.charAt( cursor ))
            {
                if (!new File( base.substring( 0, cursor ) ).isDirectory())
                {
                    // back up to a path separator.
                    while (0 < cursor && base.charAt( cursor ) != File.separatorChar)
                        cursor--;
                    if (cursor < 2)
                    {
                        // we are at either a root slash or a drive letter; in
                        // either case there is no commonality.
                        return null;
                    }
                    // Now move forward to where the relative path starts.
                    cursor++;
                    break;
                }
            }
        }
        return new File( base.substring( 0, cursor ));
    }

    
    /**
     * Save an XHTMLDocument (See XHTMLDocument.java) to the file system.
     * @param htmlDoc The XHTMLDocument instance to be saved
     * @param xhtmlFile An abstract File object representing the file-system name of the output file.
     * @throws DOMException 
     * @throws IOException 
     */
    static void saveXHTMLDocument( XHTMLDocument htmlDoc, File xhtmlFile ) 
            throws DOMException, FileNotFoundException, IOException
    {
        FileOutputStream os = new FileOutputStream( xhtmlFile );
        htmlDoc.print( os, 2, false );
        try
        {
            os.close();
        }
        catch( IOException ignore ) {}
    }
    
    
    public static boolean saveXHTMLDocumentWithBak( XHTMLDocument htmlDoc, File xhtmlFile ) 
        throws DOMException, FileNotFoundException, TempFileIOException, IOException 
    {
        File temp, parent = xhtmlFile.getParentFile();
        try
        {
            temp = File.createTempFile( "oeb", null, parent );
        }
        catch( IOException ex )
        {
            throw new TempFileIOException( ex );
        }
        saveXHTMLDocument( htmlDoc, temp );

        // The file was successfully saved in a temp file. Backup any existing file,
        // then rename the temp file.
        if (xhtmlFile.exists())
        {
            File backup = new File( xhtmlFile.getAbsolutePath() + ".bak" );
            if (backup.exists())
                backup.delete();
            if (false == xhtmlFile.renameTo( backup ))
            {
                // this will fail if either file is opened by another program
                // or the backup file could not be deleted - if so, announce the failure.
                String message =
                        "The file was successfully saved as "
                                + temp.getAbsolutePath()
                                + "\nbut was not renamed to "
                                + xhtmlFile.getAbsolutePath()
                                + " (the file may be opened by another program).\n"
                                + "Close the other program and try again, or rename the file manually.";
                LogAndShowError.logAndShowNoEx( message );
            }
        }
        return temp.renameTo( xhtmlFile );
    }

    
    // Static utility methods
    /**
     * Copies a file to a new destination on the file system. Rumor has it that this
     * functionality may become part of the Java core API in the near future.
     * 
     * @param src The source file, which must exist.
     * @param dest The destination abstract file, which must be writable.
     * @throws FileNotFoundException if the source file does not exist or is a directory rather
     *      than a regular file, or if the destination file is a directory rather than a regular 
     *      file, or does not exist but cannot be created
     * @throws IOException if a read error occurs on the source file, a write error occurs on the
     *      destination file, or the destination file is read-only or locked by another program
     */
    static public void copyFile( File src, File dest) throws FileNotFoundException, IOException
    {
        FileOutputStream fos = new FileOutputStream( dest );
        FileChannel out = fos.getChannel();
        FileInputStream fis = new FileInputStream( src );
        FileChannel in = fis.getChannel();
        long position = 0;
        do
        {
            position = in.transferTo( position, src.length(), out );
        } while (0 != position);
        in.close();
        fis.close();
        out.close();
        fos.close();
    }

    
    /**
     * 
     * @param f An abstract file object
     * @return That portion of the file name after the path and before the extension.
     */
    public static String getFileName( File f )
    {
        String name = f.getName();
        if (name.contains( "." ))
            return name.substring( 0, name.lastIndexOf( '.' )); 
        return name;
    }

    
    /**
     * 
     * @param f An abstract file object
     * @return That portion of a file name beyond the last period in the name,
     *      or an empty string if the file name does not contain a period.
     */
    public static String getExt( File f )
    {
        String name = f.getName();
        if (name.contains( "." ))
            return name.substring( name.lastIndexOf( '.' ) + 1 );
        return "";
    }

}
