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

import java.util.ArrayList;
import java.util.TreeMap;

import com.steadystate.css.dom.CSSValueImpl;
import org.w3c.dom.css.*;

public class StyleSheetCascade
{
    private ArrayList<CSSStyleSheet> sss;
    
    public StyleSheetCascade()
    {
        sss = new ArrayList<>();
    }
    
    
    public void add( CSSStyleSheet s)
    {
        sss.add( s );

    }
    
    /**
     * Get a list of styles which match <nodeName class="clazz">
     * @param nodeName The name of the node being styled, e.g. <p>, <div>, <span>, etc.
     * @param clazz The value of the "class" attribute of the named node.
     * @return A list of matching styles, alphabetically ordered.
     */
    public TreeMap<String, CSSValue> matchStyles( String nodeName, String clazz )
    {
        TreeMap<String, CSSValue> answer = new TreeMap<>();
        for (CSSStyleSheet s : sss)
        {
            // look for a match in this stylesheet
            CSSRuleList cssRuleList = s.getCssRules();
            for (int i = 0; i < cssRuleList.getLength(); i++)
            {
                CSSRule cssRule = cssRuleList.item( i );
                if (CSSRule.STYLE_RULE == cssRule.getType())
                {
                    String[] selectors = ((CSSStyleRule) cssRule).getSelectorText().split( "," );
//                    if (selectors.length > 1)
//                        System.out.println( "breaking" );
                    for (String selector : selectors)
                    {
                        if (selector.contains( ":" ))
                            selector = selector.substring( 0, selector.indexOf( ":" ) );
                        if (   selector.trim().equals( nodeName + "." + clazz )
                            || selector.trim().equals( "*." + clazz )
                            || selector.trim().equals( "." + clazz ))
                        {
                            CSSStyleDeclaration decl = ((CSSStyleRule) cssRule).getStyle();
                            if (0 == decl.getLength())
                            {
                                answer.put( clazz, new CSSValueImpl());
                            }
                            else for (int j = 0; j < decl.getLength(); j++)
                            {
                                String property = decl.item( j );
                                answer.put( property, decl.getPropertyCSSValue( property ) );
                            }
                        }
                    }
                }
            }
        }
        return answer;
    }
}
