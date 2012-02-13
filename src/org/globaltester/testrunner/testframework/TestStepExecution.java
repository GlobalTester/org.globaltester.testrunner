package org.globaltester.testrunner.testframework;

import org.globaltester.testspecification.testframework.TestStep;

public class TestStepExecution extends ActionStepExecution {

	/**
	 * @param actionStep	TestStep this execution instance should execute
	 */
	public TestStepExecution(TestStep actionStep, IExecution parent) {
		super(actionStep, parent);
	}

}
