package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.jdom.Document;
import org.jdom.Element;

public class TestCampaignExecution extends FileTestExecution {
	
	private TestSetExecution testSetExecution;
	private TestCampaignExecution previousExecution;
	
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);

		// extract testSetExecution
		Element testSetExecutionElement = root.getChild(TestSetExecution.XML_ELEMENT);
		if (testSetExecutionElement != null) {
			testSetExecution = new TestSetExecution(testSetExecutionElement);
		}
		
		try {
			// extract previous execution
			Element prevExecFileElement = root.getChild("PreviousExecution");

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
	void dumpToXml(Element root) {
		super.dumpToXml(root);
		
		// dump testSetExecution
		if (testSetExecution != null) {
			Element testSetExecutionElement = new Element(TestSetExecution.XML_ELEMENT);
			testSetExecution.dumpToXml(testSetExecutionElement);
			root.addContent(testSetExecutionElement);
		}
		
		// dump previous execution
		if (previousExecution != null) {
			Element prevExecElement = new Element("PreviousExecution");
			prevExecElement.addContent(previousExecution.getIFile()
					.getProjectRelativePath().toString());
			root.addContent(prevExecElement);
		}
		
		// dump filenames for children
		Element fileNames = new Element("FileNames");
		root.addContent(fileNames);
		
		//FIXME AAA check what to do with this code
//		Iterator<IExecution> iter = this.getChildren().iterator();
//		while (iter.hasNext()){
//			IExecution current = iter.next();
//			if (current instanceof TestCaseExecution){
//				Element childElement = new Element("FileName");
//				childElement.addContent(((TestCaseExecution) current).getIFile().getProjectRelativePath().toString());
//				fileNames.addContent(childElement);
//			}
//		}
		
	}

	public TestCampaignExecution(IFile iFile) throws CoreException {
		super(iFile);
		
		initFromIFile();
	}

	protected TestCampaignExecution(IFile iFile, TestCampaign testCampaign)
			throws CoreException {
		super(iFile);

		// persist the specFile to the GtTestCampaignProject
		specFile = getGtTestCampaignProject().getTestCampaignIFile();

		initFromTestCampaign();
		
		// store this configuration
		doSave();
	}

	private void initFromTestCampaign() {		
		//FIXME AAA persist TestCaseExecutions (optionally) from TestSetExecution
//		testSetExecution = new TestSetExecution(getTestCampaign().getTestSet(), getTestCampaign());
		testSetExecution = new TestSetExecution(getTestCampaign().getTestSet());
	}

	@Override
	public boolean hasChildren() {
		return testSetExecution != null && testSetExecution.hasChildren();
	}

	@Override
	public Collection<IExecution> getChildren() {
		ArrayList<IExecution> children = new ArrayList<IExecution>();
		children.addAll(testSetExecution.getChildren());
		return children;
	}

	@Override
	public IExecution getParent() {
		return null;
	}

	@Override
	public String getName() {
		return getTestCampaign().getName();
	}

	@Override
	public String getComment() {
		if (result != null) {
			return result.getComment();
		}
		return "";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public long getDuration() {
		return testSetExecution.getDuration();
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	protected void createIFile() {
		if(!iFile.exists()){
			Element root = new Element("TestCampaignExecution");
			XMLHelper.saveDoc(iFile, root);
		}
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
	protected String getXmlRootElementName() {
		return "TestCampaignExecution";
	}

	@Override
	protected void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution,
			boolean reExecution, IProgressMonitor monitor) {
		//just need to execute the TestSet here
		testSetExecution.execute(runtimeReqs, forceExecution, monitor);
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
		if (!rootElem.getName().equals("TestCampaignExecution")) {
			return false;
		}

		return true;
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
	public void doSave() {
		super.doSave();
		
		// save testSetExecution
		if (testSetExecution != null) {
			testSetExecution.doSaveChildren();
		}
		
		// save previous executions recursively
		if (previousExecution != null) {
			previousExecution.doSave();
		}
	}

	public TestSetExecution getTestSetExecution() {
		return testSetExecution;
	}
}
