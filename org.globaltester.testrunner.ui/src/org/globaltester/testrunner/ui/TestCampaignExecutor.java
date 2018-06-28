package org.globaltester.testrunner.ui;

import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.scriptrunner.TestExecutor;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.FileTestExecutionFactory;

/**
 * This implementation of {@link TestExecutor} executes TestCampaigns.
 * 
 * @author mboonk
 *
 */
public class TestCampaignExecutor extends TestResourceExecutor {

	public boolean canExecute(List<IResource> resources) {
		if (resources == null) return false;
		if (resources.size() != 1)
			return false;
		IResource resource = resources.iterator().next();

		if (GtTestCampaignProject.isTestCampaignProjectAvailableForResource(resource)) {
			return true;
		}

		return false;
	}	

	@Override
	protected String getLoggingDir(List<IResource> resources) throws CoreException {

		// initialize test logging for this test session
		GtTestCampaignProject project = GtTestCampaignProject.getProjectForResource(resources.iterator().next());

		IFolder defaultLoggingDir = project.getDefaultLoggingDir();
		GtResourceHelper.createWithAllParents(defaultLoggingDir);
		return project.getNewResultDir();

	}

	@Override
	protected AbstractTestExecution buildTestExecution(List<IResource> resources) throws CoreException {
		if (!canExecute(resources)) throw new IllegalArgumentException("Can't create a TestExecution for this list of resources");

		GtTestCampaignProject campaignProject = GtTestCampaignProject
				.getProjectForResource(resources.iterator().next());

		return FileTestExecutionFactory.createExecution(campaignProject.getTestCampaign());
	}
}
