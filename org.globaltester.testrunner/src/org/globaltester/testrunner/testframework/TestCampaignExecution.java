package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.scriptrunner.ScshScope;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.jdom.Document;
import org.jdom.Element;

public class TestCampaignExecution extends FileTestExecution {
	List<IExecution> elementExecutions = new ArrayList<IExecution>();
	private TestCampaignExecution previousExecution;
	private SampleConfig sampleConfig;
	
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);

		// extract sampleConfig
		Element sampleConfigElement = root.getChild("SampleConfiguration");
		if (sampleConfigElement != null) {
			sampleConfig = new SampleConfig(sampleConfigElement);
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
		
		// dump sampleConfig
		if (sampleConfig != null) {
			Element sampleConfigElement = new Element("SampleConfiguration");
			sampleConfig.dumpToXml(sampleConfigElement);
			root.addContent(sampleConfigElement);
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

		initFromTestCampaign();
		
		// store this configuration
		doSave();
	}

	private void initFromTestCampaign() {
		
		List<TestCampaignElement> elements = getTestCampaign().getTestCampaignElements();
		for (Iterator<TestCampaignElement> elemIter = elements.iterator(); elemIter
				.hasNext();) {
			TestCampaignElement curElem = elemIter.next();
			
			// create a new TestExecution this TestCampaignElement
			FileTestExecution curExec = null;
			try {
				curExec = FileTestExecutionFactory.createExecution(curElem);
				elementExecutions.add(curExec);
			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
			
		}
		
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
		if (result != null) {
			return result.getComment();
		}
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
	protected void execute(ScriptRunner sr, boolean forceExecution,
			boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		try {
		
			monitor.beginTask("Execute TestCase ", elementExecutions.size());
			
			// execute all included TestCampaignElements
			for (Iterator<IExecution> elemIter = elementExecutions.iterator(); elemIter
					.hasNext() && !monitor.isCanceled();) {
				IExecution curExec= elemIter.next();
				monitor.subTask(curExec.getName());
				curExec.execute(sr, false,
						new NullProgressMonitor());
				result.addSubResult(curExec.getResult());
				monitor.worked(1);
			}
		}finally{
			monitor.done();	
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

	public SampleConfig getSampleConfig() {
		return sampleConfig;
	}
	
	public void execute(IProgressMonitor monitor, Map<String, Object> envSettings) throws CoreException {
		
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		ScriptRunner sr = null;
		try {
			monitor.beginTask("Execute TestCampaign: ", 2 + getTestCampaign().getTestCampaignElements().size());
		
			monitor.subTask("Initialization");
			
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
		
			sr = setupScriptRunner(project.getIProject()
					.getLocation().toOSString());
			
			monitor.worked(1);

			execute(sr, false, new SubProgressMonitor(monitor,
					getTestCampaign().getTestCampaignElements().size()));

			monitor.subTask("Shutdown");
			
			// shutdown the TestLogger
			TestLogger.shutdown();
			
			monitor.worked(1);

		} finally {
			if (sr != null){
				sr.close();
			}
			monitor.done();
		}
	}

	private ScriptRunner setupScriptRunner(String directory){
		HashMap<Class<?>, Object> configuration = new HashMap<>();
		configuration.put(sampleConfig.getClass(), sampleConfig);
		ScriptRunner sr = new ScriptRunner(directory, configuration);
		sr.init(new ScshScope(sr));
		return sr;
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

	public void setSampleConfig(SampleConfig newSampleConfig) {
		this.sampleConfig = newSampleConfig;
		
	}

}
