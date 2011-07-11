package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.PlatformUI;
import org.globaltester.testrunner.GtTestRunNature;
import org.globaltester.testrunner.GtTestRunProject;
import org.globaltester.testspecification.GtTestSpecNature;

public class RunTestCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// check for dirty files and save them
		if (!PlatformUI.getWorkbench().saveAllEditors(true)) {
			return null;
		}

		ISelection iSel = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getSelectionService().getSelection();
		//check type of selection
		if (! (iSel instanceof TreeSelection)){
			return null;
		}
		
		TreeSelection treeSel = (TreeSelection) iSel;
		//check size of selection
		if (treeSel.size() != 1){
			return null;
		}
		
		Object firstSelectionElement = treeSel.getFirstElement();
		//check type of selected element
		if (! (firstSelectionElement instanceof IResource)){
			return null;
		}
		
		IResource selectedResource = (IResource) firstSelectionElement;
		
		
		//get the according run project instance
		GtTestRunProject project = null;
		try {
			if((selectedResource.getProject().hasNature(GtTestSpecNature.NATURE_ID))&&(selectedResource instanceof IFile)) {
				//create a new GtTestExecutionProject if testcase is selected from GtTestSpecificationProject
				project = CreateExecutionProjectCommandHandler.createExecutionProject((IFile) selectedResource);
			} else if (selectedResource.getProject().hasNature(GtTestRunNature.NATURE_ID)) {
				project = GtTestRunProject.getProjectForResource(selectedResource);	
			} else {
				throw new ExecutionException("No GtTestRunProject could be created form selected Resource");
			}
		} catch (CoreException e) {
			throw new ExecutionException("No GtTestRunProject could be created form selected Resource", e);
		}
		
		 

		//execute all unexecuted tests
		project.executeTests();
		
		//refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			throw new ExecutionException("Workspace could not be refreshed", e);
		}

		return null;
	}

}
