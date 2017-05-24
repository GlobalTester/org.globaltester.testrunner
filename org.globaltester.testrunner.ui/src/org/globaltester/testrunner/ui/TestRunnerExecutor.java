package org.globaltester.testrunner.ui;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.sampleconfiguration.GtSampleConfigNature;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.sampleconfiguration.SampleConfigManager;
import org.globaltester.sampleconfiguration.ui.SampleConfigSelectorDialog;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestResourceExecutor;
import org.globaltester.scriptrunner.TestResourceExecutorLock;
import org.globaltester.testrunner.GtTestCampaignProject;

/**
 * This implementation of {@link TestResourceExecutor} executes TestCampaigns.
 * @author mboonk
 *
 */
public class TestRunnerExecutor implements TestResourceExecutor {

	private GtTestCampaignProject campaign = null;
	
	public boolean canExecute(List<IResource> resources) {
		if (resources.size() != 1) return false;
		IResource resource = resources.iterator().next();
		
		if (GtTestCampaignProject.isTestCampaignProjectAvailableForResource(resource)) {
			 return true;
		}
		
		return false;
	}

	@Override
	public Object execute(GtRuntimeRequirements runtimeRequirements, List<IResource> resources, TestExecutionCallback callback) {

		if (canExecute(resources)){
			try {
				campaign = GtTestCampaignProject.getProjectForResource(resources.iterator().next());
			} catch (CoreException e) {
				throw new IllegalArgumentException("No test campaign project could be found for the given resources.");
			}
			
			addCommonRuntimeRequirements(runtimeRequirements);
			
			return executeCampaign(campaign, runtimeRequirements, callback);
		}
		throw new IllegalArgumentException("These resources can not be executed as a test campaign");
	}

	private Object executeCampaign(final GtTestCampaignProject campaignProject, GtRuntimeRequirements runtimeRequirements, final TestExecutionCallback callback) {
		
		// execute the TestCampaign
		Job job = new Job("Test execution") {

			protected IStatus run(IProgressMonitor monitor) {
				ThreadGroup threadGroup = new ThreadGroup("TestRunner test execution thread group " + Calendar.getInstance().getTimeInMillis());
				ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
					
					@Override
					public Thread newThread(Runnable r) {
						return new Thread(threadGroup, r);
					}
				});
				Future<IStatus> future = executor.submit(new Callable<IStatus>() {

					@Override
					public IStatus call() throws Exception {
						TestResourceExecutorLock.getLock().lock();
						// execute tests
						try {
							if (campaignProject != null) {

								
								// (re)initialize the TestLogger
								if (TestLogger.isInitialized()) {
									TestLogger.shutdown();
								}
								// initialize test logging for this test session
								GtTestCampaignProject project = campaignProject;
								IFolder defaultLoggingDir = project.getDefaultLoggingDir();
								GtResourceHelper.createWithAllParents(defaultLoggingDir);

								TestLogger.init(project.getNewResultDir());
								
								campaignProject.getTestCampaign().executeTests(runtimeRequirements, monitor, callback);
								
								TestLogger.shutdown();
							} else {

								TestExecutionCallback.TestResult result = new TestExecutionCallback.TestResult();
								result.testCases = 0;
								result.overallResult = 4; //return Status.UNDEFINED on callback
								
								callback.testExecutionFinished(result);
								return Status.CANCEL_STATUS;
							}
						} catch (CoreException e) {
							GtErrorLogger.log(Activator.PLUGIN_ID, e);
						} finally {
							TestResourceExecutorLock.getLock().unlock();
						}

						// refresh the workspace
						try {
							ResourcesPlugin.getWorkspace().getRoot()
									.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e) {
							// log Exception to eclipse log
							GtErrorLogger.log(Activator.PLUGIN_ID, e);

							// users most probably will ignore this behavior and refresh
							// workspace manually, so do not open annoying dialog
						}
						return Status.OK_STATUS;
					}
				});
				
				
				
				try {
					return future.get();
				} catch (InterruptedException | ExecutionException e) {
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}
	
	/**
	 * Add RuntimeRequirements that are commonly used
	 * 
	 * @param config
	 * @param interaction
	 */
	protected void addCommonRuntimeRequirements(GtRuntimeRequirements runtimeReqs) {
		
		//add o.g.protocol.Activator 
		if (!runtimeReqs.containsKey(org.globaltester.protocol.Activator.class)) {
			runtimeReqs.put(org.globaltester.protocol.Activator.class, org.globaltester.protocol.Activator.getDefault());
		}
	}
		
	protected SampleConfig getSampleConfig(Map<?, ?> parameters) {	
		// try to get SampleConfig
		SampleConfig sampleConfig = null;
		Object parameter = parameters.get("org.globaltester.testrunner.ui.SelectSampleConfigParameter");
		String selectSampleConfigParam = parameter == null ? null : parameter.toString();
		boolean forceSelection = (selectSampleConfigParam != null)
				&& selectSampleConfigParam.trim().toLowerCase().equals("true");
		if (!forceSelection) {
			// try to get SampleConfig from Selection if none was defined in
			// TestCampaign
			if (sampleConfig == null) {
				sampleConfig = getFirstSampleConfigFromSelection();
			}
		}

		// ask user for SampleConfig if none was selected
		if (sampleConfig == null) {
			SampleConfigSelectorDialog dialog = new SampleConfigSelectorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			if (dialog.open() != Window.OK) {
				return null;
			}
			sampleConfig = dialog.getSelectedSampleConfig();
		}
		
		return sampleConfig;
	}

	private SampleConfig getFirstSampleConfigFromSelection() {
		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();

		LinkedList<IResource> iResources = GtUiHelper.getSelectedIResources(
				iSel, IResource.class);
		for (IResource iFile : iResources) {
			IProject iProject = iFile.getProject();
			try {
				if (iProject.hasNature(GtSampleConfigNature.NATURE_ID)) {
					return SampleConfigManager.get(iProject.getName());
				}
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
		return null;
	}
}
