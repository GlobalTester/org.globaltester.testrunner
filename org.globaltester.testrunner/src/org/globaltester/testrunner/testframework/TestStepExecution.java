package org.globaltester.testrunner.testframework;

import org.eclipse.core.runtime.CoreException;
import org.jdom.Element;

public class TestStepExecution extends ActionStepExecution {

	public static final String XML_ELEMENT = "TestStepExecution";

	/**
	 * Constructor for new TestStepExecution
	 * 
	 * @param parent parent execution (used to dereference the specification) 
	 * @param childId id of this child within parent
	 */
	public TestStepExecution(IExecution parent, int childIndex) {
		super(parent, childIndex);
	}

	/**
	 * Constructor for new TestStepExecution to be restored from XML
	 * 
	 * @param parent parent execution (used to dereference the specification) 
	 * @param childId id of this child within parent
	 * @throws CoreException 
	 */
	public TestStepExecution(IExecution parent, int childIndex, Element xmlElement) throws CoreException {
		super(parent, childIndex);
		extractFromXml(xmlElement);
	}

	@Override
	public String getXmlRootElementName() {
		return XML_ELEMENT;
	}

}
