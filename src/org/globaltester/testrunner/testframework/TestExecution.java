package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.smartcardshell.ScriptRunner;
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
	}

	/**
	 * Execute the represented test executable
	 */
	public void execute(ScriptRunner sr, Context cx) {
		// TODO differentiate when no execution is needed
		forceExecute(sr, cx);
	}

	public abstract void forceExecute(ScriptRunner sr, Context cx);

	/**
	 * extract metadata from XML Element that is common for all TestExecutoin
	 * types
	 * 
	 * @param root
	 */
	void extractCommonMetaData(Element root) {
				String specFileName = root.getChild("SpecificationResource").getTextTrim();
		specFile = iFile.getProject().getFile(specFileName);

		// TODO define and extract additional required meta data
		// TODO handle reference to original specification resource
	}

	/**
	 * @return the specFile
	 */
	public IFile getSpecFile() {
		return specFile;
	}

}
