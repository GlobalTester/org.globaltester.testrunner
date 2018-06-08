package org.globaltester.testrunner.testframework;

import org.globaltester.testrunner.testframework.Result.Status;
import org.jdom.Element;



public class ResultFactory {

	// explicit identifier for each failure starting with 1 
	private static int failureID = 1;

	/**
	 * Delivers a new failure id
	 * 
	 */
	public static int getNewFailureID() {
		return failureID++;
	}
	
	/**
	 * Create a new Failure, with a new ID 
	 * @param status
	 * @param scriptLine
	 * @param logFileLine
	 * @param failureText
	 * @return
	 */
	public static ScriptIssue newFailure(Status status, int scriptLine, int logFileLine, String failureText) {
		int failureID = getNewFailureID();
		ScriptIssue failure = new ScriptIssue(failureID, status, scriptLine, logFileLine, failureText);
		return failure;
	}
	
	/**
	 * Create a new Failure, with a new ID 
	 * @param status
	 * @param scriptLine
	 * @param logFileLine
	 * @param failureText
	 * @param expectedValue
	 * @param receivedValue
	 * @return
	 */
	public static ScriptIssue newFailure(Status status, int scriptLine, int logFileLine, String failureText, String expectedValue, String receivedValue) {
		int failureID = getNewFailureID();
		ScriptIssue failure = new ScriptIssue(failureID, status, scriptLine, logFileLine, failureText, expectedValue, receivedValue);
		return failure;
	}

	public static void reset() {
		failureID = 1;
	}

	public static Result newEmptyResult() {
		return new Result(Result.Status.UNDEFINED);
	}
	
	public static Result resultFromXML(Element resultElement) {
		Status status = null;
		String comment = null;
		
		//extract status
		Element statusElem = resultElement.getChild("Status");
		if (statusElem != null) {
			status = Status.get(statusElem.getTextTrim());
		}
		if (status == null) {
			throw new RuntimeException("XML-Element for Result does not contain valid Status Element");
		}
		
		//extract comment if any
		Element commentElem = resultElement.getChild("Comment");
		if (commentElem != null) {
			comment = commentElem.getTextTrim();
		}
		
		//return new result
		return new Result(status, comment);
	}

}
