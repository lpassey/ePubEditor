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
  
  $Log: LogAndShowError.java,v $
  Revision 1.2  2013/07/03 22:25:13  lpassey
  Catch and ignore HeadlessExceptions so the routines can be used in non-UI applications.


*/


package com.passkeysoft.opfedit.ui;

import java.awt.HeadlessException;

import org.apache.log4j.Logger;
import javax.swing.*;


/**
 * 
 * @author Lee Passey
 *
 */
public class LogAndShowError
{
    static Logger logger = Logger.getRootLogger(); // where to log errors.
    
    static public int logAndShowEx( String message, Throwable ex )
    {
        logException( message, ex );
        try
        {
            JOptionPane.showMessageDialog( null, message, "Error",
                                           JOptionPane.ERROR_MESSAGE );
        }
        catch( HeadlessException ignore ) {}
        return 0;
    }

    static public void logAndShowNoEx( String message )
    {
        logger.error( message );
        try
        {
            JOptionPane.showMessageDialog( null, message, "Error", JOptionPane.ERROR_MESSAGE );
        }
        catch( HeadlessException ignore ) {}
   }
    
    
    static public int logException( String message, Throwable ex )
    {
        logger.error( message, ex );
        return 0;
    }
}
