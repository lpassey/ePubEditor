package com.passkeysoft.opfedit.datamodels;

import java.util.ArrayList;
import java.util.TreeMap;
import org.w3c.dom.css.*;

public class StyleSheetCascade
{
    private ArrayList<CSSStyleSheet> sss;
    
    public StyleSheetCascade()
    {
        sss = new ArrayList<CSSStyleSheet>();
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
        TreeMap<String, CSSValue> answer = new TreeMap<String, CSSValue>();
        for (CSSStyleSheet s : sss)
        {
            // look for a match in this stylesheet
            CSSRuleList cssRuleList = s.getCssRules();
            for (int i = 0; i < cssRuleList.getLength(); i++)
            {
                CSSRule cssRule = cssRuleList.item( i );
                if (CSSRule.STYLE_RULE == cssRule.getType())
                {
                    String[] selector = ((CSSStyleRule) cssRule).getSelectorText().split( "," );
                    for (int k = 0; k < selector.length; k++)
                    {
                        if (   selector[k].trim().equals( nodeName + "." + clazz ) 
                            || selector[k].trim().equals( "*." + clazz ))
                        {
                            CSSStyleDeclaration decl = ((CSSStyleRule) cssRule).getStyle();
                            for (int j = 0; j < decl.getLength(); j++)
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
