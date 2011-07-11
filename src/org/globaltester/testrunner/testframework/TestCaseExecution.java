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

	public TestCaseExecution(IFile iFile, TestCase testCase) throws CoreException {
		super(iFile);
		
		specFile = testCase.getIFile();
		storeToIFile();
	}

	/**
	 * Store the current state to the resource iFile
	 */
	private void storeToIFile() {
		// FIXME save configuration to IFile
		
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 */
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
		//TODO use variable forceExecution
		
		//dump execution information to logfile
		TestLogger.info("----- Begin TestCase execution -----");
		getTestCase().dumpTestCaseInformation();
		
		TestStepExecutor stepExecutor = new TestStepExecutor(sr, cx);
		
		//iterate over all test steps and execute them
		List<TestStep> testSteps = getTestCase().getTestSteps(); 
		for (Iterator<TestStep> testStepIter = testSteps.iterator(); testStepIter.hasNext();) {
			stepExecutor.execute(testStepIter.next());
		}
		
		//dump execution information to logfile
		TestLogger.info("----- End TestCase execution -----");
		

	}

	private TestCase getTestCase() {
		IFile testCaseResource = getSpecFile();
		try {
			return (TestCase) TestExecutableFactory.getInstance(testCaseResource);
		} catch (CoreException e) {
			throw new RuntimeException("Could not create TestCase for "+testCaseResource, e);
		}
	}

}
