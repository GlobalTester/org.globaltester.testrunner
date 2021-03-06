package org.globaltester.testrunner;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.ResourcesPlugin;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.scriptrunner.ScshScope;
import org.globaltester.smartcardshell.ocf.PreferencesPropertyLoader;
import org.globaltester.smartcardshell.preferences.PreferenceInitializer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;

import opencard.core.service.SmartCard;

/**
 * Test on the implementation of the SkriptRunner
 * 
 * @author amay
 * 
 */
public class ScriptRunnerTest {
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		// make sure OCF is initialized with test values
		if (SmartCard.isStarted()) {
			SmartCard.shutdown();
		}
		
		System.setProperty("OpenCard.loaderClassName", org.globaltester.smartcardshell.test.TestPropertyLoader.class.getName());
		SmartCard.start();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		new PreferenceInitializer().initializeDefaultPreferences();
		PreferencesPropertyLoader.restartAndInitializeOCF();
	}

	@Test
	public void testInitialSampleConfig() throws Exception {
		// init JS ScriptRunner
		ScriptRunner sr = new ScriptRunner(ResourcesPlugin.getWorkspace().getRoot(), "", GtRuntimeRequirementsTest.getTestInstance());
		sr.init(new ScshScope(sr));
		TestRunnerEnvironmentInitializer.setEnvironment(sr);

		String result = Context.toString(sr.exec("card.gt_getSampleConfig(\"ICAO9303\",\"MRZ\")"));

		sr.close();
		// asserts
		assertEquals("Returned default MRZ does not match", "P<D<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<C11T002JM4D<<9608122F2310314<<<<<<<<<<<<<<<4", result);
	}

	/**
	 * Tests if the class loaders for smartcardshell protocol extensions
	 * were properly loaded using BAC as an example by executing a "new ...BAC()"
	 * command.
	 * @throws Exception if the "new" command could not properly be executed 
	 * (or sth. went wrong when activating the context) 
	 */
	@Test
	public void testProtocolClassLoader() throws RuntimeException {
		// init JS ScriptRunner
		ScriptRunner sr = new ScriptRunner(ResourcesPlugin.getWorkspace().getRoot(), "", GtRuntimeRequirementsTest.getTestInstance());
		sr.init(new ScshScope(sr));

		// If the class loader for BAC was not activated, this will throw an exception:
		String s = "new Packages.org.globaltester.smartcardshell.protocols.bac.BAC();";
		sr.exec(s);
		
		sr.close();
	}

}
