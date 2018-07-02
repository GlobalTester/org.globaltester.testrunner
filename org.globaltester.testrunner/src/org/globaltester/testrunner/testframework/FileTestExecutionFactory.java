package org.globaltester.testrunner.testframework;

import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestCase;
import org.jdom.Document;
import org.jdom.Element;

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
			createExecution(iFile);
		}
		
		return instances.get(iFile);
	}



	public static void createExecution(IFile iFile) throws CoreException {
		Document doc = XMLHelper.readDocument(iFile);
		Element rootElem = doc.getRootElement();
		
		if (TestCaseExecution.XML_ELEMENT.equals(rootElem.getName())) {
			instances.put(iFile, new TestCaseExecution(iFile));
		} else if (TestCampaignExecution.XML_ELEMENT.equals(rootElem.getName())) {
			instances.put(iFile, new TestCampaignExecution(iFile));
		} else if (TestSetExecution.XML_ELEMENT.equals(rootElem.getName())) {
			instances.put(iFile, new TestSetExecution(iFile));
		}
	}

	public static FileTestExecution createExecution(FileTestExecutable testExecutable,
			TestCampaign testCampaign) throws CoreException {

		IFile stateFile = null;
		if (testCampaign != null) {
			stateFile = testCampaign.getProject().getNewStateIFile(testExecutable);
		}

		if (testExecutable instanceof TestCase) {
			TestCaseExecution tcExecution = new TestCaseExecution(
					stateFile, (TestCase) testExecutable);
			return tcExecution;
		} else {
			throw new RuntimeException("Unsupported type of TestExecutable: "
					+ testExecutable);
		}
	}

	public static FileTestExecution createExecution(TestCampaign testCampaign) throws CoreException {
		IFile stateFile = testCampaign.getProject().getNewCampaignStateIFile();

		return new TestCampaignExecution(stateFile, testCampaign);
	}

}
