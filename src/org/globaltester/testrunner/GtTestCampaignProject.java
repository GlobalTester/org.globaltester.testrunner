package org.globaltester.testrunner;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.globaltester.core.resources.GtResourceHelper;
import org.globaltester.core.xml.XMLHelper;
import org.globaltester.logging.logger.GTLogger;
import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.testframework.TestExecution;
import org.globaltester.testrunner.testframework.TestExecutionFactory;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.jdom.Document;
import org.jdom.Element;
import org.mozilla.javascript.Context;

public class GtTestCampaignProject {

	private static final String SPEC_FOLDER = "TestSpecification";
	private static final String CONFIG_FOLDER = "DUTconfiguration";
	private static final String STATE_FOLDER = "ExecutionState";
	private static final String RESULT_FOLDER = "TestResults";

	private static Hashtable<IProject, GtTestCampaignProject> instances = new Hashtable<IProject, GtTestCampaignProject>();
	private IProject iProject; // IProject that is represented by this
								// instance
	private ArrayList<TestExecution> executions = new ArrayList<TestExecution>();

	/**
	 * Create a GlobalTester TestSpecification Project. This includes creation
	 * of the Eclipse project, adding the according nature and creating the
	 * initial folder structure.
	 * 
	 * @param projectName
	 *            name of the project to be created
	 * @param location
	 *            location where the project shall be created. If empty the
	 *            default workspace location will be used.
	 * @return the created project
	 */
	public static IProject createProject(String projectName, URI location) {
		Assert.isNotNull(projectName);
		Assert.isTrue(projectName.trim().length() > 0);

		IProject project = createEmptyProject(projectName, location);
		try {
			addGtTestCampaignNature(project);

			String[] paths = { CONFIG_FOLDER, STATE_FOLDER, SPEC_FOLDER,
					RESULT_FOLDER };
			addToProjectStructure(project, paths);
		} catch (CoreException e) {
			e.printStackTrace();
			project = null;
		}
		
		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
						.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			// refresh of workspace failed
			// relevant CoreException will be in the eclipse log anyhow
			// users most probably will ignore this behavior and refresh manually 
		}

		return project;
	}

	/**
	 * Create an empty project
	 * 
	 * @param projectName
	 *            name of the project to be created
	 * @param location
	 *            location where the project shall be created. If empty the
	 *            default workspace location will be used.
	 * 
	 */
	// TODO refactor this to GtResourceHelper
	private static IProject createEmptyProject(String projectName, URI location) {
		IProject newProject = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(projectName);

		if (!newProject.exists()) {
			URI projectLocation = location;
			IProjectDescription desc = newProject.getWorkspace()
					.newProjectDescription(newProject.getName());
			if (location != null
					&& ResourcesPlugin.getWorkspace().getRoot()
							.getLocationURI().equals(location)) {
				projectLocation = null;
			}

			desc.setLocationURI(projectLocation);
			try {
				newProject.create(desc, null);
				if (!newProject.isOpen()) {
					newProject.open(null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return newProject;
	}

	// TODO refactor this to GtResourceHelper
	private static void createFolder(IFolder folder) throws CoreException {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder) {
			createFolder((IFolder) parent);
		}
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
	}

	/**
	 * Create a folder structure from given paths.
	 * 
	 * @param project
	 *            project to create the folders inside
	 * @param paths
	 *            array of relative paths of the folders to be created
	 * @throws CoreException
	 */
	// TODO refactor this to GtResourceHelper
	private static void addToProjectStructure(IProject project, String[] paths)
			throws CoreException {
		for (String currentPath : paths) {
			IFolder currentFolder = project.getFolder(currentPath);
			createFolder(currentFolder);
		}
	}

	/**
	 * Add the GtTestCampaignNature to the given project.
	 * 
	 * @param project
	 *            project to add the nature to
	 * @throws CoreException
	 */
	// TODO refactor this to GtResourceHelper
	private static void addGtTestCampaignNature(IProject project)
			throws CoreException {
		if (!project.hasNature(GtTestCampaignNature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = GtTestCampaignNature.NATURE_ID;
			description.setNatureIds(newNatures);

			IProgressMonitor monitor = null;
			project.setDescription(description, monitor);
		}
	}

	public static GtTestCampaignProject getProjectForResource(
			IResource selectedResource) throws CoreException {

		IProject iProject = selectedResource.getProject();

		if (!instances.containsKey(iProject)) {
			instances.put(iProject, new GtTestCampaignProject(iProject));
		}

		return instances.get(iProject);
	}

	private GtTestCampaignProject(IProject iProject) throws CoreException {
		super();
		try {
			Assert.isTrue(iProject.hasNature(GtTestCampaignNature.NATURE_ID),
					"Project does not use GtTestCampaignNature");
		} catch (CoreException e) {
			Assert.isTrue(false, "Project nature can not be checked");
		}

		this.iProject = iProject;

		IFile iFile = getIFile();
		if(iFile.exists()){
			//read current state from file
			initFromIFile();
		} else {
			//create the IFile
			storeToIFile();
		}
	}
	
	/**
	 * Initialize all values required for this instance form the already set
	 * variable iFile
	 * @throws CoreException 
	 */
	protected void initFromIFile() throws CoreException{
		IFile iFile = getIFile();
		Assert.isNotNull(iFile);
		Document doc = XMLHelper.readDocument(iFile);
		Element root = doc.getRootElement();

		// check that root element has correct name
		Assert.isTrue(root.getName().equals("TestCampaignProject"),
				"Root element is not TestCaseExecution");

		// extract TestExecutions
		@SuppressWarnings("unchecked")
		Iterator<Element> testExecutionIter = root.getChildren("TestExecution").iterator();
		while (testExecutionIter.hasNext()) {
			Element element = (Element) testExecutionIter.next();
			IFile execIFile = getIProject().getFile(element.getTextTrim());
			TestExecution curExecution = TestExecutionFactory.getInstance(execIFile);
			if (curExecution != null) {
				executions.add(curExecution);
			}
			
		}

	}

	/**
	 * @return the iProject
	 */
	public IProject getIProject() {
		return iProject;
	}

	/**
	 * Execute all tests that need to be executed e.g. which do not have a valid
	 * previous execution associated
	 */
	public void executeTests() {

		// (re)initialize the TestLogger
		if (TestLogger.isInitialized()) {
			TestLogger.shutdown();
		}
			// initialize test logging for this test session
			IFolder defaultLoggingDir = iProject.getFolder(RESULT_FOLDER
					+ File.separator + "Logging");
			try {
				GtResourceHelper.createWithAllParents(defaultLoggingDir);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			TestLogger.init(getNewResultDir());
		

		// init JS ScriptRunner and Context
		Context cx = Context.enter();
		ScriptRunner sr = new ScriptRunner(cx, iProject.getLocation()
				.toOSString());

		// execute all required tests
		for (Iterator<TestExecution> execIter = executions.iterator(); execIter
				.hasNext();) {
			// TODO configure logger for indiviual logfiles here
			TestExecution curExecution = (TestExecution) execIter.next();
			curExecution.execute(sr, cx, false);
			// TODO deconfigure logger for indiviual logfiles here

		}

		// close JS context
		Context.exit();

		// shutdown the TestLogger
		TestLogger.shutdown();

	}

	//create a new ResultDirectory
	private String getNewResultDir() {
		// initialize test logging for this test session
		IFolder defaultLoggingDir = iProject.getFolder(RESULT_FOLDER
				+ File.separator + GTLogger.getIsoDate());
		try {
			GtResourceHelper.createWithAllParents(defaultLoggingDir);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return defaultLoggingDir.getLocation().toOSString();
		
	}

	/**
	 * Create an TestExecution from an Executable, add it to the list of
	 * executions and keep the related files in sync
	 * 
	 * @param testExecutable
	 */
	public void addExecutable(TestExecutable testExecutable) {
		TestExecution testExecution = null;
		try {
			testExecution = TestExecutionFactory.createExecution(
					testExecutable, this);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (testExecution != null) {
			executions.add(testExecution);
		}
		
		try {
			this.storeToIFile();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Store 
	 * @throws CoreException 
	 */
	private void storeToIFile() throws CoreException {
		Element root = new Element("TestCampaignProject");
		
		//add executions to data to be stored
		Iterator<TestExecution> execIter = executions.iterator();
		while (execIter.hasNext()) {
			TestExecution curExecution = (TestExecution) execIter.next();
			Element elem=new Element("TestExecution");
			elem.addContent(curExecution.getIFile().getProjectRelativePath().toString());
			root.addContent(elem);
		}
		
		//create file if it does not exist yet
		IFile iFile = getIFile();
		if(!iFile.exists()){
			iFile.create(null, false, null);
		}
		
		//write to file
		XMLHelper.saveDoc(iFile, root);
	}

	/**
	 * Returns the IFile containing this projects additional data
	 * @return
	 * @throws CoreException
	 */
	private IFile getIFile() throws CoreException {
		IFile file = getIProject().getFile("project.xml");
		return file;
	}

	/**
	 * Returns an IFile where the given TestExecutable specification file should
	 * be stored for usage by this run
	 * 
	 * @param executable
	 *            TestExecutable
	 * @return IFile inside the SPEC_FOLDER of this project
	 * @throws CoreException
	 */
	public IFile getSpecificationIFile(TestExecutable executable)
			throws CoreException {
		String execProjName = executable.getIFile().getProject().getName();
		String execRelPath = executable.getIFile().getProjectRelativePath()
				.toOSString();
		String copyRelPath = GtTestCampaignProject.SPEC_FOLDER + File.separator
				+ execProjName + File.separator + execRelPath;

		// make sure that parents exist
		IFile iFile = iProject.getFile(copyRelPath);
		GtResourceHelper.createWithAllParents(iFile.getParent());

		return iFile;
	}

	/**
	 * Returns an IFile where the given TestExecution file for the given
	 * TestExecutable should be stored for usage by this run
	 * 
	 * @param executable
	 *            TestExecutable
	 * @return IFile inside the STATE_FOLDER of this project
	 * @throws CoreException
	 */
	public IFile getStateIFile(TestExecutable executable) throws CoreException {
		String execName = executable.getName();
		String copyRelPath = GtTestCampaignProject.STATE_FOLDER + File.separator
				+ execName;

		// make sure that parents exist
		IFile iFile = iProject.getFile(copyRelPath);
		GtResourceHelper.createWithAllParents(iFile.getParent());

		return iFile;
	}

}
