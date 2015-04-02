/**
 * 
 */
package org.globaltester.testrunner.ui.commands;

/**
 * Exactly the same currently as RunTestCommandHandler, but with different
 * default setting for debugMode
 * 
 * @author koelzer
 * 
 */
public class DebugTestCommandHandler extends RunTestCommandHandler {

	public DebugTestCommandHandler() {
		super();

		debugMode = true; //enable JavaScript debugging
	}

}
