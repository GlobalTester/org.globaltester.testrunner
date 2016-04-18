package org.globaltester.testrunner.testframework;

import org.globaltester.sampleconfiguration.SampleConfig;

/**
 * Default implementation of a {@link SampleConfigProvider} interface.
 * @author mboonk
 *
 */
public class SampleConfigProviderImpl implements SampleConfigProvider {

	private SampleConfig config;
	
	public SampleConfigProviderImpl(SampleConfig config) {
		this.config = config;
	}
	
	@Override
	public SampleConfig getSampleConfig() {
		return config;
	}

}
