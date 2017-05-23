package org.globaltester.testrunner;

import org.globaltester.base.DummyUserInteraction;
import org.globaltester.base.UserInteraction;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.GtRuntimeRequirements;

public class GtRuntimeRequirementsTest {

	public static GtRuntimeRequirements getTestInstance() {
		UserInteraction ui = new DummyUserInteraction();
		SampleConfig sc = null;
		return new GtRuntimeRequirements(ui, sc);
	}

}
