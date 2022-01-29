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
  
  $Log: EPubModel.java,v $
  Revision 1.14  2014/07/29 21:42:25  lpassey
  Set up for future path change functionality (not yet implemented).

  Revision 1.13  2013/07/03 21:52:00  lpassey
  1. Throw FileNotFoundException in getManifestedDocument when manifested document cannot
   be found in file system.
  2. Improve error handling when .opf file has not yet been created.

*/

package com.passkeysoft.opfedit.datamodels;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jsoup.helper.W3CDom;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.passkeysoft.XHTMLDocument;

import com.passkeysoft.opfedit.staticutil.FileUtil;
import com.passkeysoft.opfedit.ui.LogAndShowError;
import com.passkeysoft.opfedit.ui.EPubEditor;

import org.jsoup.*;

public class EPubModel extends Observable
{
    static final String metaEpubRoot = "epubeditor:rootPath";
    public static DocumentBuilder db = null;

    static
    {
        final DocumentBuilderFactory DOMfactory = DocumentBuilderFactory.newInstance();

        DOMfactory.setValidating( false );
        DOMfactory.setExpandEntityReferences( true );
        DOMfactory.setNamespaceAware( false );
        try
        {
            db = DOMfactory.newDocumentBuilder();
            db.setEntityResolver( new EntityResolver()
            {
                public InputSource resolveEntity( String arg0, String arg1 )
                {
                    return new InputSource( new StringReader( XHTMLDocument.entities ) );
                }
            } );
        }
        catch( ParserConfigurationException ex )
        {
            ex.printStackTrace();
        }
    }

    private OPFFileModel _opfData;

    public OPFFileModel getOpfData()
    {
        return _opfData;
    }

    public File baseFile = null; // The file we opened, .opf, .epub or .zip

    private File _epubRoot = null; // The publication "root", to which all files are relative.
                                   // perhaps in a temporary directory.
    
    public boolean isRooted() 
    {
        return (null != _epubRoot );
    }
    
    private EncryptionModel _encrypt = null;  // contents of the encryption.xml file, if any

    private Document _opfDom;       // A DOM constructed from the .opf file

    public Document getOpfDom()
    {
        return _opfDom;
    }

    private File _opfFile = null;   // The opf file name in the file system,

    /**
     * 
     * @return The opf file name in the file system perhaps in a temporary directory.
     */
    public File getOpfFile()
    {
        return _opfFile;
    }

    
    public File getOpfFolder()
    {
        if (null != _opfFile)
            return _opfFile.getParentFile();
        if (null != _epubRoot)
            return _epubRoot;
        return new File(".");
    }
    
    
    public boolean setOPFFile( File saveFile )
    {
        if (null != saveFile) // Don't bother unless they are different
        {
            if (!saveFile.equals( _opfFile ))
            {
                // validate that .opf file is a descendant of the epub root; if not, don't set and
                // return false;
                String relativity = FileUtil.getPathRelativeToBase( saveFile, _epubRoot );
                if (relativity.startsWith( ".." ))
                {
                    return false;
                }
                
                if (   null != _opfFile 
                    && !saveFile.getParentFile().getAbsolutePath().equals( _opfFile.getParentFile().getAbsolutePath() ))
                {
                    // TODO: If we are changing the path of the .opf file, go through the manifest and
                    // change the relative paths of all of the files to match the new .opf file path
                     System.out.println( relativity );
                }
                _opfFile = saveFile;
            }
            return true;
        }
        return false;
    }

    /**
     * Opens an .opf file either from the file system or from within a .zip or .epub archive. If the
     * file exists within an archive, the entire archive will first be extracted to a temporary
     * directory.
     * 
     * @param oebFile
     *            the abstract file which is either an .opf file or a .zip archive containing an
     *            .opf file
     * @throws DOMException
     *             when the init() function is unable to hand-build an OPF DOM.
     * @throws SAXException
     *             when container.xml or the .opf file cannot be parsed
     * @throws FileNotFoundException
     *             when the input file does not exist, or when an output file or directory cannot be
     *             created.
     * @throws IOException
     *             when any non-specific I/O error is encountered.
     */
    public EPubModel( File oebFile, File epubRoot )
            throws SAXException, IOException
    {
        if (null != oebFile)
            baseFile = _opfFile = oebFile.getCanonicalFile();
        if (null != epubRoot)
        {
            // If the root directory for the epub is specified in this constructor,
            // make sure that it exists.
            _epubRoot = epubRoot.getCanonicalFile();
            _epubRoot.mkdirs();
        }
        String _opfFolderName = null; // folder where the .opf file resides.
        if (null != _opfFile)
        {
            // check to see if it is a zip archive. If so, extract to
            // a temporary directory then use the opf file therein.
            byte[] signature = new byte[4];
            InputStream in;
            try
            {
                in = new DataInputStream( new FileInputStream( _opfFile ) );
            }
            catch( FileNotFoundException ex )
            {
                throw new FileNotFoundException( _opfFile.getCanonicalPath() );
            }
            in.read( signature );
            if (signature[0] == 0x50 && signature[1] == 0x4b && signature[2] == 0x03
                    && signature[3] == 0x04)
            {
                // Yes, its a zip file. Extract to a temp directory
                in.close();
                String tempDir = EPubEditor.prefs.node( EPubEditor.PREFS_PATHS ).get( EPubEditor.PREFS_PATHS_TEMP, null );
                if (null == tempDir || 0 == tempDir.trim( ).length())
                    tempDir = System.getProperty( "java.io.tmpdir" );
                _epubRoot = new File( tempDir + File.separator + _opfFile.getName() + ".d" );
                if (_epubRoot.exists() && !_epubRoot.isDirectory())
                    _epubRoot.delete();
                if (!_epubRoot.exists())
                {
                    if (!_epubRoot.mkdirs())
                        // probably a permissions problem
                        throw new FileNotFoundException( _epubRoot.getName() );
                }
                ZipFile zip = new ZipFile( _opfFile );
                Enumeration<? extends ZipEntry> entries = zip.entries();
                ZipEntry entry;
                while (entries.hasMoreElements())
                {
                    entry = entries.nextElement();
                    String outputFileName =
                            _epubRoot.getAbsolutePath() + File.separator + entry.getName();
                    File outputFile = new File( outputFileName );
                    if (entry.getName().endsWith( "/" ))
                        outputFile.mkdirs();
                    else
                    {
                        outputFile.getParentFile().mkdirs();
                        try
                        {
                            FileOutputStream out = new FileOutputStream( outputFile );
                            in = zip.getInputStream( entry );
                            byte[] b = new byte[4096];
                            for (int num = in.read( b ); num > 0; num = in.read( b ))
                                out.write( b, 0, num );
                            in.close();
                            out.close();
                        }
                        catch( FileNotFoundException ex )
                        {
                            zip.close();
                            throw new FileNotFoundException( outputFile.getCanonicalPath() );
                        }
                    }
                }

                File f = new File( _epubRoot.getAbsolutePath() + File.separator
                                  + "META-INF/container.xml" );
                if (f.exists())
                {
                    // Open container.xml, and find the .opf file reference.
                    Document container = db.parse( f );
                    NodeList roots = container.getElementsByTagName( "rootfile" );
                    _opfFolderName = ((Element) roots.item( 0 )).getAttribute( "full-path" );
                }
                else
                // no container.xml, is there an .opf file /anywhere/ in the
                // archive?
                {
                    entries = zip.entries();

                    while (entries.hasMoreElements())
                    {
                        entry = entries.nextElement();
                        if (entry.getName().toLowerCase().endsWith( ".opf" ))
                        {
                            _opfFolderName = entry.getName();
                            break;
                        }
                    }
                }
                zip.close();
                f = new File( _epubRoot.getAbsolutePath() + File.separator
                        + "META-INF/encryption.xml" );
                if (f.exists())
                {
                    // There is an encryption.xml file, which lists files which have been encrypted.
                    // encrypted files should be stored in a saved ePub file, and not deflated.
                    // Save the encryption list for later use.
                    Document encrypt = db.parse( f );
                    _encrypt = new EncryptionModel( this, encrypt );
                }
                if (null != _opfFolderName)
                {
                    _opfFile =
                            new File( _epubRoot.getAbsolutePath() + File.separator
                                    + _opfFolderName );
                }
            }
            else
            {
                // not a zip archive, assume it's an .opf file.
                in.close();
            }
        }
        if (null != _opfFile)
        {
            _opfDom = db.parse( new FileInputStream( _opfFile ) );
        }
        else
        {
            // build a minimal opf dom
            _opfDom = db.parse( new ByteArrayInputStream( opfSkeleton.getBytes() ) );
        }
        _opfData = new OPFFileModel( this, _opfDom );
        
        if (null != _epubRoot)
        {
            // modify or add a <meta> element containing the absolute value of the root path.
            _opfData.getMetadata().setProperty( metaEpubRoot, _epubRoot.getCanonicalPath() );
        }
        epubRoot = new File( _opfData.getMetadata().getProperty( metaEpubRoot ));
        if (epubRoot.exists())
        {
            if (!setEpubRoot( epubRoot ))
            {
                // cannot set this as the root. Why?
                _opfData.getMetadata().setProperty( metaEpubRoot, "" );
            }
        }
        else _opfData.getMetadata().setProperty( metaEpubRoot, "" );
    }

    public boolean saveOPFFile() throws FileNotFoundException, TransformerException,
            IOException
    {
        if (null == _opfFile)
        {
            LogAndShowError.logAndShowNoEx( "No OPF file name specified. Save failed" );
            return false;
        }
        File parent = _opfFile.getParentFile();
        FileOutputStream os = null;
        try
        {
            File temp = File.createTempFile( "oeb", null, parent );
            os = new FileOutputStream( temp );
            if (_opfData.saveOPF( os ))
            {
                os.close();
                os = null;
                // The file was successfully saved in a temp file. Backup any existing file,
                // then rename the temp file.
                if (_opfFile.exists())
                {
                    File backup = new File( _opfFile.getAbsolutePath() + ".bak" );
                    if (backup.exists())
                        backup.delete();
                    _opfFile.renameTo( backup );
                }
                temp.renameTo( _opfFile );
                baseFile = _opfFile;
                return true;
            }
        }
        catch( FileNotFoundException ex )
        {
            throw new FileNotFoundException( _opfFile.getAbsolutePath() );
        }
        catch( IOException tempFileFailure )
        {
            LogAndShowError.logAndShowEx( "Unable to create temporary file for: "
                    + _opfFile.getAbsolutePath(), tempFileFailure );
        }
        finally
        {
            if (null != os)
                os.close();
        }
        return false;
    }

    private long computeCRCforFile( InputStream fin ) throws IOException
    {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[4096];
        for (int num = fin.read( buffer ); num > 0; num = fin.read( buffer ))
        {
            crc.update( buffer, 0, num );
        }
        fin.close();
        return crc.getValue();
    }

    private long computeCompressedSize( InputStream in ) throws IOException
    {
        long total = 0;
        byte[] buffer = new byte[4096];

        Deflater d = new Deflater( 8 );
        DeflaterInputStream is = new DeflaterInputStream( in, d );

        int read;
        do
        {
            read = is.read( buffer, 0, 4096 );
            if (0 < read)
                total += read;
        }
        while (read >= 0);
        is.close();
        return total - 6;
    }

    /**
     * Checks to be sure a directory path is in the zip file before adding the new file,
     * adding it if it's not there already.
     * 
     * @param filePath
     *            the relative path being saved
     * @param pathList
     *            a list of subdirectory paths already created
     * @throws IOException thrown when the zip file could not be written.
     */
    private void checkSubDir( ZipOutputStream zipFile, String filePath, Set<String> pathList )
            throws IOException
    {
        File relativePath = new File( getOpfFolder(), filePath ).getParentFile();
        int rootLength = getEpubRootPath().length() + 1;
        if (relativePath.getCanonicalPath().length() > rootLength)
        {
            String zipFileFullPath =
                    relativePath.getCanonicalPath().substring( rootLength )
                            .replace( '\\', '/' );
            String[] pathParts = zipFileFullPath.split( "/" );
            // If the path is multi-level, be sure that each exists.
            StringBuilder zipFilePath = new StringBuilder();
            for (String pathPart : pathParts)
            {
                zipFilePath.append( pathPart ).append( "/" );
                if (!pathList.contains( zipFilePath.toString() ))
                {
                    // Add the path to the zip file, and to the path list.
                    ZipEntry entry = new ZipEntry( zipFilePath.toString() );
                    entry.setSize( 0 );
                    entry.setCrc( 0 );
                    entry.setCompressedSize( 0 );
                    entry.setTime( new Date().getTime() );
                    entry.setMethod( ZipEntry.DEFLATED );
                    zipFile.putNextEntry( entry );
                    zipFile.closeEntry();
                    pathList.add( zipFilePath.toString() );
                }
            }
        }
    }

    private void addToZip( String filePath, File infile, int method, ZipOutputStream zip )
            throws IOException
    {
        byte[] buffer = new byte[4096];

        ZipEntry entry;

        entry = new ZipEntry( filePath.replace( '\\', '/' ) );
        entry.setCrc( computeCRCforFile( new FileInputStream( infile ) ) );
        entry.setSize( infile.length() );
        if (0 == method)
            entry.setCompressedSize( infile.length() );
        else
            entry.setCompressedSize( computeCompressedSize( new FileInputStream( infile ) ) );
        entry.setMethod( method );
        zip.putNextEntry( entry );
        FileInputStream in = new FileInputStream( infile );
        for (int num = in.read( buffer ); num > 0; num = in.read( buffer ))
        {
            zip.write( buffer, 0, num );
        }
        in.close();
        zip.closeEntry();
    }

    /**
     * Creates and saves an ePub file based on the current in-memory representation. All manifested
     * files will be saved relative to the path returned by getEpubRootPath().
     * 
     * @param saveTo
     *            The file which will be saved to the file system.
     * @throws TransformerException
     *             when the internal OPF DOM cannot be saved because a transformer cannot be
     *             instantiated.
     * @throws FileNotFoundException
     *             when a file listed in the manifest cannot be found in the file system.
     * @throws IOException
     *             when some other, non-specific file system error occurred.
     */
    public String saveEpub( File saveTo ) throws TransformerException,
            FileNotFoundException, IOException
    {
        if (saveOPFFile())
        {
            // Create a META-INF folder which is a peer to the base file (might already exist)
            File metainf = new File( getEpubRootPath() + File.separator + "META-INF" );
            metainf.mkdir();

            // create the container.xml file in META-INF
            File container =
                    new File( metainf.getCanonicalPath() + File.separator + "container.xml" );
            FileWriter fw = new FileWriter( container );

            fw.write( "<?xml version=\"1.0\"?>\n"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
                    + "   <rootfiles>\n" + "      <rootfile full-path=\"" );
            String opfFilePath = _opfFile.getCanonicalPath();
            opfFilePath = opfFilePath.substring( getEpubRootPath().length() + 1 );

            fw.write( opfFilePath.replace( '\\', '/' ) );
            fw.write( "\" media-type=\"application/oebps-package+xml\"/>\n"
                    + "   </rootfiles>\n" + "</container>\n" );
            fw.close();

            TreeSet<String> pathList = new TreeSet<>(); // build a list of folders.

            // create the Zip file ...
            File tempFile = File.createTempFile( "epub", null, saveTo.getParentFile() );
            ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( tempFile ) );
            ZipEntry entry;

            // add those files which must be stored, not deflated
            zip.setMethod( ZipOutputStream.STORED );
            zip.setLevel( Deflater.NO_COMPRESSION );

            // A constant per the epub spec.
            byte[] mimetype = "application/epub+zip".getBytes( StandardCharsets.US_ASCII );
            entry = new ZipEntry( "mimetype" );
            entry.setTime( 0L );
            entry.setSize( 20 );
            entry.setCompressedSize( 20 );
            entry.setCrc( 0x2CAB616F ); // this value is invariant
            zip.putNextEntry( entry );
            zip.write( mimetype, 0, 20 );
            zip.closeEntry();

            entry = new ZipEntry( "META-INF/" );
            entry.setSize( 0 );
            entry.setCrc( 0 );
            zip.putNextEntry( entry );
            pathList.add( "META-INF" );

            // Add the container file.
            entry = new ZipEntry( "META-INF/" + container.getName() );
            entry.setSize( container.length() );
            entry.setCrc( computeCRCforFile( new FileInputStream( container ) ) );
            entry.setTime( 0L );

            zip.putNextEntry( entry );
            byte[] buffer = new byte[4096];
            FileInputStream fin = new FileInputStream( container );
            for (int num = fin.read( buffer ); num > 0; num = fin.read( buffer ))
            {
                zip.write( buffer, 0, num );
            }
            fin.close();
            zip.closeEntry();

            // Add the .opf file. Some programs compress this; I do not
            checkSubDir( zip, getOpfFile().getName(), pathList );

            entry = new ZipEntry( opfFilePath.replace( '\\', '/' ) );
            entry.setSize( _opfFile.length() );
            entry.setCrc( computeCRCforFile( new FileInputStream( _opfFile ) ) );
            zip.putNextEntry( entry );
            fin = new FileInputStream( _opfFile );
            for (int num = fin.read( buffer ); num > 0; num = fin.read( buffer ))
            {
                zip.write( buffer, 0, num );
            }
            fin.close();
            zip.closeEntry();

            zip.setLevel( Deflater.DEFLATED );
            zip.setMethod( ZipOutputStream.DEFLATED );

            // If there's anything else in the META-INF directory, copy that in.
            File metaInf = new File( getEpubRootPath() + "/META-INF" );
            if (metaInf.isDirectory())
            {
                File[] contained = metaInf.listFiles( new FilenameFilter()
                {
                    @Override
                    public boolean accept( File arg0, String arg1 )
                    {
                        return !arg1.equals( "container.xml" );
                    }
                } );
                if (null != contained)
                {
                    for (File f : contained)
                    {
                        addToZip( "META-INF/" + f.getName(), f, 8, zip );
                    }
                }
            }

            // Now add every file from the manifest. If something exists in
            // a subdirectory, add that subdirectory to the zip file first.
            NodeList items = _opfData.getManifest().getManifestedItems();
            for (int i = 0; i < items.getLength(); i++)
            {
                String zipPath;
                String href = ((Element) items.item( i )).getAttribute( "href" );
                String type = ((Element) items.item( i )).getAttribute( "media-type" );
                File source = new File( href );

                if (source.isAbsolute())
                {
                    // The file is outside the opf root; copy it as though it were a sibling.
                    zipPath = source.getName();
                }
                else
                {
                    zipPath = href;
                    source = new File( getOpfFolder(), href );
                    checkSubDir( zip, href, pathList );
                }
                try
                {
                    if (   type.startsWith( "image" ) 
                        || (null != _encrypt && _encrypt.isEncrypted( href )))
                    {
                        // Assume images have already been appropriately compressed,
                        // and encrypted files cannot be further compressed
                        zip.setMethod( ZipOutputStream.STORED );
                        zip.setLevel( Deflater.NO_COMPRESSION );
                        addToZip(
                                (opfFilePath.substring( 0,
                                        opfFilePath.lastIndexOf( File.separator ) + 1 ) + zipPath),
                                source, 0, zip );
                    }
                    else
                    {
                        zip.setLevel( Deflater.DEFLATED );
                        zip.setMethod( ZipOutputStream.DEFLATED );
                        addToZip(
                                (opfFilePath.substring( 0,
                                        opfFilePath.lastIndexOf( File.separator ) + 1 ) + zipPath),
                                source, 8, zip );
                    }
                }
                catch( ZipException ex )
                {
                    if (!ex.getMessage().contains( "duplicate entry" ))
                        throw ex;
                }
            }
            zip.close();

            // At this point, the epub file was successfully created. Backup any existing file,
            // then rename the temp file.
            if (saveTo.exists())
            {
                File backup = new File( saveTo.getAbsolutePath() + ".bak" );
                if (backup.exists())
                    backup.delete();
                saveTo.renameTo( backup );
            }
            if (!tempFile.renameTo( saveTo ))
            {
                // this will fail if the file is opened by another program
                // - if so, announce the failure.
                String message =
                        "The file was successfully saved as "
                                + tempFile.getAbsolutePath()
                                + "\nbut was not renamed to "
                                + saveTo.getAbsolutePath()
                                + " (the file may be opened by another program).\n"
                                + "Close the other program and try again, or rename the file manually.";
                LogAndShowError.logAndShowNoEx( message );
            }
            baseFile = saveTo;
            return baseFile.getCanonicalPath();
        }
        return null;
    }


    public Document getManifestedDocument( String id ) throws FileNotFoundException
    {
        Document doc = null;
        InputStream fileData = null;
        try
        {
            Element item = _opfData.getManifest().getItemById( id );
            if (null != item)
            {
                String fileName = item.getAttribute( "href" );
                // Look up the file name in the encryption package. If it's encrypted,
                // it can't be parsed, so just return null.
                if (null != _encrypt)
                {
                    if (_encrypt.isEncrypted( fileName ))
                        return null;
                }
                File f = new File( fileName );
                if (!f.isAbsolute())
                {
                    f = new File( getOpfFolder(), fileName );
                }
                String media_type = item.getAttribute("media-type");
                if (MediaTypeModel.isHTML( media_type ))
                {
                    // If it's an html file, parse with JSoup in case the document is not well-formed.
                    W3CDom w3cDom = new W3CDom();
                    doc = w3cDom.fromJsoup( Jsoup.parse( f, "UTF-8" ));
                }
                else
                {
                    // TODO: only parse <?xml> and <html> files
                    fileData = new FileInputStream( f );
                    doc = db.parse( fileData );
                }
            }
        }
        catch( FileNotFoundException ex )
        {
            throw ex;
        }
//        catch( ParserConfigurationException ex )
//        {
//            // ParserConfigurationException error. Log an error, then continue.
//            LogAndShowError.logAndShowEx(
//                    "Parser configuration exception while parsing the file:  \n"
//                            + _opfData.getManifest().getHrefById( id ) + "\n"
//                            + ex.getLocalizedMessage(), ex );
//       }
        catch( SAXParseException sex )
        {
            String message = "Parsing error while parsing "
                + _opfData.getManifest().getHrefById( id ) + ", at line "
                + sex.getLineNumber() + ", column " + sex.getColumnNumber()
                + "\n" + sex.getLocalizedMessage();
            LogAndShowError.logException( message, sex );
        }
        catch( SAXException sex )
        {
            // Unexpected io error. Log an error, then continue.
            LogAndShowError.logAndShowEx(
                    "Unspecified parsing error while reading: \n"
                            + _opfData.getManifest().getHrefById( id ) + "\n"
                            + sex.getLocalizedMessage(), sex );
        }
        catch( IOException ex )
        {
            // Unexpected io error. Log an error, then continue.
            LogAndShowError.logAndShowEx(
                    "Unspecified IO error while reading or parsing the file:  \n"
                            + _opfData.getManifest().getHrefById( id ) + "\n"
                            + ex.getLocalizedMessage(), ex );
        }
        finally
        {
            try
            {
                if (null != fileData)
                    fileData.close();
            }
            catch( IOException ignore ) {}
        }
        return doc;
    }

    
    /**
     * @return the path which will become the root of the epub document. We use a getter, even
     *          inside this file, to guarantee that this value will never be null.
     */
    public String getEpubRootPath()
    {
        try
        {
            if (null != _epubRoot)
                return _epubRoot.getCanonicalPath();
            else if (null != _opfFile)
                return _opfFile.getParentFile().getCanonicalPath();
        }
        catch( IOException ignore )
        {
        }
        return "";
    }

    
    /**
     * Sets a new root for the publication. This method validates that the new root is a parent of
     * the opf file, and changes the paths in the manifest to match the new root, if necessary.
     * 
     * @param root
     *            A directory indicating the new publication root.
     */
    public boolean setEpubRoot( File root )
    {
        if (null != root && root.exists() && root.isDirectory())
        {
            String relativity = "";
            if (null != _opfFile)
                relativity = FileUtil.getPathRelativeToBase( _opfFile, root );
            
            // Check to be sure the root is an ancestor of the .opf file
            // if the opf file is not yet specified, any old root will do.
            if (   (0 < relativity.length() && !relativity.startsWith( ".." ))
                || null == _opfFile)
            {
                try
                {
                    _opfData.getMetadata().setProperty( metaEpubRoot, root.getCanonicalPath() );
                    // The .opf file is a descendant of the root.
                    _epubRoot = root;
                }
                catch( IOException ignore ) {}
                return true;
            }
            // empty string means no commonality
        }
        return false;
    }

    /**
     * Calculate a file path which is relative to the .opf file, or the epub root if
     * the .opf file has not yet been specified. If there is no commonality,
     * returns the canonical file path.
     * 
     * @param opfFilePath The path to the opf file
     * @return the relative path of absFile from the .opf file
     */
    public String getPathRelativeToOpf( File opfFilePath )
    {
        String relPath;
        File base = getOpfFolder();
        try
        {
            File common = FileUtil.getSharedPath( base, opfFilePath );

            if (null != common && common.getCanonicalPath().length() < getEpubRootPath().length())
            {
                // The commonality is less that the ePub document base directory. Return
                // the absolute, canonical path name of the target file.
                return opfFilePath.getCanonicalPath().replace( '\\', '/' );
            }
            relPath = FileUtil.getPathRelativeToBase( opfFilePath, base );
        }
        catch( IOException ex )
        {
            relPath = opfFilePath.getName();
        }
        return relPath;
    }

    /**
     * Checks to see if a file exists as a peer to the OPF file. If the OPF file does not yet
     * exists, checks to see if it exists as a child of the publication root.
     * 
     * @param fileName
     *            The short name of the file whose existence we need to check.
     * @return a boolean value indicating whether the file exists
     */
    public boolean fileExistsRelativeToOPF( String fileName )
    {
        // we don't use getEpubRootPath() here because we want to prefer the
        // OPF location over the epubRoot location.
        File f = new File( getOpfFolder(), fileName );
        return f.exists();
    }

    
    public File copyFileToOpf( File file )
    {
//        if (null != getOpfFile())
        {
            File dest = new File( getOpfFolder(), file.getName() );
            try
            {
                FileUtil.copyFile( file, dest );
                return dest;
            }
            catch( IOException ex )
            {
                // Thrown if the targeted file cannot be saved.
                LogAndShowError.logAndShowEx( "Unable to write to file: " 
                        + dest.getAbsolutePath() + "\nPermission denied", ex );
            }
        }
        return null;
    }

    static final String opfSkeleton
            = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                    + "<package xmlns=\"http://www.idpf.org/2007/opf\""
                    + " version=\"2.0\""
                    + " unique-identifier=\"uid\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n"
                    + "  <metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                    + "    <dc:title>Title of this work</dc:title>\n"
                    + "    <dc:identifier id=\"uid\" opf:scheme=\"UUID\">"
                    + UUID.randomUUID().toString()
                    + "</dc:identifier>\n"
                    + "    <dc:language>en</dc:language>\n"
                    + "    <dc:creator opf:role=\"aut\" opf:file-as=\"Author, Some\">"
                    + "Some Author</dc:creator>\n"
                    + "    <dc-metadata>\n"
                    + "    <dc:contributor opf:role=\"oth\" opf:file-as=\"Else, Some One\">"
                    + "Some One Else</dc:contributor>\n"
                    + "    </dc-metadata>\n  </metadata>\n"
                    + "  <manifest xmlns=\"http://www.idpf.org/2007/opf\">\n"
//                    + "    <item href=\"ebook.css\" id=\"usercss\" media-type=\"text/css\"/>\n"
                    + "    <item href=\"toc.html\" id=\"_toc\" media-type=\"application/xhtml+xml\"/>\n"
                    + "    <item href=\"cover.jpg\" id=\"_cover\" media-type=\"image/jpeg\"/>\n"
                    + "  </manifest>\n"
                    + "  <spine xmlns=\"http://www.idpf.org/2007/opf\">\n"
                    + "    <itemref idref=\"_toc\"/>\n"
                    + "  </spine>\n"
                    + "  <guide xmlns=\"http://www.idpf.org/2007/opf\">\n"
                    + "    <reference href=\"cover.jpg\" title=\"Title of this Work\" type=\"cover\"/>\n"
                    + "    <reference href=\"toc.html\" title=\"Table of Contents\" type=\"toc\"/>\n"
                    + "  </guide>\n" + "</package>\n";


}
