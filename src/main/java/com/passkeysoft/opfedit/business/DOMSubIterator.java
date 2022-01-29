package com.passkeysoft.opfedit.business;

import org.w3c.dom.Node;

import com.passkeysoft.DOMIterator;


public class DOMSubIterator extends DOMIterator
{

    private DOMIterator parent;
    
    DOMSubIterator( DOMIterator start )
    {
        super( start.getCurrNode() );
        parent = start;
    }

    @Override
    public Node next()
    {
        _currNode = parent.next();
        return _currNode;
    }

}
