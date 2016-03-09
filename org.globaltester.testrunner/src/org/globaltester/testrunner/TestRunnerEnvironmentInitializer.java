package org.globaltester.testrunner;

import java.io.File;
import java.util.Enumeration;
import java.util.Iterator;

import org.eclipse.core.runtime.Platform;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.logger.TestLogger;
import org.globaltester.smartcardshell.ProtocolExtensions;
import org.globaltester.smartcardshell.ocf.OCFWrapper;
import org.globaltester.smartcardshell.preferences.PreferenceConstants;
import org.globaltester.smartcardshell.protocols.IScshProtocolProvider;
import org.mozilla.javascript.Context;

import opencard.core.service.CardServiceException;
import opencard.core.service.SmartCard;
import opencard.core.terminal.CardTerminal;
import opencard.core.terminal.CardTerminalException;
import opencard.core.terminal.CardTerminalRegistry;
import opencard.core.util.OpenCardPropertyLoadingException;

/**
 * This sets the environment for test runner script execution.
 * @author mboonk
 *
 */
public class TestRunnerEnvironmentInitializer {
	
	public static void setEnvironment(ScriptRunner runner) throws EnvironmentNotInitializedException{
		runner.addCleanupHook(new Runnable() {
			
			@Override
			public void run() {
				TestRunnerEnvironmentInitializer.cleanEnvironment();
			}
		});

		assert (SmartCard.isStarted());

		initOcf(runner);

		executeConfigJs(runner);
		
		// define variables
		setVariables(runner);

		
		// define AssertionError
		String cmd = "defineClass(\"org.globaltester.smartcardshell.gp.AssertionError\")";
		runner.exec(cmd, null, -1); //do not send "" as source filename, since Rhino debugger crashes in that case
		
		// handle extension points
		initExtensionPoints(runner);
		
		// load helper
		String jsHelperFile = org.globaltester.smartcardshell.Activator.getPluginDir().toPortableString()
				+ "jsHelper" + File.separator + "AllHelpers.js";
		File f = new File(jsHelperFile);
		runner.evaluateFile(f.getAbsolutePath());
	}
	
	private static void setVariables(ScriptRunner runner){
		// set the _reader and _manualReader variables
		boolean manualReaderSetting = Platform.getPreferencesService()
				.getBoolean(Activator.PLUGIN_ID,
						PreferenceConstants.OCF_MANUAL_READERSELECT, false,
						null);

		String currentReaderName = "";
		if (manualReaderSetting) {
			// get selected reader name from preferences
			String selectedReader = Platform.getPreferencesService().getString(
					Activator.PLUGIN_ID, PreferenceConstants.OCF_READER, "",
					null);

			// make sure that selected reader is available
			CardTerminalRegistry ctr = CardTerminalRegistry.getRegistry();
			Enumeration<?> ctlist = ctr.getCardTerminals();
			while (ctlist.hasMoreElements()) {
				CardTerminal ct = (CardTerminal) ctlist.nextElement();
				currentReaderName = ct.getName();
				if (currentReaderName.equals(selectedReader)) {
					break;
				}
				currentReaderName = "";
			}

		} else {
			currentReaderName = "";
		}

		String cmdReader = "_reader = \"" + currentReaderName + "\";";
		runner.exec(cmdReader);

		String cmdManualReader = "_manualReader = " + manualReaderSetting + ";";
		runner.exec(cmdManualReader);
		
		

		// init card variable
		String cmd = "card = new Card(_reader);";
		runner.exec(cmd, null, -1); // do not send "" as source filename,
									// since Rhino debugger crashes in that
									// case
		// set CardConfig as member variable
		cmd = "card.gt_cardConfig = _" + CardConfig.class.getCanonicalName().replace('.', '_') + ";";
		runner.exec(cmd, null, -1); // do not send "" as source filename,
									// since Rhino debugger crashes in that
									// case
	}

	private static void executeConfigJs(ScriptRunner runner) {
		File f;
		// execute SCSH config.js
		String sCSHconfigFile = org.globaltester.smartcardshell.Activator.getPluginDir().toPortableString()
				+ org.globaltester.smartcardshell.Activator.SCSH_FOLDER + File.separator + "config.js";
		if (Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID,
				PreferenceConstants.JS_CONF_MANUAL, false, null)) {
			sCSHconfigFile = Platform.getPreferencesService().getString(
					Activator.PLUGIN_ID, PreferenceConstants.JS_CONF_FILE,
					sCSHconfigFile, null);
		}
		f = new File(sCSHconfigFile);
		runner.evaluateFile(f.getAbsolutePath());
	}
	
	public static void cleanEnvironment(){
		TestLogger.debug("Shutting down card system...");
		try {
			SmartCard.shutdown();
		} catch (Exception e) {
			TestLogger.error(e);
		}
		try {
			OCFWrapper.shutdown();
		} catch (CardTerminalException e) {
			TestLogger.error(e);
		}
		Context.exit();
	}

	/**
	 * Create a card variable in the given Context and assign the CardConfig
	 * object.
	 * 
	 * @param cx
	 *            JS-Context to create the variable in
	 * @param varName
	 *            name of the variable in the context
	 * @param cardConfig
	 *            CardConfig object to be associated
	 */
	private static void initOcf(ScriptRunner runner) {
		try {
			OCFWrapper.start();
		} catch (OpenCardPropertyLoadingException | CardServiceException | CardTerminalException
				| ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

	}
	
	/**
	 * Create functions defined by protocols within the given Context.
	 * 
	 * @param cx
	 *            the context to install the protocols into
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
				// TODO following line should be optional (by
				// preference)
				cmd += "print(\"calling " + functionName + "\");\n";
				cmd += implementation + "\n";
				cmd += "}\n";
				
				runner.getContext().evaluateString(runner.getScope(), cmd, TestRunnerEnvironmentInitializer.class.getName(), -1, null);
			}
			
		}

	}
}
