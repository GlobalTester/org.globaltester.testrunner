package org.globaltester.testrunner.testframework;

import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestExecutable;

public class TestExecutionFactory {

	private static Hashtable<IFile, TestExecution> instances = new Hashtable<IFile, TestExecution>();

	/**
	 * Return the instance representing the state of the given IFile
	 * 
	 * @param iFile
	 * @return
	 * @throws CoreException
	 */
	public static TestExecution getInstance(IFile iFile) throws CoreException {

		if (!instances.containsKey(iFile)) {
			TestExecution newExecutionInstance = null;

			if (TestCaseExecution.isFileRepresentation(iFile)) {
				newExecutionInstance = new TestCaseExecution(iFile);
			}

			if (newExecutionInstance != null) {
				instances.put(iFile, newExecutionInstance);
			} else {
				return null;
			}
		}

		return instances.get(iFile);
	}

	public static TestExecution createExecution(TestExecutable testExecutable,
			TestCampaign testCampaign) throws CoreException {

		IFile executionFile = testCampaign.getProject().getStateIFile(testExecutable);

		if (testExecutable instanceof TestCase) {
			TestCaseExecution tcExecution = new TestCaseExecution(
					executionFile, (TestCase) testExecutable);
			return tcExecution;
		}

		throw new RuntimeException("Unsupported type of TestExecutable: "
				+ testExecutable);
	}

	public static TestExecution createExecution(
			TestCampaignElement testCampaignElement) throws CoreException {
		//TODO handle multiple TetCampaignElements referencing the same SPEC 
		return createExecution(testCampaignElement.getExecutable(), testCampaignElement.getParent());
	}

}
