package org.globaltester.testrunner.testframework;

import java.util.Date;

import org.globaltester.smartcardshell.ScriptRunner;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public abstract class AbstractTestExecution implements IExecution {

	protected Result result = ResultFactory.newEmptyResult();

	// store time and duration of last execution
	protected long lastExecutionStartTime = 0;
	protected long lastExecutionDuration = 0;

	public Result getResult() {
		return result;
	}

	/**
	 * dump this instance to the given XML Element
	 * 
	 * @param root
	 */
	void dumpToXml(Element root) {
		Element startTimeElement = new Element("LastExecutionStartTime");
		startTimeElement.addContent(Long.toString(lastExecutionStartTime));
		root.addContent(startTimeElement);

		Element durationElement = new Element("LastExecutionDuration");
		durationElement.addContent(Long.toString(lastExecutionDuration));
		root.addContent(durationElement);
	}

	/**
	 * extract data for this instance from XML Element
	 * 
	 * @param root
	 */
	void extractFromXml(Element root) {

		Element timeElem = root.getChild("LastExecutionStartTime");
		if (timeElem != null) {
			lastExecutionStartTime = Long.valueOf(timeElem.getTextTrim());
		}
		
		Element durationElem = root.getChild("LastExecutionDuration");
		if (durationElem != null) {
			lastExecutionDuration = Long.valueOf(durationElem.getTextTrim());
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
	public void execute(ScriptRunner sr, Context cx, boolean forceExecution) {
		// set the execution time
		boolean reExecution = lastExecutionStartTime != 0;

		lastExecutionStartTime = new Date().getTime();

		// forward the execution to the implementing class
		execute(sr, cx, forceExecution, reExecution);

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
	 *            if true the code is executed regardles if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 */
	protected abstract void execute(ScriptRunner sr, Context cx,
			boolean forceExecution, boolean reExecution);

	public long getLastExecutionStartTime() {
		return lastExecutionStartTime;
	}
	
	public long getLastExecutionDuration() {
		return lastExecutionDuration;
	}

	
}
