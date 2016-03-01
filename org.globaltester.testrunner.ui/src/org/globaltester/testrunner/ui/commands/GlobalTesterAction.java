/*
 * Project GlobalTester-Plugin File GlobalTesterAction.java
 * 
 * Date 14.10.2005
 * 
 * 
 * Developed by HJP Consulting GmbH Lanfert 24 33106 Paderborn Germany
 * 
 * 
 * This software is the confidential and proprietary information of HJP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * Non-Disclosure Agreement you entered into with HJP.
 */

package org.globaltester.testrunner.ui.commands;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.globaltester.testmanager.Activator;
import org.globaltester.testmanager.launcher.GlobalTesterThread;

/**
 * The main class to be used when the plugin is started.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */

public class GlobalTesterAction implements IWorkbenchWindowActionDelegate {

	// Used to store parent window
	private static IWorkbenchWindow window;

	// The current working directory
	private String workingDirectory;

	/**
	 * Constructor of GlobalTesterAction
	 */
	public GlobalTesterAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	@SuppressWarnings("unchecked")
	public void run(IAction action) {

		//check whether the working dir in preferences is still correct
		recalcWorkingDirectory();
		workingDirectory = Activator.getWorkingDir();

		// check for dirty files and save them
		if (!Activator.getDefault().getWorkbench().saveAllEditors(true)) {
			return;
		}

		List<String> testList = null;

		ISelection i = Activator.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService().getSelection();

		// this way runs with Eclipse 3.1 and 3.2
		if (i instanceof IStructuredSelection) {
			IStructuredSelection currentSelection = (IStructuredSelection) i;
			testList = getTestFileList(currentSelection.toList());

			// this way runs with Eclipse 3.1 and 3.2
			if ((testList == null) || (testList.size() == 0)) {
				// The selection might be an editor:
				testList = getFilesFromEditor();
			}
		} else {
			testList = getFilesFromEditor();
		}

		if ((testList == null) || (testList.size() == 0)) {
			return;
		}

		IWorkbenchPage page = ((IWorkbenchWindow) window).getActivePage();

		// show result view in GT perspective
		try {
			page.showView("org.globaltester.testmanager.views.ResultView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		// show problem view in GT perspective
		try {
			window.getActivePage().showView("org.eclipse.ui.views.ProblemView");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// show console view in GT perspective
		try {
			page.showView("org.eclipse.ui.console.ConsoleView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}

		//TestSession testSession = new TestSession(testList);
		GlobalTesterThread gtThread = new GlobalTesterThread("GlobalTester");
		gtThread.setFileList(testList);
		
		//FIXME what exactly does this accomplish?
		gtThread.run();
	}

	private List<String> getFilesFromEditor() {
		
		List<String> testList = null;
		IEditorPart editorpart = null;
		editorpart = Activator.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		if ((editorpart == null)|| (editorpart.getEditorInput() == null )) {
			return null;
		}
		
		IFile ff = (IFile) editorpart.getEditorInput().getAdapter(IFile.class);
		if (ff == null) {
			return null;
		}

		ArrayList<IFile> dummy = new ArrayList<IFile>();
		dummy.add(ff);
		testList = getTestFileList(dummy);
		
		workingDirectory = ff.getProject().getLocation().toOSString();
		Activator.setWorkingDir(workingDirectory);

		return testList;
	}

	/**
	 * Use this method to get the current working directory.
	 * 
	 * @return String working directory
	 */
	private String recalcWorkingDirectory() {

		String fileName = null;
		// get the current workspace and the selected project:
		IPath location = null;
		ISelection selection = Activator.getDefault().getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getSelection();
		IStructuredSelection ssel = StructuredSelection.EMPTY;
		//IStructuredSelection selection;
		if (selection instanceof IStructuredSelection) {
			ssel = (IStructuredSelection) selection;
		}
		if (!ssel.isEmpty()) {
			Object obj = ssel.getFirstElement();
			IResource resource = null;
			if (obj instanceof IResource) {
				resource = (IResource) obj;
			} else if (obj instanceof IAdaptable) {
				resource = (IResource) ((IAdaptable) obj)
						.getAdapter(IResource.class);
			}

			if (resource != null) {
				IProject project = resource.getProject();
				if (project != null) {
					location = project.getLocation();
				}
			}
		}

		if (location != null) {
			fileName = location.toOSString();
			
			if (fileName != null) {
				Activator.setWorkingDir(fileName);
			}
		}
		return fileName;
	}

	/**
	 * Delivers a list of test files with their complete name including the
	 * directory
	 * 
	 * @param List
	 *            of test files
	 * @return List of test files
	 */
	private List<String> getTestFileList(List<IFile> testFiles) {

		List<String> fileNames = new LinkedList<String>();

		for (int j = 0; j < testFiles.size(); j++) {
			fileNames.add(testFiles.get(j).getLocation().toString());

		}
		return fileNames;
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		GlobalTesterAction.window = window;
	}
}
