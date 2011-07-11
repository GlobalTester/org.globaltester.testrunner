package org.globaltester.testrunner.testframework;

import java.util.Iterator;

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
		TestLogger.info("TestStep "+ curStep.getId());
		
		//log TestStep descriptions
		Iterator<String> descrIter = curStep.getDescriptions().iterator();
		while (descrIter.hasNext()) {
			TestLogger.debug("   * "+descrIter.next());
			
		}
		
		//get and execute the code
		String code = curStep.getTechnicalCommand();
		TestLogger.trace("Code \n" + code);
		scriptRunner.executeCommand(context, code, curStep.getId(), -1);
	}

}
