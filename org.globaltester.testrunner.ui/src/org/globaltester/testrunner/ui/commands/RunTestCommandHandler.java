package org.globaltester.testrunner.ui.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.cardconfiguration.CardConfigManager;
import org.globaltester.cardconfiguration.GtCardConfigNature;
import org.globaltester.cardconfiguration.ui.CardConfigSelectorDialog;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditorInput;
import org.globaltester.testspecification.ui.editors.TestSpecEditor;

public class RunTestCommandHandler extends AbstractHandler {
	/**
	 * sets up environment, e.g. prepares settings for debugging threads and
	 * launches and starts them, dependent on what is currently activated and
	 * needed.
	 * 
	 * @param event
	 *            which triggers the handler and delivers information on
	 *            selected resource etc.
	 * @param envSettings used for adding or retrieving environment information
	 * @throws RuntimeException in case of errors
	 */
	protected void setupEnvironment(ExecutionEvent event, Map<String, Object> envSettings)  throws RuntimeException {
		// does nothing special here; can be overridden by derived classes
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}
		
		ISelection iSel = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		LinkedList<IResource> resources = GtUiHelper.getSelectedIResources(iSel, IResource.class);
		
		
		if (resources.size() == 0){
			//try to get file from editor
			IFile file = getFileFromEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart());
			if (file != null){
				resources.add(file);
			}
		}
		
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		
		if (resources.size() == 0){
			GtUiHelper.openErrorDialog(shell, "Select executable files or an editor for execution of test cases.");
			return null;
		}
		

		Map<Class<?>, Object> configuration = new HashMap<>();
		
		if (resources.size() == 1 && resources.get(0).getFileExtension().equals("gtcampaign")){
			try {
				GtTestCampaignProject campaign = GtTestCampaignProject.getProjectForResource(resources.get(0));
				CardConfig config = getConfiguration(event, campaign);
				configuration.put(config.getClass(), config);
				return executeCampaign(campaign, configuration);
			} catch (CoreException e) {
				GtUiHelper.openErrorDialog(shell, "The test campaign project could not be opened.");
				return null;
			}
		} else {
			CardConfig config = getConfiguration(event, null);
			configuration.put(config.getClass(), config);
			return executeInDialog(configuration);
		}

	}
	
	private Object executeInDialog(Map<Class<?>, Object> configuration){
		GlobalTesterAction action = new GlobalTesterAction();
		action.init(PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		action.run(configuration);
		return null;
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

	private CardConfig getConfiguration(ExecutionEvent event, GtTestCampaignProject campaign) {
		// try to get CardConfig
		CardConfig cardConfig = null;
		String selectCardConfigParam = event
				.getParameter("org.globaltester.testrunner.ui.SelectCardConfigParameter");
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
			CardConfigSelectorDialog dialog = new CardConfigSelectorDialog(
					HandlerUtil.getActiveWorkbenchWindow(event).getShell());
			if (dialog.open() != Window.OK) {
				return null;
			}
			cardConfig = dialog.getSelectedCardConfig();
		}
		return cardConfig;
	}
	
	private IFile getFileFromEditor(IWorkbenchPart activePart){
		if (activePart instanceof TestSpecEditor) {
			FileEditorInput editorInput = (FileEditorInput) ((TestSpecEditor) activePart).getEditorInput();
			return editorInput.getFile();
		}
		if (activePart instanceof TestCampaignEditor) {
			TestCampaignEditorInput editorInput = (TestCampaignEditorInput) 
					((TestCampaignEditor) activePart).getEditorInput();
			try {
				return editorInput.getGtTestCampaignProject().getTestCampaignIFile();
			} catch (CoreException e) {
				// expected behavior for some inputs
			}
		}
		return null;
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
}
