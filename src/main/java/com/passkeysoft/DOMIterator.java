/**
 * 
 */
package com.passkeysoft;

import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author W. Lee Passey
 * An iterator class to move through a DOM tree, in order.
 */

public class DOMIterator implements Iterator<Node>
{
    protected Node _start;
    protected Node _currNode;
    
    public Node getCurrNode()
    {
        return _currNode;
    }

    public DOMIterator( Node start )
    {
        _start = start;
        _currNode = start;
    }
    
    
    @Override
    public boolean hasNext()
    {
        if (!_currNode.hasChildNodes())
        {
            if (null == _currNode.getNextSibling())
            {
                Node test = _currNode.getParentNode();
                while (null != test && test != _start)
                {
                    if (null != test.getNextSibling())
                        return true;
                    test = test.getParentNode();
                }
                return false;
            }
        }
        return true;
    }

    @Override
    /**
     * 
     */
    public Node next()
    {
        // Move to next node
        if (_currNode.hasChildNodes())
            _currNode = _currNode.getFirstChild();
        else if (null != _currNode.getNextSibling())
            _currNode = _currNode.getNextSibling();
        else 
        {
            while (   _start.getParentNode() != _currNode
                   && null == _currNode.getParentNode().getNextSibling())
                _currNode = _currNode.getParentNode();
            if ( _start.getParentNode() != _currNode)
                _currNode = _currNode.getParentNode().getNextSibling();
        }
        return _currNode;
    }

    
    /**
     * This method repeatedly calls the next() method until the tree is traversed
     * or an Element node is encountered.
     * @return The next <i>Element</i> in the tree of nodes
     */
    public Element nextElement()
    {
        Node next = next();
        while (Node.ELEMENT_NODE != next.getNodeType())
        {
            if (hasNext())
                next = next();
            else
                return null;
        }
        return (Element) next;
    }
    
    
    // Additional methods to allow traversing the tree backward.
    public boolean hasPrevious()
    {
        if (_currNode == _start)
            return false;
        return true;
    }

    public Node previous()
    {
        // If I have a previous sibling, go to his /last/leaf node.
        if (null != _currNode.getPreviousSibling())
        {
            _currNode = _currNode.getPreviousSibling();
            while( null != _currNode.getLastChild())
                _currNode = _currNode.getLastChild();
        }
        else
            _currNode = _currNode.getParentNode();
        return _currNode;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

}
