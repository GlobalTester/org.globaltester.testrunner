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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;

public class CreateTestCampaignCommandHandler extends AbstractHandler {

	
	
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
		GtTestCampaignProject newProject;
		try {
			newProject = createExecutionProject((IFile) firstSelectionElement);
		} catch (CoreException e) {
			throw new ExecutionException("ExecutionProject could not be created", e);
		}
		
		//open the new TestCampaign in the Test Campaign Editor
		IFile file = newProject.getIProject().getFile("project.xml");
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			//TODO remove the hard coded reference to the TestCampaignEditor, use system default instead after configuring it as default
			IDE.openEditor(page, file, TestCampaignEditor.ID);
		} catch (PartInitException e) {
			// opening new project in editor failed
			// log CoreException to eclipse log
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
			
			// users most probably will ignore this behavior and open editor manually, so do not open annoying dialog
		}

		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
					.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// refresh workspace failed
			// log CoreException to eclipse log
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
			
			// users most probably will ignore this behavior and refresh manually, so do not open annoying dialog
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
		runProject.getTestCampaign().addExecutable(testExecutable);
		
		return runProject;
	}

}
