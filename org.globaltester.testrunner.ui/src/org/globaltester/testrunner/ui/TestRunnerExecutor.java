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
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.cardconfiguration.CardConfigManager;
import org.globaltester.cardconfiguration.GtCardConfigNature;
import org.globaltester.cardconfiguration.ui.CardConfigSelectorDialog;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.TestResourceExecutor;
import org.globaltester.testrunner.testframework.TestCampaignExecution;

/**
 * This implementation of {@link TestResourceExecutor} executes TestCampaigns.
 * @author mboonk
 *
 */
public class TestRunnerExecutor implements TestResourceExecutor {

	protected boolean canExecute(List<IResource> resources) {
		return resources.size() == 1 && resources.iterator().next().getFileExtension().equals("gtcampaign");
	}

	@Override
	public Object execute(List<IResource> resources, Map<?, ?> parameters) {

		Map<Class<?>, Object> configuration = new HashMap<>();
		
		if (canExecute(resources)){
			GtTestCampaignProject campaign;
			try {
				campaign = GtTestCampaignProject.getProjectForResource(resources.iterator().next());
			} catch (CoreException e) {
				throw new IllegalArgumentException("No test campaign project could be found for the given resources.");
			}
			CardConfig config = getConfiguration(parameters, campaign);
			configuration.put(config.getClass(), config);
			return executeCampaign(campaign, configuration);
		}
		throw new IllegalArgumentException("These resources can not be executed as a test campaign");
	}

	private Object executeCampaign(final GtTestCampaignProject campaign, final Map<Class<?>, Object> configuration) {
		
		// execute the TestCampaign
		Job job = new Job("Test execution") {

			protected IStatus run(IProgressMonitor monitor) {
				// execute tests
				try {
					if (campaign != null) {
						campaign.getTestCampaign().executeTests(configuration, monitor, Collections.emptyMap());
					} else {
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

				// open the new TestCampaign in the Test Campaign Editor
				PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

							@Override
							public void run() {
								try {
									GtUiHelper.openInEditor(campaign
											.getTestCampaignIFile());
								} catch (CoreException e) {
									// log Exception to eclipse log
									GtErrorLogger.log(Activator.PLUGIN_ID, e);

									// users most probably will ignore this
									// behavior and open editor manually, so do
									// not open annoying dialog
								}
							}
						});

				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}


	private CardConfig getLastCardConfigFromTestCampaignProject(
			GtTestCampaignProject parentCampaingProject) {
		if (parentCampaingProject == null){
			return null;
		}
		TestCampaignExecution currentExecution = parentCampaingProject
				.getTestCampaign().getCurrentExecution();
		if (currentExecution != null) {
			CardConfig config = currentExecution.getCardConfig();
			if (config != null){
				String cardConfigName = currentExecution.getCardConfig().getName();
				
				if (CardConfigManager.isAvailableAsProject(cardConfigName)) {
					return CardConfigManager.get(cardConfigName);
				}	
			}
		}
		return null;
	}

	protected CardConfig getConfiguration(Map<?, ?> parameters, GtTestCampaignProject campaign) {
		// try to get CardConfig
		CardConfig cardConfig = null;
		Object parameter = parameters.get("org.globaltester.testrunner.ui.SelectCardConfigParameter");
		String selectCardConfigParam = parameter == null ? null : parameter.toString();
		boolean forceSelection = (selectCardConfigParam != null)
				&& selectCardConfigParam.trim().toLowerCase().equals("true");
		if (!forceSelection) {
			if (campaign != null){
				// try to get CardConfig from last CampaignExecution
				cardConfig = getLastCardConfigFromTestCampaignProject(campaign);	
			}

			// try to get CardConfig from Selection if none was defined in
			// TestCampaign
			if (cardConfig == null) {
				cardConfig = getFirstCardConfigFromSelection();
			}
		}

		// ask user for CardConfig if none was selected
		if (cardConfig == null) {
			CardConfigSelectorDialog dialog = new CardConfigSelectorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
			if (dialog.open() != Window.OK) {
				return null;
			}
			cardConfig = dialog.getSelectedCardConfig();
		}
		return cardConfig;
	}

	private CardConfig getFirstCardConfigFromSelection() {
		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();

		LinkedList<IResource> iResources = GtUiHelper.getSelectedIResources(
				iSel, IResource.class);
		for (IResource iFile : iResources) {
			IProject iProject = iFile.getProject();
			try {
				if (iProject.hasNature(GtCardConfigNature.NATURE_ID)) {
					return CardConfigManager.get(iProject.getName());
				}
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
		return null;
	}
}
