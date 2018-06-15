package org.globaltester.testrunner.testframework;

import org.eclipse.core.runtime.CoreException;
import org.jdom.Element;

public class PreConditionExecution extends ActionStepExecution {

	public static final String XML_ELEMENT = "PreConditionExecution";

	/**
	 * Constructor for new PreConditionExecution
	 * 
	 * @param parent parent execution (used to dereference the specification) 
	 * @param childId id of this child within parent
	 */
	public PreConditionExecution(IExecution parent, int childIndex) {
		super(parent, childIndex);
	}

	/**
	 * Constructor for new PreConditionExecution to be restored from XML
	 * 
	 * @param parent parent execution (used to dereference the specification) 
	 * @param childId id of this child within parent
	 * @throws CoreException 
	 */
	public PreConditionExecution(IExecution parent, int childIndex, Element xmlElement) throws CoreException {
		super(parent, childIndex);
		extractFromXml(xmlElement);
	}

	@Override
	public String getXmlRootElementName() {
		return XML_ELEMENT;
	}

}
