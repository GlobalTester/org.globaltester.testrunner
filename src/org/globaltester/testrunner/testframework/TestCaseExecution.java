package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.PostCondition;
import org.globaltester.testspecification.testframework.PreCondition;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.globaltester.testspecification.testframework.TestStep;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestCaseExecution extends FileTestExecution {

	private LinkedList<ActionStepExecution> preConExecutions = new LinkedList<ActionStepExecution>();
	private LinkedList<ActionStepExecution> testStepExecutions = new LinkedList<ActionStepExecution>();
	private LinkedList<ActionStepExecution> postConExecutions = new LinkedList<ActionStepExecution>();

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
		//create execution objects for Preconditions
		List<PreCondition> preCons = getTestCase().getPreConditions();
		if (preCons != null) {
			for (Iterator<PreCondition> testStepIter = preCons.iterator(); testStepIter
			.hasNext();) {
				preConExecutions.add(new PreConditionExecution(testStepIter.next(), this));
			}
		}
		
		//create execution objects for TestSteps
		List<TestStep> testSteps = getTestCase().getTestSteps();
		if (testSteps != null) {
			for (Iterator<TestStep> testStepIter = testSteps.iterator(); testStepIter
			.hasNext();) {
				testStepExecutions.add(new TestStepExecution(testStepIter.next(), this));
			}
		}
		
		//create execution objects for Postconditions
		List<PostCondition> postCons = getTestCase().getPostConditions();
		if (postCons != null) {
			for (Iterator<PostCondition> postConIter = postCons.iterator(); postConIter
			.hasNext();) {
				postConExecutions.add(new PostConditionExecution(postConIter.next(), this));
			}
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
	protected void execute(ScriptRunner sr, Context cx, boolean forceExecution, boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null){
			monitor = new NullProgressMonitor();
		}
		try {
			
		monitor.beginTask("Execute TestCase "+getName() , getChildren().size());
		
		//make sure that failures are counted for each test case seperately
		ResultFactory.reset();
		
		// TODO use variable forceExecution

//		// dump execution information to logfile
		getTestCase().dumpTestCaseInfos();

		// iterate over all preconditions and execute them
		TestLogger.info("Running Preconditions");
		for (Iterator<ActionStepExecution> preConIter = preConExecutions.iterator(); preConIter
				.hasNext() && !monitor.isCanceled();) {
			ActionStepExecution curStepExec = preConIter.next();
			curStepExec.execute(sr, cx, forceExecution, new SubProgressMonitor(monitor, 1));
			
			result.addSubResult(curStepExec.getResult());
		}
		
		// iterate over all test steps and execute them
		TestLogger.info("Running TestSteps");
		for (Iterator<ActionStepExecution> testStepIter = testStepExecutions.iterator(); testStepIter
				.hasNext() && !monitor.isCanceled();) {
			ActionStepExecution curStepExec = testStepIter.next();
			curStepExec.execute(sr, cx, forceExecution, new SubProgressMonitor(monitor, 1));
			
			result.addSubResult(curStepExec.getResult());
		}
		

		// iterate over all postconditions and execute them
		TestLogger.info("Running Postconditions");
		for (Iterator<ActionStepExecution> postConIter = postConExecutions.iterator(); postConIter
				.hasNext() && !monitor.isCanceled();) {
			ActionStepExecution curStepExec = postConIter.next();
			curStepExec.execute(sr, cx, forceExecution, new SubProgressMonitor(monitor, 1));
			
			result.addSubResult(curStepExec.getResult());
		}
		} finally {
			monitor.done();
		}
		
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
	
	@SuppressWarnings("unchecked")
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);

		// extract preconditions
		List<Element> preConditionExecutionElements = root.getChildren("PreConditionExecution");
		List<PreCondition> preConditions = getTestCase().getPreConditions();
				
		if (preConditionExecutionElements.size() == preConditions.size()) {
			for (int i = 0; i < preConditionExecutionElements.size(); i++) {
				PreConditionExecution exec = new PreConditionExecution(preConditions.get(i), this);
				exec.extractFromXml(preConditionExecutionElements.get(i));
				preConExecutions.add(exec);
				result.addSubResult(exec.getResult());
			}
		}
		
		// extract teststeps
		List<Element> testStepExecutionElements = root.getChildren("TestStepExecution");
		List<TestStep> testSteps = getTestCase().getTestSteps();
				
		if (testStepExecutionElements.size() == testSteps.size()) {
			for (int i = 0; i < testStepExecutionElements.size(); i++) {
				TestStepExecution exec = new TestStepExecution(testSteps.get(i), this);
				exec.extractFromXml(testStepExecutionElements.get(i));
				testStepExecutions.add(exec);
				result.addSubResult(exec.getResult());
			}
		}
		
		// extract postconditions
		List<Element> postConditionExecutionElements = root.getChildren("PostConditionExecution");
		List<PostCondition> postConditions = getTestCase().getPostConditions();
				
		if (postConditionExecutionElements.size() == postConditions.size()) {
			for (int i = 0; i < postConditionExecutionElements.size(); i++) {
				PostConditionExecution exec = new PostConditionExecution(postConditions.get(i), this);
				exec.extractFromXml(postConditionExecutionElements.get(i));
				postConExecutions.add(exec);
				result.addSubResult(exec.getResult());
			}
		}
		result.rebuildStatus();
	}

	@Override
	public boolean hasChildren() {
		if ((preConExecutions != null) && (!preConExecutions.isEmpty())) return true;
		if ((testStepExecutions != null) && (!testStepExecutions.isEmpty())) return true;
		if ((postConExecutions != null) && (!postConExecutions.isEmpty())) return true;
		
		return false;
	}

	@Override
	public Collection<IExecution> getChildren() {
		LinkedList<IExecution> children = new LinkedList<IExecution>();
		
		//add test step executions to list of children
		children.addAll(preConExecutions);
		children.addAll(testStepExecutions);
		children.addAll(postConExecutions);
		
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

	@Override
	void dumpToXml(Element root) {
		super.dumpToXml(root);
		// iterate over all preconditions and dump them
		for (Iterator<ActionStepExecution> preConIter = preConExecutions
				.iterator(); preConIter.hasNext();) {
			ActionStepExecution curStepExec = preConIter.next();
			
			Element curStepElem = new Element(curStepExec.getXmlRootElementName());
			curStepExec.dumpToXml(curStepElem);
			root.addContent(curStepElem);
		}

		// iterate over all test steps and dump them
		for (Iterator<ActionStepExecution> testStepIter = testStepExecutions
				.iterator(); testStepIter.hasNext();) {
			ActionStepExecution curStepExec = testStepIter.next();
			
			Element curStepElem = new Element(curStepExec.getXmlRootElementName());
			curStepExec.dumpToXml(curStepElem);
			root.addContent(curStepElem);
		}

		// iterate over all postconditions and dump them
		for (Iterator<ActionStepExecution> postConIter = postConExecutions
				.iterator(); postConIter.hasNext();) {
			ActionStepExecution curStepExec = postConIter.next();
			
			Element curStepElem = new Element(curStepExec.getXmlRootElementName());
			curStepExec.dumpToXml(curStepElem);
			root.addContent(curStepElem);
		}

	}

}
