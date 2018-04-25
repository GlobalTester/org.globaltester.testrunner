package org.globaltester.testrunner.report;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;

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
	private String cardReaderName = "unknown";
	private String integrityOfTestSpec = "UNCHECKED";
	
	private HashSet<String> selectedProfiles = new HashSet<>();

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
		
		FileTestExecutable fileTestExecutable;
		TestCaseExecution currentTestCaseExecution;
		for(IExecution currentIexecution : campaignExec.getChildren()) {
			elements.add(new TestReportPart(currentIexecution));
			
			if(currentIexecution instanceof TestCaseExecution) {
				try {
					currentTestCaseExecution = ((TestCaseExecution) currentIexecution);
					fileTestExecutable = TestExecutableFactory.getInstance(currentTestCaseExecution.getSpecFile());
					
					if(!(currentTestCaseExecution.getStatus().equals(Status.NOT_APPLICABLE))) {
						selectedProfiles.addAll(parseProfileString(fileTestExecutable.getProfileString()));
					}
				} catch (CoreException e) {
					// do nothing
				}
			}
		}
		
		logFiles = new ArrayList<>();
		logFiles.add(campaignExec.getLogFileName());
		
		for(IExecution currentIexecution: campaignExec.getChildren()) {
			logFiles.add(currentIexecution.getLogFileName());
		}
		
		Date date = new Date(campaignExec.getLastExecutionStartTime());
		executionTime = date.toString();
		
		executingUser = campaignExec.getExecutingUser();
		
		cardReaderName = campaignExec.getCardReaderName();
		
		integrityOfTestSpec = campaignExec.getIntegrityOfTestSpec();
		
		SampleConfig sampleConfig = campaignExec.getSampleConfig();
		platformId = sampleConfig.getPlatformId();
		sampleId = sampleConfig.getSampleId();
		
	}
	
	/**
	 * Centralizes the extraction of relevant data from the given TestCampaign
	 * @param campaign
	 */
	private TestReport(TestSetExecution testSetExec) {
		fileName = "TestReport";
		
		specName = "unknown";
		specVersion = "unknown";
		
		FileTestExecutable fileTestExecutable;
		TestCaseExecution currentTestCaseExecution;
		for(IExecution currentIexecution : testSetExec.getChildren()) {
			elements.add(new TestReportPart(currentIexecution));
			
			if(currentIexecution instanceof TestCaseExecution) {
				try {
					currentTestCaseExecution = ((TestCaseExecution) currentIexecution);
					fileTestExecutable = TestExecutableFactory.getInstance(currentTestCaseExecution.getSpecFile());
					
					if(!(currentTestCaseExecution.getStatus().equals(Status.NOT_APPLICABLE))) {
						selectedProfiles.addAll(parseProfileString(fileTestExecutable.getProfileString()));
					}
				} catch (CoreException e) {
					// do nothing
				}
			}
		}
		
		logFiles = new ArrayList<>();
		logFiles.add(testSetExec.getLogFileName());
		
		for(IExecution currentIexecution: testSetExec.getChildren()) {
			logFiles.add(currentIexecution.getLogFileName());
		}
		
		Date date = new Date(testSetExec.getLastExecutionStartTime());
		executionTime = date.toString();
		
		executingUser = testSetExec.getExecutingUser();
		
		cardReaderName = testSetExec.getCardReaderName();
		
		integrityOfTestSpec = testSetExec.getIntegrityOfTestSpec();
		
		SampleConfig sampleConfig = testSetExec.getSampleConfig();
		platformId = sampleConfig.getPlatformId();
		sampleId = sampleConfig.getSampleId();
		
	}
	
	/**
	 * This method parses a String separated by commas.
	 * It returns a Set containing all unique trimmed single Strings.
	 * @param profileString the String to be parsed
	 * @return the resulting set, may be empty
	 */
	public static Set<String> parseProfileString(String profileString) {
		String currentProfile;
		String[] currentprofiles;
		HashSet<String> parsedProfiles = new HashSet<>();
		
		currentprofiles = profileString.split(",");
		
		for(String currentSingleProfile : currentprofiles) {
			currentProfile = currentSingleProfile.trim();
			if(currentProfile.length() > 0) {
				parsedProfiles.add(currentProfile);
			}
		}
		
		return parsedProfiles;
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
		
		cardReaderName = origReport.cardReaderName;
		
		integrityOfTestSpec = origReport.integrityOfTestSpec;
		
		selectedProfiles = origReport.selectedProfiles;
		
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

	/**
	 * Create a new report from the given TestCampaign and prepares to output
	 * to disk.
	 * 
	 * @param testCampaignExecution
	 * @param baseDirName
	 */
	public TestReport(TestSetExecution testSetExecution, String baseDirName) {
		this(testSetExecution);
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
	
	public Set<String> getSelectedProfiles() {
		return selectedProfiles;
	}

	public String getCardReaderName() {
		return cardReaderName;
	}

	public String getIntegrityOfTestSpec() {
		return integrityOfTestSpec;
	}

	public static File getDefaultDestinationDir() {
		//FIXME
//            boolean fixedDir = Activator.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.P_FIXEDDIRSETTINGS);
//            if(fixedDir) {
//                    String fixedDirPath = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_REPORTDIR);
//                    return new File(fixedDirPath);
//            } else {
//                    IPath reportDirIpath = Activator.getWorkingDir();
//                    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(reportDirIpath);
//                    return new File(file.getFullPath().toOSString() + File.separator + "Reports" + File.separator + getDateString("yyyyMMdd_HHmmss"));
//            }
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path("GT Reports"));
		return new File(file.getFullPath().toOSString() + File.separator + "Reports" + File.separator + getDateString("yyyyMMdd_HHmmss"));
    }
	
    /**
     * Returns the current date inclusive time in requested format
     *
     * @param dateFormat - the pattern describing the date and time format

     * @return
     */
    public static String getDateString(String dateFormat) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                            dateFormat);
            // get current date:
            Calendar cal = Calendar.getInstance();
            return sdf.format(cal.getTime());
    }


}
