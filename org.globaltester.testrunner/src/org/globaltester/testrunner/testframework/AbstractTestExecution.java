package org.globaltester.testrunner.testframework;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.testmanager.testframework.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.jdom.Element;

public abstract class AbstractTestExecution implements IExecution {

	private static final String XML_TAG_LAST_EXECUTION_LOG_FILE_LINE = "LastExecutionLogFileLine";
	private static final String XML_TAG_LAST_EXECUTION_LOG_FILE_NAME = "LastExecutionLogFileName";
	private static final String XML_TAG_LAST_EXECUTION_RESULT = "LastExecutionResult";
	private static final String XML_TAG_LAST_EXECUTION_DURATION = "LastExecutionDuration";
	private static final String XML_TAG_LAST_EXECUTION_START_TIME = "LastExecutionStartTime";

	protected Result result = ResultFactory.newEmptyResult();

	// store time and duration of last execution
	protected long lastExecutionStartTime = 0;
	protected long lastExecutionDuration = 0;
	private String logFileName;
	private int logFileLine;

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

	/**
	 * dump this instance to the given XML Element
	 * 
	 * @param root
	 */
	void dumpToXml(Element root) {
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

	/**
	 * extract data for this instance from XML Element
	 * 
	 * @param root
	 */
	void extractFromXml(Element root) {

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
	 * @param sr
	 *            ScriptRunner to execute JS code in
	 * @param cx
	 *            Context to execute JS code in
	 * @param forceExecution
	 *            if true the code is executed regardless if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 */
	public void execute(ScriptRunner sr, boolean forceExecution, IProgressMonitor monitor) {
		// set the execution time
		boolean reExecution = lastExecutionStartTime != 0;

		lastExecutionStartTime = new Date().getTime();

		//set the log file
		logFileName = TestLogger.getTestCaseLogFileName();
		logFileLine = TestLogger.getLogFileLine();
		
		// forward the execution to the implementing class
		execute(sr, forceExecution, reExecution, monitor);

		// calculate execution duration
		lastExecutionDuration = new Date().getTime() - lastExecutionStartTime;

	}

	/**
	 * (Re)Execute the code associated with this test execution
	 * 
	 * @param sr
	 *            ScriptRunner to execut JS code in
	 * @param cx
	 *            Context to execute JS code in
	 * @param forceExecution
	 *            if true the code is executed regardless if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 * @param monitor 
	 */
	protected abstract void execute(ScriptRunner sr, boolean forceExecution, boolean reExecution, IProgressMonitor monitor);

	public long getLastExecutionStartTime() {
		return lastExecutionStartTime;
	}

	public String getLastExecutionStartTimeAsString() {
		return DateFormat.getDateTimeInstance().format(
				new Date(getLastExecutionStartTime()));
	}
	
	public long getLastExecutionDuration() {
		return lastExecutionDuration;
	}

	@Override
	public Status getStatus() {
		return result.getStatus();
	}
	
	/**
	 * Return the name of the XML root element describing an instance
	 * @return
	 */
	protected abstract String getXmlRootElementName();

	
}
