package org.globaltester.testrunner.ui.editor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.ui.Activator;

public class TestCampaignEditorMatchingStrategy implements
		IEditorMatchingStrategy {

	@Override
	public boolean matches(IEditorReference editorRef, IEditorInput input) {
		try {
			IEditorInput editorInput = editorRef.getEditorInput();
			return (editorInput != null)&& editorInput.equals(input);
			
		} catch (PartInitException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}
		return false;
	}

}
