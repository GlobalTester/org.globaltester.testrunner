package org.globaltester.testrunner.testframework;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.ExpectedResult;
import org.globaltester.testspecification.testframework.TestStep;
import org.mozilla.javascript.Context;

public class TestStepExecution {

	private TestStep testStep;
	Result testStepResult;
	List<Result> expResultsExecutionResults;

	/**
	 * Constructor for new TestStepExecutionInstance
	 * @param step	TestStep this execution instance should execute
	 */
	public TestStepExecution(TestStep step) {
		testStep = step;
	}

	public void execute(ScriptRunner sr, Context cx, boolean forceExecution) {
		//log TestStep ID and Command
		TestLogger.info("TestStep "+ testStep.getId());
		TestLogger.info("Command: \n"+testStep.getCommand().getTextNormalize());
		
		//log TestStep descriptions
		Iterator<String> descrIter = testStep.getDescriptions().iterator();
		while (descrIter.hasNext()) {
			TestLogger.debug("   * "+descrIter.next());			
		}
		
		//init the executor with context and script runner
		TestStepExecutor stepExecutor = new TestStepExecutor(sr, cx);
		
		//execute the test step itself
		String command = testStep.getTechnicalCommand();
		if ((command != null) && (command.trim().length() > 0)) {
			testStepResult = stepExecutor.execute(command, testStep.getId()+" - Command");
		} else {
			//if no code can be executed the result of the step itself is always ok
			testStepResult = new Result();
		}
		
		//execute all ExpectedResults
		List<ExpectedResult> expResultDefs = testStep.getExpectedResults();
		expResultsExecutionResults = new LinkedList<Result>();
		for (Iterator<ExpectedResult> expResultIter = expResultDefs.iterator(); expResultIter
				.hasNext();) {
			ExpectedResult curResult = expResultIter.next();
			
			TestLogger.info("ExpectedResult " + curResult.getId() + " (TestStep "+ testStep.getId()+")");
			
			//log TestStep descriptions
			descrIter = curResult.getDescriptions().iterator();
			while (descrIter.hasNext()) {
				TestLogger.debug("   * "+descrIter.next());			
			}
			
			//execute the current expected result
			Result curExecutionResult = stepExecutor.execute(curResult.getTechnicalResult(), testStep.getId()+" - Expected Result "+curResult.getId());
			expResultsExecutionResults.add(curExecutionResult);
			
		}
	}

}
