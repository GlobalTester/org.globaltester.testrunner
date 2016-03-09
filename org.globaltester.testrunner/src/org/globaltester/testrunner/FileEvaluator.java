package org.globaltester.testrunner;

import java.io.File;

import org.eclipse.core.resources.IProject;

/**
 * Implementations of this interface are expected to evaluate script files and
 * keep the internal directories up to date.
 * 
 * @author mboonk
 *
 */
public interface FileEvaluator {
	public File getWorkingDirectory();

	public File getCurrentWorkingDir();

	public File getUserDir();

	public File getSystemDir();

	/**
	 * This evaluates a script file while updating the working directory
	 * accordingly.
	 * 
	 * @param filename
	 *            the script files path
	 */
	public void evaluateFile(String filename);

	/**
	 * This evaluates a script file while updating the working directory
	 * accordingly.
	 * 
	 * @param parentProject
	 * 
	 * @param filename
	 *            the script files path relative to the given parentProject
	 */
	public void evaluateFile(IProject parentProject, String filename);
}
