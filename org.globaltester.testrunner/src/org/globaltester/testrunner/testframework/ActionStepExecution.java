package org.globaltester.testrunner.testframework;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ActionStep;
import org.globaltester.testspecification.testframework.ExpectedResult;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.jdom.Element;

public abstract class ActionStepExecution extends AbstractTestExecution {

	Result expResultsExecutionResults;

	private Result commandResult;

	IExecution parent;
	private int childId;

	/**
	 * Constructor for new ActionStepExecution
	 * 
	 * @param parent parent execution (used to dereference the specification) 
	 * @param childId id of this child within parent
	 */
	protected ActionStepExecution(IExecution parent, int childId) {
		super(parent.getExecutable().getChildren().get(childId));
		this.parent = parent;
		this.childId = childId;
	}

	@Override
	public void execute(GtRuntimeRequirements provider, boolean forceExecution, boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		
		try {

			int expectedResults = 0;
			ActionStep actionStep = (ActionStep) getExecutable();
			if (actionStep.getExpectedResults() != null) {
				expectedResults = actionStep.getExpectedResults().size();
			}
			monitor.beginTask("Execute " + actionStep.getName() + ": ", 2 + expectedResults); // workitems:
																								// initialization +
																								// technicalCommand, +
																								// one per Result

			monitor.subTask("Initialization");
			// log TestStep ID and Command
			TestLogger.info("TestStep: " + actionStep.getId() + " (" + parent.getId() + ")");
			Element commandElem = actionStep.getCommand();
			if (commandElem != null) {
				String command = commandElem.getTextNormalize();
				Element commandTextElem = commandElem.getChild("Text", commandElem.getNamespace());
				if (commandTextElem != null) {
					command = command + commandTextElem.getTextNormalize();
				}
				TestLogger.info("Command: " + command);
				Element commandApduElem = commandElem.getChild("APDU", commandElem.getNamespace());
				if (commandApduElem != null) {
					TestLogger.info("APDU: " + commandApduElem.getTextNormalize());
				}
			}

			// log TestStep descriptions
			List<String> descriptions = actionStep.getDescriptions();
			if (descriptions != null) {
				Iterator<String> descrIter = descriptions.iterator();
				while (descrIter.hasNext()) {
					TestLogger.debug("Description: " + descrIter.next());
				}
			}

			boolean ignoreExecutionRequirements = checkForSubResults(result, Status.REQUIREMENT_MISSING);

			ActionStepExecutor stepExecutor = new ActionStepExecutor(provider, ignoreExecutionRequirements);

			monitor.worked(1);

			// execute the test step itself
			monitor.subTask("Execute command");
			String techCommandCode = actionStep.getTechnicalCommand();
			if ((techCommandCode != null) && (techCommandCode.trim().length() > 0)) {
				commandResult = stepExecutor.execute(techCommandCode, actionStep.getId() + " - Command");
			} else {
				// if no code can be executed the result of the step itself is always ok
				commandResult = new Result(Status.PASSED);
			}
			monitor.worked(1);

			// execute all ExpectedResults
			List<ExpectedResult> expResultDefs = actionStep.getExpectedResults();
			if (expResultDefs != null) {
				expResultsExecutionResults = new OrResult(Status.PASSED);
				for (Iterator<ExpectedResult> expResultIter = expResultDefs.iterator(); expResultIter.hasNext();) {
					ExpectedResult curResult = expResultIter.next();

					monitor.subTask("ExpectedResult: " + curResult.getId());
					TestLogger.info("ExpectedResult: " + curResult.getId() + " (" + parent.getId() + ")");

					// log ExpectedResult descriptions
					Iterator<String> descrIter = curResult.getDescriptions().iterator();
					while (descrIter.hasNext()) {
						TestLogger.debug("Description: " + descrIter.next());
					}

					// execute the current expected result
					Result curExecutionResult;
					String techResultCode = curResult.getTechnicalResult();
					if ((techResultCode != null) && (techResultCode.trim().length() > 0)) {
						curExecutionResult = stepExecutor.execute(techResultCode,
								actionStep.getId() + " - Expected Result " + curResult.getId());
					} else {
						// if no code can be executed the result of the result is always ok
						curExecutionResult = new Result(Status.PASSED);
					}
					expResultsExecutionResults.addSubResult(curExecutionResult);
					monitor.worked(1);
				}
			}

			// evaluate results
			result = new Result(Status.PASSED);
			result.addSubResult(commandResult);
			if (expResultsExecutionResults != null) {
				result.addSubResult(expResultsExecutionResults);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * Checks recursively for the given status.
	 * 
	 * @param result the result to be searched
	 * @param status the status to be searched for
	 * @return true, iff the given result or its subresults has the given status
	 */
	static boolean checkForSubResults(Result result, Status status) {
		if (result.status.equals(status)) {
			return true;
		}
		
		for (Result sub : result.subResults) {
			if (checkForSubResults(sub, status)) return true;
		}
		
		return false;
	}

	@Override
	public boolean hasChildren() {
		// test step has no children
		return false;
	}

	@Override
	public Collection<IExecution> getChildren() {
		// test step has no children
		return Collections.emptyList();
		
	}

	@Override
	public ITestExecutable getExecutable() {
		return getParent().getExecutable().getChildren().get(childId);
	}

	public IExecution getParent() {
		return parent;
	}

}
