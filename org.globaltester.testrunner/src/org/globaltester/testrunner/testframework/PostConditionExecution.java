package org.globaltester.testrunner.testframework;

import org.globaltester.testspecification.testframework.PostCondition;

public class PostConditionExecution extends ActionStepExecution {

	/**
	 * @param actionStep	PostCondition this execution instance should execute
	 */
	public PostConditionExecution(PostCondition actionStep, IExecution parent) {
		super(actionStep, parent);
	}

	@Override
	protected String getXmlRootElementName() {
		return "PostConditionExecution";
	}

}
