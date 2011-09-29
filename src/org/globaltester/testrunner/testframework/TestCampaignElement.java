package org.globaltester.testrunner.testframework;

import org.eclipse.core.runtime.CoreException;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestCampaignElement {

	public static final String XML_ELEMENT = "TestCampaignElement";
	public static final String XML_ELEM_SPEC = "ExecutableSpec";
	public static final String XML_ELEM_LAST_EXEC = "LastExecution";
	private TestCampaign parent;
	private TestExecutable spec;
	private TestExecution lastExecution;

	public TestCampaignElement(TestCampaign testCampaign, TestExecutable origTestSpec) throws CoreException {
		parent = testCampaign;
		spec = parent.getProject().persistTestExecutable(origTestSpec);
		lastExecution = null;
	}
	
	public TestCampaignElement(TestCampaign testCampaign, Element xmlElem) {
		parent = testCampaign;
		
		initFromXmlElement(xmlElem);
	}

	private void initFromXmlElement(Element xmlElem) {
		// FIXME extract all available data from xmlElem
		
	}

	public Element getXmlRepresentation() {
		//create XML element for the specification
		Element specElem = new Element(XML_ELEM_SPEC);
		specElem.addContent(spec.getIFile().getProjectRelativePath().toString());
		
		//create XML element for the last execution
		Element lastExecElem = new Element(XML_ELEM_LAST_EXEC);
		lastExecElem.addContent(lastExecution.getIFile().getProjectRelativePath().toString());
		
		//create XML element for this TestCampaignElement add all children and return it
		Element xmlElem = new Element(XML_ELEMENT);
		xmlElem.addContent(specElem);
		
		
		return xmlElem;
	}
	
	/**
	 * Execute the given executable and all following
	 * @param curExecutable
	 * @param sr
	 * @param cx
	 * @param forceExecution
	 */
	void execute(ScriptRunner sr,
			Context cx, boolean forceExecution) {
		
		//create a new TestExecution this TestCampaignElement
		TestExecution testExecution = null;
		try {
			testExecution = TestExecutionFactory.createExecution(this);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (testExecution != null) {
			//register this new execution
			testExecution.setPreviousExecution(lastExecution);
			lastExecution = testExecution;
		
			// TODO configure logger for individual logfiles here
			
			//execute the TestExecutable
			testExecution.execute(sr, cx, forceExecution);
			
			// TODO deconfigure logger for individual logfiles here
			
		}
	
				
		
	}

	public TestCampaign getParent() {
		return parent;
	}

	public TestExecutable getExecutable() {
		return spec;
	}

}
