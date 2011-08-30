package org.globaltester.testrunner.testframework;


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
	 * Create a new Failure, with a new ID, 
	 * @param rating
	 * @param scriptLine
	 * @param logFileLine
	 * @param failureText
	 * @return
	 */
	public static Failure newFailure(int rating, int scriptLine, int logFileLine,
			String failureText) {
		int failureID = getNewFailureID();
		// log.info(failureText);
		Failure failure = new Failure(failureID, rating, scriptLine, logFileLine,
				failureText);
		return failure;
	}

}
