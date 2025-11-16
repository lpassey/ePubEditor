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

  $Log: ReplaceTagDialog.java,v $
  Revision 1.4  2013/07/03 22:22:25  lpassey
  Add CVS log entries

*/

package com.passkeysoft.opfedit.ui.swing.view;

import com.passkeysoft.opfedit.ui.swing.controller.EPubEditor;
import com.passkeysoft.opfedit.ui.swing.controller.LogAndShowError;
import java.io.IOException;

import org.w3c.dom.Element;

import com.passkeysoft.opfedit.business.EPubUtil;
import com.passkeysoft.opfedit.datamodels.*;

public class ReplaceTagDialog extends TwoTagDialog implements TwoElementAction
{
    private static final long serialVersionUID = 1L;

    private EPubModel data;

    public ReplaceTagDialog( EPubEditor owner, EPubModel opfData )
    {
        super( owner );
        setTitle( "Find and replace HTML tags" );
        init( this, opfData.getOpfDom() );
        data = opfData;
    }


    @Override
    public String getPatternLabel()
    {
        return "Find Element: ";
    }

    @Override
    public String getTargetLabel()
    {
        return "Replace With: ";
    }

    @Override
    public String getActionLabel()
    {
        return "Replace";
    }

    @Override
    public String getActionReport()
    {
        return " replacements made.";
    }

    @Override
    public int act( Element pattern, Element target )
    {
        try
        {
            return EPubUtil.replaceTags( data, pattern, target );
        }
        catch (IOException ex)
        {
            LogAndShowError.logAndShowEx( "Non-specific io error while writing to file\n", ex );
        }
        return 0;
    }


    @Override
    public String validate( Element pattern, Element target )
    {
        if (null == pattern)
            return "A replacement element must be defined";
        return null;
    }
}
