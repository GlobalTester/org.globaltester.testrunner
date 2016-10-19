package org.globaltester.testrunner.testframework;

import org.globaltester.base.UserInteraction;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.RuntimeRequirementsProvider;
import org.globaltester.scriptrunner.SampleConfigProvider;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.scriptrunner.UserInteractionProvider;

/**
 * This is the default implementation of {@link RuntimeRequirementsProvider} for GlobalTester test case execution.
 * @author mboonk
 *
 */
public class TestCaseRuntimeProvider implements SampleConfigProvider, ScriptRunnerProvider, UserInteractionProvider {

	private ScriptRunner scriptRunner;
	private SampleConfigProvider sampleConfigProvider;
	private UserInteraction interaction;
	
	public TestCaseRuntimeProvider(ScriptRunner scriptRunner, SampleConfigProvider sampleConfigProvider, UserInteraction interaction) {
		this.scriptRunner = scriptRunner;
		this.sampleConfigProvider = sampleConfigProvider;
		this.interaction = interaction;
	}

	@Override
	public ScriptRunner getScriptRunner() {
		return scriptRunner;
	}

	@Override
	public SampleConfig getSampleConfig() {
		return sampleConfigProvider.getSampleConfig();
	}

	@Override
	public UserInteraction getUserInteraction() {
		return interaction;
	}

}
