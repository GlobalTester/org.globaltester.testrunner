package org.globaltester.testrunner.testframework;

import java.util.Iterator;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.TestStep;
import org.mozilla.javascript.Context;

public class TestStepExecution {

	private TestStep testStep;
	private Result testStepResult;

	/**
	 * Constructor for new TestStepExecutionInstance
	 * @param step	TestStep this execution instance should execute
	 */
	public TestStepExecution(TestStep step) {
		testStep = step;
	}

	public void execute(ScriptRunner sr, Context cx, boolean forceExecution) {
		TestLogger.info("TestStep "+ testStep.getId());
		
		//log TestStep descriptions
		Iterator<String> descrIter = testStep.getDescriptions().iterator();
		while (descrIter.hasNext()) {
			TestLogger.debug("   * "+descrIter.next());			
		}
		
		//init the executor with context and script runner
		TestStepExecutor stepExecutor = new TestStepExecutor(sr, cx);
		
		//execute the test step itself
		testStepResult = stepExecutor.execute(testStep.getTechnicalCommand(), testStep.getId()+" - Command");
		
		//execute all ExpectedResults
//		List<ExpectedResult> expResults = testStep.getExpectedResults();
//		for (Iterator<ExpectedResult> expResultIter = expResults.iterator(); expResultIter
//				.hasNext();) {
//			ExpectedResult curResult = expResultIter.next();
//			
//			TestLogger.info("ExpectedResult " + curResult.getId() + " (TestStep "+ testStep.getId()+")");
//			
//			//log TestStep descriptions
//			descrIter = curResult.getDescriptions().iterator();
//			while (descrIter.hasNext()) {
//				TestLogger.debug("   * "+descrIter.next());			
//			}
//			//execute the current expected result
//			testStepResult = stepExecutor.execute(curResult.getTechnicalCommand(), testStep.getId()+" - Expected Result "+curResult.getId());
//			
//		}
	}

}
