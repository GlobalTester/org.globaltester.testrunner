package org.globaltester.testrunner.testframework;

import org.globaltester.sampleconfiguration.SampleConfig;

/**
 * This provides {@link SampleConfig} objects for test case executions.
 * @author mboonk
 *
 */
public interface SampleConfigProvider extends RuntimeRequirementsProvider {
	/**
	 * @return a {@link SampleConfig} object
	 */
	public SampleConfig getSampleConfig();
}
