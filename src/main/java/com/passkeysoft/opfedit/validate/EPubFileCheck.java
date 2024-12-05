package com.passkeysoft.opfedit.validate;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import com.adobe.epubcheck.api.EPUBLocation;
import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.messages.LocalizedMessages;
import com.adobe.epubcheck.messages.Message;
import com.adobe.epubcheck.messages.MessageDictionary;
import com.adobe.epubcheck.messages.MessageId;
import com.adobe.epubcheck.messages.Severity;
import com.adobe.epubcheck.ocf.OCFChecker;
import com.adobe.epubcheck.ocf.OCFPackage;
import com.adobe.epubcheck.opf.ValidationContext;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.FeatureEnum;
import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.ui.LogAndShowError;

public class EPubFileCheck extends Observable implements Report
{
    private Observer _observer;
    private StringBuilder errorReport, warningReport, usageReport;

    private int errors = 0, warnings = 0, exceptions = 0;
    private int info = 0, usage = 0;
    private LocalizedMessages messages = LocalizedMessages.getInstance();

    /*
            @Override
            public void error(String resource, int line, int column, String message)
            {
                String indicator = "not allowed here; expected attribute";
                String centered = "element \"center\" not allowed anywhere;";
                String guided = "hyperlink to non-standard resource";
                String guided2 = "File listed in reference element in guide was not declared in OPF manifest";
                String noAlt = "element \"img\" missing required attribute \"alt\"";
                String noText = "text not allowed here;";
                String clear = "attribute \"clear\"";

                if ( null == resource )
                    resource = "container";
                if (message.contains( indicator ))
                {
                    if (!message.contains( clear ))
                    {
                    ++warnings;
                    // this is a minor error; it means that someone added an attribute that
                    // not all User Agents might recognize. Treat it as such
                    String newMessage = message.substring( 0, message.indexOf( indicator ) );
                    warningReport.append( String.format( "Warning at line %d in file %s: "
                            + "The %s is not allowed by the XHMTL strict model and may not be "
                            + "correctly rendered by all ePub rendering software.\n",
                            line, resource, newMessage));
                    }
                }
                else if (message.contains( centered ))
                    // I'm going to allow center, as many of the alternatives don't
                    // seem to be implemented correctly anywhere yet.
                    return;
                else if (message.contains( guided ) || message.contains( guided2 ))
                {
                    // Allow items in the <guide> section to point to non-spine items.
                    warning( resource, line, column, message );
                    return;
                }
                else if (message.contains( noAlt ))
                {
                    // if images not have alt text, count this as a just a warning.
                    warning( resource, line, column, message );
                    return;
                }
                else if (message.contains( noText ))
                {
                    // block quotes might have text in them; this is ok.
                    warning( resource, line, column, message );
                    return;
                }
                else
                {
                    ++errors;
                    errorReport.append( String.format("Error at line %d in file %s: %s\n",
                                        line, resource, message ));
                }
                _observer.update( this,  errors + warnings );
            }

            @Override
            public void warning(String resource, int line, int column, String message)
            {
                String oldDT = "Irregular DOCTYPE: found";
                if ( null == resource )
                    resource = "container";
                if (message.contains( oldDT ))
                {
                    return;
                }
                ++warnings;
                warningReport.append( String.format("Warning at line %d in file %s: %s\n",
                                      line, resource, message ));
                _observer.update( this, errors + warnings );
            }

             @Override
            public void exception( String resource, Exception e )
            {
                 ++exceptions;
            }
    */

    /**
     * Called when a violation of the standard is found in epub.
     *
     * @param messageId     Id of the message being reported
     * @param epubLocation  location information for the message
     * @param args          Arguments referenced by the format
     *                      string for the message.
     */
    @Override
    public void message( MessageId messageId, EPUBLocation epubLocation, Object... args )
    {
        Message message = messages.getMessage( messageId );
        message( message, epubLocation, args );
    }

    /**
     * Called when a violation of the standard is found in epub.
     *
     * @param message       The message being reported
     * @param epubLocation  location information for the message
     * @param args          Arguments referenced by the format
     *                      string for the message.
     */
    @Override
    public void message( Message message, EPUBLocation epubLocation, Object... args )
    {
        Severity severity = message.getSeverity();
        switch (severity)
        {
        case FATAL:
            exceptions++;
            errorReport.append( message.getMessage( args )).append( "\n" );
            break;
        case ERROR:
            // spine TOC attribute is deprecated.
            if (message.getID().equals( MessageId.RSC_005 )) // Cannot parse file
            {
                if (0 < args.length)
                {
                    String argsstr = args[0].toString();
                    if (argsstr.contains( "not allowed here" ))
                        break;
                    if (argsstr.contains( "not allowed anywhere" ))
                        break;
                    if (argsstr.contains( "text not allowed here" ))
                        break;
                    if (argsstr.contains( "incomplete; expected element" ))
                        break;
                }
            }
            else if ( message.getID().equals( MessageId.HTM_004 ))   // Irregular DOCTYPE
                break;
            errors++;
            _observer.update( this,  errors + warnings );
            errorReport.append( epubLocation.toString()).append(":").append( message.getMessage( args )).append( "\n" );
            break;
        case WARNING:
            if (message.getID().equals( MessageId.HTM_014a ))
            {
                // We don't really care what the file name is...
                if (message.getMessage().contains( "should have the extension '.xhtml'" ))
                    break;
            }
            warnings++;
            warningReport.append( epubLocation.toString()).append(":").append( message.getMessage( args )).append( "\n" );
            _observer.update( this,  errors + warnings );
            break;
        case USAGE:
            if (  message.getID().equals( MessageId.HTM_038 )
                | message.getID().equals( MessageId.ACC_007 )  // epub:type not used
                )
                break;
            usage++;
            usageReport.append( epubLocation.toString()).append(":").append( message.getMessage( args )).append( "\n" );
            break;
        case INFO:      // will this ever be called?
            info++;
            break;
        }
    }

//    @Override
    public void error(String s, int i, int i1, String s1) {

    }

//    @Override
    public void warning(String s, int i, int i1, String s1) {

    }

//    @Override
    public void exception(String s, Exception e) {

    }

    @Override
    public int getErrorCount()
    {
        return errors;
    }

    @Override
    public int getWarningCount()
    {
        return warnings;
    }

//    @Override
    public int getExceptionCount() {
        return 0;
    }

    @Override
    public int getFatalErrorCount()
    {
        return exceptions;
    }

    @Override
    public int getInfoCount()
    {
        return info;
    }

    @Override
    public int getUsageCount()
    {
        return usage;
    }

    /**
     * Called when when a feature is found in epub.
     *
     * @param resource name of the resource in the epub zip container that has this feature
     *                 or null if the feature is on the container level.
     * @param feature  a keyword to know what kind of feature has been found
     * @param value    value found
     */
    @Override
    public void info( String resource, FeatureEnum feature, String value )
    {
        info++;
//        System.out.println( resource + ":" + feature.toString() + "-" + value );
    }

//    @Override
    public void hint(String s, int i, int i1, String s1) {

    }

    /**
     * Called to create a report after the checks have been made
     *
     * WTF is the return value used for?
     */
    @Override
    public int generate()
    {
        return 0;
    }

    /**
     * Called when a report if first created
     */
    @Override
    public void initialize()
    {

    }

    @Override
    public void setEpubFileName( String s )
    {
        System.out.println( s );
    }

    @Override
    public String getEpubFileName()
    {
        return null;
    }

    @Override
    public void setCustomMessageFile( String s )
    {

    }

    @Override
    public String getCustomMessageFile()
    {
        return null;
    }

    @Override
    public int getReportingLevel()
    {
        return 0;
    }

    @Override
    public void setReportingLevel( int i )
    {

    }

    @Override
    public void close()
    {
        System.out.println( "closing" );
    }

    @Override
    public void setOverrideFile( File file )
    {

    }

    @Override
    public MessageDictionary getDictionary()
    {
        return null;
    }

    public EPubFileCheck()
    {
    }

    public String validate( EPubModel ePub, EPUBVersion version, Observer observer )
    {
        _observer = observer;
        warningReport = new StringBuilder();
        errorReport = new StringBuilder();
        usageReport = new StringBuilder();
        addObserver( observer );

        try
        {
            OCFPackage ocf = new OCFFilePackage( ePub );
            ValidationContext.ValidationContextBuilder vcb = new ValidationContext.ValidationContextBuilder(  );
            vcb.ocf( ocf );
            vcb.report( this );
            vcb.resourceProvider( ocf );
            vcb.version( version );
            ValidationContext vc = vcb.build();

            OCFChecker checker = new OCFChecker( vc );
            checker.runChecks();
        }
        catch( Exception e )
        {
            e.printStackTrace();
            LogAndShowError.logAndShowEx( "ePubChecker failed: " + e.getLocalizedMessage(), e );
        }
        return String.format( "epubcheck reported %d errors and %d warnings.\n\n" +
                "Errors:\n\n%s\nWarnings:\n\n%s\nUsage notes:\n\n%s",
            errors, warnings,
            errorReport.toString(), warningReport.toString(), usageReport.toString() );
    }

    public String toString()
    {
        return String.format( "epubcheck reported %d errors and %d warnings.\n\n" +
                "Errors:\n\n%s\nWarnings:\n\n%s\n",
            errors, warnings,
            errorReport.toString(), warningReport.toString() );

    }
}
