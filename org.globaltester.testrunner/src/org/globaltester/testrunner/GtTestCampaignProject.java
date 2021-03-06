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
import org.globaltester.testspecification.testframework.ITestExecutable;

/**
 * Represents and handles the workspace representation of a TestCampaign
 * 
 * @author amay
 * 
 */
public class GtTestCampaignProject implements ITreeObservable {

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

			String[] paths = { STATE_FOLDER, RESULT_FOLDER };
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
		if (selectedResource == null) return null;

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
	 * Returns a new IFile where the given TestExecution file for the given
	 * TestExecutable should be stored for usage by this run
	 * 
	 * @param executable
	 *            TestExecutable
	 * @return IFile inside the STATE_FOLDER of this project
	 * @throws CoreException
	 */
	public IFile getNewStateIFile(ITestExecutable executable) throws CoreException {
		return getNewStateIFile(executable.getName());
	}
	
	public IFile getNewCampaignStateIFile() throws CoreException {
		curExecutionTimeStamp = GtDateHelper.getCurrentTimeString();
		curExecutionCounter = 0;
		return getNewStateIFile(getName());
	}
	
	String curExecutionTimeStamp = GtDateHelper.getCurrentTimeString();
	int curExecutionCounter = 0;
	
	private IFile getNewStateIFile(String execName) throws CoreException {

		String format = GtTestCampaignProject.STATE_FOLDER
				+ File.separator + "%s_%03d_%s" + ".gtstate"; 
		String name = String.format(format, curExecutionTimeStamp, curExecutionCounter++, execName);
		IFile iFile = iProject.getFile(name);
		
		// make sure that parents exist
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
				return ((IProject) resource).hasNature(GtTestCampaignNature.NATURE_ID);
			} catch (CoreException e) { //NOSONAR this Exception is API behavior for the call and properly handled by returning false
				// seems not to be a TestCampaign
				return false;
			}
		} else if ((resource instanceof IFile)) {
			return GtTestCampaignProject.FILE_ENDING_GT_CAMPAIGN.equals(((IFile) resource).getFileExtension());
		} else {
			return false;
		}
	}

}
