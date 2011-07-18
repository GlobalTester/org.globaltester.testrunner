package org.globaltester.testrunner;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.AssertionFailedException;
import org.junit.Assert;
import org.junit.Test;

public class GtTestCampaignProjectTest {

	@Test(expected = AssertionFailedException.class)
	public void testCreateProjectWithEmptyNameArg() {
		String projectName = " "; //$NON-NLS-1$
		GtTestCampaignProject.createProject(projectName, null);
	}

	@Test(expected = AssertionFailedException.class)
	public void testCreateProjectWithNullNameArg() {
		String projectName = null;
		GtTestCampaignProject.createProject(projectName, null);
	}

	@Test
	public void testCreateProjectWithGoodArgs() throws Exception {
		String projectName = "junitTestProject-deleteMe";

		IProject project = GtTestCampaignProject.createProject(projectName,
				null);

		// check nature is added
		Assert.assertTrue("GtTestRunNature is not correctly added",
				project.hasNature(GtTestCampaignNature.NATURE_ID));

		// check directory structure is created correctly
		String[] paths = { "DUTconfiguration", "ExecutionState", "TestSpecification",
		"TestResults" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		String projectPath = project.getLocation().toString();
		for (String path : paths) {
			File file = new File(projectPath + "/" + path);
			if (!file.exists()) {
				Assert.fail("Folder structure " + path + " does not exist.");
			}
		}

		// delete the project after the test
		project.delete(true, null);
	}

}
