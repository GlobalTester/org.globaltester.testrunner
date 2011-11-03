package org.globaltester.testrunner.report;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaign;

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
	
	private LinkedList<TestReportPart> elements = new LinkedList<TestReportPart>();

	/**
	 * Centralizes the extraction of relevant data from the given TestCampaign
	 * @param campaign
	 */
	private TestReport(TestCampaign campaign) {

		fileName = campaign.getName();
		
		specName = campaign.getSpecName();
		specVersion = campaign.getSpecVersion();
		
		Iterator<IExecution> elemIter = campaign.getChildren().iterator();
		while (elemIter.hasNext()) {
			IExecution iExecution = (IExecution) elemIter.next();
			elements.add(new TestReportPart(iExecution));
		}
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
	 * @param testCampaign
	 * @param baseDirName
	 */
	public TestReport(TestCampaign testCampaign, String baseDirName) {
		this(testCampaign);
		this.baseDir = new File(baseDirName);
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

	// TODO add extension for e.g. forensic results see addForensic... in GT2

}
