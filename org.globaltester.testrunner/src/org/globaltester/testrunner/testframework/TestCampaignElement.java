package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
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
	 * @param runtimeReqs
	 *            The {@link GtRuntimeRequirements} to deliver all needed
	 *            data and functions for this execution
	 * @param forceExecution
	 * @return
	 * @throws CoreException 
	 */
	FileTestExecution execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution, IProgressMonitor monitor) throws CoreException {

		// create a new TestExecution this TestCampaignElement
		FileTestExecution testExecution = FileTestExecutionFactory.createExecution(this);

		if (testExecution != null) {
			// execute the TestExecutable
			testExecution.execute(runtimeReqs, forceExecution, monitor);
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
