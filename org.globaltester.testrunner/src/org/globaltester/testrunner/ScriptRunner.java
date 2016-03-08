/*
 * Project GlobalTester File ScriptRunner.java
 * 
 * Date 16.09.2005
 * 
 * 
 * Developed by HJP Consulting GmbH Lanfert 24 33106 Paderborn Germany
 * 
 * 
 * This software is the confidential and proprietary information of HJP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * Non-Disclosure Agreement you entered into with HJP.
 */
package org.globaltester.testrunner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.globaltester.logger.TestLogger;
import org.globaltester.smartcardshell.jsinterface.RhinoJavaScriptAccess;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

/**
 * This class implements the methods of an internal shell.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
public class ScriptRunner implements FileEvaluator {

	private static Map<Context, ScriptRunner> runners = new HashMap<>();

	// Script context of this shell
	private Context context;

	// streams of this shell
	PrintStream ostream = null; // Stream to send output to
	InputStream istream = null; // Stream to read input from
	PrintStream estream = null; // Stream to send errors to

	// Active directories
	private File currentWorkingDir; // Current working directory
	private File userDir; // User directory
	private File systemDir; // System (executable) directory

	private Map<Class<?>, Object> configurationObjects;
	private List<Runnable> cleanupHooks = new LinkedList<>();

	private ScriptableObject scope;

	/**
	 * Create ScriptRunner
	 * 
	 * @param scriptPath
	 *            path where the scripts could be found
	 */
	public ScriptRunner(String scriptPath, Map<Class<?>, Object> configurationObjects) {
		currentWorkingDir = new File(scriptPath);
		this.configurationObjects = configurationObjects;
	}

	/**
	 * Return class name to identify different shells
	 * 
	 * @return className
	 */
	public String getClassName() {
		return "ScriptRunner";
	}

	/**
	 * Initialisation of this shell. Create new Card object and set the
	 * environment.
	 */
	public void init(ScriptableObject scope) {
		this.scope = scope;
		try {
			userDir = currentWorkingDir;
			systemDir = userDir;
			// Initialize ECMAScript environment
			RhinoJavaScriptAccess rhinoAccess = new RhinoJavaScriptAccess();
			context = rhinoAccess.activateContext();
			ScriptRunner.runners.put(context, this);

			addClassLoader(this.getClass().getClassLoader());

			WrapFactory wf = new GTWrapFactory();
			context.setWrapFactory(wf);
			context.initStandardObjects(scope);

			String[] names = { "print", "load", "defineClass", "getAbsolutePathForProject", "eval",
					"getRunnerInstance" };

			scope.defineFunctionProperties(names, scope.getClass(), ScriptableObject.DONTENUM);

			injectConfiguration();
		} catch (Exception e) {
			TestLogger.error(makeExceptionMessage(e));
		}
	}

	private void injectConfiguration() {
		for (Class<?> key : configurationObjects.keySet()) {
			Object wrappedCardConfig = Context.javaToJS(configurationObjects.get(key), scope);
			ScriptableObject.putProperty(scope, "_" + key.getCanonicalName().replace('.', '_'), wrappedCardConfig);
		}
	}

	public void addClassLoader(ClassLoader loader) {
		RhinoJavaScriptAccess.getClassLoader().addClass(loader);
	}

	/**
	 * Shut down card system
	 * 
	 */
	public void close() {
		runners.remove(context);
		for (Runnable runnable : cleanupHooks) {
			runnable.run();
		}
	}

	/**
	 * Turn various exceptions into a single string
	 * 
	 * @param e
	 *            Exception object
	 * @return String with meaningful message
	 */
	public static String makeExceptionMessage(Exception e) {
		if (e instanceof JavaScriptException) {
			JavaScriptException jse = (JavaScriptException) e;
			// Some form of JavaScript error.
			return (jse.getMessage());
		} else {
			return (e.toString());
		}
	}

	public Object exec(String command) throws JavaScriptException {
		return exec(command, "ScriptRunner", 1);
	}

	/**
	 * Execute command in this shell
	 * 
	 * @param command
	 *            command that should be executed
	 * @return object result of executed command
	 */
	public Object exec(String command, String sourceName, int lineNumber) throws JavaScriptException {

		if (!context.stringIsCompilableUnit(command)) {
			System.out.println("Error: Command is not compilable unit!");
			System.out.println("Command: " + command);
			return null;
		}

		Object result = context.evaluateString(scope, command, sourceName, lineNumber, null);

		// If the evaluator returns a function object, then we call
		// this object with the last result as an argument
		if (result instanceof Function) {
			Function fo = (Function) result;
			Object lastresult = scope.get("lastresult", scope);

			if (lastresult != null) {
				Object args[] = { lastresult };
				result = fo.call(context, scope, scope, args);
			} else {
				Object args[] = {};
				result = fo.call(context, scope, scope, args);
			}
		}

		if (result != Context.getUndefinedValue()) {
			scope.put("lastresult", scope, result);
			// FIXME Is this really needed? Remove if possible
			Context.toString(result);
		}
		return result;
	}

	/**
	 * Load and evaluate file
	 * 
	 * @param cx
	 *            current context
	 * @param filename
	 *            file to load and execute
	 */
	public void evaluateFile(String filename) {
		evaluateFile(null, filename);
	}

	/**
	 * Load and evaluate file
	 * 
	 * @param cx
	 *            current context
	 * @param parentProject
	 *            the parent project the given filename is relative to
	 * @param filename
	 *            file to load and execute
	 */
	public void evaluateFile(IProject parentProject, String filename) {

		FileReader in = null;

		File file = new File(filename);
		
		if (parentProject != null) {
			file = new File(parentProject.getLocation().toOSString() + File.separator + filename);
		} else {
			if (!file.isAbsolute()){
				file = new File(this.currentWorkingDir + File.separator + filename);	
			}
		}

		File oldCWD = this.currentWorkingDir;
		this.currentWorkingDir = file.getParentFile();

		try {
			in = new FileReader(file);
		}

		catch (Exception e) {
			this.currentWorkingDir = oldCWD;
			Context.reportError(e.toString());
			return;
		}

		try {
			Object result = context.evaluateReader(scope, in, file.getAbsolutePath(), 1, null);

			if (result != Context.getUndefinedValue()) {
				scope.put("lastresult", scope, result);
			}
		}

		catch (IOException e) {
			estream.println(e);
		}

		finally {
			this.currentWorkingDir = oldCWD;
			try {
				in.close();
			} catch (Exception e) {
				estream.println(e);
			}
		}
	}

	/**
	 * Returns the current working directory
	 * 
	 * @return working directory File
	 */
	public File getWorkingDirectory() {
		return currentWorkingDir;
	}

	public File getCurrentWorkingDir() {
		return currentWorkingDir;
	}

	public File getUserDir() {
		return userDir;
	}

	public File getSystemDir() {
		return systemDir;
	}

	public void addCleanupHook(Runnable hook) {
		cleanupHooks.add(hook);
	}

	public Context getContext() {
		return this.context;
	}

	public Scriptable getScope() {
		return scope;
	};

	public static ScriptRunner getRunnerForContext(Context context) {
		if (!runners.containsKey(context)) {
			throw new IllegalArgumentException("No runner is registered for this context");
		}
		return runners.get(context);
	}

}
