package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditorInput;
import org.globaltester.testspecification.ui.editors.TestSpecEditor;

public class RunTestCommandHandler extends org.globaltester.scriptrunner.ui.commands.RunTestCommandHandler {
	
	@Override
	protected IFile getFileFromEditor(IWorkbenchPart activePart){
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
	
	@Override
	protected SampleConfig getSampleConfig(ExecutionEvent event) {
		boolean selectionRequested = Boolean.parseBoolean(event.getParameter("org.globaltester.testrunner.ui.SelectSampleConfigParameter"));
		SampleConfig candidate;
		try {
			candidate = getSampleConfigFromResources();
		} catch (CoreException e) {
			GtUiHelper.openErrorDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), "Running failed: " + e.getMessage());
			return null;
		} 
		if (candidate != null && !selectionRequested){
			return candidate;	
		}
		return super.getSampleConfig(event);
	}
	
	private SampleConfig getSampleConfigFromResources() throws CoreException {
		for (IResource current : getResources()){
			if (GtTestCampaignProject.isTestCampaignProjectAvailableForResource(current)){
				TestCampaignExecution execution = GtTestCampaignProject.getProjectForResource(current).getTestCampaign().getCurrentExecution();
				if (execution != null){
					return execution.getSampleConfig();	
				}
			}
		}
		return null;
	}

	@Override
	protected void modifyWorkbench() {
		// does nothing as everything is shown in the campaign editor
	}
}
