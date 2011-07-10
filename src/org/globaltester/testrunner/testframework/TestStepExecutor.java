package org.globaltester.testrunner.testframework;

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
		scriptRunner.executeCommand(context, "print(\"Hello World\");");
	}

}
