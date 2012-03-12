package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.cardconfiguration.CardConfig;
import org.globaltester.core.resources.GtResourceHelper;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class TestCampaignExecution extends FileTestExecution {
	// FIXME AMY CardConfig implement TestCampaignExecution
	List<IExecution> elementExecutions = new ArrayList<IExecution>();
	private CardConfig cardConfig;

	public TestCampaignExecution(IFile iFile) throws CoreException {
		super(iFile);
	}

	protected TestCampaignExecution(IFile iFile, TestCampaign testCampaign)
			throws CoreException {
		super(iFile);

		// persist the specFile to the GtTestCampaignProject
		specFile = getGtTestCampaignProject().getTestCampaignIFile();

		// store this configuration
		doSave();
	}

	@Override
	public boolean hasChildren() {
		return !elementExecutions.isEmpty();
	}

	@Override
	public Collection<IExecution> getChildren() {
		ArrayList<IExecution> children = new ArrayList<IExecution>();
		children.addAll(elementExecutions);
		return children;
	}

	@Override
	public IExecution getParent() {
		return getTestCampaign();
	}

	@Override
	public String getName() {
		return getTestCampaign().getName();
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public double getTime() {
		double time = 0;
		for (Iterator<IExecution> execIter = elementExecutions.iterator(); execIter
				.hasNext();) {
			time += execIter.next().getTime();
		}

		return time;
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	protected void createIFile() {
		if(!iFile.exists()){
			Element root = new Element("TestCampaignExecution");			
			XMLHelper.saveDoc(iFile, root);
		}
	}

	private TestCampaign getTestCampaign() {
		try {
			return getGtTestCampaignProject().getTestCampaign();
		} catch (CoreException e) {
			throw new RuntimeException(
					"Could not get TestCampaign for this Execution", e);
		}
	}

	@Override
	protected String getXmlRootElementName() {
		return "TestCampaignExecution";
	}

	@Override
	protected void execute(ScriptRunner sr, Context cx, boolean forceExecution,
			boolean reExecution) {
		// TODO Auto-generated method stub

	}

	/**
	 * checks whether the given file represents an TestCaseExecution object
	 * 
	 * @param iFile
	 * @return
	 */
	public static boolean isFileRepresentation(IFile iFile) {
		Document doc = XMLHelper.readDocument(iFile);
		Element rootElem = doc.getRootElement();

		// check that root element has correct name
		if (!rootElem.getName().equals("TestCampaignExecution")) {
			return false;
		}

		return true;
	}

	public CardConfig getCardConfig() {
		return cardConfig;
	}

	public void execute() throws CoreException {
		// (re)initialize the TestLogger
		if (TestLogger.isInitialized()) {
			TestLogger.shutdown();
		}
		// initialize test logging for this test session
		GtTestCampaignProject project = getTestCampaign().getProject();
		IFolder defaultLoggingDir = project.getDefaultLoggingDir();
		GtResourceHelper.createWithAllParents(defaultLoggingDir);
		
		TestLogger.init(project.getNewResultDir());
		setLogFileName(TestLogger.getLogFileName());

		// init JS ScriptRunner and Context
		Context cx = Context.enter();
		ScriptRunner sr = new ScriptRunner(cx, project.getIProject()
				.getLocation().toOSString());
		sr.init(cx);
		sr.initCard(cx, "card", cardConfig);

		// execute all included TestCampaignElements
		List<TestCampaignElement> elements = getTestCampaign().getTestCampaignElements();
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			elementExecutions.add(elemIter.next().execute(sr, cx, false));
		}

		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();

		
	}

	public void setCardConfig(CardConfig newCardConfig) {
		this.cardConfig = newCardConfig;
		
	}

}
