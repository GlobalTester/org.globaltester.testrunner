package org.globaltester.testrunner.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.UserInteraction;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestExecutionCallback.SubTestResult;
import org.globaltester.scriptrunner.TestExecutor;
import org.globaltester.scriptrunner.TestResourceExecutorLock;
import org.globaltester.testrunner.TestLogHelper;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.ui.editor.ReportGenerationJob;
import org.globaltester.testrunner.ui.views.ResultView;
import org.globaltester.testrunner.utils.IntegrityCheckResult;
import org.globaltester.testrunner.utils.TestSpecIntegrityChecker;

public abstract class TestResourceExecutor extends TestExecutor {

	@Override
	protected void addRuntimeRequirements(GtRuntimeRequirements runtimeReqs) {
		
		//add o.g.protocol.Activator 
		if (!runtimeReqs.containsKey(org.globaltester.sampleconfiguration.Activator.class)) {
			runtimeReqs.put(org.globaltester.sampleconfiguration.Activator.class, org.globaltester.sampleconfiguration.Activator.getDefault());
		}
	}

	@Override
	protected Job getExecutionJob(List<IResource> resources, GtRuntimeRequirements runtimeRequirements,
			TestExecutionCallback callback) {

		// execute the TestCampaign
		String jobName = "Executing tests...";
		return  new Job(jobName) {

			protected IStatus run(IProgressMonitor monitor) {

				ThreadGroup threadGroup = new ThreadGroup(
						"TestResourceExecutor thread group " + Calendar.getInstance().getTimeInMillis());
				ExecutorService executor = Executors.newSingleThreadExecutor( r -> new Thread(threadGroup, r));
				Future<IStatus> future = executor
						.submit(getExecutionCallable(resources, runtimeRequirements, callback, monitor));

				try {
					return future.get();
				} catch (InterruptedException | ExecutionException e) {
					BasicLogger.logException("Job " + jobName + " was aborted", e, LogLevel.WARN);
					return Status.CANCEL_STATUS;
				}
			}
		};

	}

	protected Callable<IStatus> getExecutionCallable(final List<IResource> resources,
			GtRuntimeRequirements runtimeRequirements, final TestExecutionCallback callback, IProgressMonitor monitor) {

		return new Callable<IStatus>() {

			@Override
			public IStatus call() throws Exception {
				TestResourceExecutorLock.getLock().lock();
				AbstractTestExecution execution = null;
				
				SampleConfig sampleConfig = runtimeRequirements.get(SampleConfig.class);

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
					
					if (sampleConfig != null) {
						sampleConfig.lock();
					}
					
					TestLogHelper.dumpLogfileHeaderToTestLogger();
					
					
					// check integrity
					if (!checkIntegrity(runtimeRequirements, execution)) {
						return Status.CANCEL_STATUS;
					}
					
					TestLogger.info("Start TestExecution");
					
					// execute the TestExecutable
					execution.execute(runtimeRequirements, false, monitor);
					
					TestLogHelper.dumpLogfileFooterToTestLogger(execution);
					
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean automaticReport = store.getBoolean(PreferenceConstants.P_REPORT_AUTOMATIC);
					if (automaticReport) {
						Job job = new ReportGenerationJob(execution, null);
						job.schedule();
						job.join();
					}

				} catch (Exception e) {
					GtErrorLogger.log(Activator.PLUGIN_ID, e);

					return Status.CANCEL_STATUS;
				} finally {
					if (sampleConfig != null) {
						sampleConfig.unlock();
					}
					TestLogger.shutdown();
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
		Display.getDefault().asyncExec(new Runnable() {
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

}
