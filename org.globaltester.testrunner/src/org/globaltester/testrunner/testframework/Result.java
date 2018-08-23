package org.globaltester.testrunner.testframework;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
	public static final String STATUS_REQUIREMENT_MISSING = "REQUIREMENT_MISSING";

	private static final long serialVersionUID = 1690869079522455149L;
	
	public enum Status {
		PASSED(STATUS_PASSED,0), WARNING(STATUS_WARNING,2), FAILURE(STATUS_FAILURE,1), UNDEFINED(STATUS_UNDEFINED,4), NOT_APPLICABLE(STATUS_NOT_APPLICABLE,3), REQUIREMENT_MISSING(STATUS_REQUIREMENT_MISSING,5);
		
		private String textualRepresentation;
		private int rating;
		
		private Status(String textualRepresentation, int rating) {
			this.textualRepresentation = textualRepresentation;
			this.rating = rating;
		}
		
		@Override
		public String toString() {
			return getTextualRepresentation();
		}
		
		public String getTextualRepresentation() {
			return textualRepresentation;
		}
		
		public int getRating() {
			return rating;
		}
		
		/**
		 * This method returns a {@link Status} object for a matching String representation.
		 * If no match is found an IllegalArgumentException is thrown.
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
			
			throw new IllegalArgumentException("The value \"" + textualRepresentation + "\" does not represent a known status.");
		}

		public static boolean isExecuted(Status s) {
			return (s != NOT_APPLICABLE) && (s != UNDEFINED);
		}
		
	}

	public Status status; // status this result represents
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
	
	@Override
	public String toString() {
		return "Result overall status: " + getStatus();
	}

	/**
	 * Add a new subResult and rebuild the overall status.
	 * 
	 * Priority of the status during rebuilding is as follows:
	 * 
	 * <ol>
	 * 	<li>{@link Status#FAILURE}</li>
	 * 	<li>{@link Status#REQUIREMENT_MISSING}</li>
	 * 	<li>{@link Status#WARNING}</li>
	 * 	<li>{@link Status#UNDEFINED}</li>
	 * 	<li>{@link Status#PASSED}</li>
	 * 	<li>{@link Status#NOT_APPLICABLE}</li>
	 * </ol>
	 * 
	 * This decides the overall status after adding a new sub result.
	 * The overall status will be the highest priority status that is found in all sub results.
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
		Status tmpStatus = Status.NOT_APPLICABLE;

		Iterator<Result> subResultIter = subResults.iterator();
		while (subResultIter.hasNext()) {
			Status curStatus = ((Result) subResultIter.next()).getStatus();

			if (curStatus == Status.FAILURE) {
				tmpStatus = curStatus;
				break;
			}
			
			switch (tmpStatus) {
			case NOT_APPLICABLE:
				if (curStatus == Status.FAILURE || curStatus == Status.REQUIREMENT_MISSING || curStatus == Status.WARNING || curStatus == Status.UNDEFINED || curStatus == Status.PASSED) {
					tmpStatus = curStatus;
				}
				break;
			case PASSED:
				if (curStatus == Status.FAILURE || curStatus == Status.REQUIREMENT_MISSING || curStatus == Status.WARNING || curStatus == Status.UNDEFINED) {
					tmpStatus = curStatus;
				}
				break;
			case UNDEFINED:
				if (curStatus == Status.FAILURE || curStatus == Status.REQUIREMENT_MISSING || curStatus == Status.WARNING) {
					tmpStatus = curStatus;
				}
				break;
			case WARNING:
				if (curStatus == Status.FAILURE || curStatus == Status.REQUIREMENT_MISSING) {
					tmpStatus = curStatus;
				}
				break;
			case REQUIREMENT_MISSING:
				if (curStatus == Status.FAILURE) {
					tmpStatus = curStatus;
				}
				break;
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

	public List<Result> getSubResults() {
		return subResults;
	}

}
