package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ActionStep;
import org.globaltester.testspecification.testframework.ExpectedResult;
import org.jdom.Element;

public abstract class ActionStepExecution extends AbstractTestExecution {

	private ActionStep actionStep;
	
	Result expResultsExecutionResults;

	private Result commandResult;

	private IExecution parent;

	/**
	 * Constructor for new TestStepExecutionInstance
	 * @param step	TestStep this execution instance should execute
	 */
	protected ActionStepExecution(ActionStep step, IExecution parentExecution) {
		actionStep = step;
		parent = parentExecution;
	}

	@Override
	public void execute(GtRuntimeRequirements provider, boolean forceExecution, boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		
		try {
			
			int expectedResults = 0;
			if(actionStep.getExpectedResults()!=null){
				expectedResults = actionStep.getExpectedResults().size();
			}
			monitor.beginTask("Execute " + actionStep.getName() + ": ", 2+expectedResults); //workitems: initialization + technicalCommand, + one per Result
		
			monitor.subTask("Initialization");
		
		//log TestStep ID and Command
		TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "TestStep: "+ actionStep.getId()));
		Element commandElem = actionStep.getCommand();
		if (commandElem != null) {
			String command = commandElem.getTextNormalize();
			Element commandTextElem = commandElem.getChild("Text", commandElem.getNamespace());
			if (commandTextElem != null) {
				command = command + commandTextElem.getTextNormalize();
			}
			TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "Command: "+command));
			Element commandApduElem = commandElem.getChild("APDU", commandElem.getNamespace());
			if (commandApduElem != null) {
				TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "APDU: "+commandApduElem.getTextNormalize()));
			}
		}
		
		//log TestStep descriptions
		List<String> descriptions = actionStep.getDescriptions();
		if(descriptions!=null){
			Iterator<String> descrIter = descriptions .iterator();
			while (descrIter.hasNext()) {
				TestLogger.debug(String.format(TestLogger.DEFAULTFORMAT, "Description: "+descrIter.next()));			
			}
		}
		
		ActionStepExecutor stepExecutor = new ActionStepExecutor(provider);
		
		monitor.worked(1);
		
		//execute the test step itself
		monitor.subTask("Execute command");
		String techCommandCode = actionStep.getTechnicalCommand();
		if ((techCommandCode != null) && (techCommandCode.trim().length() > 0)) {
			commandResult = stepExecutor.execute(techCommandCode, actionStep.getId()+" - Command");
		} else {
			//if no code can be executed the result of the step itself is always ok
			commandResult = new Result(Status.PASSED);
		}
		monitor.worked(1);
		
		//execute all ExpectedResults
		List<ExpectedResult> expResultDefs = actionStep.getExpectedResults();
		if(expResultDefs!=null){
			expResultsExecutionResults = new OrResult(Status.PASSED);
			for (Iterator<ExpectedResult> expResultIter = expResultDefs.iterator(); expResultIter
					.hasNext();) {
				ExpectedResult curResult = expResultIter.next();
				
				monitor.subTask("ExpectedResult: " + curResult.getId());
				TestLogger.info(String.format(TestLogger.DEFAULTFORMAT, "ExpectedResult: " + curResult.getId()));
				
				//log ExpectedResult descriptions
				Iterator<String> descrIter = curResult.getDescriptions().iterator();
				while (descrIter.hasNext()) {
					TestLogger.debug(String.format(TestLogger.DEFAULTFORMAT, "Description: "+descrIter.next()));
				}
				
				//execute the current expected result
				Result curExecutionResult;
				String techResultCode = curResult.getTechnicalResult();
				if ((techResultCode != null) && (techResultCode.trim().length() > 0)) {
					curExecutionResult = stepExecutor.execute(techResultCode, actionStep.getId()+" - Expected Result "+curResult.getId());
				} else {
					//if no code can be executed the result of the result is always ok
					curExecutionResult = new Result(Status.PASSED);
				}
				expResultsExecutionResults.addSubResult(curExecutionResult);
				monitor.worked(1);
			}
		}

		
		
		//evaluate results
		result = new Result(Status.PASSED);
		result.addSubResult(commandResult);
		if(expResultsExecutionResults!=null){
			result.addSubResult(expResultsExecutionResults);
		}
		} finally {
			monitor.done();
		}
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
		return actionStep.getName();
	}

	@Override
	public String getComment() {
		return "";
	}

	@Override
	public String getDescription() {
		return "";
	}

	@Override
	public Status getStatus() {
		return Status.UNDEFINED;
	}

	@Override
	public String getId() {
		return actionStep.getId();
	}

}
