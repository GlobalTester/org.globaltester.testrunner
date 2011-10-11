package org.globaltester.testrunner;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
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
import org.globaltester.interfaces.ITreeChangeListener;
import org.globaltester.interfaces.ITreeObservable;
import org.globaltester.logging.logger.GTLogger;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testspecification.testframework.TestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;

/**
 * Represents and handles the workspace representation of a TestCampaign
 * 
 * @author amay
 * 
 */
public class GtTestCampaignProject implements ITreeObservable {

	private static final String SPEC_FOLDER = "TestSpecification";
	private static final String CONFIG_FOLDER = "DUTconfiguration";
	private static final String STATE_FOLDER = "ExecutionState";
	private static final String RESULT_FOLDER = "TestResults";

	private static Hashtable<IProject, GtTestCampaignProject> instances = new Hashtable<IProject, GtTestCampaignProject>();
	private IProject iProject; // IProject that is represented by this
								// instance
	private TestCampaign testCampaign;
	private HashSet<ITreeChangeListener> treeChangeListeners = new HashSet<ITreeChangeListener>();

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
			// refresh workspace failed
			// log CoreException to eclipse log
			GtErrorLogger.log(Activator.PLUGIN_ID, e);

			// users most probably will ignore this behavior and refresh
			// manually, so do not open annoying dialog
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
		try {
			Assert.isTrue(iProject.hasNature(GtTestCampaignNature.NATURE_ID),
					"Project does not use GtTestCampaignNature");
		} catch (CoreException e) {
			Assert.isTrue(false, "Project nature can not be checked");
		}

		this.iProject = iProject;

		this.testCampaign = new TestCampaign(this);

		IFile iFile = getTestCampaignIFile();
		if (iFile.exists()) {
			// read current state from file
			this.testCampaign.initFromIFile(iFile);
		} else {
			// create the IFile and fill with initial content
			this.testCampaign.storeToIFile(iFile);
		}
	}

	/**
	 * @return the iProject
	 */
	public IProject getIProject() {
		return iProject;
	}

	// create a new ResultDirectory
	public String getNewResultDir() {
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
	 * Returns the IFile containing this projects additional data
	 * 
	 * @return
	 * @throws CoreException
	 */
	public IFile getTestCampaignIFile() throws CoreException {
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
		String copyRelPath = GtTestCampaignProject.STATE_FOLDER
				+ File.separator + execName;

		// make sure that parents exist
		IFile iFile = iProject.getFile(copyRelPath);
		GtResourceHelper.createWithAllParents(iFile.getParent());

		return iFile;
	}

	public String getName() {
		return getIProject().getName();
	}

	public TestCampaign getTestCampaign() {
		return testCampaign;
	}

	public IFolder getDefaultLoggingDir() {
		return getIProject().getFolder(
				RESULT_FOLDER + File.separator + "Logging");
	}

	/**
	 * Store the given TestExecutable in the TestCampaignProject. If the
	 * specification is already present this will return the existing instance.
	 * If not the specification will be copied and a new TestExecutable will be
	 * created and returned.
	 * 
	 * @param executable
	 * @return
	 * @throws CoreException 
	 */
	public TestExecutable persistTestExecutable(TestExecutable executable) throws CoreException {
		//no further action needed if executable is already located in this GtTestCampaignProject
		if (executable.getIFile().getProject().equals(this.iProject))
			return executable;
		
		// generate the IFile representing the local specification
		IFile localSpecIFile = getSpecificationIFile(executable);
		
		//if the specification is not yet present in this project copy it to the given IFile
		if (!localSpecIFile.exists()){
			executable.copyTo(localSpecIFile);
		}
		
		return TestExecutableFactory.getInstance(localSpecIFile);
	}

	public void doSave() throws CoreException {
		testCampaign.doSave();
	}

	@Override
	public void removeTreeChangeListener(ITreeChangeListener oldListener) {
		treeChangeListeners.remove(oldListener);
	}

	@Override
	public void addTreeChangeListener(ITreeChangeListener newListener) {
		treeChangeListeners.add(newListener);
	}
	
	public void notifyTreeChangeListeners(boolean structureChanged,
			Object[] changedElements, String[] properties){
		Iterator<ITreeChangeListener> listenerIter = treeChangeListeners.iterator();
		while (listenerIter.hasNext()) {
			ITreeChangeListener currentListener = (ITreeChangeListener) listenerIter
					.next();
			
			currentListener.notifyTreeChange(this, structureChanged, changedElements, properties);
		}
	}

}
