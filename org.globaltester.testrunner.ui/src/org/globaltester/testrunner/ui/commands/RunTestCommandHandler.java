package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.scriptrunner.TestResourceExecutor;
import org.globaltester.testrunner.ui.TestRunnerExecutor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor;
import org.globaltester.testrunner.ui.editor.TestCampaignEditorInput;
import org.globaltester.testspecification.ui.editors.TestSpecEditor;

public class RunTestCommandHandler extends org.globaltester.scriptrunner.ui.commands.RunTestCommandHandler {
	@Override
	protected TestResourceExecutor getExecutor() {
		return new TestRunnerExecutor();
	}
	
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
}
