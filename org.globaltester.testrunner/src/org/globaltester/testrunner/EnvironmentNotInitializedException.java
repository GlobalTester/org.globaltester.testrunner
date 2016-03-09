package org.globaltester.testrunner;

/**
 * This {@link Exception} is intended to be thrown during script environment
 * initialization in case of not recoverable errors.
 * 
 * @author mboonk
 *
 */
public class EnvironmentNotInitializedException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public EnvironmentNotInitializedException(String message) {
		super(message);
	}

}
