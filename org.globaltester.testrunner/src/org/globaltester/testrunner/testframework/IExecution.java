package org.globaltester.testrunner.testframework;

import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.testframework.Result.Status;

public interface IExecution {

	public abstract boolean hasChildren();
	
	public abstract Collection<IExecution> getChildren();
	
	public abstract IExecution getParent();

	public abstract String getName();

	public abstract String getComment();

	public abstract String getDescription();

	public abstract Status getStatus();

	/**
	 * Start time of this {@link IExecution}, formated as {@link java.util.Date#getTime()}
	 * @return
	 */
	public abstract long getStartTime();
	
	/**
	 * Duration of this {@link IExecution} in milliseconds
	 * @return
	 */
	public abstract long getDuration();

	public abstract String getId();
	
	public abstract String getLogFileName();
	
	public abstract int getLogFileLine();

	public abstract Result getResult();

	public abstract void execute(GtRuntimeRequirements runtimeReqs, boolean b,
			IProgressMonitor monitor);

}
