package com.passkeysoft.opfedit.validate;

import java.util.Observable;
import java.util.Observer;

import com.adobe.epubcheck.api.Report;
import com.adobe.epubcheck.ocf.OCFChecker;
import com.adobe.epubcheck.ocf.OCFPackage;
import com.adobe.epubcheck.util.EPUBVersion;
import com.adobe.epubcheck.util.FeatureEnum;
import com.passkeysoft.opfedit.datamodels.EPubModel;
import com.passkeysoft.opfedit.ui.LogAndShowError;

public class EPubFileCheck extends Observable implements Report 
{
    private Observer _observer;
    StringBuilder errorReport, warningReport;

    int errors = 0, warnings = 0, exceptions = 0;
        
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
            _observer.update( this, new Integer( errors + warnings ));
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
            _observer.update( this, new Integer( errors + warnings ));
        }

         @Override
        public void exception( String resource, Exception e )
        {
             ++exceptions;
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

        @Override
        public int getExceptionCount()
        {
             return exceptions;
        }

        @Override
        public void info( String resource, FeatureEnum feature, String value )
        {
        }


    
    public EPubFileCheck()
    {
//       reporter = new ProxyReport();
    }
    
    public String validate( EPubModel ePub, Observer observer )
    {
        _observer = observer;
        warningReport = new StringBuilder();
        errorReport = new StringBuilder();
        addObserver( observer );
        try
        {
            OCFPackage ocf = new OCFFilePackage( ePub );
            OCFChecker checker = new OCFChecker( ocf, this, EPUBVersion.VERSION_2 );
            checker.runChecks();
        }
        catch (RuntimeException e)
        {
            e.printStackTrace();
            LogAndShowError.logAndShowEx( "ePubChecker failed: " + e.getLocalizedMessage(), e );
        }
        catch (Exception e)
        {
            e.printStackTrace();
            LogAndShowError.logAndShowEx( "ePubChecker failed: " + e.getLocalizedMessage(), e );
        }
        return String.format( "epubcheck reported %d errors and %d warnings.\n\n" +
                              "Errors:\n\n%s\nWarnings:\n\n%s\n",
                              errors,  warnings, 
                             errorReport.toString(), warningReport.toString());
    }
    
    public String toString()
    {
        return String.format( "epubcheck reported %d errors and %d warnings.\n\n" +
                "Errors:\n\n%s\nWarnings:\n\n%s\n",
                errors, warnings, 
               errorReport.toString(), warningReport.toString());

    }
}
