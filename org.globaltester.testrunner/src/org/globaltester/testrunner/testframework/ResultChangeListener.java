package org.globaltester.testrunner.testframework;

/**
 * Listener interface that gets properly notified upon changes of TestExecution
 * results
 * 
 * @author amay
 *
 */
public interface ResultChangeListener {

	/**
	 * This method gets called whenever a TestExecutionResult changed so that
	 * Listeners can update their internal sttus
	 */
	public void resultChanged();

}
