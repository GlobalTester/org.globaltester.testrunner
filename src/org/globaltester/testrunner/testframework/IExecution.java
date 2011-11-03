package org.globaltester.testrunner.testframework;

import java.util.Collection;

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

}
