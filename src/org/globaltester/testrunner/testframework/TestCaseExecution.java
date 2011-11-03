package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
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

public class TestCaseExecution extends FileTestExecution {

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
		testStepExecutions = new LinkedList<TestStepExecution>();
		List<TestStep> testSteps = getTestCase().getTestSteps();
		for (Iterator<TestStep> testStepIter = testSteps.iterator(); testStepIter
		.hasNext();) {
			testStepExecutions.add(new TestStepExecution(testStepIter.next()));
		}
		
	}
	
	@Override
	protected void createIFile() {
		if(!iFile.exists()){
			Element root = new Element("TestCaseExecution");			
			XMLHelper.saveDoc(iFile, root);
		}
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
			TestStepExecution curStepExec = testStepIter.next();
			curStepExec.execute(sr, cx, forceExecution);
			
			result.addSubResult(curStepExec.getResult());
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

	@Override
	protected String getXmlRootElementName() {
		return "TestCaseExecution";
	}
	
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);
		initFromTestCase();
	}

	@Override
	public boolean hasChildren() {
		return !testStepExecutions.isEmpty();
	}

	@Override
	public Collection<IExecution> getChildren() {
		LinkedList<IExecution> children = new LinkedList<IExecution>();
		
		//add test step executions to list of children
		children.addAll(testStepExecutions);
		
		return children;
	}

	@Override
	public IExecution getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return getTestCase().getName();
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String getDescription() {
		return this.getTestCase().getTestCasePurpose();
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getId() {
		return getTestCase().getTestCaseID();
	}

}
