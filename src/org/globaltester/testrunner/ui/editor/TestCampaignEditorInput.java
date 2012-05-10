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
		super(GtTestCampaignProject.getProjectForResource(file)
				.getTestCampaignIFile());
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

	public TestCampaign getTestCampaign() {
		return project.getTestCampaign();
	}

	public TestCampaignExecution getCurrentTestCampaignExecution() {
		return project.getTestCampaign().getCurrentExecution();
	}

	public void stepToNewest() {
		indexOfCurrentExecution = executions.indexOf(project.getTestCampaign()
				.getCurrentExecution());
	}

	public void stepToOldest() {
		indexOfCurrentExecution = executions.size() - 1;
	}

	public void stepToIndex(int newIndex) {
		if ((newIndex >= 0) && (newIndex < executions.size())) {
			indexOfCurrentExecution = newIndex;
		}
	}

	/**
	 * 
	 * Makes a step forward in the list of TestCampaignExecutions
	 * 
	 */
	public void stepForward() {
		if (isStepForwardsPossible()) {
			indexOfCurrentExecution--;
		}
	}

	/**
	 * 
	 * Makes a step backwards in the list of TestCampaignExecution
	 * 
	 */
	public void stepBackward() {
		if (isStepBackwardsPossible()) {
			indexOfCurrentExecution++;
		}
	}

	public boolean isStepForwardsPossible() {
		return indexOfCurrentExecution > 0;
	}

	public boolean isStepBackwardsPossible() {
		return indexOfCurrentExecution < executions.size() - 1;
	}

	/**
	 * @return the currently displayed execution or null if there is no
	 *         execution yet
	 */
	public TestCampaignExecution getCurrentlyDisplayedTestCampaignExecution() {
		if (executions.size() > indexOfCurrentExecution) {
			return executions.get(indexOfCurrentExecution);
		}
		return null;
	}

	/**
	 * Calculates a an Array of String containig lables representing the
	 * TestCampaignExecutions while mentioning name execution start time and (if
	 * present) the associated comment.
	 * 
	 * @return
	 */
	public String[] getArrayOfTestCampaignExecutions() {
		String[] execStrings = new String[executions.size()];
		for (int i = 0; i < executions.size(); i++) {
			TestCampaignExecution curExec = executions.get(i);
			execStrings[i] = curExec.getName() + " ("
					+ curExec.getLastExecutionStartTimeAsString() + ") - " +
			curExec.getStatus();
			String comment = curExec.getComment();
			if ((comment != null) && (comment.trim().length() > 0)) {
				execStrings[i] += "// " + comment.trim();
			}
		}
		return execStrings;
	}

	public int getIndexOfCurrentlyDisplayedTestCampaignExecution() {
		return indexOfCurrentExecution;
	}

}
