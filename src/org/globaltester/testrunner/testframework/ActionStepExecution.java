package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ActionStep;
import org.globaltester.testspecification.testframework.ExpectedResult;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public abstract class ActionStepExecution extends AbstractTestExecution {

	private ActionStep testStep;
	
	Result expResultsExecutionResults;

	private Result commandResult;

	private IExecution parent;

	/**
	 * Constructor for new TestStepExecutionInstance
	 * @param step	TestStep this execution instance should execute
	 */
	protected ActionStepExecution(ActionStep step, IExecution parentExecution) {
		testStep = step;
		parent = parentExecution;
	}

	@Override
	public void execute(ScriptRunner sr, Context cx, boolean forceExecution, boolean reExecution) {
		//log TestStep ID and Command
		TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "TestStep: "+ testStep.getId()));
		Element commandElem = testStep.getCommand();
		if (commandElem != null) {
			String command = commandElem.getTextNormalize();
			Element commandTextElem = commandElem.getChild("Text", commandElem.getNamespace());
			if (commandTextElem != null) {
				command = command + commandTextElem.getTextNormalize();
			}
			TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "Command: "+command));
			Element commandApduElem = commandElem.getChild("APDU", commandElem.getNamespace());
			if (commandApduElem != null) {
				TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "APDU: \n"+commandApduElem.getTextNormalize()));
			}
		}
		
		//log TestStep descriptions
		Iterator<String> descrIter = testStep.getDescriptions().iterator();
		while (descrIter.hasNext()) {
			TestLogger.debug(String.format(TestLogger.DEFAULTFORMAT, "Description: "+descrIter.next()));			
		}
		
		//init the executor with context and script runner
		ActionStepExecutor stepExecutor = new ActionStepExecutor(sr, cx);
		
		//execute the test step itself
		String techCommandCode = testStep.getTechnicalCommand();
		if ((techCommandCode != null) && (techCommandCode.trim().length() > 0)) {
			commandResult = stepExecutor.execute(techCommandCode, testStep.getId()+" - Command");
		} else {
			//if no code can be executed the result of the step itself is always ok
			commandResult = new Result(Status.PASSED);
		}
		
		//execute all ExpectedResults
		List<ExpectedResult> expResultDefs = testStep.getExpectedResults();
		expResultsExecutionResults = new OrResult(Status.PASSED);
		for (Iterator<ExpectedResult> expResultIter = expResultDefs.iterator(); expResultIter
				.hasNext();) {
			ExpectedResult curResult = expResultIter.next();
			
			TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "ExpectedResult: " + curResult.getId() + " (TestStep "+ testStep.getId()+")"));
			
			//log ExpectedResult descriptions
			descrIter = curResult.getDescriptions().iterator();
			while (descrIter.hasNext()) {
				TestLogger.debug(String.format(TestLogger.DEFAULTFORMAT, "Description: "+descrIter.next()));
			}
			
			//execute the current expected result
			Result curExecutionResult;
			String techResultCode = curResult.getTechnicalResult();
			if ((techResultCode != null) && (techResultCode.trim().length() > 0)) {
				curExecutionResult = stepExecutor.execute(techResultCode, testStep.getId()+" - Expected Result "+curResult.getId());
			} else {
				//if no code can be executed the result of the result is always ok
				curExecutionResult = new Result(Status.PASSED);
			}
			expResultsExecutionResults.addSubResult(curExecutionResult);
		}
		
		
		//evaluate results
		result = new Result(Status.PASSED);
		result.addSubResult(commandResult);
		result.addSubResult(expResultsExecutionResults);
	}

	@Override
	public boolean hasChildren() {
		// test step has no children
		return false;
	}

	@Override
	public Collection<IExecution> getChildren() {
		// test step has no children
		return null;
		
	}

	@Override
	public IExecution getParent() {
		return parent;
	}

	@Override
	public String getName() {
		return testStep.getName();
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return Status.UNDEFINED;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getId() {
		return testStep.getId();
	}

}
