package org.globaltester.testrunner.testframework;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.globaltester.testspecification.testframework.TestStep;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestCaseExecution extends TestExecution {

	private LinkedList<TestStepExecution> testStepExecutions;

	protected TestCaseExecution(IFile iFile) throws CoreException {
		super(iFile);
		initFromIFile();
	}

	protected TestCaseExecution(IFile iFile, TestCase testCase)
			throws CoreException {
		super(iFile);

		//persist the specFile to the GtTestCampaignProject
		specFile = getGtTestCampaignProject().persistTestExecutable(testCase).getIFile();
		
		//create execution instances from testcase
		initFromTestCase();

		//store this configuration
		doSave();
	}

	/**
	 * create all required execution instances from test case. E.g. TestStepExecutions
	 */
	private void initFromTestCase() {
		// TODO Auto-generated method stub
		testStepExecutions = new LinkedList<TestStepExecution>();
		List<TestStep> testSteps = getTestCase().getTestSteps();
		for (Iterator<TestStep> testStepIter = testSteps.iterator(); testStepIter
		.hasNext();) {
			testStepExecutions.add(new TestStepExecution(testStepIter.next()));
		}
		
	}

	@Override
	protected void doSave() {
		Element root = new Element("TestCaseExecution");
		dumpCommonMetaData(root);
		
		XMLHelper.saveDoc(iFile, root);

	}
	
	@Override
	protected void createIFile() {
		if(!iFile.exists()){
			Element root = new Element("TestCaseExecution");			
			XMLHelper.saveDoc(iFile, root);
		}
	}

	@Override
	protected void initFromIFile() {
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCaseExecution"),
				"Root element is not TestCaseExecution");

		// extract meta data
		extractCommonMetaData(root);
		
		//create execution instances from testcase
		initFromTestCase();

	}

	/**
	 * checks whether the given file represents an TestCaseExecution object
	 * 
	 * @param iFile
	 * @return
	 */
	public static boolean isFileRepresentation(IFile iFile) {
		Document doc = XMLHelper.readDocument(iFile);
		Element rootElem = doc.getRootElement();

		// check that root element has correct name
		if (!rootElem.getName().equals("TestCaseExecution")) {
			return false;
		}

		return true;
	}

	@Override
	protected void execute(ScriptRunner sr, Context cx, boolean forceExecution, boolean reExecution) {
		//make sure that failures are counted for each test case seperately
		ResultFactory.reset();
		
		// TODO use variable forceExecution

		// dump execution information to logfile
		TestLogger.initTestExecutable(getTestCase().getTestCaseID());
		getTestCase().dumpTestCaseInfos();

		// TODO handle preconditions etc.

		// iterate over all test steps and execute them
		TestLogger.info("Running TestSteps");
		for (Iterator<TestStepExecution> testStepIter = testStepExecutions.iterator(); testStepIter
				.hasNext();) {
			testStepIter.next().execute(sr, cx, forceExecution);
		}
		

		// TODO handle postconditions etc.

		// dump execution information to logfile
		TestLogger.shutdownTestExecutableLogger();

	}

	private TestCase getTestCase() {
		IFile testCaseResource = getSpecFile();
		try {
			return (TestCase) TestExecutableFactory
					.getInstance(testCaseResource);
		} catch (CoreException e) {
			throw new RuntimeException("Could not create TestCase for "
					+ testCaseResource, e);
		}
	}

}
