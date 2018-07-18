package org.globaltester.testrunner.ui.commands;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.EditorPart;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.utils.GtDateHelper;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.globaltester.testspecification.testframework.TestLayer;
import org.globaltester.testspecification.testframework.TestSuiteLegacy;
import org.globaltester.testspecification.testframework.TestUnit;

public class CreateTestCampaignCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			// IMPL handle this case properly
			return null;
		}

		ISelection iSel = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getSelectionService().getSelection();

		// try to create the project
		GtTestCampaignProject newProject = null;
		try {
			Shell shell = HandlerUtil.getActiveWorkbenchWindow(event).getShell();
			newProject = createTestCampaignProject(iSel, shell);
		} catch (CoreException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}
		
		if (newProject == null) {
			//no project was created, user is already informed
			return null;
		} else {
			// open the new TestCampaign in the Test Campaign Editor
			try {
				GtUiHelper.openInEditor(newProject.getTestCampaignIFile());
			} catch (CoreException e) {
				// log Exception to eclipse log
				GtErrorLogger.log(Activator.PLUGIN_ID, e);

				// users most probably will ignore this behavior and open editor
				// manually, so do not open annoying dialog
			}
		}

		return newProject;
	}

	public static String getNewProjectName() {
		// construct the name of the new TestCampaign project
		String projectName = "TestCampaign_"
				+ GtDateHelper.getCurrentTimeString();
		return projectName;
	}

	/**
	 * Creates a TestCampaign using the selected Tests from the TestExplorer or
	 * the opened testcase in the TestEditor
	 * 
	 * @param projectName name of created project
	 * @param iSel selection that contains
	 * @param shell Shell used for error dialogs
	 * @return
	 * @throws CoreException
	 */
	public static GtTestCampaignProject createTestCampaignProject(
			String projectName, ISelection iSel, Shell shell) throws CoreException {
		
		LinkedList<IFile> selectedIFiles = GtUiHelper.getSelectedIResources(iSel, IFile.class);
		if (selectedIFiles.isEmpty()) {
			//try to use testfile opened in editor instead
			IWorkbenchPart activePart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
			
			if(activePart instanceof EditorPart){ //avoid NPE if e.g. the outline is selected
				EditorPart editor = (EditorPart) activePart;
					IWorkspace workspace= ResourcesPlugin.getWorkspace(); 
					IPathEditorInput input = (IPathEditorInput) editor.getEditorInput();
					IPath location= input.getPath();
					IFile file = workspace.getRoot().getFileForLocation(location); 
					selectedIFiles.add(file);
			} else {
				GtUiHelper.openErrorDialog(shell,
						"No TestCampaign could be created because selection does not contain an executable test file.");
					return null;
			}
		}

		return createTestCampaignProject(projectName, selectedIFiles, shell);
	}
	
	/**
	 * This method extracts TestCases from grouped entities (e.g. TestSuites, TestUnits and TestLayers)
	 * @param testExecutableFiles: A Collection of iFiles (Test Cases, Suites etc.)
	 * @return list of test files
	 */
	public static List<IFile> analyzeListOfTests(List<IFile> testExecutableFiles) {
		LinkedList<IFile> retVal = new LinkedList<>();
		
		for (IFile iFile : testExecutableFiles) {
			
			if(TestSuiteLegacy.isFileRepresentation(iFile)){
				retVal.addAll(TestSuiteLegacy.extractTests(iFile));
			} else if(TestUnit.isFileRepresentation(iFile)){
				retVal.addAll(TestUnit.extractTests(iFile));
			} else if(TestLayer.isFileRepresentation(iFile)){
				retVal.addAll(TestLayer.extractTests(iFile));
			} else {
				retVal.add(iFile);
			}
		}
		return retVal;
	}

	public static GtTestCampaignProject createTestCampaignProject(
			IFile testExecutableFile, Shell shell) throws CoreException {

		// build a list containing the given testExecutableFile
		LinkedList<IFile> listExecutableFiles = new LinkedList<IFile>();
		listExecutableFiles.add(testExecutableFile);

		// create the TestCampaignProject
		return createTestCampaignProject(getNewProjectName(),
				listExecutableFiles, shell);
	}

	public static GtTestCampaignProject createTestCampaignProject(
			String projectName, List<IFile> testExecutableFiles, Shell shell)
			throws CoreException {
		
		// create the new TestCampaign project
		IProject iProject = GtTestCampaignProject.createProject(projectName,
				null);
		GtTestCampaignProject runProject = GtTestCampaignProject
				.getProjectForResource(iProject);

		//Analyze TestSuites, Units and Layers for testcases
		testExecutableFiles=analyzeListOfTests(testExecutableFiles);
		
		// add the selected resources to the list of executables
		Iterator<IFile> execFilesIter = testExecutableFiles.iterator();
		while (execFilesIter.hasNext()) {
			IFile iFile = execFilesIter.next();
				
			FileTestExecutable testExecutable = null;
			try {
				testExecutable = TestExecutableFactory.getInstance(iFile);
			} catch (CoreException e) {

				continue;
			}
			if (testExecutable != null) {
				runProject.getTestCampaign().addExecutable(testExecutable);
			}
		}
		
		if (runProject.getTestCampaign().getTestSet().isEmpty()) {
			GtUiHelper.openErrorDialog(shell,
				"None of the given files represents an executable test. Refuse to create empty TestCampaign.");
			iProject.delete(true, true, new NullProgressMonitor());
			return null;
		}

		// save the new project
		runProject.doSave();

		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
					.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// refresh workspace failed
			// log CoreException to eclipse log
			GtErrorLogger.log(Activator.PLUGIN_ID, e);

			// users most probably will ignore this behavior and refresh
			// manually, so do not open annoying dialog
		}

		return runProject;
	}

	public static GtTestCampaignProject createTestCampaignProject(
			ISelection iSel, Shell shell) throws CoreException {
		return createTestCampaignProject(getNewProjectName(), iSel, shell);
	}

}
