package org.globaltester.testrunner;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.globaltester.logger.TestLogger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import de.cardcontact.scdp.js.GPRuntime;
import de.cardcontact.scdp.js.GPTracer;

/**
 * This is used as a top level scope for the scripting environment.
 * @author mboonk
 *
 */
public class ScshScope extends ImporterTopLevel implements GPRuntime {

	private static final long serialVersionUID = 1L;
	private FileEvaluator fileEvaluator;

	public ScshScope(FileEvaluator fileEvaluator) {
		this.fileEvaluator = fileEvaluator;
	}

	/**
	 * Load and execute script files
	 * 
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 */
	public static void load(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {		
		if (args.length > 0){
			IProject project = findProjectWithName(Context.toString(args[0]));
			if (project != null){
				for (int i = 1; i < args.length; i++) {
					String fileName = Context.toString(args[i]);
					ScriptRunner.getRunnerForContext(cx).evaluateFile(project, fileName);
				}
				return;
			}
			
			for (int i = 0; i < args.length; i++) {
				String fileName = Context.toString(args[i]);
				ScriptRunner.getRunnerForContext(cx).evaluateFile(fileName);
			}
		}
	}

	/**
	 * Evaluate javascript in this context
	 * 
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 */
	public static void eval(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {

		ScriptRunner shell = (ScriptRunner) getTopLevelScope(thisObj);
		
		if (args.length == 1){
			shell.exec(Context.toString(args[0]));
		}
	}

	/**
	 * Returns the scriptrunner for the calling javascript context
	 * 
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 */
	public static ScriptRunner getRunnerInstance(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {

		return ScriptRunner.getRunnerForContext(cx);
	}
	
	/**
	 * @param name
	 * @return the project with the given name or null if no such project could
	 *         be found
	 */
	private static IProject findProjectWithName(String name) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (IProject project : projects) {
			if (project.getName().equals(name)) {
				return project;
			}
		}
		return null;
	}
	
	/**
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 * @return the OS dependent file path for the given workspace project
	 */
	public static String getAbsolutePathForProject(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {
		
		if (args.length > 0){
			IProject project = findProjectWithName(Context.toString(args[0]));
			if (project != null){
				return project.getLocation().toOSString();
			}
		}
		return null;
	}

	/**
	 * Load a Java Class defining an ECMAScript native object
	 * 
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws InvocationTargetException
	 * @throws ClassNotFoundException
	 */
	@SuppressWarnings("unchecked") //reflection requires unchecked conversion
	public static void defineClass(Context cx, Scriptable thisObj,
			Object[] args, Function funObj) throws IllegalAccessException,
			InstantiationException, InvocationTargetException,
			ClassNotFoundException {

		String name = Context.toString(args[0]);
		Class<Scriptable> clazz = (Class<Scriptable>) Class.forName(name);
		ScriptableObject.defineClass(thisObj, clazz);
	}

	/**
	 * Print value
	 * 
	 * @param cx
	 *            Context from runtime
	 * @param thisObj
	 *            Object for which method is called
	 * @param args
	 *            Arguments passed to method call
	 * @param funObj
	 *            Function object associated with this method call
	 */
	public static void print(Context cx, Scriptable thisObj, Object[] args,
			Function funObj) {

		for (int i = 0; i < args.length; i++) {
			// Convert the arbitrary JavaScript value into a string form.
			String str = Context.toString(args[i]);

			TestLogger.info(str + " ");
		}
	}

	/**
	 * Search for a file and return the system specific absolute path
	 * 
	 * @param filename
	 *            Filename
	 * @param location
	 *            Locations to search
	 */
	public String mapFilename(String filename, int location) {

		File file = new File(filename);

		if (file.isAbsolute()) {
			return filename;
		}

		String fn = null;

		switch (location) {

		case GPRuntime.CWD:
			file = new File(fileEvaluator.getCurrentWorkingDir(), filename);
			fn = file.getAbsolutePath();
			break;

		case GPRuntime.USR:
			file = new File(fileEvaluator.getUserDir(), filename);
			fn = file.getAbsolutePath();
			break;

		case GPRuntime.SYS:
			file = new File(fileEvaluator.getSystemDir(), filename);
			fn = file.getAbsolutePath();
			break;

		case GPRuntime.AUTO:
			fn = mapFilename(filename, GPRuntime.CWD);
			if (!new File(fn).exists()) {
				fn = mapFilename(filename, GPRuntime.USR);
				if (!new File(fn).exists()) {
					fn = mapFilename(filename, GPRuntime.SYS);
					if (!new File(fn).exists()) {
						fn = null;
					}
				}
			}
			break;

		default:
			fn = null;
		}

		return fn;
	}
	
	@Override
	public byte[] getSystemID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrintStream getTracePrintStream() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GPTracer getTracer() {
		// TODO Auto-generated method stub
		return null;
	}

}
