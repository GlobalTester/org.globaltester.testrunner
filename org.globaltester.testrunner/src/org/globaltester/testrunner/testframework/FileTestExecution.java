package org.globaltester.testrunner.testframework;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.base.xml.XMLHelper;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;
import org.jdom.Document;
import org.jdom.Element;

/**
 * This abstract class defines all common methods of individually executable
 * test portions (e.g. test cases or test suites)
 * 
 * @author amay
 * 
 */
public abstract class FileTestExecution extends CompositeTestExecution {

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
		if ((iFile!= null)) {
			if (iFile.exists()) {
				initFromIFile();
			} else {
				createIFile();	
			}
		}
	}

	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 */
	
	protected void initFromIFile() throws CoreException {
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
	protected void createIFile() {
		if(!iFile.exists()){
			Element root = new Element(getXmlRootElementName());
			XMLHelper.saveDoc(iFile, root);
		}
	}

	@Override
	public void extractFromXml(Element root) throws CoreException {
		// extract SpecificationResource
		Element specFileElem = root.getChild("SpecificationResource");
		if (specFileElem != null) {
			String specFileName = specFileElem.getTextTrim();
			specFile = iFile.getProject().getFile(specFileName);
		}
		
		// extract all elements of parent class
		super.extractFromXml(root);
	}

	@Override
	public void dumpToXml(Element root) {
		// dump all elements of parent class
		super.dumpToXml(root);

		// dump ref to specification resource
		if (specFile != null) {
			Element specFileElement = new Element("SpecificationResource");
			specFileElement
					.addContent(specFile.getProjectRelativePath().toString());
			root.addContent(specFileElement);
		}


	}

	/**
	 * 
	 * @return the iFile
	 */
	public IFile getIFile() {
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
		if (iFile == null) return;
		
		// save this element
		Element root = new Element(getXmlRootElementName());
		dumpToXml(root);
		XMLHelper.saveDoc(iFile, root);
		
		//save all children
		doSaveChildren();


	}

	FileTestExecutable cachedExecutable = null;
	@Override
	public ITestExecutable getExecutable() {
		if (cachedExecutable == null) {
			if (specFile != null) {
				try {
					cachedExecutable = TestExecutableFactory.getInstance(specFile);
				} catch (CoreException e) {
					return null;
				}
			}
		}
		return cachedExecutable;
	}

}
