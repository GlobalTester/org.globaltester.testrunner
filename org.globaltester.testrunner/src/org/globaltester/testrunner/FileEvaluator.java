package org.globaltester.testrunner;

import java.io.File;

import org.eclipse.core.resources.IProject;

public interface FileEvaluator {
	public File getWorkingDirectory();
	
	public File getCurrentWorkingDir();

	public File getUserDir();

	public File getSystemDir();
	
	public void evaluateFile(String filename);
	
	public void evaluateFile(IProject parentProject, String filename);
}
