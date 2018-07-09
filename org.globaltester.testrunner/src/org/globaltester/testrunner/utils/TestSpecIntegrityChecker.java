package org.globaltester.testrunner.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.SequenceInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Manifest;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.osgi.framework.Bundle;

/**
 * This class encapsulates behavior to check the integrity of available
 * TestSpecifications (either from {@link IProjects} or the {@link IFolders}
 * persisted inside of {@link GtTestCampaignProjects}.
 * 
 * @author may.alexander
 *
 */
public class TestSpecIntegrityChecker {

	private static final String FILELIST_FILENAME = "filelist.a32";
	private static final String CHECKSUM_FILENAME = "checksum.a32";

	private HashSet<IProject> specsToCheck = new HashSet<>();

	/**
	 * Adds specifications to the list of specifications to be checked by this
	 * {@link TestSpecIntegrityChecker}
	 * 
	 * @param additionalSpecsToCheck
	 */
	public void addSpecsToCheck(IProject ... additionalSpecsToCheck) {
		specsToCheck.addAll(Arrays.asList(additionalSpecsToCheck));
	}

	/**
	 * Check the integrity of all specifications currently stored in this
	 * object.
	 * 
	 * @return results of each integrity check
	 */
	public Map<String, IntegrityCheckResult> check() {
		Map<String, IntegrityCheckResult> retVal = new HashMap<>();

		for (IProject curSpec : specsToCheck) {
			retVal.put(curSpec.getName(), verifyChecksum(curSpec));
		}

		return retVal;
	}

	/**
	 * verify the checksum for the given IResource 
	 * @param projectToCheck
	 * @return
	 */
	public IntegrityCheckResult verifyChecksum(IProject projectToCheck) {
		
		if (!checksumAvailable(projectToCheck)) {
			return new IntegrityCheckResult(IntegrityCheckResult.IntegrityCheckStatus.UNCHECKED);
		}
		
		IntegrityCheckResult.IntegrityCheckStatus status = IntegrityCheckResult.IntegrityCheckStatus.INVALID;
		long expectedChecksum = -1;
		long calculatedChecksum = -1;
		
		try {
			expectedChecksum = getExpectedChecksum(projectToCheck);
		} catch (Exception e) {
			//ignore as result will be INVALID anyhow 
		}
		
		try {
			calculatedChecksum = generateChecksum(projectToCheck);
		} catch (Exception e) {
			//ignore as result will be INVALID anyhow
		}	
	
		
		if (calculatedChecksum != -1 && expectedChecksum == calculatedChecksum) {
			status = IntegrityCheckResult.IntegrityCheckStatus.VALID;
		}
		
		
		return new IntegrityCheckResult(status, expectedChecksum, calculatedChecksum);
	}

	/**
	 * Returns whether a checksum is available for the given container. This
	 * check availability of checksum and fileleist files and may be extended in
	 * the future to check for actual code signing signatures
	 * 
	 * @param projectToCheck
	 * @return
	 */
	public static boolean checksumAvailable(IProject projectToCheck) {

		if (!projectToCheck.exists()) {
			return false;
		}

		if (projectToCheck.findMember(FILELIST_FILENAME) == null) {
			return false;
		}

		if (projectToCheck.findMember(CHECKSUM_FILENAME) == null) {
			return false;
		}

		// IMPL check for signatur in bundle, when codesigning is used

		return true;
	}

	/**
	 * Extract expected checksum for the given IContainer.
	 * 
	 * If a (potentially codesigned) bundle exists the checksum is extracted from that bundle.
	 * 
	 * @param projectToCheck
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public static long getExpectedChecksum(IProject projectToCheck) throws CoreException, IOException {
		long expectedChecksum = -1;
		
		//extract checksum from IContainer
		IResource checksumFile = projectToCheck.findMember(CHECKSUM_FILENAME);
		if (checksumFile == null || !checksumFile.exists() || !(checksumFile instanceof IFile)) {
			throw new FileNotFoundException("Checksum file for "+projectToCheck.getName()+" not found.");
		}
		expectedChecksum = readChecksumFromInputStream(((IFile)checksumFile).getContents());			
		
		//try to extract checksum from Bundle
		String bundleSymbolicName = getBundleSymbolicName(projectToCheck);
		if(bundleSymbolicName != null) {
			Bundle originalBundle = Platform.getBundle(bundleSymbolicName);
			
			if(originalBundle != null) {
				expectedChecksum = -1;
				
				try (InputStream checkSumInputStream = FileLocator.openStream(originalBundle, new Path(CHECKSUM_FILENAME), false)) {
					expectedChecksum = readChecksumFromInputStream(checkSumInputStream);
				} 
				
			}
			
		}
		
		return expectedChecksum;
	}

	/**
	 * Generate checksum of all test file to assure integrity of test spec
	 * 
	 * @param projectToCheck
	 * @return checksum of test files
	 * @throws CoreException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public long generateChecksum(IProject projectToCheck) throws IOException, CoreException {
		
		IResource fileListFile = projectToCheck.findMember(FILELIST_FILENAME);
		if (fileListFile == null || !fileListFile.exists() || !(fileListFile instanceof IFile)) {
			throw new FileNotFoundException("No filelist found for "+projectToCheck.getName()+".");
		}
		
		Vector<InputStream> v = new Vector<InputStream>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(((IFile)fileListFile).getContents()))) {
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				
				IResource currentFile = projectToCheck.findMember(currentLine);
				if (currentFile == null || !currentFile.exists() || !(currentFile instanceof IFile)) {
					throw new FileNotFoundException(projectToCheck.getName()+" does not contain file "+ currentLine +", referenced in filelist.");
				}
				
				v.addElement(((IFile)currentFile).getContents());
			}
		}
		
		
		
		long checksum = -1;
		try (InputStream seq = new SequenceInputStream(v.elements());
				CheckedInputStream in = new CheckedInputStream(seq, new Adler32())) {

			byte[] buf = new byte[4096];

			while ((in.read(buf)) > 0) {
				// read everything and let the stream generate the checksum :)
			}
			checksum = in.getChecksum().getValue();
		}

		return checksum;
	}

	
	/**
	 * Read checksum value from InputStream
	 * 
	 * @param is
	 *            the InputStream to read from
	 * @return the read checksum
	 * @throws IOException 
	 */
	public static long readChecksumFromInputStream(InputStream is) throws IOException {
		return readChecksumFromReader(new InputStreamReader(is));
	}

	/**
	 * Read checksum value from Reader
	 * 
	 * @param reader
	 *            the Reader to read from
	 * @return the read checksum
	 * @throws IOException 
	 */
	private static long readChecksumFromReader(Reader reader) throws IOException {
		long checksum = -1;
		try (BufferedReader br = new BufferedReader(reader)) {
			checksum = Long.parseLong(br.readLine());
		}
		
		return checksum;
	}

	/**
	 * This method returns the symbolic name of a provided IContainer
	 * @param projectToCheck the project to check
	 * @return the symbolic name (String)
	 * @throws IOException 
	 */
	public static String getBundleSymbolicName(IProject projectToCheck) {
		IPath pathManifest = new Path(projectToCheck.getLocation().toOSString()+File.separator+"META-INF"+File.separator+"MANIFEST.MF");
		File manifestFile = pathManifest.toFile();
		Manifest manifest;
		try {
			if (manifestFile.exists()) {
				manifest = new Manifest(new FileInputStream(manifestFile));
				String bundleSymbolicNameUnprocessed = manifest.getMainAttributes().getValue("Bundle-SymbolicName");
				String bundleSymbolicNameProcessed = bundleSymbolicNameUnprocessed.split(";")[0];
				
				return bundleSymbolicNameProcessed;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void addRecursive(ITestExecutable testExecutable) {
		if (testExecutable instanceof FileTestExecutable) {
			addSpecsToCheck(((FileTestExecutable) testExecutable).getIFile().getProject());
		}
		
		for (ITestExecutable curChild : testExecutable.getChildren()) {
			addRecursive(curChild);
		}
	}

	public void addDependencies() {
		
		HashMap<String, IProject> workspaceProjects = getWorkspaceProjects();
		
		
		LinkedList<String> dependenciesToAdd = new LinkedList<>();
		for (IProject curProject : specsToCheck) {
			dependenciesToAdd.addAll(getRequiredBundles(curProject));
		}
		
		while (!dependenciesToAdd.isEmpty()) {
			String curDependency = dependenciesToAdd.pop();
			if (workspaceProjects.containsKey(curDependency)) {
				IProject curProject = workspaceProjects.get(curDependency);
				if (!specsToCheck.contains(curProject)) {
					addSpecsToCheck(curProject);
					dependenciesToAdd.addAll(getRequiredBundles(curProject));
				}
			}
		}
		
	}
	
	/**
	 * This method checks the manifest of a specific project for required
	 * Bundles and returns them
	 * 
	 * @param currentProject: the project (IProject) to check
	 * @return A String Array that contains the symbolic names. Beware that the
	 *         symbolic names can differ from the 'real' name
	 */
	public List<String> getRequiredBundles(IProject projectToCheck){
			
		IPath currentProjectLocation = projectToCheck.getLocation();
		IPath pathManifest = new Path(currentProjectLocation.toOSString() + File.separator + "META-INF" + File.separator + "MANIFEST.MF");
		File manifestFile = pathManifest.toFile();

		try {
			Manifest manifest = new Manifest(new FileInputStream(manifestFile));
			String requiredBundles = manifest.getMainAttributes().getValue("Require-Bundle");
			
			if(requiredBundles==null) return Collections.emptyList();
			
			String[] bundleSymbolicNames = requiredBundles.split(","); 
			
			for(int i = 0; i<bundleSymbolicNames.length; i++){
				//still need to remove optional version numbers
				bundleSymbolicNames[i]=bundleSymbolicNames[i].split(";")[0];
			}
			return Arrays.asList(bundleSymbolicNames);
			
		} catch (IOException e) {
			TestLogger.error("Unable to get required bundles from Manifest file", e);
			return Collections.emptyList();
		}
	}

	
	/**
	 * Resolves Bundle names and adds them to the list if possible (avoids redundancy)
	 * @param requiredBundles: The Required Bundles field from the Manifest as String
	 * @return 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private HashMap<String, IProject> getWorkspaceProjects(){

		HashMap<String, IProject> retVal = new HashMap<>();
		
		for (IProject curProj : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			retVal.put(getBundleSymbolicName(curProj), curProj);
		}
		
		return retVal;
	}
}
