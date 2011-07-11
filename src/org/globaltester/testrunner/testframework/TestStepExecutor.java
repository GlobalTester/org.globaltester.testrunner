package org.globaltester.testrunner.testframework;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testspecification.testframework.TestStep;
import org.mozilla.javascript.Context;

public class TestStepExecutor {

	private ScriptRunner scriptRunner;
	private Context context;
	
	public TestStepExecutor(ScriptRunner sr, Context cx) {
		scriptRunner= sr;
		context = cx;
	}

	public void execute(TestStep curStep) {
		// FIXME implement execution of TestStep
		TestLogger.debug("TestStep Description");
		TestLogger.trace("TestStep code");
		
		String code = curStep.getTechnicalCommand(); //FIXME get this code from TestStep
		
		scriptRunner.executeCommand(context, code, "SourceName", -1);
	}

}
