package org.globaltester.testrunner.testframework;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestCampaignElement implements IExecution {

	public static final String XML_ELEMENT = "TestCampaignElement";
	public static final String XML_ELEM_SPEC = "ExecutableSpec";
	public static final String XML_ELEM_LAST_EXEC = "LastExecution";
	private TestCampaign parent;
	private TestExecutable spec;
	private FileTestExecution lastExecution;

	public TestCampaignElement(TestCampaign testCampaign,
			TestExecutable origTestSpec) throws CoreException {
		parent = testCampaign;
		spec = parent.getProject().persistTestExecutable(origTestSpec);
		lastExecution = null;
	}

	public TestCampaignElement(TestCampaign testCampaign, Element xmlElem)
			throws CoreException {
		parent = testCampaign;

		initFromXmlElement(xmlElem);
	}

	private void initFromXmlElement(Element xmlElem) throws CoreException {
		// TODO check the name of the given xmlElem

		// extract TestExecutable
		Element specElem = xmlElem.getChild(XML_ELEM_SPEC);
		if (specElem != null) {
			String fileName = specElem.getTextTrim();
			IFile iFile = parent.getProject().getIProject().getFile(fileName);
			spec = TestExecutableFactory.getInstance(iFile);
		} else {
			throw new RuntimeException("TestCampaignElement can not be ");
		}

		// extract the last Execution if any
		Element lastExecElem = xmlElem.getChild(XML_ELEM_LAST_EXEC);
		if (lastExecElem != null) {
			String fileName = lastExecElem.getTextTrim();
			IFile iFile = parent.getProject().getIProject().getFile(fileName);
			lastExecution = FileTestExecutionFactory.getInstance(iFile);
		}

	}

	public Element getXmlRepresentation() {
		// create XML element for this TestCampaignElement add all children and
		// return it
		Element xmlElem = new Element(XML_ELEMENT);

		// create XML element for the specification and add it to xmlElem
		Element specElem = new Element(XML_ELEM_SPEC);
		specElem.addContent(spec.getIFile().getProjectRelativePath().toString());
		xmlElem.addContent(specElem);

		// create XML element for the last execution and add it to xmlElem
		if (lastExecution != null) {
			Element lastExecElem = new Element(XML_ELEM_LAST_EXEC);
			lastExecElem.addContent(lastExecution.getIFile()
					.getProjectRelativePath().toString());
			xmlElem.addContent(lastExecElem);
		}

		return xmlElem;
	}

	/**
	 * Execute the given executable and all following
	 * 
	 * @param curExecutable
	 * @param sr
	 * @param cx
	 * @param forceExecution
	 */
	void execute(ScriptRunner sr, Context cx, boolean forceExecution) {

		// create a new TestExecution this TestCampaignElement
		FileTestExecution testExecution = null;
		try {
			testExecution = FileTestExecutionFactory.createExecution(this);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (testExecution != null) {
			// register this new execution
			testExecution.setPreviousExecution(lastExecution);
			lastExecution = testExecution;

			// TODO configure logger for individual logfiles here

			// execute the TestExecutable
			testExecution.execute(sr, cx, forceExecution);

			// TODO deconfigure logger for individual logfiles here

		}

	}

	public IExecution getParent() {
		return parent;
	}

	public TestExecutable getExecutable() {
		return spec;
	}

	public FileTestExecution getLastExecution() {
		return lastExecution;
	}

	@Override
	public boolean hasChildren() {
		return (lastExecution != null) && lastExecution.hasChildren();
	}

	@Override
	public Collection<IExecution> getChildren() {
		if (hasChildren()) {
			return lastExecution.getChildren();
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return spec.getName();
	}

}
