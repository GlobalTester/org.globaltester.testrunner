<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
      xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
      xmlns:fo="http://www.w3.org/1999/XSL/Format">
  <xsl:output method="xml" indent="yes"/>
  <xsl:template match="/">
    <fo:root>
      <fo:layout-master-set>
        <fo:simple-page-master master-name="A4-portrait"
              page-height="29.7cm" page-width="21.0cm" margin="2cm">
          <fo:region-body/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence master-reference="A4-portrait">
        <fo:flow flow-name="xsl-region-body">

          <fo:table text-align="center" border-spacing="0pt" width="100%" table-layout="fixed">
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell padding="0pt" >
                  <fo:block text-align="left">
                   <fo:external-graphic src="url('GT_Logo.gif')"
                      content-height="75%" content-width="75%" />
                  </fo:block>
                </fo:table-cell>
              </fo:table-row>
              <fo:table-row>
                <fo:table-cell padding="0pt" >
                  <fo:block text-align="left">
                    <fo:leader leader-length="100%"
                             leader-pattern="rule"
                             rule-thickness="8pt" color="#FFA017"/>
                    </fo:block>
                  </fo:table-cell>
              </fo:table-row>
            </fo:table-body>
          </fo:table>

          <fo:block font-weight="bold" line-height="2"> GlobalTester Test Report</fo:block>

          <fo:block border="none" padding="0.2cm">
          </fo:block>


          <fo:block line-height="2"> 1) Test summary </fo:block>

          <xsl:apply-templates select="TESTREPORT/SPECNAME"/>
          <xsl:apply-templates select="TESTREPORT/SPECVERSION" />
          <xsl:apply-templates select="TESTREPORT/TESTSUITEID" />
          <xsl:apply-templates select="TESTREPORT/SHORTDESCRIPTION" />
          <xsl:apply-templates select="TESTREPORT/RELEASE" />
          <xsl:apply-templates select="TESTREPORT/RELEASEDATE" />
          <xsl:apply-templates select="TESTREPORT/STATUS" />


          <fo:block border="none" padding="0.2cm">
          </fo:block>

          <fo:block line-height="2"> 2) Test setup </fo:block>
          
          <xsl:apply-templates select="TESTREPORT/PLATFORMID" />
          <xsl:apply-templates select="TESTREPORT/SAMPLEID" />
          <xsl:apply-templates select="TESTREPORT/READER" />
          <xsl:apply-templates select="TESTREPORT/INTEGRITY" />
          <xsl:apply-templates select="TESTREPORT/PROFILES" />
          <xsl:apply-templates select="TESTREPORT/ADDITIONALINFO" />
          
          
          <fo:block border="none" padding="0.2cm">
          </fo:block>

          <fo:block line-height="2"> 3) Test statistics </fo:block>

          <xsl:apply-templates select="TESTREPORT/DATE" />
          <xsl:apply-templates select="TESTREPORT/USER" />
          <xsl:apply-templates select="TESTREPORT/TESTSESSIONTIME" />
          <xsl:apply-templates select="TESTREPORT/EXECUTEDTESTS" />
          <xsl:apply-templates select="TESTREPORT/PASSEDTESTS" />
          <xsl:apply-templates select="TESTREPORT/FAILEDTESTS" />
          <xsl:apply-templates select="TESTREPORT/FAILURES" />
          <xsl:apply-templates select="TESTREPORT/WARNINGS" />
          <xsl:apply-templates select="TESTREPORT/LOGFILE" />


          <fo:block break-before="page">
          </fo:block>


          <fo:block line-height="2"> 4) Overview of executed test cases </fo:block>

          <fo:table border="0.8pt solid black" text-align="left" font-size="8pt" width="100%" table-layout="fixed">
            <fo:table-column column-width="20%"/>
            <fo:table-column column-width="55%"/>
            <fo:table-column column-width="10%"/>
            <fo:table-column column-width="15%"/>
            <fo:table-body>
              <fo:table-row>
                <fo:table-cell padding="1pt" border="0.8pt solid black" font-weight="bold" text-align="center" background-color="lightgrey">
                  <fo:block> Test Case ID </fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1pt" border="0.8pt solid black" font-weight="bold" text-align="center" background-color="lightgrey">
                  <fo:block> Description </fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1pt" border="0.8pt solid black" font-weight="bold" text-align="center" background-color="lightgrey">
                  <fo:block> Time (s) </fo:block>
                </fo:table-cell>
                <fo:table-cell padding="1pt" border="0.8pt solid black" font-weight="bold" text-align="center" background-color="lightgrey">
                  <fo:block> Result </fo:block>
                </fo:table-cell>
              </fo:table-row>

              <xsl:for-each select="TESTREPORT/TESTCASE">
                <fo:table-row>
                  <fo:table-cell padding="1pt" border="0.8pt solid black">
                    <fo:block> <xsl:value-of select="TESTCASEID" /> </fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="1pt" border="0.8pt solid black">
                    <fo:block> <xsl:value-of select="TESTCASEDESCR" /> </fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="1pt" border="0.8pt solid black">
                    <fo:block> <xsl:value-of select="TESTCASETIME" /> </fo:block>
                  </fo:table-cell>
                  <fo:table-cell padding="1pt" border="0.8pt solid black">
                    <xsl:if test="TESTCASESTATUS='PASSED'">
                      <fo:block color="green"> <xsl:value-of select="TESTCASESTATUS" /> </fo:block>
                    </xsl:if>
                    <xsl:if test="TESTCASESTATUS='FAILURE'">
                      <fo:block color="red"> <xsl:value-of select="TESTCASESTATUS" /> </fo:block>
                    </xsl:if>
                    <xsl:if test="TESTCASESTATUS='WARNING'">
                      <fo:block color="red"> <xsl:value-of select="TESTCASESTATUS" /> </fo:block>
                    </xsl:if>
                    <xsl:if test="TESTCASESTATUS='SKIPPED'">
                      <fo:block color="red"> <xsl:value-of select="TESTCASESTATUS" /> </fo:block>
                    </xsl:if>
                    <xsl:if test="TESTCASESTATUS='NOT APPLICABLE'">
                      <fo:block color="blue"> <xsl:value-of select="TESTCASESTATUS" /> </fo:block>
                    </xsl:if>

                  </fo:table-cell>
                </fo:table-row>
              </xsl:for-each>
            </fo:table-body>
          </fo:table>


          <fo:block border="none" padding="0.2cm">
          </fo:block>


          <fo:block line-height="2"> 5) Test case results </fo:block>

          <xsl:for-each select="TESTREPORT/TESTCASE">
            <fo:block border="none" padding="0.2cm">
            </fo:block>

            <fo:block font-size="8pt" line-height="1.5">
              <fo:inline font-weight="bold">Test case ID: </fo:inline>
              <xsl:value-of select="TESTCASEID"/>
            </fo:block>
            <fo:block font-size="8pt" line-height="1.5">
              <fo:inline font-weight="bold">Description: </fo:inline>
              <xsl:value-of select="TESTCASEDESCR"/>
            </fo:block>

            <xsl:variable name="numberOfFailures" select="count(./child::TESTCASEFAILURE)"/>
            
            <xsl:if test="$numberOfFailures = 0">
              <fo:block font-size="8pt" line-height="1.5">
                <fo:inline font-weight="bold">Status: </fo:inline>
                Test case passed. For test results please refer the log file generated by GlobalTester in the specified logging folder.
              </fo:block>
            </xsl:if>
   	    <xsl:if test="$numberOfFailures > 0">
              <fo:block font-size="8pt" line-height="1.5">
                <fo:inline font-weight="bold">Status: </fo:inline>
                Test case failed!
              </fo:block>
            </xsl:if>

            <xsl:for-each select="TESTCASEFAILURE">

              <fo:table border="none" text-align="left" font-size="8pt" width="100%" table-layout="fixed">
                <fo:table-column column-width="15%"/>
                <fo:table-column column-width="85%"/>
                <fo:table-body>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> FailureID: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="FAILUREID"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> Description: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="DESCRIPTION"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt">
                      <fo:block> Expected value: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="EXPECTEDVALUE"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> Received value: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="RECEIVEDVALUE"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> Rating: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="RATING"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> Line in log file: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> <xsl:value-of select="LINELOGFILE"/> </fo:block>
                    </fo:table-cell>
                  </fo:table-row>
                  <fo:table-row border="none" text-align="left">
                    <fo:table-cell padding="2pt" >
                      <fo:block> Comment: </fo:block>
                    </fo:table-cell>
                    <fo:table-cell>
                      <fo:block> For full details please refer to the log file of GlobalTester. </fo:block>
                    </fo:table-cell>
                  </fo:table-row>

                </fo:table-body>
              </fo:table>
              
              <fo:block border="none" padding="0.2cm">
              </fo:block>

            </xsl:for-each>
          </xsl:for-each>


          <fo:block border="none" padding="0.3cm">
          </fo:block>


          <fo:block font-size="8pt" font-style="italic">
          Test Report generated by GlobalTester. For further information look at www.globaltester.org. 
          GlobalTester is a project of HJP Consulting GmbH, Borchen, Germany (www.hjp-consulting.com).
          </fo:block>

        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>



  <xsl:template match="SPECNAME">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Specification: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="SPECVERSION">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Specification Version: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="RELEASE">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Release: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="RELEASEDATE">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Release Date: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="TESTSUITEID">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Test Suite ID: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="SHORTDESCRIPTION">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Short Description: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="STATUS">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Result: </fo:inline>
     <xsl:if test=".='PASSED'">
       <fo:inline color="green"> <xsl:apply-templates /> </fo:inline>
     </xsl:if>
     <xsl:if test=".='FAILURE'">
       <fo:inline color="red"> <xsl:apply-templates /> </fo:inline>
     </xsl:if>
     <xsl:if test=".='WARNING'">
       <fo:inline color="yellow"> <xsl:apply-templates /> </fo:inline>
     </xsl:if>
     <xsl:if test=".='NOT APPLICABLE'">
       <fo:inline color="blue"> <xsl:apply-templates /> </fo:inline>
     </xsl:if>

   </fo:block>
  </xsl:template>


  <xsl:template match="DATE">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Test executed at: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="USER">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Test executed by: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>

  <xsl:template match="PLATFORMID">
    <fo:block font-size="10pt" line-height="2">
      <fo:inline font-weight="bold">Platform ID: </fo:inline>
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>
  
  <xsl:template match="SAMPLEID">
    <fo:block font-size="10pt" line-height="2">
      <fo:inline font-weight="bold">Sample ID: </fo:inline>
      <xsl:apply-templates />
    </fo:block>
  </xsl:template>
  
  <xsl:template match="READER">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Card Reader: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>
  
  <xsl:template match="INTEGRITY">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Integrity of test suite: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>

  <xsl:template match="PROFILES">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Profiles tested: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>

  
  <xsl:template match="ADDITIONALINFO">
    <fo:block font-size="10pt" line-height="2">
      <xsl:for-each select="INFOELEMENT">
        <fo:inline font-weight="bold" padding-right="2mm"> <xsl:value-of select="INFOSOURCE" /> </fo:inline>
            <fo:inline> <xsl:value-of select="INFOTEXT" /> </fo:inline>
      </xsl:for-each>
    </fo:block>
  </xsl:template>


  <xsl:template match="TESTSESSIONTIME">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Execution time: </fo:inline>
     <xsl:apply-templates /> sec.
   </fo:block>
  </xsl:template>


  <xsl:template match="EXECUTEDTESTS">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Number of tests executed: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="PASSEDTESTS">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Number of tests passed: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="FAILEDTESTS">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Number of tests failed: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="FAILURES">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Number of failures: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="WARNINGS">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Number of warnings: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


  <xsl:template match="LOGFILE">
   <fo:block font-size="10pt" line-height="2">
     <fo:inline font-weight="bold">Corresponding logging file: </fo:inline>
     <xsl:apply-templates />
   </fo:block>
  </xsl:template>


</xsl:stylesheet>