package org.globaltester.testrunner.ui.editor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaign;

public class TestCampaignEditorInput implements IEditorInput {

	private GtTestCampaignProject project;
	
	public TestCampaignEditorInput(FileEditorInput input) throws CoreException {
		// TODO Auto-generated constructor stub
		project = GtTestCampaignProject.getProjectForResource(input.getFile());
	}

	@SuppressWarnings("rawtypes")	// is required by interface
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO check whether the corresponding file exists
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO IMAGE
		return ImageDescriptor.getMissingImageDescriptor();
	}

	@Override
	public String getName() {
		return project.getName();
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return project.getIProject().getName();
	}

	public GtTestCampaignProject getGtTestCampaignProject() {
		return project;
	}

	public TestCampaign getTestCampaign() {
		return project.getTestCampaign();
	}

}
