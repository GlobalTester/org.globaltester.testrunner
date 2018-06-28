package org.globaltester.testrunner.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.SeverityLevel;
import org.globaltester.base.UserInteraction;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestExecutionCallback.SubTestResult;
import org.globaltester.scriptrunner.TestExecutor;
import org.globaltester.scriptrunner.TestResourceExecutorLock;
import org.globaltester.testrunner.EnvironmentInspector;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.ui.views.ResultView;
import org.globaltester.testrunner.utils.IntegrityCheckResult;
import org.globaltester.testrunner.utils.TestSpecIntegrityChecker;

public abstract class TestResourceExecutor extends TestExecutor {

	@Override
	protected void addRuntimeRequirements(GtRuntimeRequirements runtimeReqs) {
		
		//add o.g.protocol.Activator 
		if (!runtimeReqs.containsKey(org.globaltester.protocol.Activator.class)) {
			runtimeReqs.put(org.globaltester.protocol.Activator.class, org.globaltester.protocol.Activator.getDefault());
		}
	}

	@Override
	protected Job getExecutionJob(List<IResource> resources, GtRuntimeRequirements runtimeRequirements,
			TestExecutionCallback callback) {

		// execute the TestCampaign
		Job job = new Job("Executing tests...") {

			protected IStatus run(IProgressMonitor monitor) {

				// FIXME AAC integrate monitor implementation here that updates
				// ResultView

				ThreadGroup threadGroup = new ThreadGroup(
						"TestResourceExecutor thread group " + Calendar.getInstance().getTimeInMillis());
				ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

					@Override
					public Thread newThread(Runnable r) {
						return new Thread(threadGroup, r);
					}
				});
				Future<IStatus> future = executor
						.submit(getExecutionCallable(resources, runtimeRequirements, callback, monitor));

				try {
					return future.get();
				} catch (InterruptedException | ExecutionException e) {
					return Status.CANCEL_STATUS;
				}
			}
		};

		return job;
	}

	protected Callable<IStatus> getExecutionCallable(final List<IResource> resources,
			GtRuntimeRequirements runtimeRequirements, final TestExecutionCallback callback, IProgressMonitor monitor) {

		return new Callable<IStatus>() {

			@Override
			public IStatus call() throws Exception {
				TestResourceExecutorLock.getLock().lock();
				AbstractTestExecution execution = null;

				try {
					// create TestExecution from resources					
					execution = buildTestExecution(resources);
					
					//show TestExecution in ResultView
					showTestExecutionInResultView(execution);
					
					// (re)initialize the TestLogger
					if (TestLogger.isInitialized()) {
						TestLogger.shutdown();
					}

					TestLogger.init(getLoggingDir(resources));
					
					//FIXME AAC Logfile "Header"
					EnvironmentInspector.dumpEnvironmentInfoToLogfile();
					
					
					// check integrity
					if (!checkIntegrity(runtimeRequirements, execution)) {
						return Status.CANCEL_STATUS;
					}
					
					TestLogger.info("Start TestExecution");
					
					// execute the TestExecutable
					execution.execute(runtimeRequirements, false, monitor);
					
					//FIXME AAD warning message when no testcases where executed
					// display warning message if no test cases where executed (and the user was not informed about the abort earlier)
//					if (!abortExecution && !wasTestsApplicable()) {
//						interaction.notify(SeverityLevel.WARNING, "No testcases have been executed! Probably none of the selected cases was applicable to your sample. ");
//					}
					
					//FIXME AAC logfile marking (check how this works in the Campaign)
//					addLogReferencesToFailureList(TestLogger.getLogFileName(), failureList);
//					markLogFile(TestLogger.getLogFileName(), failureList);
					
					//FIXME AAC Logfile "Footer"
//					double testTime = execution.getLastExecutionDuration() / 1000.;
//					
//					TestLogger.info("");
//					TestLogger.info("---------------------------------");
//					TestLogger.info("Test Summary");
//					TestLogger.info("");
//					TestLogger.info("Time for complete session: " + testTime + " sec");
//					TestLogger.info("Number of test cases: " + execution.getChildren().size());
//					TestLogger.info("Session failures: " + execution.getNumberOfTestsWithStatus(Status.FAILURE));
//					TestLogger.info("Session warnings: " + execution.getNumberOfTestsWithStatus(Status.WARNING));
					
					//FIXME AAC implement automatic report generation
//					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
//					boolean automaticReport = store.getBoolean(PreferenceConstants.P_AUTOMATICREPORT);
			//
//					if (automaticReport) {
//						TestReport.generate(this, ReportXML.getDefaultDestinationDir());
//					}

				} catch (Exception e) {
					GtErrorLogger.log(Activator.PLUGIN_ID, e);

					return Status.CANCEL_STATUS;
				} finally {
					TestLogger.shutdown(); //FIXME AAF concurrency problem here?
					returnTestResultsToCallback(execution, callback);
					TestResourceExecutorLock.getLock().unlock();
					monitor.done();
				}

				// refresh the workspace
				try {
					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					// log Exception to eclipse log
					GtErrorLogger.log(Activator.PLUGIN_ID, e);

					// users most probably will ignore this behavior and refresh
					// workspace manually, so do not open annoying dialog
				}
				return Status.OK_STATUS;
			}
		};
	}

	private void showTestExecutionInResultView(AbstractTestExecution execution) {
		try {
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage page = Activator.getDefault().getWorkbench()
							.getActiveWorkbenchWindow().getActivePage();
					IViewPart vp = null;
					try {
						vp = page.showView(ResultView.VIEW_ID);
					} catch (PartInitException e) {
						BasicLogger.logException("ResultView could not be initialized", e, org.globaltester.logging.tags.LogLevel.WARN);
					}

					if (vp instanceof ResultView) {
						ResultView resultView = (ResultView) vp;
						resultView.setInput(execution);
					}
				}
			});
		} catch (RuntimeException ex) { //FIXME AAD remove this catch if possible
			TestLogger.error(ex);
		}
	}

	/**
	 * Notify the callback about the execution results
	 * 
	 * @param currentExecution
	 *            Execution to extract the Result from or null if Execution was
	 *            not possible
	 * @param callback
	 *            TestExecutionCallback to communicate the result to
	 */
	protected void returnTestResultsToCallback(AbstractTestExecution currentExecution, TestExecutionCallback callback) {
		TestExecutionCallback.TestResult result = new TestExecutionCallback.TestResult();
		
		if (currentExecution != null) {
			result.testCases = currentExecution.getChildren().size();
			result.overallResult = currentExecution.getResult().status.getRating();	
			
			ArrayList<TestExecutionCallback.SubTestResult> subResults = new ArrayList<>();
			
			for (IExecution curChild : currentExecution.getChildren()) {
				if (curChild instanceof TestCaseExecution) {
					TestCaseExecution currTestCaseResult = (TestCaseExecution) curChild; 
					SubTestResult curSubResult = new TestExecutionCallback.SubTestResult();
					curSubResult.testCaseId = currTestCaseResult.getId();
					curSubResult.logFileName = currTestCaseResult.getLogFileName();
					curSubResult.resultString = currTestCaseResult.getResult().toString();
					subResults.add(curSubResult);
				}
			}
			if (!subResults.isEmpty()) {
				result.subResults = subResults.toArray(new TestExecutionCallback.SubTestResult[]{} );
			}
		} else {
			result.testCases = 0;
			result.overallResult = org.globaltester.testrunner.testframework.Result.Status.UNDEFINED.getRating();	
		}
		
		callback.testExecutionFinished(result);
	}

	/**
	 * Return the Logging dir used to initialize the TestLogger
	 * @param resources
	 * @return
	 * @throws CoreException
	 */
	protected abstract String getLoggingDir(List<IResource> resources) throws CoreException;

	/**
	 * Create the {@link AbstractTestExecution} that shall be executed
	 * 
	 * @param resources
	 * @throws CoreException
	 */
	protected abstract AbstractTestExecution buildTestExecution(List<IResource> resources) throws CoreException;



	/**
	 * Checks the Integrity of TestSpecifications and persists the check result in RuntimeRequirements.
	 * 
	 * Returns true if either all specs are valid, preferences are set to ignore mismatches or user manually accepts through user interaction.
	 * @param execution 
	 * @param runtimeRequirements 
	 * @return
	 */
	private boolean checkIntegrity(GtRuntimeRequirements runtimeRequirements, AbstractTestExecution execution) {
		
		TestSpecIntegrityChecker integrityChecker = new TestSpecIntegrityChecker();
		integrityChecker.addRecursive(execution.getExecutable());
		integrityChecker.addDependencies();
		
		//check integrity of specs and log results
		Map<String, IntegrityCheckResult> integrityResult = integrityChecker.check();
		ArrayList<String> specNames = new ArrayList<>(integrityResult.keySet());
		Collections.sort(specNames);
		String nonValidProjects = "";
		for (String curSpec: specNames) {
			TestLogger.info("Checksum of "+ curSpec + " is "+ integrityResult.get(curSpec).getStatus());
			TestLogger.trace("Expected checksum: "+ integrityResult.get(curSpec).getExpectedChecksum());
			TestLogger.trace("Actual checksum: "+ integrityResult.get(curSpec).getCalculatedChecksum());
			
			if (integrityResult.get(curSpec).getStatus() != IntegrityCheckResult.IntegrityCheckStatus.VALID) {
				nonValidProjects+="\n-"+curSpec + ": " +integrityResult.get(curSpec).getStatus() ;
			} 
		}

		// persist check result in runtimeRequirements
		runtimeRequirements.put(IntegrityCheckResult.class, IntegrityCheckResult.combineCheckStatus(integrityResult.values()));		
		
		
		//handle non-valid specs
		if (!nonValidProjects.isEmpty()) {
			String message = "Functional integrity of testcases is not assured!\n\nThe following Scripts have been modified since delivery or remain unchecked:\n"
					+ nonValidProjects + "\n";
			TestLogger.warn(message);
			
			//proceed if preferences say so
			if (Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, PreferenceConstants.P_IGNORECHECKSUMRESULT))) {
				return true;
			}
			
			//ask user to how to proceed
			UserInteraction interaction = runtimeRequirements.get(UserInteraction.class);
			if (interaction.select(message + "\n\nExecute test case(s) anyway?", null, "Ok", "Cancel") != 0) {
				return false;
			}
			
		}		
		
		return true;
	}
	//FIXME AAD check implemention of  user abort functionality (IProgressMonitor.isCanceled())

}
