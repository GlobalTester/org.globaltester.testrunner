package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.testrunner.ScriptRunner;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.jdom.Element;

public class TestCampaignElement {

	public static final String XML_ELEMENT = "TestCampaignElement";
	public static final String XML_ELEM_SPEC = "ExecutableSpec";
	public static final String XML_ELEM_LAST_EXEC = "LastExecution";
	private TestCampaign campaign;
	private FileTestExecutable spec;

	public TestCampaignElement(TestCampaign testCampaign,
			FileTestExecutable origTestSpec) throws CoreException {
		campaign = testCampaign;
		spec = campaign.getProject().persistTestExecutable(origTestSpec);
	}

	public TestCampaignElement(TestCampaign testCampaign, Element xmlElem)
			throws CoreException {
		campaign = testCampaign;

		initFromXmlElement(xmlElem);
	}

	private void initFromXmlElement(Element xmlElem) throws CoreException {
		// TODO check the name of the given xmlElem

		// extract TestExecutable
		Element specElem = xmlElem.getChild(XML_ELEM_SPEC);
		if (specElem != null) {
			String fileName = specElem.getTextTrim();
			IFile iFile = campaign.getProject().getIProject().getFile(fileName);
			spec = TestExecutableFactory.getInstance(iFile);
		} else {
			throw new RuntimeException("TestCampaignElement can not be ");
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

		return xmlElem;
	}

	/**
	 * Execute the given executable and all following
	 * 
	 * @param curExecutable
	 * @param sr
	 * @param cx
	 * @param forceExecution
	 * @return 
	 */
	FileTestExecution execute(ScriptRunner sr, boolean forceExecution, IProgressMonitor monitor) {

		// create a new TestExecution this TestCampaignElement
		FileTestExecution testExecution = null;
		try {
			testExecution = FileTestExecutionFactory.createExecution(this);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (testExecution != null) {
			// execute the TestExecutable
			testExecution.execute(sr, forceExecution, monitor);
		}
		
		return testExecution;

	}

	public FileTestExecutable getExecutable() {
		return spec;
	}
	
	public TestCampaign getTestCampaign(){
		return campaign;
	}

}
