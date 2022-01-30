<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:html="http://www.w3.org/1999/xhtml">

    <xsl:variable name="smallcase" select="'abcdefghijklmnopqrstuvwxyz'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'" />
    <xsl:output method="xml" indent="yes" cdata-section-elements="style" />

    <!-- standard copy template -->
    <xsl:template match="node()">
        <!-- if this node has an italic attribute, start an italic, if it has a bold, start a bold -->
        <xsl:variable name="value" select="normalize-space( translate( @class, $uppercase, $smallcase ))" />
        <xsl:choose>
            <xsl:when test="contains(value, 'italic') and contains(value,'bold')">
                <xsl:copy>
                    <xsl:call-template name="copyAttributes" />
                        <i><b><xsl:apply-templates /></b></i>
                  </xsl:copy>
            </xsl:when>

            <xsl:when test="contains( value, 'italic')">
                <xsl:copy>
                    <xsl:call-template name="copyAttributes" />
                    <i><xsl:apply-templates /></i>
                </xsl:copy>
            </xsl:when>

            <xsl:when test="contains( value, 'bold')">
                <xsl:copy>
                    <xsl:call-template name="copyAttributes" />
                    <b><xsl:apply-templates /></b>
                </xsl:copy>
            </xsl:when>

            <xsl:otherwise>
                <xsl:copy>
                    <xsl:call-template name="copyAttributes" />
                    <xsl:apply-templates />
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- We only need a single <div> for a body. If there's already one,
        remove any others -->
    <!-- <xsl:template match="html:body/html:div[not(@*)]">
        <xsl:apply-templates />
    </xsl:template -->

    <!-- epubcheck insists that many tags be inside of a div. Putting the entire 
        body inside an anonymous div adds nothing to the document, but keeps epubcheck 
        from complaining. -->
    <xsl:template match="body|html:body|BODY|html:BODY">
        <body>
            <xsl:for-each select="@*">  <!-- remove the class attribute from the body, but keep all else -->
                <xsl:if test="name()!='class'">
                    <xsl:copy />
                </xsl:if>
            </xsl:for-each>
             <xsl:choose>
            <!-- if the first, and only, child of the body is a div, no intervention is needed -->
            <xsl:when test="count(./*)=1 and name(./*[1]) ='div'">
                <xsl:apply-templates />
            </xsl:when>
            <xsl:otherwise>
            <div>
                <xsl:apply-templates />
            </div>
            </xsl:otherwise>
            </xsl:choose>
        </body>
    </xsl:template>


    <!-- remove adobe garbage -->
    <xsl:template match="html:head/html:link[@type='application/adobe-page-template+xml']" />

    <xsl:template match="head/link[@type='application/adobe-page-template+xml']" />

    <xsl:template match="html:head/html:link[@type='application/vnd.adobe-page-template+xml']" />

    <xsl:template match="head/link[@type='application/vnd.adobe.page-template+xml']" />
    
    <xsl:template match="html:head/html:link[@type='application/vnd.adobe-page-template+xml']" />
    
    <xsl:template match="head/link[@type='application/vnd.adobe-page-template+xml']" />
    
    <!-- TODO: fix to do a case-insensitive comparison -->
    <xsl:template name="removeClass">
        <xsl:param name="input" />
        <xsl:param name="unwanted" />
        <xsl:choose>
            <xsl:when test="contains($input, $unwanted)">
                <xsl:variable name="firstPart1" select="substring-before($input, $unwanted)" />
                <xsl:variable name="secondPart1" select="substring-after($input, $unwanted)" />
                <xsl:value-of select="concat($firstPart1, $secondPart1)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$input" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="removePrefix">
        <xsl:param name="input" />
        <xsl:param name="prefix" />
        <xsl:choose>
            <xsl:when test="contains($input,$prefix)">
                <xsl:variable name="first" select="substring-before($input, $prefix)" />
                <xsl:variable name="second" select="substring-after($input, $prefix)" />
                <xsl:variable name="last" select="substring-after($second, ' ')" />
                <xsl:value-of select="concat($first, $last)" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$input" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="clearAttributes">
        <xsl:param name="class" />
        <xsl:variable name="classValue" select="normalize-space($class)" />

        <xsl:variable name="noItalic">
            <xsl:call-template name="removeClass">
                <xsl:with-param name="input" select="$classValue" />
                <xsl:with-param name="unwanted">italic</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="nobold">
            <xsl:call-template name="removeClass">
                <xsl:with-param name="input" select="$noItalic" />
                <xsl:with-param name="unwanted">bold</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="notx">
            <xsl:call-template name="removePrefix">
                <xsl:with-param name="input" select="$nobold" />
                <xsl:with-param name="prefix">tx</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>

        <xsl:variable name="nocalibre">
            <xsl:call-template name="removePrefix">
                <xsl:with-param name="input" select="$notx" />
                <xsl:with-param name="prefix">calibre</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>

       <xsl:variable name="nocenter">
            <xsl:call-template name="removePrefix">
                <xsl:with-param name="input" select="$nocalibre" />
                <xsl:with-param name="prefix">center</xsl:with-param>
            </xsl:call-template>
        </xsl:variable>

        <xsl:value-of select="$nocenter" />

    </xsl:template>

    <xsl:template name="copyAttributes">
        <xsl:for-each select="@*">  <!-- remove italic, bold, tx and calibre classes, but keep all else -->
            <xsl:if test="name()='class'">
                <xsl:variable name="classValue">
                    <xsl:call-template name="clearAttributes">
                        <xsl:with-param name="class" select="." />
                    </xsl:call-template>
                </xsl:variable>

                <xsl:if test="string-length($classValue) > 0">
                    <xsl:attribute name="class">
                        <xsl:value-of select="$classValue" />
                    </xsl:attribute>
                </xsl:if>
            </xsl:if>
            <xsl:if test="name()!='class'">
                <xsl:copy />
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="p//span[@class='dropcaps']|html:p//html:span[@class='dropcaps']|p//span[@class='dropcap']|html:p//html:span[@class='dropcap']">
         <xsl:apply-templates />
    </xsl:template>
    
    <xsl:template match="p/span[@class='dropcaps']|html:p/html:span[@class='dropcaps']|p//span[@class='dropcap']|html:p//html:span[@class='dropcap']" priority="2">
         <xsl:attribute name="class">first</xsl:attribute>
         <xsl:apply-templates />
    </xsl:template>


    <xsl:template match="span|html:span">
        <!-- if this span has an italic attribute, start an italic, if it has a 
            bold, start a bold, if strike start a strike. TODO: What about underline? -->
        <xsl:choose>

            <xsl:when test="contains(@class, 'italic') and contains(@class,'bold')">
                <i><b><xsl:call-template name="copyAttributes" />
                        <xsl:apply-templates /></b></i>
            </xsl:when>

            <xsl:when test="contains( @class, 'italic')">
                <i><xsl:call-template name="copyAttributes" />
                    <xsl:apply-templates /></i>
            </xsl:when>

            <xsl:when test="contains( @class, 'bold')">
                <b><xsl:call-template name="copyAttributes" />
                    <xsl:apply-templates /></b>
            </xsl:when>

            <xsl:when test="contains( @class, 'strike')">
                <strike>
                    <xsl:apply-templates /></strike>
            </xsl:when>

            <xsl:when test="contains( @class, 'big')">
                <big>
                    <xsl:apply-templates /></big>
            </xsl:when>

            <xsl:when test="contains( @class, 'small')">
                <small>
                    <xsl:apply-templates /></small>
            </xsl:when>

            <xsl:when test="contains( @class, 'sup')">
                <sup>
                    <xsl:apply-templates /></sup>
            </xsl:when>

            <xsl:when test="contains( @class, 'sub')">
                <sub>
                    <xsl:apply-templates /></sub>
            </xsl:when>

            <xsl:otherwise>
                <xsl:copy>
                    <!-- copyAttributes first calls clearAttributes -->
                    <xsl:call-template name="copyAttributes" />
                    <xsl:apply-templates />
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!-- This replaces empty paragraphs with dingbats. This may be overkill... -->
    <xsl:template match="p|html:p">
        <xsl:if test="count(node())=0">
            <hr class="dingbat" />
        </xsl:if>
        <xsl:if test="count(node())>0">
            <p>
                <xsl:call-template name="copyAttributes" />
                <xsl:apply-templates />
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="div|DIV|html:div|html:DIV">
        <!-- TODO: if this div has no attributes, and is not the child of a body element 
             or a blockquote, save the contents, but skip the div.  -->
        <xsl:choose>
        <xsl:when test="count(@*)=0 
                  and 'body' != normalize-space( translate( name(..), $uppercase, $smallcase ))
                  and 'blockquote' != normalize-space( translate( name(..), $uppercase, $smallcase ))
                  ">
            <br /><xsl:apply-templates />
        </xsl:when>

        <xsl:otherwise>
            <xsl:copy>
                <xsl:call-template name="copyAttributes" />
                <xsl:apply-templates />
            </xsl:copy>
        </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- divs of class 'tx' are supposed to be paragraphs -->
    <xsl:template match="html:div[@class='tx']|div[@class='tx']">
      <p><xsl:call-template name="copyAttributes" /><xsl:apply-templates/></p>
    </xsl:template>
    
    <!-- divs of class 'tx1' are paragraphs which are at the beginning of a section -->
    <xsl:template match="html:div[@class='tx1']|div[@class='tx1']" priority="5">
      <p class="first"><xsl:call-template name="copyAttributes" /><xsl:apply-templates/></p>
    </xsl:template>
    
    <xsl:template match="@text">
        <xsl:value-of select="." />
    </xsl:template>

    <xsl:template match="div[@class='center'] | html:div[@class='center']">
        <div align="center"
             style="text-align:center; margin-right:auto; margin-left:auto">
            <xsl:apply-templates />
        </div>
     </xsl:template>

    <!-- All specific paragraph tests must occur after this one  -->
    <!-- Convert centered paragraphs to div's. Support for centered blocks -->
    <!-- varies widely across user agents, so throw in as many options as we can -->
    <!-- Check for any class name that even /starts/ with the word 'center' -->
    <xsl:template match="p[@class]|html:p[@class]">
         <xsl:variable name="centered">
            <xsl:for-each select="@*">
                <xsl:if test="name()='class' and starts-with( current(), 'center' )">centered</xsl:if>
                <xsl:if test="name()='class' and current()='paraCenter'">centered</xsl:if>
            </xsl:for-each>
        </xsl:variable>

        <xsl:if test="$centered = 'centered'">
            <div align="center"
                 style="text-align:center; margin-right:auto; margin-left:auto">
  
               <!-- if this node has an italic attribute, start an italic, if it has 
                    a bold, start a bold -->
                <xsl:variable name="value" select="normalize-space( translate( @class, $uppercase, $smallcase ))" />
                <xsl:choose>

                    <xsl:when test="contains(value, 'italic') and contains(value,'bold')">
                        <xsl:call-template name="copyAttributes" />
                        <i><b><xsl:apply-templates /></b></i>
                    </xsl:when>

                    <xsl:when test="contains( value, 'italic')">
                        <xsl:call-template name="copyAttributes" />
                        <i><xsl:apply-templates /></i>
                    </xsl:when>

                    <xsl:when test="contains( value, 'bold')">
                        <xsl:call-template name="copyAttributes" />
                        <b><xsl:apply-templates /></b>
                    </xsl:when>

                    <xsl:otherwise>
                        <xsl:call-template name="copyAttributes" />
                        <xsl:apply-templates />
                    </xsl:otherwise>
                </xsl:choose>
            </div>
        </xsl:if>
        
        <xsl:if test="$centered != 'centered'">
            <xsl:copy>
                <!-- if this node has an italic attribute, start an italic, if it has 
                    a bold, start a bold -->
                <xsl:variable name="value" select="normalize-space( translate( @class, $uppercase, $smallcase ))" />
                <xsl:choose>

                    <xsl:when test="contains(value, 'italic') and contains(value,'bold')">
                        <xsl:call-template name="copyAttributes" />
                        <i><b><xsl:apply-templates /></b></i>
                    </xsl:when>

                    <xsl:when test="contains( value, 'italic')">
                        <xsl:call-template name="copyAttributes" />
                        <i><xsl:apply-templates /></i>
                    </xsl:when>

                    <xsl:when test="contains( value, 'bold')">
                        <xsl:call-template name="copyAttributes" />
                        <b><xsl:apply-templates /></b>
                    </xsl:when>

                    <xsl:otherwise>
                        <xsl:call-template name="copyAttributes" />
                        <xsl:apply-templates />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:copy>
        </xsl:if>
    </xsl:template>

    <!-- standardize footnote terminology  -->
    <xsl:template match="div[@class='footnote']|html:div[@class='footnote']">
        <div class="footnotes">
            <xsl:call-template name="copyAttributes" />
            <xsl:apply-templates />
        </div>
     </xsl:template>

    <xsl:template match="p[@class='footnote']|html:p[@class='footnote']" priority="5">
        <div class="note">
        <xsl:for-each select="@*">  <!-- remove italic, bold, tx and calibre classes, but keep all else -->
            <xsl:if test="name()!='class'">
                <xsl:copy />
            </xsl:if>
        </xsl:for-each>
            <xsl:apply-templates />
        </div>
     </xsl:template>
     
     <xsl:template match="span[@class='footnotePara']|span[@class='footnotePara']">
            <xsl:apply-templates />
     </xsl:template>
<!-- hyperion, penguin and harpercollins -->
  <xsl:template match=" *[@class='titlePageTitle'] | *[@class='titlePageTitle1'] 
                      | *[@class='halfTitlePageTitle']" priority="5">
    <h1 class="title">
      <xsl:for-each select="@*">
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates />
    </h1>
  </xsl:template>

  <xsl:template match=" *[@class='titlePageAuthor'] 
                      | *[@class='titlePageAuthor1'] 
                      | *[@class='titlePageAuthor2']" priority="5">
    <h1 class="author">
      <xsl:for-each select="@*">
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates />
    </h1>
  </xsl:template>

  <xsl:template match="html:*[@class='partTitle'] | html:*[@class='partTitle1']">
    <h2 class="title">
      <xsl:for-each select="@*">
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates />
    </h2>
  </xsl:template>

  <xsl:template match="div[@class='copyrightPage'] | html:div[@class='copyrightPage']">
      <div class="copyright">
        <xsl:apply-templates/>
      </div>
  </xsl:template>
                         
  <!-- tracking divisions of the document might be interesting, if anything is done with it... -->
  <xsl:template match="html:div[@class='copyrightPage'] 
                         | html:div[@class='titlePage'] 
                         | html:div[@class='halfTitlePage']
                         | html:div[@class='contents']
                         | html:div[@class='frontMatterPage']
                         | html:div[@class='frontMatter']
                         | html:div[@class='illustrationCreditPage']
                         | html:div[@class='illustrationPage']
                         | html:div[@class='aboutAuthorPage']
                         | html:div[@class='aboutPublisherPage']
                         | html:div[@class='glossary']
                         | html:div[@class='index']
                         | html:div[@class='contents']
                         | html:div[@class='backMatter']
                         | html:div[@class='backMatterPage']">
    <div>
      <!-- preserve attributes other than class -->
      <xsl:for-each select="@*">
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="html:div[@class='acknowledgePage']|div[@class='acknowledgePage']">
    <div class="acknowledgements">
      <xsl:for-each select="@*">
        <!-- preserve attributes other than class -->
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="div[@class='dedicationPage']|html:div[@class='dedicationPage']">
    <div class="dedication">
      <xsl:for-each select="@*">
        <!-- preserve attributes other than class -->
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <xsl:template match="p[@class='dedicationText']|html:p[@class='dedicationText']
                        |p[@class='copyrightText']|html:p[@class='copyrightText']
                        |p[@class='aboutAuthorText']|html:p[@class='aboutAuthorText']
                        |p[@class='aboutPublisherText']|html:p[@class='aboutPublisherText']" priority="5">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="html:p[@class='paraCenter']|p[@class='paraCenter']">
        <div style="text-align:center; margin:auto">
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    
    <xsl:template match="p[@class='indent']|html:p[@class='indent']" priority="5">
        <p>
            <xsl:apply-templates />
        </p>
    </xsl:template>

    <xsl:template match="p[@class='noindent']|html:p[@class='noindent']
                        |p[@class='nonindent']|html:p[@class='nonindent']
                        |p[@class='paraNoIndent']|html:p[@class='paraNoIndent']" priority="5">
        <p>
            <xsl:apply-templates />
        </p>
    </xsl:template>
    
    <!-- paragraphs are paragraphs - no need to be repetitive -->
    <xsl:template match="p[@class='para']|html:p[@class='para']" priority="5">
        <p>
            <xsl:apply-templates />
        </p>
    </xsl:template>

  <!-- Tables of contents from Penguin, Harper Collins and Hyperion -->
  <!-- Table of contents header title -->
  <xsl:template match="html:p[@class='contentsHead']|p[@class='contentsHead']|
                       html:div[@class='contentsHead']|div[@class='contentsHead']" priority="5">
    <h3 class="chapter">
      <xsl:apply-templates/>
    </h3>
  </xsl:template>

  <xsl:template match="html:div[@class='contentsEntry']|div[@class='contentsEntry']
                        |html:div[@class='cct']|div[@class='cct']
                        |html:div[@class='cfmh']|div[@class='cfmh']
                        |html:div[@class='cbmh']|div[@class='cbmh']
                        ">
    <li>
      <xsl:for-each select="@*">
        <xsl:if test="name()!='class'">
          <xsl:copy/>
        </xsl:if>
      </xsl:for-each>
      <xsl:apply-templates/>
    </li>
  </xsl:template>

  <!-- xsl:template match="div[@class='contentsEntry']|html:div[@class='contentsEntry']">
    <li>
      <xsl:apply-templates/>
    </li>
  </xsl:template -->

  <xsl:template match="p[@class='contentsEntryNumber']|html:p[@class='contentsEntryNumber']" priority="5">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="p[@class='contentsEntryName']|html:p[@class='contentsEntryName']" priority="5">
    <xsl:apply-templates/>
  </xsl:template>
  
  <xsl:template match="p[@class='contentsEntryName']/child::node()|html:p[@class='contentsEntryName']/child::node()" priority="2">
    <xsl:copy>
	    <xsl:if test="string-length(../@id) > 0">
	      <xsl:attribute name="id"><xsl:value-of select="../@id" /></xsl:attribute>
	    </xsl:if>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
 
  <!-- observed in Penguin and Random House publications. -->
    <xsl:template match="p[@class='spaceBreak']|html:p[@class='spaceBreak']" priority="5">
      <hr class="dingbat" /><br style="page-break-before:always"/>
    </xsl:template>

    <!-- spans without attibutes are worthless, get rid of them -->
    <xsl:template match="span[not(@*)]|html:span[not(@*)]">
        <xsl:apply-templates />
    </xsl:template>

<!-- created when converting from MobiPocket/Kindle -->
    <xsl:template match="div[@class='mbppagebreak']|html:div[@class='mbppagebreak']">
        <br style="page-break-before:always" />
    </xsl:template>
    
    <xsl:template match="p[@class='pagebreak']|html:p[@class='pagebreak']">
        <br style="page-break-before:always" />
    </xsl:template>
    
    <!-- no classification is necessary on anchors - we know what they are -->
    <xsl:template match="a|html:a">
        <xsl:copy>
            <xsl:for-each select="@*">
                <xsl:choose>
                    <xsl:when test="name()='class'">
                      <xsl:if test="current()='page-break'"><xsl:copy /></xsl:if>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <xsl:apply-templates />
        </xsl:copy>
    </xsl:template>

  <xsl:template match="span[@class='xrefInternal']/a|html:span[@class='xrefInternal']/html:a">
     <xsl:copy>
         <xsl:for-each select="@*">
             <xsl:choose>
                 <xsl:when test="name()='class'">
                 </xsl:when>
                 <xsl:when test="name()='href'">
                   <xsl:attribute name="id"><xsl:value-of select="substring-after(.,'#')"/></xsl:attribute>
                 </xsl:when>
                 <xsl:otherwise>
                     <xsl:copy />
                 </xsl:otherwise>
             </xsl:choose>
         </xsl:for-each>
         <xsl:apply-templates />
     </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
