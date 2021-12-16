package com.passkeysoft.opfedit.business;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.ccil.cowan.tagsoup.AutoDetector;

public class EncodingReader implements AutoDetector
{
    public String _encoding;

    public EncodingReader( String enc )
    {
        _encoding = enc;
    }
    
    @Override
    public Reader autoDetectingReader( InputStream is )
    {
        BufferedReader reader;
        try
        {
            reader = new BufferedReader( new InputStreamReader( is, _encoding ));
            return reader;
        }
        catch( UnsupportedEncodingException e )
        {
            e.printStackTrace();
        }
        return null;
//        return  new BufferedReader( new InputStreamReader( is ));
    }
}