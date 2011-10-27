package org.globaltester.testrunner.testframework;

import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.FileTestExecutable;

public class FileTestExecutionFactory {

	private static Hashtable<IFile, FileTestExecution> instances = new Hashtable<IFile, FileTestExecution>();

	/**
	 * Return the instance representing the state of the given IFile
	 * 
	 * @param iFile
	 * @return
	 * @throws CoreException
	 */
	public static FileTestExecution getInstance(IFile iFile) throws CoreException {

		if (!instances.containsKey(iFile)) {
			FileTestExecution newExecutionInstance = null;

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

	public static FileTestExecution createExecution(FileTestExecutable testExecutable,
			TestCampaign testCampaign) throws CoreException {

		IFile executionFile = testCampaign.getProject().getNewStateIFile(testExecutable);

		if (testExecutable instanceof TestCase) {
			TestCaseExecution tcExecution = new TestCaseExecution(
					executionFile, (TestCase) testExecutable);
			return tcExecution;
		}

		throw new RuntimeException("Unsupported type of TestExecutable: "
				+ testExecutable);
	}

	public static FileTestExecution createExecution(
			TestCampaignElement testCampaignElement) throws CoreException {
		//TODO handle multiple TetCampaignElements referencing the same SPEC 
		return createExecution(testCampaignElement.getExecutable(), (TestCampaign) testCampaignElement.getParent());
	}

}
