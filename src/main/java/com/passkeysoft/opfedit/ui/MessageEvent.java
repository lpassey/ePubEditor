package com.passkeysoft.opfedit.ui;

import java.util.EventObject;

public class MessageEvent extends EventObject
{
    private static final long serialVersionUID = 2783218320296582363L;
    private String _message;
    
    public MessageEvent( Object source, String message )
    {
        super( source );
        _message = message;
    }

    public String getMessage()
    {
        return _message;
    }

}
