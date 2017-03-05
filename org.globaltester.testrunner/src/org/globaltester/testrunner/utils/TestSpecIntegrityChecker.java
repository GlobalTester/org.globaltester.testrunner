package org.globaltester.testrunner.utils;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;

/**
 * This class encapsulates behavior to check the integrity of available
 * TestSpecifications (either from {@link IProjects} or the {@link IFolders}
 * persisted inside of {@link GtTestCampaignProjects}.
 * 
 * @author may.alexander
 *
 */
public class TestSpecIntegrityChecker {

	private HashSet<IResource> specsToCheck = new HashSet<>();

	/**
	 * Adds specifications to the list of specifications to be checked by this
	 * {@link TestSpecIntegrityChecker}
	 * 
	 * @param additionalSpecsToCheck
	 */
	public void addSpecsToCheck(IResource[] additionalSpecsToCheck) {
			specsToCheck.addAll(Arrays.asList(additionalSpecsToCheck));
	}

	
	/**
	 * 
	 * @param storeIntegrityViolations
	 * @return
	 */
	public boolean check(boolean storeIntegrityViolations) {
		// FIXME implement this method based on code refactored from o.g.testmanager.testframework.FileChecksum
		return false;
	}
}
