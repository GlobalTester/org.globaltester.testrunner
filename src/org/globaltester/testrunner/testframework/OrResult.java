package org.globaltester.testrunner.testframework;

import java.util.Iterator;

/**
 * Special result that calculates the overall status as best result of all subResults. 
 * @author amay
 *
 */
public class OrResult extends Result {

	private static final long serialVersionUID = -8777975378795956356L;

	public OrResult(Status newStatus) {
		super(newStatus);
	}

	public OrResult(Status newStatus, String newComment) {
		super(newStatus, newComment);
	}

	/**
	 * Recalculate the overall status of this result from the subResults.
	 * 
	 * This returns the best status given by any sub result or PASSED if no
	 * subresult is present
	 */
	@Override
	void rebuildStatus() {
		Status tmpStatus = Status.FAILURE;

		// search best status in sub results
		Iterator<Result> subResultIter = subResults.iterator();
		while (subResultIter.hasNext()) {
			Result curResult = (Result) subResultIter.next();
			if (curResult.getStatus() == Status.WARNING) {
				tmpStatus = Status.WARNING;
			} else if (curResult.getStatus() == Status.PASSED) {
				tmpStatus = Status.PASSED;
				break;
			}
		}
		
		//set result to PASSED if no sub results are present
		if(!subResults.isEmpty()){
			tmpStatus = Status.PASSED;
		}

		// change status of this result
		status = tmpStatus;

	}

}
