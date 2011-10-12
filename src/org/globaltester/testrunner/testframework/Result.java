package org.globaltester.testrunner.testframework;


/**
 * Represents the test result of an executable unit, e.g. of a TestCase a
 * TestStep or even only a single line of code.
 * 
 * @author amay
 * 
 */
public class Result {
	

	public enum Status {
		PASSED,
		FAILURE,
		WARNING,
		NOT_APPLICABLE,
		ABORTED,
		SKIPPED,
		RESUMED,
		UNDEFINED;
		
	}

	private Status status; // status this result represents
	private String comment; // comment associated with this result

	public Result(Status newStatus) {
		this.status = newStatus;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Status getStatus() {
		return status;
	}
	
	

}
