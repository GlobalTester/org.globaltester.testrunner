package org.globaltester.testrunner.report;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.testframework.ScriptIssue;
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
	
	//this class is not meant to be instantiated
	private ReportXmlGenerator() {
		
	}

	/**
	 * Generates a new xml structure representing this test report
	 * 
	 * @return xml structure
	 */
	public static Element createXmlReport(TestReport report) {
		
		// test counters below represent all possible Status options
		int testsPassed = 0;
		int testsFailed = 0;
		int testsWarning = 0;
		
		// counter for executed tests should be equivalent to the sum of the above counters
		int executedTests = 0;
		
		double sessionTime = 0;

		Element root = new Element("TESTREPORT");


		Element reportDate = new Element("DATE");
		reportDate.setText(report.getExecutionTime());
		root.addContent(reportDate);

		Element reportUser = new Element("USER");
		reportUser.setText(report.getExecutingUser());
		root.addContent(reportUser);

		Element platformID = new Element("PLATFORMID");
		platformID.setText(report.getPlatformId());
		root.addContent(platformID);

		Element sampleID = new Element("SAMPLEID");
		sampleID.setText(report.getSampleId());
		root.addContent(sampleID);

		Element readerName = new Element("READER");
		String cardReaderName = report.getCardReaderName();
		readerName.setText(cardReaderName);
		root.addContent(readerName);

		Element integrityOfTestSuite = new Element("INTEGRITY");
		integrityOfTestSuite.setText(report.getIntegrityOfTestSpec());
		root.addContent(integrityOfTestSuite);
		
		StringJoiner profileStringJoiner = new StringJoiner(", ");
		for(String currentProfile : report.getSelectedProfiles()) {
			profileStringJoiner.add(currentProfile);
		}
		
		Element profileNames = new Element("PROFILES");
		profileNames.setText(profileStringJoiner.toString());
		root.addContent(profileNames);

		Iterator<TestReportPart> elemIter = report.getElements().iterator();
		while (elemIter.hasNext()) {
			TestReportPart testReportPart = elemIter.next();
			Element reportTestCase = new Element("TESTCASE");
			Element reportTestCaseID = new Element("TESTCASEID");
			reportTestCaseID.setText(formattedTestCaseId(testReportPart.getID()));
			reportTestCase.addContent(reportTestCaseID);
			
			Element reportTestCaseTime = new Element("TESTCASETIME");
			reportTestCaseTime.setText(String.valueOf(Math
					.rint(testReportPart.getTime()) / 1000.));
			reportTestCase.addContent(reportTestCaseTime);
			sessionTime = sessionTime + testReportPart.getTime();

			Element reportTestCaseStatus = new Element("TESTCASESTATUS");
			reportTestCaseStatus.setText(testReportPart.getStatus().toString());
			reportTestCase.addContent(reportTestCaseStatus);
			
			executedTests++;
			
			switch(testReportPart.getStatus()) {
				case PASSED:
					testsPassed++;
					break;
				case REQUIREMENT_MISSING:
				case FAILURE:
					testsFailed++;
					break;
				case WARNING:
					testsWarning++;
					break;
				default:
					// currently NOT_APPLICABLE and UNDEFINED are not counted
			}
			
			Element reportTestCaseComment = new Element("TESTCASECOMMENT");
			reportTestCaseComment.setText(testReportPart.getComment());
			reportTestCase.addContent(reportTestCaseComment);

			Element reportTestCaseDescr = new Element("TESTCASEDESCR");
			reportTestCaseDescr.setText(testReportPart.getDescription());
			reportTestCase.addContent(reportTestCaseDescr);
			
			//persist ScriptIssues
			List<ScriptIssue> scriptIssues = testReportPart.getScriptIssues();
			for (ScriptIssue currentFailure : scriptIssues) {
			
					Element failure = new Element("TESTCASEFAILURE");

					Element failureID = new Element("FAILUREID");
					failureID.setText(Integer.toString(currentFailure.getId()));
					failure.addContent(failureID);

					Element failureRating = new Element("RATING");
					failureRating.setText(currentFailure.getStatus().getTextualRepresentation());
					failure.addContent(failureRating);

					Element failureText = new Element("DESCRIPTION");
					failureText.setText(currentFailure.getIssueText());
					failure.addContent(failureText);

					Element failureLineScript = new Element("LINESCRIPT");
					failureLineScript.setText(Integer.toString(currentFailure.getLineScript()));
					failure.addContent(failureLineScript);

					Element failureLineLogFile = new Element("LINELOGFILE");
					failureLineLogFile.setText(Integer.toString(currentFailure.getLineLogFile()));
					failure.addContent(failureLineLogFile);

					Element failureExpectedVal = new Element("EXPECTEDVALUE");
					failureExpectedVal.setText(currentFailure
							.getExpectedValue());
					failure.addContent(failureExpectedVal);

					Element failureReceivedVal = new Element("RECEIVEDVALUE");
					failureReceivedVal.setText(currentFailure
							.getReceivedValue());
					failure.addContent(failureReceivedVal);

					reportTestCase.addContent(failure);
			}
			
			
			root.addContent(reportTestCase);
		}
		
		Element reportExcutedTests = new Element("EXECUTEDTESTS");
		reportExcutedTests.setText(Integer.toString(executedTests));
		root.addContent(reportExcutedTests);

		Element reportTestsPassed = new Element("PASSEDTESTS");
		reportTestsPassed.setText(Integer.toString(testsPassed));
		root.addContent(reportTestsPassed);
		
		Element reportTestsFailed = new Element("FAILEDTESTS");
		reportTestsFailed.setText(Integer.toString(testsFailed));
		root.addContent(reportTestsFailed);
		
		Element reportStatus = new Element("STATUS");
		
		String status = Status.PASSED.getTextualRepresentation();
		if(testsWarning > 0) {
			status = Status.WARNING.getTextualRepresentation();
		}
		if(testsFailed > 0) {
			status = Status.FAILURE.getTextualRepresentation();
		}
		
		reportStatus.setText(status);
		root.addContent(reportStatus);

		Element reportTestSessionTime = new Element("TESTSESSIONTIME");
		reportTestSessionTime
				.setText(String.valueOf(Math.rint(sessionTime) / 1000.));
		root.addContent(reportTestSessionTime);
		
		Element reportLogFile = new Element("LOGFILE");
		
		File f = new File(report.getLogFiles().get(0));
		reportLogFile.setText(f.getName());
		root.addContent(reportLogFile);

		Element reportDirectory = new Element("REPORTDIR");
		reportDirectory.setText(report.getReportDir().toURI().toString());
		root.addContent(reportDirectory);

		return root;
	}
	
	/**
	 * Format the TestCaseId of this report
	 * 
	 * @param tcId
	 * @return formattedString
	 */
	public static String formattedTestCaseId(String tcId) {
		String res = "";
		String prefix = "(ICAO_p3|EAC(1|2)|ESIGN|SE)";
		Pattern p = Pattern.compile("^(?<prefix>" + prefix + ")_(?<nameWithSuffixes>.+)$");
		Matcher m = p.matcher(tcId);
		// check if regex matches
		if (m.find()) {
			res = m.group("prefix") + " " + m.group("nameWithSuffixes");
		} else {
			res = tcId;
		}
		return res;
	}

	/**
	 * Generate a XML representation of this report and write it to disk
	 * 
	 * @param report
	 */
	public static void writeXmlReport(TestReport report) {
		// prepare parent directories
		File outputFile = new File(report.getFileName("xml"));
		if (!outputFile.getParentFile().exists()) {
			GtErrorLogger.log(Activator.PLUGIN_ID, new RuntimeException(
					"Parent directories for report is not present"));
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

		// copy stylesheet and header graphic
		Bundle curBundle = Platform.getBundle(Activator.PLUGIN_ID);
		URL url = FileLocator.find(curBundle, new Path("/"), null);
		try {
			IPath styleSheetSourcePath = new Path(FileLocator.toFileURL(url).getPath())
			.append("stylesheets/report/");
			File stylesheetSource = styleSheetSourcePath.toFile();
			File stylesheetTarget = outputFile.getParentFile();

			String[] files = { "Header_GT.png", "testreport.dtd",
					"testreport.xsl" };
			for (String currentFile : files) {
				copyFile(stylesheetSource, stylesheetTarget, currentFile);
			}
		} catch (IOException ex) {
			GtErrorLogger.log(Activator.PLUGIN_ID, ex);
		}
		// copy required status icons
		try {
			IPath iconSourcePath = new Path(FileLocator.toFileURL(url).getPath())
			.append("icons/");
			File iconSource = iconSourcePath.toFile();
			
			File iconTarget = new File(outputFile.getParent(), "icons");
			iconTarget.mkdir();

			String[] files = { "sts_failed.png",
					"sts_na.png",
					"sts_nye.png",
					"sts_passed.png",
					"sts_warning.png" };
			for (String currentFile : files) {
				copyFile(iconSource, iconTarget, currentFile);
			}
		} catch (IOException ex) {
			GtErrorLogger.log(Activator.PLUGIN_ID, ex);
		}
	}

	/**
	 * Copy a single file and just log any exceptions potentially thrown
	 * 
	 * @param srcDir
	 * @param targetDir
	 * @param fileName
	 */
	private static void copyFile(File srcDir, File targetDir, String fileName) {
		try {
			GtResourceHelper.copyFiles(
					new File(srcDir, fileName), new File(
							targetDir, fileName));
		} catch (IOException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}
	}
	
}
