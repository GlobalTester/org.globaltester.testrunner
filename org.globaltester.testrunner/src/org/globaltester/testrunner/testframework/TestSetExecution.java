package org.globaltester.testrunner.testframework;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.utils.IntegrityCheckResult;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.globaltester.testspecification.testframework.TestSet;
import org.jdom.Element;

//FIXME AAB read this class for consistency (e.g. string labels etc)
public class TestSetExecution extends FileTestExecution {
	
	public static final String XML_ELEMENT = "TestSetExecution";
	private static final String XML_CHILD_SAMPLECONFIG = "SampleConfiguration";
	private static final String XML_CHILD_CARDREADER = "CardReaderName";
	private static final String XML_CHILD_INTEGRITY = "IntegrityOfTestSuiteProvided";

	
	private SampleConfig sampleConfig;
	private String cardReaderName;
	private String integrityOfTestSpec;



	public TestSetExecution(IFile iFile) throws CoreException {
		super(iFile);
	}

	@Override
	public void extractFromXml(Element root) throws CoreException {
		super.extractFromXml(root);

		// extract sampleConfig
		Element sampleConfigElement = root.getChild(XML_CHILD_SAMPLECONFIG);
		if (sampleConfigElement != null) {
			sampleConfig = new SampleConfig(sampleConfigElement);
		}
		
		// extract cardReaderName
		Element cardReaderNameElement = root.getChild(XML_CHILD_CARDREADER);
		if (cardReaderNameElement != null) {
			cardReaderName = cardReaderNameElement.getTextTrim();
		}
		
		// extract integrityOfTestSuiteProvided
		Element integrityOfTestSuiteProvidedElement = root.getChild(XML_CHILD_INTEGRITY);
		if (integrityOfTestSuiteProvidedElement != null) {
			integrityOfTestSpec = integrityOfTestSuiteProvidedElement.getTextTrim();
		}
	}

	@Override
	public void dumpToXml(Element root) {
		super.dumpToXml(root);
		
		// dump sampleConfig
		if (sampleConfig != null) {
			Element sampleConfigElement = new Element(XML_CHILD_SAMPLECONFIG);
			sampleConfig.dumpToXml(sampleConfigElement);
			root.addContent(sampleConfigElement);
		}
		
		// dump cardReaderName
		if (cardReaderName != null) {
			Element cardReaderNameElement = new Element(XML_CHILD_CARDREADER);
			cardReaderNameElement.addContent(cardReaderName);
			root.addContent(cardReaderNameElement);
		}
		
		// dump integrityOfTestSuiteProvided
		Element integrityOfTestSuiteProvidedElement = new Element(XML_CHILD_INTEGRITY);
		integrityOfTestSuiteProvidedElement.addContent(String.valueOf(integrityOfTestSpec));
		root.addContent(integrityOfTestSuiteProvidedElement);
		
	}

	/**
	 * 
	 * @param testSet
	 * @param campaign to store ExecutionStateFiles in, or null if no execution state needs to be preserved
	 * @throws CoreException 
	 */
	public TestSetExecution(TestSet testSet, TestCampaign campaign) throws CoreException {
		super(campaign != null ? campaign.getProject().getNewStateIFile(testSet): null);
		setId(campaign != null ? "Persistent TestSet" : "Volatile TestSet");
		cachedExecutable = testSet;
		
		// create executions for children
		for (ITestExecutable curTestExecutable : testSet.getChildren()) {
			try {
				if (curTestExecutable instanceof FileTestExecutable) {
					FileTestExecutable curFileTestCase = (FileTestExecutable) curTestExecutable;
					FileTestExecution tcExecution = FileTestExecutionFactory.createExecution(curFileTestCase, campaign);
					addChildExecution(tcExecution);
				} else {

					throw new RuntimeException("Unsupported type of TestExecutable: " + curTestExecutable);
				}

			} catch (CoreException e) {
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
			
		}

	}

	@Override
	public String getXmlRootElementName() {
		return "TestSetExecution";
	}

	@Override
	protected void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution,
			boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		SubMonitor progress = SubMonitor.convert(monitor, 1 + childExecutions.size());
		
		try {
			progress.subTask("Initialization");
			
			// initialize test logging for this test session
			setLogFileName(TestLogger.getLogFileName());
			setLogFileLine(TestLogger.getLogFileLine());
			
			progress.worked(1);
			
			progress.beginTask("Execute TestCases ", childExecutions.size());
			
			//persist SampleConfig used
			if (runtimeReqs.containsKey(SampleConfig.class)){
				Element xmlRepresentation = new Element(XML_CHILD_SAMPLECONFIG);
				runtimeReqs.get(SampleConfig.class).dumpToXml(xmlRepresentation);
				this.sampleConfig = new SampleConfig(xmlRepresentation);	
			}
			
			if (runtimeReqs.containsKey(IntegrityCheckResult.class)){
				this.integrityOfTestSpec = runtimeReqs.get(IntegrityCheckResult.class).getStatus().toString();	
			} else {
				this.integrityOfTestSpec = "unknown";
			}
			
			// execute all included TestExecutables
			for (Iterator<IExecution> elemIter = childExecutions.iterator(); elemIter
					.hasNext() && !progress.isCanceled();) {
				IExecution curExec= elemIter.next();
				progress.subTask(curExec.getId());
				curExec.execute(runtimeReqs, false,
						new NullProgressMonitor());
				result.addSubResult(curExec.getResult());
				progress.worked(1);
			}	
			
		} finally {
			monitor.done();
		}
	}

	public SampleConfig getSampleConfig() {
		return sampleConfig;
	}

	public String getCardReaderName() {
		return cardReaderName;
	}

	public void setCardReaderName(String cardReaderName) {
		this.cardReaderName = cardReaderName;
	}

	public String getIntegrityOfTestSpec() {
		return integrityOfTestSpec;
	}

	public long getNumberOfExecutedTests() {
		return childExecutions.stream().filter(IExecution::isExecuted).count();
	}

	public long getNumberOfTestsWithStatus(Status expectedStatus) {
		return childExecutions.stream().filter(exec -> exec.getStatus() == expectedStatus).count();
	}
}
