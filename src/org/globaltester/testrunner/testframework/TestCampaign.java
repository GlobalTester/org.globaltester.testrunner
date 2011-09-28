package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.core.resources.GtResourceHelper;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

/**
 * A TestCampaign defines a set of tests together with the history of their results.
 * @author amay
 *
 */
public class TestCampaign {

	private GtTestCampaignProject project;
	private ArrayList<TestExecutable> executables = new ArrayList<TestExecutable>();

	public TestCampaign(GtTestCampaignProject gtTestCampaignProject) {
		this.project = gtTestCampaignProject;
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 * @throws CoreException 
	 */
	public void initFromIFile(IFile iFile) throws CoreException{
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCampaign"),
				"Root element is not TestCampaign");

		// extract TestExecutables
		@SuppressWarnings("unchecked")
		Iterator<Element> testExecutionIter = root.getChildren("TestExecutable").iterator();
		while (testExecutionIter.hasNext()) {
			Element element = (Element) testExecutionIter.next();
			IFile execIFile = project.getIProject().getFile(element.getTextTrim());
			TestExecutable curExecutable = TestExecutableFactory.getInstance(execIFile);
			if (curExecutable != null) {
				executables.add(curExecutable);
			}
			
		}

	}

	/**
	 * Store 
	 * @throws CoreException 
	 */
	public void storeToIFile(IFile iFile) throws CoreException {
		Element root = new Element("TestCampaignProject");
		
		//add executions to data to be stored
		Iterator<TestExecutable> execIter = executables.iterator();
		while (execIter.hasNext()) {
			TestExecutable curExecutable = execIter.next();
			Element elem=new Element("TestExecutable");
			elem.addContent(curExecutable.getIFile().getProjectRelativePath().toString());
			root.addContent(elem);
		}
		
		//create file if it does not exist yet
		if(!iFile.exists()){
			iFile.create(null, false, null);
		}
		
		//write to file
		XMLHelper.saveDoc(iFile, root);
	}

	public List<TestExecutable> getTestExecutables() {
		return executables;
	}

	public TestExecutable addExecutable(TestExecutable origTestExecutable) throws CoreException {
		// TODO copy executable to the TestCampaignProject
		TestExecutable localTestExecutable = origTestExecutable.copyTo(project.getSpecificationIFile(origTestExecutable));
		
		//add the new local executable to the list
		executables.add(localTestExecutable);
		
		
		// TODO invalidate all results/ or check earlier and allow user to create a copy
		
		return localTestExecutable;
	}
	
	public String getName() {
		return project.getName();
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 */
	public void executeTests() {

		// (re)initialize the TestLogger
		if (TestLogger.isInitialized()) {
			TestLogger.shutdown();
		}
			// initialize test logging for this test session
			IFolder defaultLoggingDir = project.getDefaultLoggingDir();
			try {
				GtResourceHelper.createWithAllParents(defaultLoggingDir);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TestLogger.init(project.getNewResultDir());
		

		// init JS ScriptRunner and Context
		Context cx = Context.enter();
		ScriptRunner sr = new ScriptRunner(cx, project.getIProject().getLocation()
				.toOSString());

		// execute all required tests
		for (Iterator<TestExecutable> execIter = executables.iterator(); execIter
				.hasNext();) {
			// TODO configure logger for indiviual logfiles here
			TestExecutable curExecutable = (TestExecutable) execIter.next();
			execute(curExecutable, sr, cx, false);
			// TODO deconfigure logger for indiviual logfiles here

		}

		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();

	}

	/**
	 * Execute the given executable and all following
	 * @param curExecutable
	 * @param sr
	 * @param cx
	 * @param forceExecution
	 */
	private void execute(TestExecutable curExecutable, ScriptRunner sr,
			Context cx, boolean forceExecution) {
		// FIXME create TestExecution for the executale and execute it
		TestExecution testExecution = null;
		try {
			testExecution = TestExecutionFactory.createExecution(
					curExecutable, this);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//FIXME register this new execution
//		if (testExecution != null) {
//			executions.add(testExecution);
//		}
		
		testExecution.execute(sr, cx, forceExecution);
		
		
		
	}

	public GtTestCampaignProject getProject() {
		return project;
	}

}
