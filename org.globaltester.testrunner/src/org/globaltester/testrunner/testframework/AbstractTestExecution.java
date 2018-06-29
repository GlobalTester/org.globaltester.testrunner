package org.globaltester.testrunner.testframework;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.jdom.Element;

public abstract class AbstractTestExecution implements IExecution {

	private static final String XML_TAG_ID = "ID";
	private static final String XML_TAG_COMMENT = "Comment";
	private static final String XML_TAG_DESCRIPTION = "Description";
	private static final String XML_TAG_LAST_EXECUTION_LOG_FILE_LINE = "LastExecutionLogFileLine";
	private static final String XML_TAG_LAST_EXECUTION_LOG_FILE_NAME = "LastExecutionLogFileName";
	private static final String XML_TAG_LAST_EXECUTION_RESULT = "LastExecutionResult";
	private static final String XML_TAG_LAST_EXECUTION_DURATION = "LastExecutionDuration";
	private static final String XML_TAG_LAST_EXECUTION_START_TIME = "LastExecutionStartTime";
	
	private String id;
	private String description;
	private String comment;


	public AbstractTestExecution(String id) {
		this.id = id;
	}
	
	public AbstractTestExecution(String id, String description, String comment) {
		this(id);
		this.description = description;
		this.comment = comment;
	}

	public AbstractTestExecution() {
		this("no id set");
	}
	
	public AbstractTestExecution(ITestExecutable testExecutable) {
		this(testExecutable.getName());
	}

	@Override
	public String getId() {
		return id;
	}

	protected void setId(String id) {
		this.id = id;
	}

	@Override
	public String getComment() {
		return comment;
	}

	protected void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String getDescription() {
		return description;
	}
	
	protected void setDescription(String description) {
		this.description = description;
	}







	protected Result result = ResultFactory.newEmptyResult();

	// store time and duration of last execution
	protected long lastExecutionStartTime = 0;
	protected long lastExecutionDuration = 0;
	private String logFileName = "unknown";
	private int logFileLine;
	private String executingUser = "unknown";

	public Result getResult() {
		return result;
	}
	
	public void setLogFileName(String logFileName){
		this.logFileName = logFileName;
	}
	
	public String getLogFileName(){
		return logFileName;
	}
	
	public int getLogFileLine(){
		return logFileLine;
	}

	public void setLogFileLine(int logFileLine) {
		this.logFileLine = logFileLine;
	}

	@Override
	public void dumpToXml(Element root) {
		
		Element idElement = new Element(XML_TAG_ID);
		idElement.addContent(id);
		root.addContent(idElement);
		
		if (description != null) {
			Element descriptionElement = new Element(XML_TAG_DESCRIPTION);
			descriptionElement.addContent(description);
			root.addContent(descriptionElement);
		}
		
		if (comment != null) {
			Element commentElement = new Element(XML_TAG_COMMENT);
			commentElement.addContent(comment);
			root.addContent(commentElement);
		}
		
		Element startTimeElement = new Element(XML_TAG_LAST_EXECUTION_START_TIME);
		startTimeElement.addContent(Long.toString(lastExecutionStartTime));
		root.addContent(startTimeElement);

		Element durationElement = new Element(XML_TAG_LAST_EXECUTION_DURATION);
		durationElement.addContent(Long.toString(lastExecutionDuration));
		root.addContent(durationElement);
		
		Element resultElement = new Element(XML_TAG_LAST_EXECUTION_RESULT);
		getResult().dumpToXML(resultElement);
		root.addContent(resultElement);
		
		Element logFileNameElement = new Element(XML_TAG_LAST_EXECUTION_LOG_FILE_NAME);
		logFileNameElement.addContent(logFileName);
		root.addContent(logFileNameElement);
		
		Element logFileLineElement = new Element(XML_TAG_LAST_EXECUTION_LOG_FILE_LINE);
		logFileLineElement.addContent(Long.toString(logFileLine));
		root.addContent(logFileLineElement);
	}

	@Override
	public void extractFromXml(Element root) throws CoreException {
		
		Element idElem = root.getChild(XML_TAG_ID);
		if (idElem != null) {
			id = idElem.getTextTrim();
		}

		Element descriptionElem = root.getChild(XML_TAG_DESCRIPTION);
		if (descriptionElem != null) {
			description = descriptionElem.getTextTrim();
		}
		
		Element commentElem = root.getChild(XML_TAG_COMMENT);
		if (commentElem != null) {
			comment = commentElem.getTextTrim();
		}
		
		Element timeElem = root.getChild(XML_TAG_LAST_EXECUTION_START_TIME);
		if (timeElem != null) {
			lastExecutionStartTime = Long.valueOf(timeElem.getTextTrim());
		}
		
		Element durationElem = root.getChild(XML_TAG_LAST_EXECUTION_DURATION);
		if (durationElem != null) {
			lastExecutionDuration = Long.valueOf(durationElem.getTextTrim());
		}
		
		Element resultElem = root.getChild(XML_TAG_LAST_EXECUTION_RESULT);
		if (resultElem != null) {
			result = ResultFactory.resultFromXML(resultElem);
		}
		
		Element logFileNameElement = root.getChild(XML_TAG_LAST_EXECUTION_LOG_FILE_NAME);
		if (logFileNameElement != null) {
			logFileName = logFileNameElement.getTextTrim();
		}
		
		Element logFileLineElement = root.getChild(XML_TAG_LAST_EXECUTION_LOG_FILE_LINE);
		if (logFileLineElement != null) {
			logFileLine = Integer.valueOf(logFileLineElement.getTextTrim());
		}

	}

	/**
	 * (Re)Execute the code associated with this test execution
	 * 
	 * @param runtimeReqs
	 *            The {@link GtRuntimeRequirements} to deliver all needed
	 *            data and functions for this execution
	 * @param forceExecution
	 *            if true the code is executed regardless if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 */
	public void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution, IProgressMonitor monitor) {
		// set the execution time
		boolean reExecution = lastExecutionStartTime != 0;

		lastExecutionStartTime = new Date().getTime();
		
		//set the log file
		setLogFileName(TestLogger.getTestCaseLogFileName());
		setLogFileLine(TestLogger.getLogFileLine());
		
		// forward the execution to the implementing class
		execute(runtimeReqs, forceExecution, reExecution, monitor);

		// calculate execution duration
		lastExecutionDuration = new Date().getTime() - lastExecutionStartTime;
		
		executingUser = System.getProperty("user.name");
		
		notifyResultChangeListeners(this);
		
	}

	/**
	 * (Re)Execute the code associated with this test execution
	 * 
	 * @param runtimeReqs
	 *            The {@link GtRuntimeRequirements} to deliver all needed
	 *            data and functions for this execution
	 * @param forceExecution
	 *            if true the code is executed regardless if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 * @param monitor 
	 */
	protected abstract void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution, boolean reExecution, IProgressMonitor monitor);

	public long getLastExecutionStartTime() {
		return lastExecutionStartTime;
	}
	
	@Override
	public long getStartTime() {
		return getLastExecutionStartTime();
	}

	public String getLastExecutionStartTimeAsString() {
		return DateFormat.getDateTimeInstance().format(
				new Date(getLastExecutionStartTime()));
	}
	
	public long getLastExecutionDuration() {
		return lastExecutionDuration;
	}
	
	@Override
	public long getDuration() {
		return getLastExecutionDuration();
	}

	@Override
	public Status getStatus() {
		return result.getStatus();
	}
	
	public String getExecutingUser() {
		return executingUser;
	}

	HashSet<ResultChangeListener> resultChangeListeners = new HashSet<>();
	
	@Override
	public void addResultListener(ResultChangeListener newListener) {
		resultChangeListeners.add(newListener);
	}

	@Override
	public void removeResultListener(ResultChangeListener obsoleteListener) {
		resultChangeListeners.remove(obsoleteListener);
	}

	@Override
	public void notifyResultChangeListeners(IExecution changedObject) {
		for (ResultChangeListener resultChangeListener : resultChangeListeners) {
			resultChangeListener.resultChanged(changedObject);
		}
	}

	
}
