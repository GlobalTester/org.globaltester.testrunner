<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" indent="yes" encoding="utf-8" cdata-section-elements="TESTCASEFAILURE"/>

<xsl:preserve-space elements="test" />

<xsl:variable name="platformID"><xsl:value-of select="TESTREPORT/PLATFORMID" /></xsl:variable>
<xsl:variable name="sampleID"><xsl:value-of select="TESTREPORT/SAMPLEID" /></xsl:variable>

<xsl:variable name="classNameExtension">
  <xsl:call-template name="maskAsClassName">
    <xsl:with-param name="text">_<xsl:value-of select="$platformID" />_<xsl:value-of select="$sampleID" /></xsl:with-param>
  </xsl:call-template>
</xsl:variable>
 
<xsl:template match="TESTREPORT">
 	<testsuite>
  	
  	<xsl:apply-templates select="TESTCASE" />
 	
 	</testsuite>
</xsl:template>

<xsl:template match="TESTCASE">
    <testcase>
        <xsl:attribute name="classname">org.globaltester.testmanager.testframework.TestCase<xsl:value-of select="$classNameExtension" /></xsl:attribute>
        <xsl:attribute name="name"><xsl:value-of select="TESTCASEID" /></xsl:attribute>

    <xsl:apply-templates select="TESTCASEFAILURE" />
    
    </testcase>
</xsl:template>

<xsl:template match="TESTCASEFAILURE">
    <failure type="{RATING}">
        <xsl:value-of select="DESCRIPTION"/>
        Expected value: <xsl:value-of select="EXPECTEDVALUE"/>
        Received value: <xsl:value-of select="RECEIVEDVALUE"/>
        Line in logfile: <xsl:value-of select="LINELOGFILE" />
        
    </failure>
</xsl:template>

<xsl:template name="maskAsClassName">
	<xsl:param name="text" />
	<xsl:call-template name="string-replace-all">
		<xsl:with-param name="text">
			<xsl:call-template name="string-replace-all">
				<xsl:with-param name="text">
					<xsl:call-template name="string-replace-all">
						<xsl:with-param name="text">
							<xsl:call-template name="string-replace-all">
								<xsl:with-param name="text">
									<xsl:call-template name="string-replace-all">
										<xsl:with-param name="text" select="$text" />
										<xsl:with-param name="replace" select="' '" />
										<xsl:with-param name="by" select="'_'" />
									</xsl:call-template>
								</xsl:with-param>
								<xsl:with-param name="replace" select="'.'" />
								<xsl:with-param name="by" select="'_'" />
							</xsl:call-template>
						</xsl:with-param>
						<xsl:with-param name="replace" select="'('" />
						<xsl:with-param name="by" select="''" />
					</xsl:call-template>
				</xsl:with-param>
				<xsl:with-param name="replace" select="')'" />
				<xsl:with-param name="by" select="''" />
			</xsl:call-template>
		</xsl:with-param>
		<xsl:with-param name="replace" select="'-'" />
		<xsl:with-param name="by" select="'_'" />
	</xsl:call-template>
</xsl:template>
    
<xsl:template name="string-replace-all">
    <xsl:param name="text" />
    <xsl:param name="replace" />
    <xsl:param name="by" />
    <xsl:choose>
      <xsl:when test="contains($text, $replace)">
        <xsl:value-of select="substring-before($text,$replace)" />
        <xsl:value-of select="$by" />
        <xsl:call-template name="string-replace-all">
          <xsl:with-param name="text"
          select="substring-after($text,$replace)" />
          <xsl:with-param name="replace" select="$replace" />
          <xsl:with-param name="by" select="$by" />
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$text" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>
