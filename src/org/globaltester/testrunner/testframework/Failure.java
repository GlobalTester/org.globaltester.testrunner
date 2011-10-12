/*
 * Project GlobalTester File Failure.java
 * 
 * Date 19.10.2005
 * 
 * 
 * Developed by HJP Consulting GmbH Lanfert 24 33106 Paderborn Germany
 * 
 * 
 * This software is the confidential and proprietary information of HJP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * Non-Disclosure Agreement you entered into with HJP.
 */

package org.globaltester.testrunner.testframework;

import java.io.Serializable;

import org.globaltester.logging.logger.TestLogger;

/**
 * This class implements the information of a test failure.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
//TODO check code copied from GT2
public class Failure extends Result implements Serializable {

	private static final long serialVersionUID = -1031815873323547519L;

	//TODO replace following constants and mapping with enum type
	
	// constants defining failure Rating
	// do not change these values, as they are referred to in TestCase class for
	// further constants
	public static final int FAILURE = 1;
	public static final int WARNING = 2;
	
	//map failure ratings to strings
	public static final String[] RATING_STRINGS = new String[] { "undefined", "FAILURE",
		"WARNING"};

	// explicit identifier for each failure in test session and log file
	private int id;

	// failure must match one of TestCase.STATUS_WARNING or
	// TestCase.STATUS_FAILURE
	private int rating;

	// the line in script where the failure occured
	private int lineScript;

	// the line in log file where the failure occured
	private int lineLogFile;

	// failure describing text
	private String failureText;

	// expected value of this failure
	private String expectedValue;

	// received value of this failure
	private String receivedValue;

	/**
	 * Implements Failure constructor
	 * 
	 * @param id
	 *            explicit identifier for each failure in test session and log
	 *            file
	 * @param rating
	 *            failure could be "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the failure occured
	 * @param lineLogFile
	 *            the line in log file where the failure occured
	 * @param failureText
	 *            failure describing text
	 */
	public Failure(int id, int rating, int lineScript, int lineLogFile,
			String failureText) {
		this(id, rating, lineScript, lineLogFile, failureText, "", "");
	}

	/**
	 * Implements Failure constructor
	 * 
	 * @param id
	 *            explicit identifier for each failure in test session and log
	 *            file
	 * @param rating
	 *            failure could be "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the failure occured
	 * @param lineLogFile
	 *            the line in log file where the failure occured
	 * @param failureText
	 *            failure describing text
	 * @param expectedValue
	 *            expected value of this failure
	 * @param receivedValue
	 *            receivedValue
	 */
	public Failure(int id, int rating, int lineScript, int lineLogFile,
			String failureText, String expectedValue, String receivedValue) {
		super(Status.FAILURE);
		if ((rating != Failure.FAILURE) && (rating != Failure.WARNING))
			throw new RuntimeException("Failure rating must be either FAILURE or WARNING");
		this.id = id;
		this.rating = rating;
		this.lineScript = lineScript;
		this.lineLogFile = lineLogFile;
		this.failureText = failureText;
		this.expectedValue = expectedValue;
		this.receivedValue = receivedValue;
		TestLogger.info("@FailureID" + id + ":  " + failureText);		
	}

	/**
	 * Getter for failureID
	 * 
	 * @return failureID int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Getter for lineLogFile
	 * 
	 * @return lineLogFile int
	 */
	public int getLineLogFile() {
		return lineLogFile;
	}

	/**
	 * Setter for lineLogFile
	 * 
	 * @param lineLogFile
	 *            int
	 */
	public void setLineLogFile(int lineLogFile) {
		this.lineLogFile = lineLogFile;
	}

	/**
	 * Getter for lineScript
	 * 
	 * @return lineScript int
	 */
	public int getLineScript() {
		return lineScript;
	}

	/**
	 * Getter for rating
	 * 
	 * @return rating
	 */
	public int getRating() {
		return rating;
	}

	/**
	 * Return failureID when using toString()
	 * 
	 * @return failureID String
	 */
	public String toString() {
		return "FailureID" + id;
	}

	/**
	 * Getter for failureText
	 * 
	 * @return failureText String
	 */
	public String getFailureText() {
		return failureText;
	}

	/**
	 * Getter for expectedValue
	 * 
	 * @return expectedValue String
	 */
	public String getExpectedValue() {
		return expectedValue;
	}

	/**
	 * Getter for receivedValue
	 * 
	 * @return receivedValue String
	 */
	public String getReceivedValue() {
		return receivedValue;
	}

}
