package org.globaltester.testrunner.testframework;

import org.globaltester.scriptrunner.RuntimeRequirementsProvider;
import org.globaltester.scriptrunner.ScriptRunner;

/**
 * This provides a {@link ScriptRunner} instance for test case execution.
 * @author mboonk
 *
 */
public interface ScriptRunnerProvider extends RuntimeRequirementsProvider{
	/**
	 * @return a {@link ScriptRunner} instance
	 */
	public ScriptRunner getScriptRunner();
}
