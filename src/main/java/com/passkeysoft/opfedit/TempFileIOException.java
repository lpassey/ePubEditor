package com.passkeysoft.opfedit;

import java.io.IOException;

public class TempFileIOException extends IOException
{
    private static final long serialVersionUID = 1L;
    
    public TempFileIOException( IOException ex)
    {
        super( ex );
    }
}
