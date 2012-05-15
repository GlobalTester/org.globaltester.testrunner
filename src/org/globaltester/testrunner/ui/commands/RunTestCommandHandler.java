package org.globaltester.testrunner.ui.commands;

import java.util.LinkedList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.cardconfiguration.CardConfigManager;
import org.globaltester.cardconfiguration.GtCardConfigNature;
import org.globaltester.cardconfiguration.ui.CardConfigSelectorDialog;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditorInput;

public class RunTestCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		
		//get TestCampaign and CardConfig from user selection
		GtTestCampaignProject campaingProject = null;
		CardConfig cardConfig = null;
		
		IWorkbenchPart activePart = Activator.getDefault().getWorkbench()
		.getActiveWorkbenchWindow().getActivePage().getActivePart();
		
		if (activePart instanceof EditorPart) {
			campaingProject = getCampaignProjectFromEditor(activePart);
			cardConfig = getCardConfigFromEditor(activePart);
		} else {
			ISelection iSel = PlatformUI.getWorkbench()
			.getActiveWorkbenchWindow().getSelectionService().getSelection();
			
			campaingProject = getCampaignProjectFromSelection(iSel);
			cardConfig = getFirstCardConfigFromSelection(iSel);
		}
		
		//ask user for CardConfig if none was selected
		if (cardConfig == null) {
			CardConfigSelectorDialog dialog = new CardConfigSelectorDialog(HandlerUtil.getActiveWorkbenchWindow(event).getShell());
			if (dialog.open() != Window.OK) {
				return null;
			}
			cardConfig = dialog.getSelectedCardConfig();
		}
	
		
		//execute the TestCampaign
		try {
			if (campaingProject != null) {
				campaingProject.getTestCampaign().executeTests(cardConfig);
			}
		} catch (CoreException e) {
			throw new ExecutionException("Test execution failed", e);
		}
		
		//refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new ExecutionException("Workspace could not be refreshed", e);
		}
		
		// open the new TestCampaign in the Test Campaign Editor
		try {
			GtUiHelper.openInEditor(campaingProject.getTestCampaignIFile());
		} catch (CoreException e) {
			// log Exception to eclipse log
			GtErrorLogger.log(Activator.PLUGIN_ID, e);

			// users most probably will ignore this behavior and open editor
			// manually, so do not open annoying dialog
		}

		return null;
	}

	private GtTestCampaignProject getCampaignProjectFromSelection(
			ISelection iSel) throws ExecutionException {
		//try to create a TestCampaignProject from current selection
		try {
			return CreateTestCampaignCommandHandler.createTestCampaignProject(iSel);
		} catch (CoreException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e); 
		}
		return null;
	}

	private GtTestCampaignProject getCampaignProjectFromEditor(
			IWorkbenchPart activePart) {
		if (activePart instanceof TestCampaignEditor) {
			TestCampaignEditorInput editorInput = (TestCampaignEditorInput) ((TestCampaignEditor) activePart).getEditorInput();
			return editorInput.getGtTestCampaignProject();
		}
		return null;
	}

	private CardConfig getFirstCardConfigFromSelection(ISelection iSel) {
		LinkedList<IResource> iResources = GtUiHelper.getSelectedIResource(iSel, IResource.class);
		for (IResource iFile : iResources) {
			IProject iProject = iFile.getProject();
			try {
				if (iProject.hasNature(GtCardConfigNature.NATURE_ID)){
					return CardConfigManager.get(iProject.getName());
				}
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
		return null;
	}

	private CardConfig getCardConfigFromEditor(IWorkbenchPart activePart) {
		if (activePart instanceof TestCampaignEditor) {
			return ((TestCampaignEditor) activePart).getSelectedCardConfig();
		}
		return null;
	}

}
