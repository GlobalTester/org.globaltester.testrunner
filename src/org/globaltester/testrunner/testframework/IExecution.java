package org.globaltester.testrunner.testframework;

import java.util.Collection;

public interface IExecution {

	public abstract boolean hasChildren();
	
	public abstract Collection<IExecution> getChildren();
	
	public abstract IExecution getParent();

	public abstract String getName();

}
