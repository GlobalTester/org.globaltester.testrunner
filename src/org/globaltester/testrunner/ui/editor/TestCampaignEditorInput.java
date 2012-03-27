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
		return project.getTestCampaign().getCurrentExecution();
	}
	
	public void stepToNewest(){
		indexOfCurrentExecution = executions.indexOf(project.getTestCampaign().getCurrentExecution());
	}
	
	/**
	 * 
	 * Makes a step forward in the list of TestCampaignExecutions
	 * 
	 */
	public void stepForward(){
		if (isStepForwardsPossible()){
			indexOfCurrentExecution--;
		}
	}
	
	/**
	 * 
	 * Makes a step backwards in the list of TestCampaignExecution
	 * 
	 */
	public void stepBackward(){
		if (isStepBackwardsPossible()){
			indexOfCurrentExecution++;
		}
	}

	public boolean isStepForwardsPossible(){
		return indexOfCurrentExecution > 0;
	}
	
	public boolean isStepBackwardsPossible(){
		return indexOfCurrentExecution < executions.size() -1;
	}
	
	/**
	 * @return the currently displayed execution or null if there is no execution yet
	 */
	public TestCampaignExecution getCurrentlyDisplayedTestCampaignExecution(){
		if (executions.size() > indexOfCurrentExecution){
			return executions.get(indexOfCurrentExecution);
		}
		return null;
	}

}
