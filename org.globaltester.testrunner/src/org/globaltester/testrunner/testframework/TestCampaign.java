package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testspecification.testframework.FileTestExecutable;
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
	private ArrayList<TestCampaignElement> elements = new ArrayList<TestCampaignElement>();
	
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
		
		// extract TestExecutables
		@SuppressWarnings("unchecked")
		Iterator<Element> testExecutionIter = root.getChildren(
				TestCampaignElement.XML_ELEMENT).iterator();
		while (testExecutionIter.hasNext()) {
			Element xmlElem = (Element) testExecutionIter.next();
			TestCampaignElement curTestCampaignElement = new TestCampaignElement(
					this, xmlElem);
			if (curTestCampaignElement != null) {
				elements.add(curTestCampaignElement);
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
		
		// add the newest execution
		if (executions.size() > 0){
			Element lastExecElem = new Element("LastExecution");
			lastExecElem.addContent(executions.getFirst().getIFile()
					.getProjectRelativePath().toString());
			root.addContent(lastExecElem);
		}
		
		// add TestCampaignElements to data to be stored
		Iterator<TestCampaignElement> elemIter = elements.iterator();
		while (elemIter.hasNext()) {
			TestCampaignElement curElem = elemIter.next();
			root.addContent(curElem.getXmlRepresentation());
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

	public void addExecutable(FileTestExecutable origTestExecutable)
			throws CoreException {
		// create a new TestCampaignElement and add it
		TestCampaignElement newElement = new TestCampaignElement(this,
				origTestExecutable);
		elements.add(newElement);

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(true,
				new Object[] { this, newElement }, null);
	}

	public String getName() {
		return project.getName();
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 * @param configuration 
	 * @param monitor 
	 * @param callback 
	 * 
	 * @throws CoreException
	 */
	public void executeTests(Map<Class<?>, Object> configuration, IProgressMonitor monitor, 
			Map<String, Object> envSettings, TestExecutionCallback callback) throws CoreException {

		// create a new TestExecution this TestCampaignElement
		TestCampaignExecution currentExecution = FileTestExecutionFactory.createExecution(this);
		
		if (executions.size() > 0) {
			// register this new execution
			currentExecution.setPreviousExecution(executions.getFirst());
		}
		executions.addFirst(currentExecution);

		currentExecution.setSampleConfig((SampleConfig)configuration.get(SampleConfig.class));
		
		String cardReaderName = PreferenceHelper.getPreferenceValue("org.globaltester.testmanager", "Card reader name");
		currentExecution.setCardReaderName(cardReaderName);
		
		String integrityOfTestSuiteProvidedRaw = PreferenceHelper.getPreferenceValue("org.globaltester.testmanager", "Integrity of test suite");
		boolean integrityOfTestSuiteProvided = Boolean.getBoolean(integrityOfTestSuiteProvidedRaw);
		currentExecution.setIntegrityOfTestSuiteProvided(integrityOfTestSuiteProvided);
		
		// execute the TestExecutable
		currentExecution.execute(monitor, envSettings);
		
		// save the new state
		project.doSave();

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(false, elements.toArray(),
				new String[] { "lastExecution" });
		
		//notify the callback about the execution results
		TestExecutionCallback.TestResult result = new TestExecutionCallback.TestResult();
		result.testCases = currentExecution.getChildren().size();
		result.overallResult = currentExecution.getResult().status.ordinal();
		//IMPL: propagate subresults
		callback.testExecutionFinished(result);
		
		
		// refresh the project in workspace
		project.getIProject().refreshLocal(IResource.DEPTH_INFINITE, null);

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

	public List<TestCampaignElement> getTestCampaignElements() {
		ArrayList<TestCampaignElement> children = new ArrayList<TestCampaignElement>();

		// add elements to list of children
		children.addAll(elements);

		return children;
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
	
}
