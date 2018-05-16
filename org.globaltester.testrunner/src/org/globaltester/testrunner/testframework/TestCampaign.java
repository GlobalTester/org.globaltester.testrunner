package org.globaltester.testrunner.testframework;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestSet;
import org.jdom.Document;
import org.jdom.Element;

/**
 * A TestCampaign defines a set of tests together with the history of their
 * results.
 * 
 * @author amay
 * 
 */
public class TestCampaign {

	private GtTestCampaignProject project;
	private TestSet testSet = new TestSet();
	
	private String specName = "";
	private String specVersion = "unknown";
	
	private LinkedList<TestCampaignExecution> executions = null;
	
	public TestCampaign(GtTestCampaignProject gtTestCampaignProject) {
		this.project = gtTestCampaignProject;
		this.executions = new LinkedList<TestCampaignExecution>();
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 * 
	 * @throws CoreException
	 */
	public void initFromIFile(IFile iFile) throws CoreException {
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCampaign"),
				"Root element is not TestCampaign");
		
		//extract meta data
		Element specNameElem = root.getChild("SpecificationName");
		if (specNameElem != null) {
			specName = specNameElem.getTextTrim();
		}
		Element specVersionElem = root.getChild("SpecificationVersion");
		if (specVersionElem != null) {
			specVersion = specVersionElem.getTextTrim();
		}
		
		// extract the TestSet
		Element testSetElem = root.getChild(TestSet.XML_ELEMENT);
		if (testSetElem != null) {
			testSet = new TestSet(testSetElem);
		}

		// extract the last executions, if any
		Element lastExecElem = root.getChild("LastExecution");
		if (lastExecElem != null) {
			String fileName = lastExecElem.getTextTrim();
			IFile lastExecIFile = project.getIProject().getFile(fileName);
			TestCampaignExecution lastExecution = (TestCampaignExecution) FileTestExecutionFactory
					.getInstance(lastExecIFile);

			executions.add(lastExecution);
			while (lastExecution.getPreviousExecution() != null) {
				lastExecution = lastExecution
						.getPreviousExecution();
				executions.add(lastExecution);
			}
		}

	}

	/**
	 * Store
	 * 
	 * @throws CoreException
	 */
	public void storeToIFile(IFile iFile) throws CoreException {
		Element root = new Element("TestCampaign");

		//add meta data
		Element specNameElem = new Element("SpecificationName");
		specNameElem.addContent(specName);
		root.addContent(specNameElem);
		Element specVersionElem = new Element("SpecificationVersion");
		specVersionElem.addContent(specVersion);
		root.addContent(specVersionElem);

		// add TestCampaignElements to data to be stored
		root.addContent(testSet.getXmlRepresentation());
		
		// add the newest execution
		if (executions.size() > 0){
			Element lastExecElem = new Element("LastExecution");
			lastExecElem.addContent(executions.getFirst().getIFile()
					.getProjectRelativePath().toString());
			root.addContent(lastExecElem);
		}
		

		// create file if it does not exist yet
		if (!iFile.exists()) {
			iFile.create(null, false, null);
		}

		// write to file
		XMLHelper.saveDoc(iFile, root);

	}

	/**
	 * Save this TEstCampaign and its children in the workspace
	 * 
	 * @throws CoreException
	 */
	public void doSave() throws CoreException {
		// save this
		storeToIFile(project.getTestCampaignIFile());

		// save the newest execution, which causes all executions to be saved recursively
		if (executions.size() > 0){
			executions.getFirst().doSave();
		}
	}

	public void addExecutable(FileTestExecutable newChild)
			throws CoreException {
		testSet.add(newChild);

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(true,
				new Object[] { this, newChild }, null);
	}

	public String getName() {
		return project.getName();
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 * @param runtimeReqs 
	 * @param monitor 
	 * @param callback 
	 * 
	 * @throws CoreException
	 */
	public void executeTests(GtRuntimeRequirements runtimeReqs, IProgressMonitor monitor, TestExecutionCallback callback) throws CoreException {

		// create a new TestExecution this TestCampaignElement
		TestCampaignExecution currentExecution = FileTestExecutionFactory.createExecution(this);
		
		if (executions.size() > 0) {
			// register this new execution
			currentExecution.setPreviousExecution(executions.getFirst());
		}
		executions.addFirst(currentExecution);
		
		// execute the TestExecutable
		currentExecution.execute(runtimeReqs, false, monitor);
		
		// save the new state
		project.doSave();

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(false, new Object[] {testSet},
				new String[] { "lastExecution" });
		
		//notify the callback about the execution results
		TestExecutionCallback.TestResult result = new TestExecutionCallback.TestResult();
		result.testCases = currentExecution.getChildren().size();
		result.overallResult = currentExecution.getResult().status.ordinal();
		//IMPL: propagate subresults
		callback.testExecutionFinished(result);
		
		// refresh the project in workspace
		project.getIProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		
		
		//FIXME AAC check whether this can be removed
		// The card reader is selected as part of the test case execution and stored for reference,
		// hence the actual reader information is only available _after_ test case execution otherwise
		// the retrieved card reader name may be undefined or refer to a previous test run
//		String cardReaderName = SmartCardShellInfo.getActiveReaderName();
//		currentExecution.setCardReaderName(cardReaderName);
	}
	
	public String getLogFileName(){
		if (executions.size() > 0) {
			return executions.getFirst().getLogFileName();
		}
		return "";
	}

	public GtTestCampaignProject getProject() {
		return project;
	}

	public String getSpecName() {
		return specName;
	}

	public void setSpecName(String newName) {
		specName = newName;
	}

	public String getSpecVersion() {
		return specVersion;
	}

	public void setSpecVersion(String newVersion) {
		specVersion = newVersion;
	}

	public TestCampaignExecution getCurrentExecution() {
		if (executions.size() >0){
			return executions.getFirst();
		} else {
			return null;
		}
	}

	public List<TestCampaignExecution> getCampaignExecutions(){
		return executions;
	}

	public TestSet getTestSet() {
		return testSet;
	}
	
}
