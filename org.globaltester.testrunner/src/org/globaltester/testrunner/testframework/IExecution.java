package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.jdom.Element;

public interface IExecution {

	public abstract boolean hasChildren();

	public abstract Collection<IExecution> getChildren();

	public abstract String getId();

	public abstract String getComment();

	public abstract String getDescription();
	
	public abstract void putAdditionalInfo(String key, String value);
	public abstract String getAdditionalInfoValue(String key);
	public abstract Map<String, String> getAdditionalInfos();

	public abstract Status getStatus();
	
	public ITestExecutable getExecutable();

	/**
	 * Start time of this {@link IExecution}, formated as
	 * {@link java.util.Date#getTime()}
	 * 
	 * @return
	 */
	public abstract Long getStartTime();

	/**
	 * Duration of this {@link IExecution} in milliseconds
	 * 
	 * @return
	 */
	public long getDuration();

	public String getLogFileName();

	public int getLogFileLine();

	public Result getResult();

	public void execute(GtRuntimeRequirements runtimeReqs, boolean b, IProgressMonitor monitor);

	public void addResultListener(ResultChangeListener newListener);

	public void removeResultListener(ResultChangeListener obsoleteListener);

	public void notifyResultChangeListeners(IExecution changedObject);

	/**
	 * dump this instance to the given XML Element
	 * 
	 * @param root
	 */
	public void dumpToXml(Element root);

	/**
	 * extract data for this instance from XML Element
	 * 
	 * @param root
	 * @throws CoreException 
	 */
	public void extractFromXml(Element root) throws CoreException;

	/**
	 * Return the name of the XML root element describing an instance
	 * 
	 * @return
	 */
	public String getXmlRootElementName();

	static boolean isExecuted(IExecution execution) {
		return Status.isExecuted(execution.getStatus());
	}



}
