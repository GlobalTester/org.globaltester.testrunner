package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.utils.IntegrityCheckResult;
import org.globaltester.testrunner.utils.TestSpecIntegrityChecker;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestSet;
import org.jdom.Document;
import org.jdom.Element;

public class TestSetExecution extends AbstractTestExecution {
	List<IExecution> elementExecutions = new ArrayList<IExecution>();

	private SampleConfig sampleConfig;
	private String cardReaderName;
	private String integrityOfTestSpec;

	private HashSet<IProject> specsToCheck = new HashSet<>();
	
	@Override
	void extractFromXml(Element root) {
		super.extractFromXml(root);

		// extract sampleConfig
		Element sampleConfigElement = root.getChild("SampleConfiguration");
		if (sampleConfigElement != null) {
			sampleConfig = new SampleConfig(sampleConfigElement);
		}
		
		// extract cardReaderName
		Element cardReaderNameElement = root.getChild("CardReaderName");
		if (cardReaderNameElement != null) {
			cardReaderName = cardReaderNameElement.getTextTrim();
		}
		
		// extract integrityOfTestSuiteProvided
		Element integrityOfTestSuiteProvidedElement = root.getChild("IntegrityOfTestSuiteProvided");
		if (integrityOfTestSuiteProvidedElement != null) {
			integrityOfTestSpec = integrityOfTestSuiteProvidedElement.getTextTrim();
		}

		// FIXME AAA
//		try {
//			//extract test case executions
//			Element fileNames = root.getChild("FileNames");
//			if (fileNames != null) {
//				@SuppressWarnings("unchecked")
//				List<Element> children = fileNames.getChildren("FileName");
//				for (Element child : children) {
//					String filename = child.getTextTrim();
//					FileTestExecution fileTestExecution;
//
//					fileTestExecution = FileTestExecutionFactory
//							.getInstance(iFile.getProject().getFile(filename));
//
//					if (fileTestExecution instanceof TestCaseExecution) {
//						TestCaseExecution exec = (TestCaseExecution) fileTestExecution;
//						elementExecutions.add(exec);
//						result.addSubResult(exec.getResult());
//					}
//				}
//				
//				Element status = root.getChild("LastExecutionResult").getChild("Status");
//				result.status = Result.Status.get(status.getTextTrim());
//			}
//		} catch (CoreException e) {
//			// ignore empty set of executions
//		}
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
		
		// dump cardReaderName
		if (cardReaderName != null) {
			Element cardReaderNameElement = new Element("CardReaderName");
			cardReaderNameElement.addContent(cardReaderName);
			root.addContent(cardReaderNameElement);
		}
		
		// dump integrityOfTestSuiteProvided
		Element integrityOfTestSuiteProvidedElement = new Element("IntegrityOfTestSuiteProvided");
		integrityOfTestSuiteProvidedElement.addContent(String.valueOf(integrityOfTestSpec));
		root.addContent(integrityOfTestSuiteProvidedElement);
		

		//FIXME AAA
		// dump filenames for children
//		Element fileNames = new Element("FileNames");
//		root.addContent(fileNames);
//		
//		Iterator<IExecution> iter = this.getChildren().iterator();
//		while (iter.hasNext()){
//			IExecution current = iter.next();
//			if (current instanceof TestCaseExecution){
//				Element childElement = new Element("FileName");
//				childElement.addContent(((TestCaseExecution) current).getIFile().getProjectRelativePath().toString());
//				fileNames.addContent(childElement);
//			}
//		}
		
	}

	public TestSetExecution(TestSet testSet) {
		// create executions for children
		for (ITestExecutable curTestExecutable : testSet.getChildren()) {
			try {
				if (curTestExecutable instanceof TestCase) {
					TestCase curTestCase = (TestCase) curTestExecutable;
					TestCaseExecution tcExecution = new TestCaseExecution(null, curTestCase);
					elementExecutions.add(tcExecution);
					specsToCheck .add(curTestCase.getIFile().getProject());
				} else {

					throw new RuntimeException("Unsupported type of TestExecutable: " + curTestExecutable);
				}

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
		return "Volatile TestSet";
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
		return "";
	}

	@Override
	public long getDuration() {
		long duration = 0;
		for (Iterator<IExecution> execIter = elementExecutions.iterator(); execIter
				.hasNext();) {
			duration += execIter.next().getDuration();
		}

		return duration;
	}

	@Override
	public String getId() {
		return "";
	}

	@Override
	protected String getXmlRootElementName() {
		return "TestSetExecution";
	}

	@Override
	protected void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution,
			boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		
		SubMonitor progress = SubMonitor.convert(monitor, 100);
		
		try {
			progress.subTask("Initialization");
			
			// initialize test logging for this test session
			setLogFileName(TestLogger.getLogFileName());
			setLogFileLine(TestLogger.getLogFileLine());
			
			progress.worked(1);
			
			progress.subTask("Integrity check of TestSpecifications");
			performIntegrityCheck();
			progress.worked(1);
			
			progress.beginTask("Execute TestCase ", elementExecutions.size());
			
			//persist SampleConfig used
			if (runtimeReqs.containsKey(SampleConfig.class)){
				Element xmlRepresentation = new Element("SampleConfiguration");
				runtimeReqs.get(SampleConfig.class).dumpToXml(xmlRepresentation);
				this.sampleConfig = new SampleConfig(xmlRepresentation);	
			}
			
			// execute all included TestCampaignElements
			for (Iterator<IExecution> elemIter = elementExecutions.iterator(); elemIter
					.hasNext() && !monitor.isCanceled();) {
				IExecution curExec= elemIter.next();
				monitor.subTask(curExec.getName());
				curExec.execute(runtimeReqs, false,
						new NullProgressMonitor());
				result.addSubResult(curExec.getResult());
				monitor.worked(1);
			}	
			
			// shutdown the TestLogger
			progress.subTask("Shutdown");
			progress.worked(1);
		} finally {
			monitor.done();
		}
	}

	private void performIntegrityCheck() {
		TestSpecIntegrityChecker integrityChecker = new TestSpecIntegrityChecker();
		for (IResource curResource : specsToCheck) {
			if (curResource instanceof IContainer) {
				integrityChecker.addSpecsToCheck((IContainer) curResource);
			}
		}
		Map<String, IntegrityCheckResult> integrityResult = integrityChecker.check();
		ArrayList<String> specNames = new ArrayList<>(integrityResult.keySet());
		Collections.sort(specNames);
		String nonValidProjects = "";
		for (String curSpec: specNames) {
			TestLogger.info("Checksum of "+ curSpec + " is "+ integrityResult.get(curSpec).getStatus());
			TestLogger.trace("Expected checksum: "+ integrityResult.get(curSpec).getExpectedChecksum());
			TestLogger.trace("Actual checksum: "+ integrityResult.get(curSpec).getCalculatedChecksum());
			
			if (integrityResult.get(curSpec).getStatus() != IntegrityCheckResult.IntegrityCheckStatus.VALID) {
				nonValidProjects+="\n-"+curSpec + ": " +integrityResult.get(curSpec).getStatus() ;
			} 
		}

		
		if (!nonValidProjects.isEmpty()) {

			String message = "Functional integrity of testcases is not assured!\n\nThe following Scripts have been modified since delivery or remain unchecked:\n"
					+ nonValidProjects + "\n";

			TestLogger.warn(message);	
		}

		integrityOfTestSpec = TestSpecIntegrityChecker.getSimplifiedCheckResult(integrityResult);
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
		return elementExecutions.stream().filter(IExecution::isExecuted).count();
	}

	public long getNumberOfTestsWithStatus(Status expectedStatus) {
		return elementExecutions.stream().filter(exec -> exec.getStatus() == expectedStatus).count();
	}

}
