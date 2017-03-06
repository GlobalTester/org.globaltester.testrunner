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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;
import java.util.jar.Manifest;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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

	/**
	 * Reduces the complete integrity check results to a single short string
	 * usable i.e. in reports.
	 * 
	 * @param completeResult
	 * @return
	 */
	public static String getSimplifiedCheckResult(Map<String, IntegrityCheckResult> completeResult) {
		String retVal = "consistent";
		for (IntegrityCheckResult curResult : completeResult.values()) {
			switch (curResult.getStatus()) {
			case VALID:
				break;
			case INVALID:
				return "inconsistent";
			case UNCHECKED:
				retVal = "(partially) unchecked";
				break;

			}
		}
		return retVal;
	}

	private HashSet<IContainer> specsToCheck = new HashSet<>();

	/**
	 * Adds specifications to the list of specifications to be checked by this
	 * {@link TestSpecIntegrityChecker}
	 * 
	 * @param additionalSpecsToCheck
	 */
	public void addSpecsToCheck(IContainer... additionalSpecsToCheck) {
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

		for (IContainer curSpec : specsToCheck) {
			retVal.put(curSpec.getName(), verifyChecksum(curSpec));
		}

		return retVal;
	}

	/**
	 * verify the checksum for the given IResource 
	 * @param containerToCheck
	 * @return
	 */
	public IntegrityCheckResult verifyChecksum(IContainer containerToCheck) {
		
		if (!checksumAvailable(containerToCheck)) {
			return new IntegrityCheckResult(IntegrityCheckResult.IntegrityCheckStatus.UNCHECKED);
		}
		
		IntegrityCheckResult.IntegrityCheckStatus status = IntegrityCheckResult.IntegrityCheckStatus.INVALID;
		long expectedChecksum = -1;
		long calculatedChecksum = -1;
		
		try {
			expectedChecksum = getExpectedChecksum(containerToCheck);
		} catch (Exception e) {
			//ignore as result will be INVALID anyhow 
		}
		
		try {
			calculatedChecksum = generateChecksum(containerToCheck);
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
	public static boolean checksumAvailable(IContainer projectToCheck) {

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
	 * @param containerToCheck
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public static long getExpectedChecksum(IContainer containerToCheck) throws CoreException, IOException {
		long expectedChecksum = -1;
		
		//extract checksum from IContainer
		IResource checksumFile = containerToCheck.findMember(CHECKSUM_FILENAME);
		if (checksumFile == null || !checksumFile.exists() || !(checksumFile instanceof IFile)) {
			throw new FileNotFoundException("Checksum file for "+containerToCheck.getName()+" not found.");
		}
		expectedChecksum = readChecksumFromInputStream(((IFile)checksumFile).getContents());			
		
		//try to extract checksum from Bundle
		String bundleSymbolicName = getBundleSymbolicName(containerToCheck);
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
	 * @param containerToCheck
	 * @return checksum of test files
	 * @throws CoreException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public long generateChecksum(IContainer containerToCheck) throws IOException, CoreException {
		
		IResource fileListFile = containerToCheck.findMember(FILELIST_FILENAME);
		if (fileListFile == null || !fileListFile.exists() || !(fileListFile instanceof IFile)) {
			throw new FileNotFoundException("No filelist found for "+containerToCheck.getName()+".");
		}
		
		Vector<InputStream> v = new Vector<InputStream>();
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(((IFile)fileListFile).getContents()))) {
			String currentLine;
			while ((currentLine = br.readLine()) != null) {
				
				IResource currentFile = containerToCheck.findMember(currentLine);
				if (currentFile == null || !currentFile.exists() || !(currentFile instanceof IFile)) {
					throw new FileNotFoundException(containerToCheck.getName()+" does not contain file "+ currentLine +", referenced in filelist.");
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
	 * @param projectContainer the project to check
	 * @return the symbolic name (String)
	 * @throws IOException 
	 */
	public static String getBundleSymbolicName(IContainer projectContainer) {
		IPath pathManifest = new Path(projectContainer.getLocation().toOSString()+File.separator+"META-INF"+File.separator+"MANIFEST.MF");
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
}
