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
package com.passkeysoft.opfedit.ui.swing.model;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author z075954
 *
 */
public class ManifestIdComboBoxModel extends ManifestComboBoxModel
{
    private static final long serialVersionUID = 1L;

    /**
     * @param manifest the ManifestModel that holds the data for this combo box
     */
    public ManifestIdComboBoxModel( ManifestModel manifest, boolean all )
    {
        super( manifest, all );
    }

    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#getElementAt(int)
     */
    @Override
    public Object getElementAt( int index )
    {
        Element item = (Element) super.getElementAt( index );
        if (null != item)
        {
            return item.getAttribute( "id" );
        }
        return null;
    }


    /* (non-Javadoc)
     * @see javax.swing.ComboBoxModel#setSelectedItem(java.lang.Object)
     */
    @Override
    public void setSelectedItem( Object arg0 )
    {
        if (null != arg0)
        {
            NodeList items = _manifest.getManifestedItems();
            int i;
            for (i = 0; i < items.getLength(); i++)
            {
                Element item = (Element) items.item( i );
                String attr = item.getAttribute( "id" );
                if (arg0.equals( attr ))
                {
                    super.setSelectedItem( item );
                    break;
                }
            }
        }
    }
}
