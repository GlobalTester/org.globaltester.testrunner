package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.core.resources.GtResourceHelper;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

/**
 * A TestCampaign defines a set of tests together with the history of their
 * results.
 * 
 * @author amay
 * 
 */
public class TestCampaign implements IExecution {

	private GtTestCampaignProject project;
	private ArrayList<TestCampaignElement> elements = new ArrayList<TestCampaignElement>();
	
	private String specName = "";
	private String specVersion = "unknown";

	public TestCampaign(GtTestCampaignProject gtTestCampaignProject) {
		this.project = gtTestCampaignProject;
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 * 
	 * @throws CoreException
	 */
	public void initFromIFile(IFile iFile) throws CoreException {
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCampaign"),
				"Root element is not TestCampaign");
		
		//extract meta data
		Element specNameElem = root.getChild("SPECIFICATIONNAME");
		if (specNameElem != null) {
			specName = specNameElem.getTextTrim();
		}
		Element specVersionElem = root.getChild("SPECIFICATIONVERSION");
		if (specVersionElem != null) {
			specVersion = specVersionElem.getTextTrim();
		}

		// extract TestExecutables
		@SuppressWarnings("unchecked")
		Iterator<Element> testExecutionIter = root.getChildren(
				TestCampaignElement.XML_ELEMENT).iterator();
		while (testExecutionIter.hasNext()) {
			Element xmlElem = (Element) testExecutionIter.next();
			TestCampaignElement curTestCampaignElement = new TestCampaignElement(
					this, xmlElem);
			if (curTestCampaignElement != null) {
				elements.add(curTestCampaignElement);
			}

		}

	}

	/**
	 * Store
	 * 
	 * @throws CoreException
	 */
	public void storeToIFile(IFile iFile) throws CoreException {
		Element root = new Element("TestCampaign");

		//add meta data
		Element specNameElem = new Element("SPECIFICATIONNAME");
		specNameElem.addContent(specName);
		root.addContent(specNameElem);
		Element specVersionElem = new Element("SPECIFICATIONVERSION");
		specVersionElem.addContent(specVersion);
		root.addContent(specVersionElem);
		
		
		// add TestCampaignElements to data to be stored
		Iterator<TestCampaignElement> elemIter = elements.iterator();
		while (elemIter.hasNext()) {
			TestCampaignElement curElem = elemIter.next();
			root.addContent(curElem.getXmlRepresentation());
		}

		// create file if it does not exist yet
		if (!iFile.exists()) {
			iFile.create(null, false, null);
		}

		// write to file
		XMLHelper.saveDoc(iFile, root);

	}

	/**
	 * Save this TEstCampaign and its children in the workspace
	 * 
	 * @throws CoreException
	 */
	public void doSave() throws CoreException {
		// save this
		storeToIFile(project.getTestCampaignIFile());

		// save the last executions of associated TestCampaignElements
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			FileTestExecution curLastExec = elemIter.next().getLastExecution();
			if (curLastExec != null) {
				curLastExec.doSave();
			}
		}
	}

	public void addExecutable(FileTestExecutable origTestExecutable)
			throws CoreException {
		// create a new TestCampaignElement and add it
		TestCampaignElement newElement = new TestCampaignElement(this,
				origTestExecutable);
		elements.add(newElement);

		// TODO invalidate all results(or check earlier and allow user to create
		// a copy)

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(true,
				new Object[] { this, newElement }, null);
	}

	public String getName() {
		return project.getName();
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 * 
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
		ScriptRunner sr = new ScriptRunner(cx, project.getIProject()
				.getLocation().toOSString());

		// execute all included TestCampaignElements
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			elemIter.next().execute(sr, cx, false);
		}

		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();

		// save the new state
		project.doSave();

		// notify viewers of parent about this change
		project.notifyTreeChangeListeners(false, elements.toArray(),
				new String[] { "lastExecution" });

	}

	public GtTestCampaignProject getProject() {
		return project;
	}

	@Override
	public boolean hasChildren() {
		return !elements.isEmpty();
	}

	@Override
	public Collection<IExecution> getChildren() {
		LinkedList<IExecution> children = new LinkedList<IExecution>();

		// add elements to list of children
		children.addAll(elements);

		return children;
	}

	@Override
	public IExecution getParent() {
		// TestCampaign is a root element
		return null;
	}

	public String getSpecName() {
		return specName;
	}

	public void setSpecName(String newName) {
		specName = newName;
	}

	public String getSpecVersion() {
		return specVersion;
	}

	public void setSpecVersion(String newVersion) {
		specVersion = newVersion;
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Status getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

}
