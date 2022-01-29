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

package com.passkeysoft.opfedit.datamodels;

import org.w3c.dom.Element;

/**
 * @author W. Lee Passey
 *
 */
public interface TwoElementAction
{
    String getTitle();

    String getPatternLabel();
    
    String getTargetLabel();
    
    String getActionLabel();
    
    String getActionReport();

    /**
     * Validates that the two elements are valid.
     * @param pattern The element matching what we are seeking
     * @param target The target element we are validating
     * @return an empty string if it is OK to proceed, or a message to be displayed if it is not.
     */
    String validate( Element pattern, Element target );

    int act( Element pattern, Element target );
}
