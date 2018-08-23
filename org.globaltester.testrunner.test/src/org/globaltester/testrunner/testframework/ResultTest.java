package org.globaltester.testrunner.testframework;

import static org.junit.Assert.assertEquals;

import org.globaltester.testrunner.testframework.Result.Status;
import org.junit.Test;

public class ResultTest {
	@Test
	public void testAddSubResultAllPassed() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.PASSED));
		assertEquals(Status.PASSED, result.getStatus());
	}
	
	@Test
	public void testAddSubResultOneFailure() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.FAILURE));
		assertEquals(Status.FAILURE, result.getStatus());
	}
	
	@Test
	public void testAddSubResultOneRequirementMissing() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.REQUIREMENT_MISSING));
		assertEquals(Status.REQUIREMENT_MISSING, result.getStatus());
	}
	
	@Test
	public void testAddSubResultOneUndefined() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.UNDEFINED));
		assertEquals(Status.UNDEFINED, result.getStatus());
	}
	
	@Test
	public void testAddSubResultOneWarning() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.WARNING));
		assertEquals(Status.WARNING, result.getStatus());
	}
	
	@Test
	public void testAddSubResultFailurePriority() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.WARNING));
		result.addSubResult(new Result(Status.FAILURE));
		result.addSubResult(new Result(Status.REQUIREMENT_MISSING));
		result.addSubResult(new Result(Status.UNDEFINED));
		assertEquals(Status.FAILURE, result.getStatus());
	}
	
	@Test
	public void testAddSubResultRequirementMissingPriorityNoFailure() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.WARNING));
		result.addSubResult(new Result(Status.REQUIREMENT_MISSING));
		result.addSubResult(new Result(Status.UNDEFINED));
		assertEquals(Status.REQUIREMENT_MISSING, result.getStatus());
	}
	
	@Test
	public void testAddSubResultRequirementMissingPriorityNoWarning() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.REQUIREMENT_MISSING));
		result.addSubResult(new Result(Status.UNDEFINED));
		assertEquals(Status.REQUIREMENT_MISSING, result.getStatus());
	}
	
	@Test
	public void testAddSubResultOneNotApplicable() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.PASSED));
		result.addSubResult(new Result(Status.NOT_APPLICABLE));
		assertEquals(Status.PASSED, result.getStatus());
	}
	
	@Test
	public void testAddSubResultAllNotApplicable() {
		Result result = new Result(Status.PASSED);
		result.addSubResult(new Result(Status.NOT_APPLICABLE));
		result.addSubResult(new Result(Status.NOT_APPLICABLE));
		assertEquals(Status.NOT_APPLICABLE, result.getStatus());
	}
}
