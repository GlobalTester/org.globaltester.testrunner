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
	List<IExecution> elementExecutions = new ArrayList<IExecution>();
	private CardConfig cardConfig;

	private TestCampaignExecution previousExecution;
	
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);

		// extract cardConfig
		Element cardConfigElement = root.getChild("CardConfiguration");
		if (cardConfigElement != null) {
			cardConfig = new CardConfig(cardConfigElement);
		}
		
		try {
			// extract previous execution
			Element prevExecFileElement = root.getChild("PreviousExecution");

			if (prevExecFileElement != null) {
				String prevExecFileName = prevExecFileElement.getTextTrim();
				FileTestExecution execution = FileTestExecutionFactory.getInstance(iFile
						.getProject().getFile(prevExecFileName));
				if (execution instanceof TestCampaignExecution){
					previousExecution = (TestCampaignExecution) execution;
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			//extract test case executions
			Element fileNames = root.getChild("FileNames");
			if (fileNames != null) {
				@SuppressWarnings("unchecked")
				List<Element> children = fileNames.getChildren("FileName");
				for (Element child : children) {
					String filename = child.getTextTrim();
					FileTestExecution fileTestExecution;

					fileTestExecution = FileTestExecutionFactory
							.getInstance(iFile.getProject().getFile(filename));

					if (fileTestExecution instanceof TestCaseExecution) {
						TestCaseExecution exec = (TestCaseExecution) fileTestExecution;
						elementExecutions.add(exec);
						result.addSubResult(exec.getResult());
					}
				}
				result.rebuildStatus();
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	void dumpToXml(Element root) {
		super.dumpToXml(root);
		
		// dump cardConfig
		if (cardConfig != null) {
			Element cardConfigElement = new Element("CardConfiguration");
			cardConfig.dumpToXml(cardConfigElement);
			root.addContent(cardConfigElement);
		}
		
		// dump previous execution
		if (previousExecution != null) {
			Element prevExecElement = new Element("PreviousExecution");
			prevExecElement.addContent(previousExecution.getIFile()
					.getProjectRelativePath().toString());
			root.addContent(prevExecElement);
		}
		
		// dump filenames for children
		Element fileNames = new Element("FileNames");
		root.addContent(fileNames);
		
		Iterator<IExecution> iter = this.getChildren().iterator();
		while (iter.hasNext()){
			IExecution current = iter.next();
			if (current instanceof TestCaseExecution){
				Element childElement = new Element("FileName");
				childElement.addContent(((TestCaseExecution) current).getIFile().getProjectRelativePath().toString());
				fileNames.addContent(childElement);
			}
		}
		
	}

	public TestCampaignExecution(IFile iFile) throws CoreException {
		super(iFile);
		initFromIFile();
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
		return null;
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

	public TestCampaign getTestCampaign() {
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
		// execute all included TestCampaignElements
		List<TestCampaignElement> elements = getTestCampaign().getTestCampaignElements();
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			FileTestExecution curExec = elemIter.next().execute(sr, cx, false);
			elementExecutions.add(curExec);
			result.addSubResult(curExec.getResult());
		}

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
		
		execute(sr, cx, false);


		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();
	}

	/**
	 * @param previousExecution
	 *            the previousExecution to set
	 */
	public void setPreviousExecution(TestCampaignExecution previousExecution) {
		this.previousExecution = previousExecution;
	}

	/**
	 * @return the previousExecution
	 */
	public TestCampaignExecution getPreviousExecution() {
		return previousExecution;
	}
	@Override
	public void doSave() {
		super.doSave();
		
		// save element executions
		for (IExecution element : elementExecutions){
			if (element instanceof TestCaseExecution){
				((TestCaseExecution) element).doSave();
			}
		}
		
		// save previous executions recursively
		if (previousExecution != null) {
			previousExecution.doSave();
		}
	}

	public void setCardConfig(CardConfig newCardConfig) {
		this.cardConfig = newCardConfig;
		
	}
}
