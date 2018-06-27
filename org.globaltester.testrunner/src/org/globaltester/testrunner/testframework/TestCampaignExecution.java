package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.jdom.Element;

public class TestCampaignExecution extends FileTestExecution {
	
	public static final String XML_ELEMENT = "TestCampaignExecution";
	private static final String XML_CHILD_PREVIOUS_EXECUTION = "PreviousExecution";
	
	private TestCampaignExecution previousExecution;
	
	@Override
	public void extractFromXml(Element root) throws CoreException {
		super.extractFromXml(root);

		try {
			// extract previous execution
			Element prevExecFileElement = root.getChild(XML_CHILD_PREVIOUS_EXECUTION);

			if (prevExecFileElement != null) {
				String prevExecFileName = prevExecFileElement.getTextTrim();
				FileTestExecution execution = FileTestExecutionFactory.getInstance(iFile
						.getProject().getFile(prevExecFileName));
				if (execution instanceof TestCampaignExecution){
					previousExecution = (TestCampaignExecution) execution;
				}
			}
		} catch (CoreException e) {
			// ignore if no previous executions can be found
		}
		
	}

	@Override
	public void dumpToXml(Element root) {
		super.dumpToXml(root);
		
		// dump previous execution
		if (previousExecution != null) {
			Element prevExecElement = new Element(XML_CHILD_PREVIOUS_EXECUTION);
			prevExecElement.addContent(previousExecution.getIFile()
					.getProjectRelativePath().toString());
			root.addContent(prevExecElement);
		}
		
	}

	public TestCampaignExecution(IFile iFile) throws CoreException {
		super(iFile);
	}

	protected TestCampaignExecution(IFile iFile, TestCampaign testCampaign)
			throws CoreException {
		super(iFile);
		cachedExecutable = testCampaign;

		// persist the specFile to the GtTestCampaignProject
		specFile = getGtTestCampaignProject().getTestCampaignIFile();

		initFromTestCampaign();
		
		// store this configuration
		doSave();
	}

	private void initFromTestCampaign() throws CoreException {
		setId(getTestCampaign().getName());
		//FIXME AAD persist TestCaseExecutions (optionally) from TestSetExecution
		addChildExecution(new TestSetExecution(getTestCampaign().getTestSet(), getTestCampaign()));
	}

	public TestCampaign getTestCampaign() {
		try {
			return getGtTestCampaignProject().getTestCampaign();
		} catch (CoreException e) {
			throw new RuntimeException(
					"Could not get TestCampaign for this Execution", e);
		}
	}

	@Override
	public String getXmlRootElementName() {
		return XML_ELEMENT;
	}

	@Override
	protected void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution,
			boolean reExecution, IProgressMonitor monitor) {
		
		getTestCampaign().registerNewExecution(this);
		
		//execute the TestSet
		TestSetExecution testSetExecution = getTestSetExecution();
		testSetExecution.execute(runtimeReqs, forceExecution, monitor);
		
		try {
			//FIXME AAD check whether this is late enough
			// save the new state
			getGtTestCampaignProject().doSave();

			// notify viewers of parent about this change
			getGtTestCampaignProject().notifyTreeChangeListeners(false, new Object[] {getTestCampaign().getTestSet()},
						new String[] { "lastExecution" });
		} catch (CoreException e) {
			BasicLogger.logException("Unable to persist/propagate new ExecutionState in TestCampaign", e, LogLevel.WARN);
		}
	}
	
	/**
	 * @param previousExecution
	 *            the previousExecution to set
	 */
	public void setPreviousExecution(TestCampaignExecution previousExecution) {
		this.previousExecution = previousExecution;
	}

	/**
	 * @return the previousExecution
	 */
	public TestCampaignExecution getPreviousExecution() {
		return previousExecution;
	}
	
	@Override
	public void doSaveChildren() {
		super.doSaveChildren();
		
		// save previous executions recursively
		if (previousExecution != null) {
			previousExecution.doSave();
		}
	}

	public TestSetExecution getTestSetExecution() {
		if (getChildren().isEmpty()) {
			return null;
		}
		return (TestSetExecution) getChildren().iterator().next();

	}
}
