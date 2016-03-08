package org.globaltester.testrunner.testframework;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.testrunner.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;

public interface IExecution {

	public abstract boolean hasChildren();
	
	public abstract Collection<IExecution> getChildren();
	
	public abstract IExecution getParent();

	public abstract String getName();

	public abstract String getComment();

	public abstract String getDescription();

	public abstract Status getStatus();

	public abstract double getTime();

	public abstract String getId();
	
	public abstract String getLogFileName();
	
	public abstract int getLogFileLine();

	public abstract Result getResult();

	public abstract void execute(ScriptRunner sr, boolean b,
			IProgressMonitor monitor);

}
