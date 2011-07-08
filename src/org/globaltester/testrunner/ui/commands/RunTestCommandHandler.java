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
import org.globaltester.testrunner.GtTestRunProject;

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
		if (! (firstSelectionElement instanceof IFile)){
			return null;
		}
		 
		IFile selectedResource = (IFile) firstSelectionElement;
		
		//create the new testrun project
		GtTestRunProject.createProject(selectedResource.getName()+"_run", null);
		
		//refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		return null;
	}

}
