package org.globaltester.testrunner.ui;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
import org.globaltester.testrunner.ui.commands.CreateTestCampaignCommandHandler;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestSet;
import org.globaltester.testspecification.testframework.TestSuiteLegacy;

/**
 * This executes lists of test cases w/o a TestCampaign
 * @author amay
 *
 */
public class TestSetExecutor extends TestResourceExecutor {
	
	@Override
	public boolean canExecute(List<IResource> resources) {
		if (resources == null) return false;
		if (resources.isEmpty()) return false;
			
		for (IResource r : resources) {
			if (!(r instanceof IFile)) {
				return false;
			}
			
			if (!(
					((IFile) r).getFileExtension().equals(TestCase.FILE_ENDING_GT_TEST_CASE)
					|| ((IFile) r).getFileExtension().equals(TestSuiteLegacy.FILE_ENDING_GT_TEST_SUITE)
				)) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected String getLoggingDir(List<IResource> resources) throws CoreException {
		IFolder logDir = resources.iterator().next().getProject().getFolder("Logging");
		if (!logDir.exists()) {
			logDir.isSynchronized(IResource.DEPTH_ONE);
			try {
				logDir.create(false, true, new NullProgressMonitor());
			} catch (CoreException e) {
				throw new IllegalArgumentException("No logging directory could be or created.", e);
			}
		}
		
		return logDir.getLocation().toOSString();
	}

	@Override
	protected AbstractTestExecution buildTestExecution(List<IResource> resources) throws CoreException {

		List<IFile> files = new LinkedList<>();
		for (IResource r : resources){
			files.add((IFile)r);
		}
		
		//check TestUnits and TestLayers
		files = CreateTestCampaignCommandHandler.analyzeListOfTests(files);
		
		TestSet testSet = new TestSet(files);
		
		// create a new TestExecution this TestCampaignElement
		return new TestSetExecution(testSet, null);

	}

}
