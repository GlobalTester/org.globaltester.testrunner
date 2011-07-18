package org.globaltester.testrunner.ui.commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PlatformUI;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;

public class CreateExecutionProjectCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();
		// check type of selection
		if (!(iSel instanceof TreeSelection)) {
			return null;
		}

		TreeSelection treeSel = (TreeSelection) iSel;
		// check size of selection
		if (treeSel.size() != 1) {
			return null;
		}
		//TODO configure command so that it is only enabled when a single file is selected

		Object firstSelectionElement = treeSel.getFirstElement();
		// check type of selected element
		if (!(firstSelectionElement instanceof IFile)) {
			return null;
		}

		//create the project
		try {
			createExecutionProject((IFile) firstSelectionElement);
		} catch (CoreException e) {
			throw new ExecutionException("ExecutionProject could not be created", e);
		}

		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
					.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new ExecutionException("Workspace could not be refreshed", e);
		}

		return null;
	}

	public static GtTestCampaignProject createExecutionProject(IFile testExecutableFile) throws ExecutionException, CoreException {
		// create the new TestCampaign project
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		IProject iProject = GtTestCampaignProject.createProject(
				testExecutableFile.getName() + "_" + sdf.format(cal.getTime()),
				null);
		GtTestCampaignProject runProject = GtTestCampaignProject.getProjectForResource(iProject);
		
		//add the selected resource to the list of executables
		TestExecutable testExecutable;
		try {
			testExecutable = TestExecutableFactory.getInstance(testExecutableFile);
		} catch (CoreException e) {
			throw new ExecutionException("No TestExectubale could be created from selected Resource", e);
		}
		runProject.addExecutable(testExecutable);
		
		return runProject;
	}

}
