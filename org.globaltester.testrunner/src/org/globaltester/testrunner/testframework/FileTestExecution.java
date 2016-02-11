package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.jdom.Document;
import org.jdom.Element;

/**
 * This abstract class defines all common methods of individually executable
 * test portions (e.g. test cases or test suites)
 * 
 * @author amay
 * 
 */
public abstract class FileTestExecution extends AbstractTestExecution {

	// Constants defining status of specific testexecution
	public static final int STATUS_PASSED = 0;
	public static final int STATUS_WARNING = Failure.WARNING;
	public static final int STATUS_FAILURE = Failure.FAILURE;
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
	public FileTestExecution(IFile iFile) throws CoreException {
		this.iFile = iFile;
		if (!iFile.exists()) {
			// create the IFile
			createIFile();
		}
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 */
	
	protected void initFromIFile() {
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		String xmlRootName = getXmlRootElementName();
		Assert.isTrue(root.getName().equals(xmlRootName),
				"Root element is not " + xmlRootName);

		// extract meta data
		extractFromXml(root);
	}

	/**
	 * Create the resource iFile with initial content
	 */
	protected abstract void createIFile();

	@Override
	void extractFromXml(Element root) {
		// extract all elements of parent class
		super.extractFromXml(root);

		// extract SpecificationResource
		String specFileName = root.getChild("SpecificationResource")
				.getTextTrim();
		specFile = iFile.getProject().getFile(specFileName);

	}

	@Override
	void dumpToXml(Element root) {
		// dump all elements of parent class
		super.dumpToXml(root);

		// dump ref to specification resource
		Element specFileElement = new Element("SpecificationResource");
		specFileElement
				.addContent(specFile.getProjectRelativePath().toString());
		root.addContent(specFileElement);


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
	 * 
	 * @return
	 * @throws CoreException
	 */
	public GtTestCampaignProject getGtTestCampaignProject()
			throws CoreException {
		return GtTestCampaignProject.getProjectForResource(iFile);
	}

	/**
	 * 
	 */
	public void doSave() {
		// save this element
		Element root = new Element(getXmlRootElementName());
		dumpToXml(root);
		XMLHelper.saveDoc(iFile, root);


	}

}
