package org.globaltester.testrunner.testframework;

import java.util.Iterator;
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

	protected TestCaseExecution(IFile iFile) throws CoreException {
		super(iFile);
		initFromIFile();
	}

	protected TestCaseExecution(IFile iFile, TestCase testCase)
			throws CoreException {
		super(iFile);

		//copy the specFile to the GtTestRunProject
		specFile = getGtTestRunProject().getSpecificationIFile(testCase);
		testCase.getIFile().copy(specFile.getFullPath(), false, null);

		//store this configuration
		storeToIFile();
	}

	@Override
	protected void storeToIFile() {
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
		Document doc = XMLHelper.readDocument(iFile, false); // TODO enable
																// validation
																// here
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCaseExecution"),
				"Root element is not TestCaseExecution");

		// extract meta data
		extractCommonMetaData(root);

	}

	/**
	 * checks whether the given file represents an TestCaseExecution object
	 * 
	 * @param iFile
	 * @return
	 */
	public static boolean isFileRepresentation(IFile iFile) {
		Document doc = XMLHelper.readDocument(iFile, false); // TODO enable
																// validation
																// here
		Element rootElem = doc.getRootElement();

		// check that root element has correct name
		if (!rootElem.getName().equals("TestCaseExecution")) {
			return false;
		}

		return true;
	}

	@Override
	public void execute(ScriptRunner sr, Context cx, boolean forceExecution) {
		// TODO use variable forceExecution

		// dump execution information to logfile
		TestLogger.initTestExecutable(getTestCase().getTestCaseID());
		getTestCase().dumpTestCaseInfos();

		// TODO handle preconditions etc.

		// iterate over all test steps and execute them
		TestLogger.info("Running TestSteps");
		TestStepExecutor stepExecutor = new TestStepExecutor(sr, cx);
		List<TestStep> testSteps = getTestCase().getTestSteps();
		for (Iterator<TestStep> testStepIter = testSteps.iterator(); testStepIter
				.hasNext();) {
			stepExecutor.execute(testStepIter.next());
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
