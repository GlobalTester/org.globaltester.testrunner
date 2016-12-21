package org.globaltester.testrunner.testframework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.jdom.Element;

/**
 * Represents the test result of an executable unit, e.g. of a
 * TestCaseExecution.
 * 
 * The result may consist of several subResults. In this case the overall result
 * will reflect the worst subResult. This behavior may be changed by subclasses.
 * 
 * @author amay
 * 
 */
public class Result implements Serializable{
	
	public static final String STATUS_PASSED = "PASSED";
	public static final String STATUS_FAILURE = "FAILURE";
	public static final String STATUS_WARNING = "WARNING";
	public static final String STATUS_NOT_APPLICABLE = "NOT APPLICABLE";
	public static final String STATUS_UNDEFINED = "UNDEFINED";

	private static final long serialVersionUID = 1690869079522455149L;

	public enum Status {
		PASSED(STATUS_PASSED), FAILURE(STATUS_FAILURE), WARNING(STATUS_WARNING), NOT_APPLICABLE(STATUS_NOT_APPLICABLE), UNDEFINED(STATUS_UNDEFINED);
		
		private String textualRepresentation;
		
		private Status(String textualRepresentation) {
			this.textualRepresentation = textualRepresentation;
		}
		
		@Override
		public String toString() {
			return textualRepresentation;
		}
		
		/**
		 * This method returns a {@link Status} object for a matching String representation.
		 * If no match is found null is returned.
		 * If more than one match is available, the first is returned.
		 * @param textualRepresentation a textual representation of the {@link Status} object
		 * @return the matched {@link Status} object
		 */
		public static Status get(String textualRepresentation) {
			for(Status currentStatus : Status.values()) {
				if(textualRepresentation.equals(currentStatus.toString())) {
					return currentStatus;
				}
			}
			
			return null;
		}
		
	}

	protected Status status; // status this result represents
	protected String comment; // comment associated with this result
	protected ArrayList<Result> subResults = new ArrayList<Result>();

	public Result(Status newStatus) {
		this(newStatus, "");
	}

	public Result(Status newStatus, String newComment) {
		this.status = newStatus;
		setComment(newComment);
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

	/**
	 * Add a new subResult and rebuild the overall status
	 * 
	 * @param result
	 */
	public void addSubResult(Result subResult) {
		subResults.add(subResult);
		rebuildStatus();
	}

	/**
	 * Recalculate the overall status of this result from the subResults.
	 * 
	 * By default this returns the worst status given by a sub result.
	 */
	void rebuildStatus() {
		Status tmpStatus = Status.PASSED;

		// search worst status in sub results
		Iterator<Result> subResultIter = subResults.iterator();
		iterationLoop: while (subResultIter.hasNext()) {
			Result curResult = (Result) subResultIter.next();
			switch (curResult.getStatus()) {
			case UNDEFINED:
				if (tmpStatus == Status.PASSED) {
					tmpStatus = Status.UNDEFINED;
				}
				break;
			case WARNING:
				tmpStatus = Status.WARNING;
				break;
			case FAILURE:
				tmpStatus = Status.FAILURE;
				break iterationLoop;
			default:
				break;
			}
		}
		
		//change status of this result
		status = tmpStatus;

	}

	public void dumpToXML(Element root) {
		Element statusElement = new Element("Status");
		statusElement.addContent(status.toString());
		root.addContent(statusElement);
		
		Element commentElement = new Element("Comment");
		commentElement.addContent(comment);
		root.addContent(commentElement);
		
	}

}
