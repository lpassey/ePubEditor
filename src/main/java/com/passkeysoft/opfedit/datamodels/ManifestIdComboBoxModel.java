/**
 * 
 */
package com.passkeysoft.opfedit.datamodels;

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
     * @param manifest
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
            String attr = item.getAttribute( "id" );
            return attr;
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
