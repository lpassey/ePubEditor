package com.passkeysoft.opfedit.datamodels;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ManifestHrefComboBoxModel extends ManifestComboBoxModel
{
    private static final long serialVersionUID = 1L;


    public ManifestHrefComboBoxModel( ManifestModel manifest, boolean all )
    {
        super( manifest, all );
    }

    
    /* (non-Javadoc)
     * @see javax.swing.ListModel#getElementAt(int)
     */
    @Override
    public Object getElementAt( int index )
    {
        Element item = (Element) super.getElementAt( index );
        if (null != item)
        {
            String attr = item.getAttribute( "href" );
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
        NodeList items = _manifest.getManifestedItems();
        int i;
        for (i = 0; i < items.getLength(); i++)
        {
            Element item = (Element) items.item( i );
            String attr = item.getAttribute( "href" );
            if (arg0.equals( attr ))
            {
                super.setSelectedItem( item );
                break;
            }
        }
    }
}
