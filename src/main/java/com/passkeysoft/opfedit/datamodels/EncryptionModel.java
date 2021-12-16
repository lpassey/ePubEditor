package com.passkeysoft.opfedit.datamodels;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class EncryptionModel
{
    public EPubModel fileData;
    Document doc;

    public EncryptionModel( EPubModel owner, Document encrypt )
    {
        fileData = owner;
        doc = encrypt;
    }
    
    public boolean isEncrypted( String fileName )
    {
        NodeList data = doc.getElementsByTagName( "EncryptedData" );
        for (int i = 0; i < data.getLength(); i++)
        {
            Node datum = data.item( i );
            Node ref = ((Element) datum).getElementsByTagName( "CipherReference" ).item( 0 );
            if (null != ref)
            {
                String uri = ((Element) ref).getAttribute( "URI" );
                if (fileName.equalsIgnoreCase( uri ))
                    return true;
            }
        }
        return false;
    }
}
