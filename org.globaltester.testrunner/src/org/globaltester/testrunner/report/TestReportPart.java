package org.globaltester.testrunner.report;

import java.util.ArrayList;

import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.testframework.ScriptIssue;

/**
 * Represents the results of a test case as shown in the report. This might
 * reflect the result of a TestCampaignElement, a TestCase or any other
 * IExecution. Naming is known to be not good yet and will be refactored
 * together with the according naming in the report xml schema.
 * 
 * @author amay
 * 
 */
public class TestReportPart {

	private String id;
	private String comment;
	private String description;
	private Status status;
	private double time;
	private ArrayList<ScriptIssue> scriptIssues = new ArrayList<>();

	public TestReportPart(IExecution iExecution) {
		id = iExecution.getId();
		comment = iExecution.getComment();
		description = iExecution.getDescription();
		status = iExecution.getStatus();
		time = iExecution.getDuration();
		
		addScriptIssues(iExecution.getResult());
	}

	private void addScriptIssues(Result result) {
		if (result instanceof ScriptIssue) {
			scriptIssues.add((ScriptIssue) result);
			return;
		}
		
		for (Result curSubResult : result.getSubResults()){
			addScriptIssues(curSubResult);
		}
		
	}

	public String getID() {
		return id;
	}

	public double getTime() {
		return time;
	}

	public String getDescription() {
		return description;
	}

	public Status getStatus() {
		return status;
	}

	public String getComment() {
		return comment;
	}

	public ArrayList<ScriptIssue> getScriptIssues() {
		return scriptIssues;
	}

}
