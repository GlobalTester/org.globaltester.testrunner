package org.globaltester.testrunner.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.UiImages;

public class TestCampaignEditorInput extends FileEditorInput {

	private GtTestCampaignProject project;

	public TestCampaignEditorInput(IFile file) throws CoreException {
		super(GtTestCampaignProject.getProjectForResource(file).getTestCampaignIFile());
		project = GtTestCampaignProject.getProjectForResource(file);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return UiImages.TESTCAMPAIGN.getImageDescriptor();
	}

	@Override
	public String getName() {
		return project.getName();
	}

	@Override
	public String getToolTipText() {
		return project.getIProject().getName();
	}

	public GtTestCampaignProject getGtTestCampaignProject() {
		return project;
	}

	public TestCampaign getTestCampaign(){
		return project.getTestCampaign();
	}
	
	public TestCampaignExecution getCurrentTestCampaignExecution() {
		return project.getTestCampaign().getCurrentExecution();
	}

}
