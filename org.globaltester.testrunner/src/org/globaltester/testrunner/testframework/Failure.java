package org.globaltester.testrunner.testframework;

import java.io.Serializable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.logging.legacy.logger.TestLogger;

/**
 * This class implements the information of a test failure.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
public class Failure extends Result implements Serializable {

	private static final long serialVersionUID = -1031815873323547519L;

	// explicit identifier for each failure in test session and log file
	private int id;

	// the line in script where the failure occurred
	private int lineScript;

	// the line in log file where the failure occurred
	private int lineLogFile;

	// failure describing text
	private String failureText;

	// expected value of this failure
	private String expectedValue;

	// received value of this failure
	private String receivedValue;

	/**
	 * Implements Failure constructor
	 * 
	 * @param id
	 *            explicit identifier for each failure in test session and log
	 *            file
	 * @param status
	 *            valid status information would be either "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the failure occured
	 * @param lineLogFile
	 *            the line in log file where the failure occured
	 * @param failureText
	 *            failure describing text
	 */
	public Failure(int id, Status status, int lineScript, int lineLogFile,
			String failureText) {
		this(id, status, lineScript, lineLogFile, failureText, "", "");
	}

	/**
	 * Implements Failure constructor
	 * 
	 * @param id
	 *            explicit identifier for each failure in test session and log
	 *            file
	 * @param status
	 *            valid status information would be either "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the failure occurred
	 * @param lineLogFile
	 *            the line in log file where the failure occurred
	 * @param failureText
	 *            failure describing text
	 * @param expectedValue
	 *            expected value of this failure
	 * @param receivedValue
	 *            receivedValuee
	 */
	public Failure(int id, Status status, int lineScript, int lineLogFile, String failureText, String expectedValue, String receivedValue) {
		
		super(status);
		if ((status != Status.FAILURE) && (status != Status.WARNING)) {
			throw new RuntimeException("Failure rating must be either FAILURE or WARNING");
		}
		
		this.id = id;
		this.lineScript = lineScript;
		this.lineLogFile = lineLogFile;
		this.failureText = failureText;
		this.expectedValue = expectedValue;
		this.receivedValue = receivedValue;
		TestLogger.info("@FailureID" + id + ":  " + failureText);
		
		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
					.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			//ignore, this may lead to problems later when adding markers, but these will be hendled then
		}
		
		//add markers
		IFile logFile = GtResourceHelper.getIFileForLocation(TestLogger.getLogFileName());
		IFile tcLogFile = GtResourceHelper.getIFileForLocation(TestLogger.getTestCaseLogFileName());
		addMarker(logFile);
		if ((tcLogFile != null) && (!tcLogFile.equals(logFile))) {
			addMarker(tcLogFile);
		}
	}
	
	private void addMarker(IFile file) {
			if (file == null) return;
		try {
			IMarker marker = file
					.createMarker("org.globaltester.testrunner.GTFailureMarker");
			marker.setAttribute("expectedValue", getExpectedValue());
			marker.setAttribute("receivedValue", getReceivedValue());
			marker.setAttribute(IMarker.LINE_NUMBER, getLineLogFile());

			//set message
			if (getExpectedValue() != null
					&& getReceivedValue() != null) {
				String message = getFailureText()
						+ " (Expected value: "
						+ getExpectedValue()
						+ "; received value: "
						+ getReceivedValue() + ")";
				marker.setAttribute(IMarker.MESSAGE, message);
			} else {
				marker.setAttribute(IMarker.MESSAGE, getFailureText());
			}
			
			//set severity
			if (getStatus() == Status.WARNING) {
				marker.setAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_WARNING);
			} else {
				marker.setAttribute(IMarker.SEVERITY,
						IMarker.SEVERITY_ERROR);
			}

			// store markers persistent if needed
			IPreferencesService prefService = Platform.getPreferencesService();
			if (prefService.getBoolean(org.globaltester.logging.legacy.Activator.PLUGIN_ID,
					org.globaltester.logging.legacy.preferences.PreferenceConstants.P_TEST_PERSISTENTMARKER, false, null)) {
				marker.setAttribute(IMarker.TRANSIENT, false);
			} else {
				marker.setAttribute(IMarker.TRANSIENT, true);
			}
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Getter for failureID
	 * 
	 * @return failureID int
	 */
	public int getId() {
		return id;
	}

	/**
	 * Getter for lineLogFile
	 * 
	 * @return lineLogFile int
	 */
	public int getLineLogFile() {
		return lineLogFile;
	}

	/**
	 * Setter for lineLogFile
	 * 
	 * @param lineLogFile
	 *            int
	 */
	public void setLineLogFile(int lineLogFile) {
		this.lineLogFile = lineLogFile;
	}

	/**
	 * Getter for lineScript
	 * 
	 * @return lineScript int
	 */
	public int getLineScript() {
		return lineScript;
	}

	/**
	 * Return failureID when using toString()
	 * 
	 * @return failureID String
	 */
	public String toString() {
		return "FailureID" + id;
	}

	/**
	 * Getter for failureText
	 * 
	 * @return failureText String
	 */
	public String getFailureText() {
		return failureText;
	}

	/**
	 * Getter for expectedValue
	 * 
	 * @return expectedValue String
	 */
	public String getExpectedValue() {
		return expectedValue;
	}

	/**
	 * Getter for receivedValue
	 * 
	 * @return receivedValue String
	 */
	public String getReceivedValue() {
		return receivedValue;
	}

}
