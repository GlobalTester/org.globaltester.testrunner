package org.globaltester.testrunner;

import java.io.File;
import java.net.URI;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.globaltester.base.interfaces.ITreeChangeListener;
import org.globaltester.base.interfaces.ITreeObservable;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.utils.GtDateHelper;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.TestExecutableFactory;

/**
 * Represents and handles the workspace representation of a TestCampaign
 * 
 * @author amay
 * 
 */
public class GtTestCampaignProject implements ITreeObservable {

	public static final String SPEC_FOLDER = "TestSpecification";
	public static final String STATE_FOLDER = "ExecutionState";
	public static final String RESULT_FOLDER = "TestResults";
	
	public static final String FILE_ENDING_GT_CAMPAIGN = "gtcampaign";
	public static final String DEFAULT_FILE_NAME_GT_CAMPAIGN = "testCampaign." + FILE_ENDING_GT_CAMPAIGN;

	private static Hashtable<IProject, GtTestCampaignProject> instances = new Hashtable<IProject, GtTestCampaignProject>();
	private IProject iProject; // IProject that is represented by this
								// instance
	private TestCampaign testCampaign;
	private HashSet<ITreeChangeListener> treeChangeListeners = new HashSet<ITreeChangeListener>();

	/**
	 * Create a GlobalTester TestCampaign Project. This includes creation
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

		IProject project = GtResourceHelper.createEmptyProject(projectName, location);
		try {
			GtResourceHelper.addNature(project, GtTestCampaignNature.NATURE_ID);

			String[] paths = { STATE_FOLDER, SPEC_FOLDER,
					RESULT_FOLDER };
			GtResourceHelper.addToProjectStructure(project, paths);
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

	public static GtTestCampaignProject getProjectForResource(
			IResource selectedResource) throws CoreException {

		IProject iProject = selectedResource.getProject();

		if (!instances.containsKey(iProject)) {
			instances.put(iProject, new GtTestCampaignProject(iProject));
		}

		return instances.get(iProject);
	}

	private GtTestCampaignProject(IProject iProject) throws CoreException {
		Assert.isTrue(iProject.hasNature(GtTestCampaignNature.NATURE_ID),
					"Project does not use GtTestCampaignNature");
		
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
	public String getNewResultDir() throws CoreException {
		// initialize test logging for this test session
		IFolder defaultLoggingDir = iProject.getFolder(RESULT_FOLDER
				+ File.separator + GtDateHelper.getCurrentTimeString());
		
		GtResourceHelper.createWithAllParents(defaultLoggingDir);

		return defaultLoggingDir.getLocation().toOSString();

	}

	/**
	 * Returns the IFile containing this projects additional data
	 * 
	 * @return
	 * @throws CoreException
	 */
	public IFile getTestCampaignIFile() throws CoreException {
		IFile file = getIProject().getFile(DEFAULT_FILE_NAME_GT_CAMPAIGN);
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
	public IFile getSpecificationIFile(FileTestExecutable executable)
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
	 * Returns the IFolder that is used to persist TestSpecifications. Although
	 * IResource operations are handle operations only this will create the
	 * folder (and its parents if needed).
	 * 
	 * @return the IFolder that is used to persist TestSpecifications
	 * @throws CoreException 
	 */
	public IFolder getSpecificationFolder() throws CoreException {
		IFolder specFolder = iProject.getFolder(SPEC_FOLDER);
		GtResourceHelper.createWithAllParents(specFolder);
		return specFolder;
	}

	/**
	 * Returns a new IFile where the given TestExecution file for the given
	 * TestExecutable should be stored for usage by this run
	 * 
	 * @param executable
	 *            TestExecutable
	 * @return IFile inside the STATE_FOLDER of this project
	 * @throws CoreException
	 */
	public IFile getNewStateIFile(FileTestExecutable executable) throws CoreException {
		String execName = GtDateHelper.getCurrentTimeString() + "_" + executable.getName()+".gt";

		return getNewStateIFile(execName);
	}
	
	public IFile getNewCampaignStateIFile() throws CoreException {
		String execName = GtDateHelper.getCurrentTimeString() + "_" + getName()+".gt";

		return getNewStateIFile(execName);
	}
	
	private IFile getNewStateIFile(String execName) throws CoreException {
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
				RESULT_FOLDER);
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
	public FileTestExecutable persistTestExecutable(FileTestExecutable executable) throws CoreException {
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

	public static boolean isTestCampaignProjectAvailableForResource(IResource resource) {
		if (resource instanceof IProject) {
			 try {
				if (((IProject) resource).hasNature(GtTestCampaignNature.NATURE_ID)) {
				return true;
				 }
			} catch (CoreException e) {
				// seems not to be a TestCampaign
			}
		} else if ((resource instanceof IFile) && 
			((IFile) resource).getFileExtension().equals(GtTestCampaignProject.FILE_ENDING_GT_CAMPAIGN)) {
			return true;
		}
		return false;
	}

}
