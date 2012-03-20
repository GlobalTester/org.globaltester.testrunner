package org.globaltester.testrunner.ui.editor;

import java.util.List;

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
	private int indexOfCurrentExecution;
	List<TestCampaignExecution> executions;
	
	public TestCampaignEditorInput(IFile file) throws CoreException {
		super(GtTestCampaignProject.getProjectForResource(file).getTestCampaignIFile());
		project = GtTestCampaignProject.getProjectForResource(file);
		executions = project.getTestCampaign().getCampaignExecutions();
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
		TestCampaignExecution result = project.getTestCampaign().getCurrentExecution();
		indexOfCurrentExecution = executions.indexOf(result);
		return project.getTestCampaign().getCurrentExecution();
	}
	
	/**
	 * 
	 * Makes a step forward in the list of TestCampaignExecutions
	 * 
	 * @return true if there is another step possible in this direction
	 */
	public boolean stepForward(){
		if (indexOfCurrentExecution > 0){
			indexOfCurrentExecution--;
			// check if end of list
			if (indexOfCurrentExecution == 0){
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * Makes a step backwards in the list of TestCampaignExecutions
	 * 
	 * @return true if there is another step possible in this direction
	 */
	public boolean stepBackward(){
		if (indexOfCurrentExecution < executions.size()-1){
			indexOfCurrentExecution++;
			// check if end of list
			if (indexOfCurrentExecution == executions.size()-1){
				return false;
			}
		}
		return true;
	}
	
	public TestCampaignExecution getCurrentlyDisplayedTestCampaignExecution(){
		return executions.get(indexOfCurrentExecution);
	}

}
