package org.globaltester.testrunner.report;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.globaltester.core.resources.GtResourceHelper;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.Result.Status;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.ProcessingInstruction;
import org.osgi.framework.Bundle;


/**
 * TestReport that can be stored as XML file
 * @author amay
 *
 */
public class ReportXmlGenerator {

	/**
	 * Generates a new xml structure representing this test report
	 * 
	 * @return xml structure
	 */
	public static Element createXmlReport(TestReport report) {

		int passedTests = 0;
		int failedTests = 0;
		double sessionTime = 0;

		Element root = new Element("TESTREPORT");

		//TODO fix and uncomment the following code to add features to Report
//		// new tags for spec and release:
		Element reportSpecName = new Element("SPECNAME");
		reportSpecName.setText(report.getSpecName());
		root.addContent(reportSpecName);

		Element reportSpecVersion = new Element("SPECVERSION");
		reportSpecVersion.setText(report.getSpecVersion());
		root.addContent(reportSpecVersion);

//		Element reportID = new Element("TESTSUITEID");
//		reportID.setText(testSuite.testSuiteID);
//		root.addContent(reportID);
//
//		Element reportDescr = new Element("SHORTDESCRIPTION");
//		reportDescr.setText(testSuite.testSuiteShortDescr);
//		root.addContent(reportDescr);
//
//		Element reportRelease = new Element("RELEASE");
//		reportRelease.setText(testSuite.testSuiteVersion);
//		root.addContent(reportRelease);
//
//		Element reportReleaseDate = new Element("RELEASEDATE");
//		reportReleaseDate.setText(testSuite.testSuiteDate);
//		root.addContent(reportReleaseDate);

		Element reportDate = new Element("DATE");
		reportDate.setText(report.getExecutionTime());
		root.addContent(reportDate);

//		Element reportUser = new Element("USER");
//		reportUser.setText(System.getProperty("user.name"));
//		root.addContent(reportUser);
//
//		Element platformID = new Element("PLATFORMID");
//		String platformIDString = Activator.getDefault().getPreferenceStore()
//				.getString(PreferenceConstants.P_CSVPLATFORMID);
//		platformID.setText(platformIDString);
//		root.addContent(platformID);
//
//		Element sampleID = new Element("SAMPLEID");
//		String sampleIDString = Activator.getDefault().getPreferenceStore()
//				.getString(PreferenceConstants.P_CSVSAMPLEID);
//		sampleID.setText(sampleIDString);
//		root.addContent(sampleID);
//
//		Element readerName = new Element("READER");
//		String cardReaderName = Activator.getDefault().getPreferenceStore()
//				.getString(PreferenceConstants.P_CARDREADERNAME);
//		readerName.setText(cardReaderName);
//		root.addContent(readerName);
//
//		Element integrityOfTestSuite = new Element("INTEGRITY");
//		String integrity = FileChecksum.RESULTS[Activator.getDefault()
//				.getPreferenceStore()
//				.getInt(PreferenceConstants.P_TESTSUITEINTEGRITY)];
//		integrityOfTestSuite.setText(integrity);
//		root.addContent(integrityOfTestSuite);
//
//		Element profileNames = new Element("PROFILES");
//		profileNames.setText(testSuite.getProfiles());
//		root.addContent(profileNames);
//
//		Element reportAddInfo = new Element("ADDITIONALINFO");
//		// let all dependent plug-ins integrate in start process
//		Iterator<ITestExtender> iter = Activator.testExtenders.iterator();
//		while (iter.hasNext()) {
//			iter.next().extendReport(reportAddInfo);
//		}
//		if (reportAddInfo.getChildren().size() != 0) {
//			if (DataStore.includeForensicData()) {
//				modulationType = reportAddInfo.getChild("INFOELEMENT")
//						.getChildText("INFOTEXT");
//			}
//			root.addContent(reportAddInfo);
//		}
//
//		Element reportStatus = new Element("STATUS");
//		reportStatus.setText(testSuite.getTestSuiteStatus());
//		root.addContent(reportStatus);
//
//		Element reportExcutedTests = new Element("EXECUTEDTESTS");
//		reportExcutedTests.setText(new Integer(testSuite.getTestCases().size())
//				.toString());
//		root.addContent(reportExcutedTests);
//
//		Element reportFailures = new Element("FAILURES");
//		reportFailures.setText((new Integer(testSuite.getFailures()))
//				.toString());
//		root.addContent(reportFailures);
//
//		Element reportWarnings = new Element("WARNINGS");
//		reportWarnings.setText((new Integer(testSuite.getWarnings()))
//				.toString());
//		root.addContent(reportWarnings);
//
//		// Element reportReferences = new Element("REFERENCES");
//		// reportReferences.setText(testSuite.testSuiteReferences);
//		// root.addContent(reportReferences);
//
//		// <link href="link.to.what.ever">Textlink</link>
//		Element reportLogFile = new Element("LOGFILE");
//		// reportLogFile.setText("file://"+TestLogger.getLogFileName());
//		// reportLogFile.setText("<link href=\"www.heise.de\">textlink</link>");
//
//		File logFile = new File(TestLogger.getLogFileName());
//		reportLogFile.setText(logFile.getName());
//		root.addContent(reportLogFile);
//
//		// Element reportLogFile = new Element("LOGFILE");
//		// reportLogFile.setText("file://"+log.getHtmlFileName());
//		// // String link =
//		// "<link href=\"file://"+log.getHtmlFileName()+"\">Name</link>";
//		// // reportLogFile.setText(link);
//		// // <link href="file://C:\Dokumente und
//		// Einstellungen\hfunke\runtime-EclipseApplication\ePassport Conformity
//		// Testing Layer6/Logging/globaltester_20060404141549.html">Name</link>
//		// root.addContent(reportLogFile);
//
//		// Element reportTestFailure = new Element("TESTFAILURE");

		Iterator<TestReportElement> elemIter = report.getElements().iterator();
		while (elemIter.hasNext()) {
			TestReportElement testReportPart = (TestReportElement) elemIter
					.next();
			Element reportTestCase = new Element("TESTCASE");
			Element reportTestCaseID = new Element("TESTCASEID");
			reportTestCaseID.setText(testReportPart.getID());
			reportTestCase.addContent(reportTestCaseID);
			
			Element reportTestCaseDescr = new Element("TESTCASEDESCR");
			reportTestCaseDescr.setText(testReportPart.getDescription());
			reportTestCase.addContent(reportTestCaseDescr);

			Element reportTestCaseTime = new Element("TESTCASETIME");
			reportTestCaseTime.setText(String.valueOf(Math
					.rint(testReportPart.getTime()) / 1000.));
			reportTestCase.addContent(reportTestCaseTime);
			sessionTime = sessionTime + testReportPart.getTime();

			Element reportTestCaseStatus = new Element("TESTCASESTATUS");
			reportTestCaseStatus.setText(testReportPart.getStatus().toString());
			reportTestCase.addContent(reportTestCaseStatus);
			if (Status.PASSED.equals(testReportPart.getStatus())) {
				passedTests++;
			} else if (Status.FAILURE.equals(testReportPart.getStatus())) {
				failedTests++;
			}

			//root.addContent(reportTestCase);
		}
//		for (int i = 0; i < testSuite.getTestCases().size(); i++) {
//			boolean testCaseAdd = true;
//			TestCase tc = (TestCase) testSuite.getTestCases().get(i);
//			Element reportTestCase = new Element("TESTCASE");
//
//			Element reportTestCaseID = new Element("TESTCASEID");
//			reportTestCaseID.setText(tc.getTestCaseID());
//			reportTestCase.addContent(reportTestCaseID);
//
//			Element reportTestCaseTime = new Element("TESTCASETIME");
//			reportTestCaseTime
//					.setText(String.valueOf(Math.rint(tc.getTime()) / 1000.));
//			reportTestCase.addContent(reportTestCaseTime);
//			sessionTime = sessionTime + tc.getTime();
//
//			// Element reportTestCaseLink = new Element ("TESTCASELINK");
//			//
//			// // get path to test case:
//			// String currentTestCase = (String)testCaseList.get(i);
//			// String pathTestCase = new String();
//			// pathTestCase = "file://"+workingDirectory +"//"+ currentTestCase;
//			// reportTestCaseLink.setText(pathTestCase);
//			// reportTestCase.addContent(reportTestCaseLink);
//
//			if (DataStore.includeForensicData()) {
//				testCaseAdd = checkProtocolExecutionOrder(tc);
//				checkForensicContent(tc);
//			}
//
//			Element reportTestCaseStatus = new Element("TESTCASESTATUS");
//
//			if (!testCaseAdd) {
//				reportTestCaseStatus
//						.setText(TestCase.STATUS_STRINGS[TestCase.STATUS_NOT_APPLICABLE]);
//			} else {
//				reportTestCaseStatus.setText(TestCase.STATUS_STRINGS[tc
//						.getStatus()]);
//			}
//
//			reportTestCase.addContent(reportTestCaseStatus);
//
//			if (tc.getStatus() == TestCase.STATUS_PASSED) {
//				passedTests++;
//			}
//			if (tc.getStatus() == TestCase.STATUS_FAILURE) {
//				failedTests++;
//			}
//
//			Element reportTestCaseComment = new Element("TESTCASECOMMENT");
//			reportTestCaseComment.setText(tc.getComment());
//			reportTestCase.addContent(reportTestCaseComment);
//
//			Element reportTestCaseDescr = new Element("TESTCASEDESCR");
//			reportTestCaseDescr.setText(tc.getTestCaseDescr());
//			reportTestCase.addContent(reportTestCaseDescr);
//
//			LinkedList<Failure> failureList = tc.getFailureList();
//			if (failureList != null) {
//				for (int j = 0; j < failureList.size(); j++) {
//					Element failure = new Element("TESTCASEFAILURE");
//					Failure currentFailure = failureList.get(j);
//
//					Element failureID = new Element("FAILUREID");
//					// <a name="failureID2">@FailureID2</a>
//					failureID.setText((new Integer(currentFailure.getId()))
//							.toString());
//					failure.addContent(failureID);
//
//					Element failureRating = new Element("RATING");
//					failureRating.setText(Failure.RATING_STRINGS[currentFailure
//							.getRating()]);
//					failure.addContent(failureRating);
//
//					Element failureText = new Element("DESCRIPTION");
//					failureText.setText(currentFailure.getFailureText());
//					failure.addContent(failureText);
//
//					Element failureLineScript = new Element("LINESCRIPT");
//					failureLineScript.setText((new Integer(currentFailure
//							.getLineScript())).toString());
//					failure.addContent(failureLineScript);
//
//					Element failureLineLogFile = new Element("LINELOGFILE");
//					failureLineLogFile.setText((new Integer(currentFailure
//							.getLineLogFile())).toString());
//					failure.addContent(failureLineLogFile);
//
//					Element failureExpectedVal = new Element("EXPECTEDVALUE");
//					failureExpectedVal.setText(currentFailure
//							.getExpectedValue());
//					failure.addContent(failureExpectedVal);
//
//					Element failureReceivedVal = new Element("RECEIVEDVALUE");
//					failureReceivedVal.setText(currentFailure
//							.getReceivedValue());
//					failure.addContent(failureReceivedVal);
//
//					if (testCaseAdd) {
//						reportTestCase.addContent(failure);
//						// reportTestFailure.addContent(failure);
//					}
//
//				}
//			}
//
//			addForensicContent(tc, reportTestCase, testCaseAdd);
//			root.addContent(reportTestCase);
//
//		}
//
		Element reportTestsFailed = new Element("FAILEDTESTS");
		reportTestsFailed.setText(Integer.valueOf(failedTests).toString());
		root.addContent(reportTestsFailed);

		Element reportTestsPassed = new Element("PASSEDTESTS");
		reportTestsPassed.setText(Integer.valueOf(passedTests).toString());
		root.addContent(reportTestsPassed);

		Element reportTestSessionTime = new Element("TESTSESSIONTIME");
		reportTestSessionTime
				.setText(String.valueOf(Math.rint(sessionTime) / 1000.));
		root.addContent(reportTestSessionTime);

		Element reportDirectory = new Element("REPORTDIR");
		reportDirectory.setText(report.getReportDir().toURI().toString());
		root.addContent(reportDirectory);

		return root;
	}

	/**
	 * Generate a XML representation of this report and write it to disk
	 * 
	 * @param report
	 */
	public static void writeXmlReport(TestReport report) {
		// prepare parent directories
		File outputFile = new File(report.getFileName("xml"));
		if (!outputFile.getParentFile().mkdirs()) {
			GtErrorLogger.log(Activator.PLUGIN_ID, new RuntimeException(
					"Parent directories for report could not be created"));
		}

		//prepare content
		DocType type = new DocType("TESTREPORT", "testreport.dtd");
		ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet",
				"type=\"text/xsl\" href=\"testreport.xsl\"");
		Element rootElement = ReportXmlGenerator.createXmlReport(report);

		//create XML document
		Document xmlDoc = new Document();
		xmlDoc.setDocType(type);
		xmlDoc.addContent(pi);
		xmlDoc.setRootElement(rootElement);

		//store XML document to file
		XMLHelper.saveDoc(outputFile, xmlDoc);

		// copy stylesheet and required graphics
		try {
			Bundle curBundle = Platform.getBundle(Activator.PLUGIN_ID);
			URL url = FileLocator.find(curBundle, new Path("/"), null);
			IPath styleSheetPath = new Path(FileLocator.toFileURL(url).getPath())
					.append("stylesheets/report/");

			File sourcePath = styleSheetPath.toFile();

			String[] files = { "Header_GT.png", "testreport.dtd",
					"testreport.xsl" };
			for (String currentFile : files) {
				try {
					GtResourceHelper.copyFiles(
							new File(sourcePath, currentFile), new File(
									outputFile.getParent(), currentFile));
				} catch (IOException e) {
					GtErrorLogger.log(Activator.PLUGIN_ID, e);
				}
			}
		} catch (IOException ex) {
			GtErrorLogger.log(Activator.PLUGIN_ID, ex);
		}
	}
	

//	/**
//	 * Creates the neccessary environment for this test case
//	 * 
//	 * @param newReportDirName
//	 *            directory name of test report
//	 */
//	private void createReportEnvironment(String newReportDirName,
//			String newReportFileName) {
//		// set dirname and filename
//		reportDirName = newReportDirName;
//		reportFileName = newReportFileName;
//
//		// create report directory if it does not exist
//		File reportDir = new File(newReportDirName);
//		if (!reportDir.exists()) {
//			reportDir.mkdir();
//		}
//
//		// copy stylesheets to report directory:
//		IPath pluginDir = Activator.getPluginDir();
//		String path = pluginDir + internalPath;
//		File internalTRLogo = new File(path + testReportLogo);
//		File externalTRLogo = new File(newReportDirName + testReportLogo);
//		copy(internalTRLogo, externalTRLogo);
//
//		File internalTRDTD = new File(path + testReportDTD);
//		File externalTRDTD = new File(newReportDirName + testReportDTD);
//		copy(internalTRDTD, externalTRDTD);
//
//		File internalTRXSL = new File(path + testReportXSL);
//		File externalTRXSL = new File(newReportDirName + testReportXSL);
//		copy(internalTRXSL, externalTRXSL);
//
//	}
	
}
