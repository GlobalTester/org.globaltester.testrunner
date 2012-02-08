package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.globaltester.testrunner.GtTestCampaignProject;

public class RunTestCommandHandler extends AbstractHandler implements IWorkbenchWindowActionDelegate {

	
	/**
	 * Constructor of RunTestCommandHandler
	 */
	public RunTestCommandHandler() {
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		ISelection iSel = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService().getSelection();
		
		//try to create a TestCampaignProject from current selection
		GtTestCampaignProject campaingProject;
		try {
			campaingProject = CreateTestCampaignCommandHandler.createTestCampaignProject(iSel);
		} catch (CoreException e) {
			throw new ExecutionException("TestCampaign could not be created from current selection", e);
		}
		
		//execute all unexecuted tests
		try {
			if (campaingProject != null) {
				campaingProject.getTestCampaign().executeTests();
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
		CreateTestCampaignCommandHandler.openInEditor(campaingProject);

		return null;
	}

	@Override
	public void run(IAction action) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub
		
	}

}
