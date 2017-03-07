package org.globaltester.testrunner.ui;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import org.globaltester.base.UserInteraction;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.sampleconfiguration.GtSampleConfigNature;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.sampleconfiguration.SampleConfigManager;
import org.globaltester.sampleconfiguration.ui.SampleConfigSelectorDialog;
import org.globaltester.scriptrunner.RuntimeRequirementsProvider;
import org.globaltester.scriptrunner.SampleConfigProvider;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestResourceExecutor;
import org.globaltester.scriptrunner.UserInteractionProvider;
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
	public Object execute(RuntimeRequirementsProvider runtimeRequirements, List<IResource> resources, TestExecutionCallback callback) {

		if (canExecute(resources)){
			try {
				campaign = GtTestCampaignProject.getProjectForResource(resources.iterator().next());
			} catch (CoreException e) {
				throw new IllegalArgumentException("No test campaign project could be found for the given resources.");
			}
			
			UserInteraction interaction = null;
			if (runtimeRequirements instanceof UserInteractionProvider){
				interaction = ((UserInteractionProvider)runtimeRequirements).getUserInteraction();
			}
			
			SampleConfig sampleConfig = null;
			if (runtimeRequirements instanceof SampleConfigProvider){
				sampleConfig = ((SampleConfigProvider)runtimeRequirements).getSampleConfig();
			}
			
			Map<Class<?>, Object> config = getConfiguration(sampleConfig, interaction);
			if(config == null) {
				return null;
			}
			
			return executeCampaign(campaign, runtimeRequirements, config, callback);
		}
		throw new IllegalArgumentException("These resources can not be executed as a test campaign");
	}

	private Object executeCampaign(final GtTestCampaignProject campaign, RuntimeRequirementsProvider runtimeRequirements, final Map<Class<?>, Object> configuration, final TestExecutionCallback callback) {
		
		// execute the TestCampaign
		Job job = new Job("Test execution") {

			protected IStatus run(IProgressMonitor monitor) {
				// execute tests
				try {
					if (campaign != null) {
						Map<String, Object> emptyMap = Collections.emptyMap();
						campaign.getTestCampaign().executeTests(configuration, monitor, emptyMap, callback);
					} else {

						TestExecutionCallback.TestResult result = new TestExecutionCallback.TestResult();
						result.testCases = 0;
						result.overallResult = 4; //return Status.UNDEFINED on callback
						
						callback.testExecutionFinished(result);
						return Status.CANCEL_STATUS;
					}
				} catch (CoreException e) {
					GtErrorLogger.log(Activator.PLUGIN_ID, e);
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
		};
		job.setUser(true);
		job.schedule();

		return null;
	}
	
	protected Map<Class<?>, Object> getConfiguration(SampleConfig config, UserInteraction interaction) {
		
		Map<Class<?>, Object> configuration = new HashMap<>();
		
		//add SampleConfig
		if(config != null) {
			configuration.put(config.getClass(), config);
		}
		
		//add o.g.protocol.Activator 
		configuration.put(org.globaltester.protocol.Activator.class, org.globaltester.protocol.Activator.getDefault());
		
		if(interaction != null){
			configuration.put(UserInteraction.class, interaction);
		}
		
		return configuration;
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
