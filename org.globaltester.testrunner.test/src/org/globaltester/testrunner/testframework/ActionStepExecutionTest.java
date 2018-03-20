package org.globaltester.testrunner.testframework;

import static org.junit.Assert.*;

import org.globaltester.testrunner.testframework.Result.Status;
import org.junit.Test;

public class ActionStepExecutionTest {
	@Test
	public void testCheckForSubResultsFound() {
		Result result = new Result(Status.REQUIREMENT_MISSING);
		assertTrue(ActionStepExecution.checkForSubResults(result, Status.REQUIREMENT_MISSING));
	}
	
	@Test
	public void testCheckForSubResultsFoundRecursive() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.REQUIREMENT_MISSING));
		assertTrue(ActionStepExecution.checkForSubResults(result, Status.REQUIREMENT_MISSING));
	}

	@Test
	public void testCheckForSubResultsNotFound() {
		Result result = new Result(Status.PASSED);
		assertFalse(ActionStepExecution.checkForSubResults(result, Status.REQUIREMENT_MISSING));
	}
}
