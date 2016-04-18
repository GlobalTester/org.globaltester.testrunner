package org.globaltester.testrunner.testframework;

import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.ScriptRunner;

/**
 * This is the default implementation of {@link RuntimeRequirementsProvider} for GlobalTester test case execution.
 * @author mboonk
 *
 */
public class TestCaseRuntimeProvider implements SampleConfigProvider, ScriptRunnerProvider {

	private ScriptRunner scriptRunner;
	private SampleConfigProvider sampleConfigProvider;
	
	public TestCaseRuntimeProvider(ScriptRunner scriptRunner, SampleConfigProvider sampleConfigProvider) {
		this.scriptRunner = scriptRunner;
		this.sampleConfigProvider = sampleConfigProvider;
	}

	@Override
	public ScriptRunner getScriptRunner() {
		return scriptRunner;
	}

	@Override
	public SampleConfig getSampleConfig() {
		return sampleConfigProvider.getSampleConfig();
	}

}
