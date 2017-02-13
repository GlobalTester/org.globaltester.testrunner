package org.globaltester.testrunner.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;

/**
 * Represents a test report with fixed values stored from the results of a
 * TestCampaign
 * 
 * @author amay
 * 
 */
public class TestReport {

	/**
	 * Directory all generated report files will be located in
	 */
	private File baseDir = null;
	
	/**
	 * Basename of all created Report files, defaults to the name of the TestCampaign
	 */
	private String fileName = "TestReport";

	/**
	 * Information contained in the report header
	 */
	private String specName = "DummyTestSpecification";
	private String specVersion = "DummyVersion";

	private String executionTime = "unknown";
	
	private String executingUser = "unknown";
	
	private String platformId = "unknown";
	private String sampleId = "unknown";

	private LinkedList<TestReportPart> elements = new LinkedList<TestReportPart>();
	
	private ArrayList<String> logFiles; // adding file names is unchecked
	
	

	/**
	 * Centralizes the extraction of relevant data from the given TestCampaign
	 * @param campaign
	 */
	private TestReport(TestCampaignExecution campaignExec) {
		TestCampaign campaign = campaignExec.getTestCampaign();
		fileName = campaign.getName();
		
		specName = campaign.getSpecName();
		specVersion = campaign.getSpecVersion();
		
		for(IExecution currentIexecution : campaignExec.getChildren()) {
			elements.add(new TestReportPart(currentIexecution));
		}
		
		logFiles = new ArrayList<>();
		logFiles.add(campaignExec.getLogFileName());
		
		for(IExecution currentIexecution: campaignExec.getChildren()) {
			logFiles.add(currentIexecution.getLogFileName());
		}
		
		Date date = new Date(campaignExec.getLastExecutionStartTime());
		executionTime = date.toString();
		
		executingUser = campaignExec.getExecutingUser();
		
		SampleConfig sampleConfig = campaignExec.getSampleConfig();
		platformId = sampleConfig.getPlatformId();
		sampleId = sampleConfig.getSampleId();
		
	}

	/**
	 * Create a copy from the given original report instance. This does not
	 * involve using a factory to create a clone but only creates a TestReport
	 * and copies the given fields of a TestReport. If cloning subclasses is
	 * desired this has to be done with the corresponding subclass.
	 * 
	 * @param origReport
	 */
	public TestReport(TestReport origReport) {
		baseDir = origReport.baseDir;
		fileName = origReport.fileName;
		
		specName = origReport.specName;
		specVersion = origReport.specVersion;
		
		executionTime = origReport.executionTime;
		
		executingUser = origReport.executingUser;
		
		sampleId = origReport.sampleId;
		platformId = origReport.platformId;
		
		Iterator<TestReportPart> elemIter = origReport.elements.iterator();
		while (elemIter.hasNext()) {
			TestReportPart testReportElement = (TestReportPart) elemIter
					.next();
			elements.add(testReportElement);
		}
	}

	/**
	 * Create a new report from the given TestCampaign and prepares to output
	 * to disk.
	 * 
	 * @param testCampaignExecution
	 * @param baseDirName
	 */
	public TestReport(TestCampaignExecution testCampaignExecution, String baseDirName) {
		this(testCampaignExecution);
		this.baseDir = new File(baseDirName);
	}
	
	public ArrayList<String> getLogFiles() {
		return logFiles;
	}

	public String getFileName(String extension) {
		return baseDir.getAbsolutePath()+File.separator+fileName+"."+extension;
	}

	public String getSpecName() {
		return specName;
	}

	public String getSpecVersion() {
		return specVersion;
	}

	public String getExecutionTime() {
		return executionTime;
	}

	public File getReportDir() {
		return baseDir;
	}
	
	public List<TestReportPart> getElements() {
		return elements;
	}
	
	public String getExecutingUser() {
		return executingUser;
	}
	
	public String getPlatformId() {
		return platformId;
	}
	
	public String getSampleId() {
		return sampleId;
	}

}
