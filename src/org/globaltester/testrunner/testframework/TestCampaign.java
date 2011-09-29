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
	private ArrayList<TestCampaignElement> elements = new ArrayList<TestCampaignElement>();

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
		Iterator<Element> testExecutionIter = root.getChildren(TestCampaignElement.XML_ELEMENT).iterator();
		while (testExecutionIter.hasNext()) {
			Element xmlElem = (Element) testExecutionIter.next();
			TestCampaignElement curTestCampaignElement = new TestCampaignElement(this, xmlElem);
			if (curTestCampaignElement != null) {
				elements.add(curTestCampaignElement);
			}
			
		}

	}

	/**
	 * Store 
	 * @throws CoreException 
	 */
	public void storeToIFile(IFile iFile) throws CoreException {
		Element root = new Element("TestCampaignProject");
		
		//add TestCampaignElements to data to be stored
		Iterator<TestCampaignElement> elemIter = elements.iterator();
		while (elemIter.hasNext()) {
			TestCampaignElement curElem = elemIter.next();
			root.addContent(curElem.getXmlRepresentation());
		}
		
		//create file if it does not exist yet
		if(!iFile.exists()){
			iFile.create(null, false, null);
		}
		
		//write to file
		XMLHelper.saveDoc(iFile, root);
	}

	public void addExecutable(TestExecutable origTestExecutable) throws CoreException {
		// create a new TestCampaignElement and add it 
		TestCampaignElement newElement = new TestCampaignElement(this, origTestExecutable);
		elements.add(newElement);
		
		
		// TODO invalidate all results(or check earlier and allow user to create a copy)

	}
	
	public String getName() {
		return project.getName();
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 * @throws CoreException 
	 */
	public void executeTests() throws CoreException {

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

		// execute all included TestCampaignElements
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			elemIter.next().execute(sr, cx, false);
		}

		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();
		
		//save the new state
		project.doSave();

	}

	public GtTestCampaignProject getProject() {
		return project;
	}

	public List<TestCampaignElement> getElements() {
		return elements;
	}

}
