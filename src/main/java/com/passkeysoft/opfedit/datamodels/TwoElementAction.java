/**
 * 
 */
package com.passkeysoft.opfedit.datamodels;

import org.w3c.dom.Element;

/**
 * @author lee
 *
 */
public interface TwoElementAction
{
    public String getTitle();

    public String getPatternLabel();
    
    public String getTargetLabel();
    
    public String getActionLabel();
    
    public String getActionReport();

    /**
     * Validates that the two elements are valid.
     * @param pattern
     * @param target
     * @return an empty string if it is OK to proceed, or a message to be displayed if it is not.
     */
    public String validate( Element pattern, Element target );
    
    public int act( Element pattern, Element target );
}
