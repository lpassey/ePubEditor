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
*/

package com.passkeysoft.opfedit.datamodels;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class OpfFileFilter extends FileFilter
{
    @Override
    public boolean accept( File file )
    {
        //  This allows us to browse.
        if (file.isDirectory())
            return true;
        return file.getName().toLowerCase().endsWith( ".opf" );
    }

    @Override
    public String getDescription()
    {
        return "Open eBook Project Files (*.opf)";
    }

}
