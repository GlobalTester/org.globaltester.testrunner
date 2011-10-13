package org.globaltester.testrunner.ui.commands;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
			//TODO handle this case properly
			return null;
		}

		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();

		// try to create the project
		GtTestCampaignProject newProject;
		try {
			newProject = createTestCampaignProject(getNewProjectName(), iSel);
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

	public static String getNewProjectName() {
		// construct the name of the new TestCampaign project
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		Calendar cal = Calendar.getInstance();
		String projectName = "TestCampaign_"
				+ sdf.format(cal.getTime());
		return projectName;
	}

	public GtTestCampaignProject createTestCampaignProject(String projectName, ISelection iSel) throws ExecutionException, CoreException {
		
		LinkedList<IFile> selectedIFiles = new LinkedList<IFile>();
		
		// check type of selection
		if (iSel instanceof IStructuredSelection) {
			Iterator<?> selectionIter = ((IStructuredSelection) iSel).iterator();
			while (selectionIter.hasNext()) {
				Object curElem = (Object) selectionIter.next();
				if (curElem instanceof IFile) {
					selectedIFiles.add((IFile) curElem);
				}
			}
		}
		
		if (selectedIFiles.isEmpty()){
			throw new ExecutionException("No TestCampaignProject could be created because selection does not contain an IFile");
		}

		return createTestCampaignProject(projectName, selectedIFiles);
	}

	

	public static GtTestCampaignProject createTestCampaignProject(
			IFile testExecutableFile) throws ExecutionException, CoreException {

		// build a list containing the given testExecutableFile
		LinkedList<IFile> listExecutableFiles = new LinkedList<IFile>();
		listExecutableFiles.add(testExecutableFile);

		// create the TestCampaignProject
		return createTestCampaignProject(getNewProjectName(), listExecutableFiles);
	}

	public static GtTestCampaignProject createTestCampaignProject(
			String projectName, Collection<IFile> testExecutableFiles)
			throws ExecutionException, CoreException {
		// create the new TestCampaign project
		IProject iProject = GtTestCampaignProject.createProject(projectName,
				null);
		GtTestCampaignProject runProject = GtTestCampaignProject
				.getProjectForResource(iProject);

		// add the selected resources to the list of executables
		Iterator<IFile> execFilesIter = testExecutableFiles.iterator();
		while (execFilesIter.hasNext()) {
			IFile iFile = execFilesIter.next();

			TestExecutable testExecutable;
			try {
				testExecutable = TestExecutableFactory.getInstance(iFile);
			} catch (CoreException e) {
				throw new ExecutionException(
						"No TestExectubale could be created from one of the selected resources",
						e);
			}
			runProject.getTestCampaign().addExecutable(testExecutable);
		}

		// save the new project
		runProject.doSave();

		return runProject;
	}

}
