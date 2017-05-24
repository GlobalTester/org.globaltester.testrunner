package org.globaltester.testrunner;

import org.globaltester.scriptrunner.EnvironmentNotInitializedException;
import org.globaltester.scriptrunner.ScriptRunner;

/**
 * This sets the environment for simple script execution.
 * 
 * @author mboonk
 *
 */
public class SimpleEnvironmentInitializer {

	private SimpleEnvironmentInitializer() {
		// Do not instantiate
	}

	/**
	 * Sets up the environment for test script execution. The only initialized
	 * variable will be the sampleConfig variable containing
	 * 
	 * @param runner
	 *            the {@link ScriptRunner} to modify
	 * @throws EnvironmentNotInitializedException
	 */
	public static void setEnvironment(ScriptRunner runner) throws EnvironmentNotInitializedException {
	}

}
