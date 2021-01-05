package org.globaltester.testrunner;

import java.io.File;
import java.util.Iterator;

import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.EnvironmentNotInitializedException;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.smartcardshell.GTWrapFactory;
import org.globaltester.smartcardshell.ProtocolExtensions;
import org.globaltester.smartcardshell.preferences.SmartCardShellInfo;
import org.globaltester.smartcardshell.protocols.IScshProtocolProvider;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.WrapFactory;

import opencard.core.service.CardServiceException;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CardTerminalException;
import opencard.core.util.OpenCardPropertyLoadingException;

/**
 * This sets the environment for test runner script execution.
 * @author mboonk
 *
 */
public class TestRunnerEnvironmentInitializer {
	
	private TestRunnerEnvironmentInitializer() {
		//Do not instantiate
	}
	
	/**
	 * Sets up the environment for test script execution.
	 * The OCF will be initialized using the {@link OCFWrapper} and {@link SmartCard} services.
	 * Additionally the SCSH config file will be executed. This is either the default configuration
	 * delivered with the SCSH or a config file set by the user in preferences.
	 * 
	 * Global variables are set up as defined in {@link #setVariables(ScriptRunner)}.
	 * 
	 * Additionally the AllHelpers.js file from the org.globaltester.smartcardshell bundle is loaded
	 * into the context.
	 * 
	 * @param runner the {@link ScriptRunner} to modify
	 * @throws EnvironmentNotInitializedException
	 */
	public static void setEnvironment(ScriptRunner runner) throws EnvironmentNotInitializedException{
		runner.addCleanupHook(new Runnable() {
			
			@Override
			public void run() {
				TestRunnerEnvironmentInitializer.cleanEnvironment();
			}
		});
		
		WrapFactory wf = new GTWrapFactory();
		runner.getContext().setWrapFactory(wf);

		try {
			SmartCardShellInfo.startSmartcard();
		} catch (OpenCardPropertyLoadingException | CardServiceException | CardTerminalException
				| ClassNotFoundException e) {
			TestLogger.error(e);
		}

		// execute SCSH config.js
		String sCSHconfigFileName = SmartCardShellInfo.getConfigFile();
		File scshConfigFile = new File(sCSHconfigFileName);
		runner.evaluateFile(scshConfigFile.getAbsolutePath());
		
		// define variables
		setVariables(runner);

		
		// define AssertionError
		String cmd = "defineClass(\"org.globaltester.smartcardshell.gp.AssertionError\")";
		runner.exec(cmd, null, -1); //do not send "" as source filename, since Rhino debugger crashes in that case
		
		// handle extension points
		initExtensionPoints(runner);
		
		// load helper
		String jsHelperFileName = SmartCardShellInfo.getPluginDir().toPortableString() + "jsHelper" + File.separator + "AllHelpers.js";
		File jsHelperFile = new File(jsHelperFileName);
		runner.evaluateFile(jsHelperFile.getAbsolutePath());
	}
	
	/**
	 * This sets several variables for use in the javascript context:</br>
	 * 
	 * <code>_reader</code> - The currently used smart card reader name</br>
	 * <code>_manualReader</code> - Indicates manual override of the auto reader selection</br>
	 * <code>card</code> - The SCDP card object used for APDU transmission</br>
	 * <code>card.gt_sampleConfig</code> - The {@link SampleConfig} object for this test case execution</br>
	 * 
	 * @param runner
	 */
	private static void setVariables(ScriptRunner runner){
		// set the _reader and _manualReader variables
		boolean manualReaderSetting = SmartCardShellInfo.isManualReader();
		
		String currentReaderName = SmartCardShellInfo.setActiveReaderName();
		String cmdReader = "_reader = \"" + currentReaderName + "\";";
		runner.exec(cmdReader);
		TestLogger.info("Active card reader: " + currentReaderName);
		
		String cmdManualReader = "_manualReader = " + manualReaderSetting + ";";
		runner.exec(cmdManualReader);
		
		// init card variable
		String cmd = "card = new Card(_reader);";
		runner.exec(cmd, null, -1); // do not send "" as source filename,
									// since Rhino debugger crashes in that
									// case
		// set SampleConfig as member variable
		cmd = "card.gt_sampleConfig = getRunnerInstance().getRuntimeRequirement(Packages.org.globaltester.sampleconfiguration.SampleConfig);";
		runner.exec(cmd, null, -1); // do not send "" as source filename,
									// since Rhino debugger crashes in that
									// case
	}
	
	public static void cleanEnvironment(){
		TestLogger.debug("Shutting down card system...");
		try {
			SmartCardShellInfo.shutdownSmartcard();
		} catch (Exception e) {
			TestLogger.error(e);
		}
		if (Context.getCurrentContext() != null){
			Context.exit();	
		}
	}
	
	/**
	 * Create functions defined by protocols within the given Context.
	 * 
	 * @param runner
	 *            the ScriptRunner to install the protocols into
	 */
	private static void initExtensionPoints(ScriptRunner runner) {
		for (IScshProtocolProvider curProtocolProvider : ProtocolExtensions.getInstance().getAllAvailableProtocols()) {
			runner.addClassLoader(curProtocolProvider.getClass().getClassLoader());
			for (Iterator<String> commandIter = curProtocolProvider
					.getCommands().iterator(); commandIter.hasNext();) {
				// extract name of current command
				String curCommand = commandIter.next();

				// extract list of parameters
				String paramList = "";
				Iterator<String> paramIter = curProtocolProvider
						.getParams(curCommand).iterator();
				while ((paramIter != null) && (paramIter.hasNext())) {
					paramList += ", " + paramIter.next();
				}
				if (paramList.length() > 0) {
					paramList = paramList.substring(2);
				}

				// extract implementation of current command
				String implementation = curProtocolProvider
						.getImplementation(curCommand);

				String functionName = "gt_" + curProtocolProvider.getName() + "_"
						+ curCommand;
				// build and execute the command
				String cmd = "";
				cmd += "Card.prototype." + functionName
						+ " = function(" + paramList + ") {\n";
				cmd += "print(\"calling " + functionName + "\");\n";
				cmd += implementation + "\n";
				cmd += "}\n";
				
				runner.getContext().evaluateString(runner.getScope(), cmd, TestRunnerEnvironmentInitializer.class.getName(), -1, null);
			}
			
		}

	}
}
