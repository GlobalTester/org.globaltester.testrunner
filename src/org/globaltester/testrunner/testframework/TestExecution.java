package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.jdom.Element;
import org.mozilla.javascript.Context;

/**
 * This abstract class defines all common methods of individually executable
 * test portions (e.g. test cases or test suites)
 * 
 * @author amay
 * 
 */
public abstract class TestExecution {
	
	// Constants defining status of specific testexecution
	public static final int STATUS_PASSED = 0;
	public static final int STATUS_FAILURE = Failure.FAILURE;
	public static final int STATUS_WARNING = 2;
	public static final int STATUS_UNDEFINED = 3;
	public static final int STATUS_NOT_APPLICABLE = 4;
	public static final int STATUS_ABORTED = 5;
	public static final int STATUS_SKIPPED = 6;
	public static final int STATUS_RESUMED = 7;
	
	IFile iFile;
	protected IFile specFile;

	/**
	 * Constructor referencing the workspace file which describes the test
	 * execution. All required data is extracted from the workspace file and its
	 * surrounding project.
	 * 
	 * @param iFile
	 *            IFile that contains the test case data and is located inside
	 *            an GTTestSpecProject
	 * @throws CoreException
	 */
	public TestExecution(IFile iFile) throws CoreException {
		this.iFile = iFile;
		if(iFile.exists()){
			//read current state from file
			initFromIFile();
		} else {
			//create the IFile
			createIFile();
		}
	}
	
	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 */
	protected abstract void initFromIFile();

	/**
	 * Store the current state to the resource iFile
	 */
	protected abstract void storeToIFile();

	/**
	 * Create the resource iFile with initial content
	 */
	protected abstract void createIFile();

	/**
	 * (Re)Execute the code associated with this test execution
	 * 
	 * @param sr
	 *            ScriptRunner to execut JS code in
	 * @param cx
	 *            Context to execute JS code in
	 * @param forceExecution
	 *            if true the code is executed regardles if previous execution
	 *            is still valid, if true code is only executed if no previous
	 *            execution is still valid
	 */
	public abstract void execute(ScriptRunner sr, Context cx,
			boolean forceExecution);

	/**
	 * extract metadata from XML Element that is common for all TestExecutoin
	 * types
	 * 
	 * @param root
	 */
	void extractCommonMetaData(Element root) {
		String specFileName = root.getChild("SpecificationResource")
				.getTextTrim();
		specFile = iFile.getProject().getFile(specFileName);

		// TODO define and extract additional required meta data
		// TODO handle reference to original specification resource
	}
	
	/**
	 * dump metadata from XML Element that is common for all TestExecutoin
	 * types
	 * 
	 * @param root
	 */
	void dumpCommonMetaData(Element root) {
		Element specFileElement = new Element("SpecificationResource");
		specFileElement.addContent(specFile.getProjectRelativePath().toString());
		root.addContent(specFileElement);

		// TODO define and extract additional required meta data
		// TODO handle reference to original specification resource
	}


	/**
	 * 
	 * @return the iFile
	 */
	public IResource getIFile() {
		return iFile;
	}

	/**
	 * @return the specFile
	 */
	public IFile getSpecFile() {
		return specFile;
	}
	


	/**
	 * Returns the GtTestProject instance this TestExecution is associated with
	 * @return
	 * @throws CoreException 
	 */
	protected GtTestCampaignProject getGtTestCampaignProject() throws CoreException {
		return GtTestCampaignProject.getProjectForResource(iFile);
	}

}
