package org.globaltester.testrunner.testframework;

import org.globaltester.testspecification.testframework.PreCondition;

public class PreConditionExecution extends ActionStepExecution {

	/**
	 * @param actionStep	PreCondition this execution instance should execute
	 */
	public PreConditionExecution(PreCondition actionStep, IExecution parent) {
		super(actionStep, parent);
	}

}
