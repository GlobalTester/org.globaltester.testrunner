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
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.testrunner.Activator;

/**
 * This class extends the usual test execution {@link Result} to provide additional information about script issues that occurred during execution.
 * This includes i.e. the kind off issue like error or warning as well as the respective line numbers in script and log files, see provided object methods.
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */
public class ScriptIssue extends Result implements Serializable {

	private static final long serialVersionUID = -1031815873323547519L;

	// explicit identifier for each issue in test session and log file
	private int id;

	// the line in script where the issue occurred
	private int lineScript;

	// the line in log file where the issue occurred
	private int lineLogFile;

	// issue describing text
	private String issueText;

	// expected value of this issue
	private String expectedValue;

	// received value of this issue
	private String receivedValue;

	/**
	 * Implements {@link ScriptIssue} constructor
	 * 
	 * @param id
	 *            explicit identifier for each issue in test session and log
	 *            file
	 * @param status
	 *            valid status information would be either "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the issue occurred
	 * @param lineLogFile
	 *            the line in log file where the issue occurred
	 * @param issueText
	 *            issue describing text
	 */
	public ScriptIssue(int id, Status status, int lineScript, int lineLogFile,
			String issueText) {
		this(id, status, lineScript, lineLogFile, issueText, "", "");
	}

	/**
	 * Implements {@link ScriptIssue} constructor
	 * 
	 * @param id
	 *            explicit identifier for each issue in test session and log
	 *            file
	 * @param status
	 *            valid status information would be either "WARNING" or "FAILURE"
	 * @param lineScript
	 *            the line in script where the issue occurred
	 * @param lineLogFile
	 *            the line in log file where the issue occurred
	 * @param issueText
	 *            issue describing text
	 * @param expectedValue
	 *            expected value of this issue
	 * @param receivedValue
	 *            receivedValue
	 */
	public ScriptIssue(int id, Status status, int lineScript, int lineLogFile, String issueText, String expectedValue, String receivedValue) {
		
		super(status);
		if ((status != Status.FAILURE) && (status != Status.WARNING) && (status != Status.REQUIREMENT_MISSING)) {
			throw new IllegalArgumentException("ScriptIssue status must be either FAILURE or WARNING or REQUIREMENT_MISSING but is: " + status);
		}
		
		this.id = id;
		this.lineScript = lineScript;
		this.lineLogFile = lineLogFile;
		this.issueText = issueText;
		this.expectedValue = expectedValue;
		this.receivedValue = receivedValue;
		TestLogger.info("@FailureID" + id + ":  " + issueText);
		
		// refresh the workspace
		try {
			ResourcesPlugin.getWorkspace().getRoot()
					.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			//ignore, this may lead to problems later when adding markers, but these will be handled then
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
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
				String message = getIssueText()
						+ " (Expected value: "
						+ getExpectedValue()
						+ "; received value: "
						+ getReceivedValue() + ")";
				marker.setAttribute(IMarker.MESSAGE, message);
			} else {
				marker.setAttribute(IMarker.MESSAGE, getIssueText());
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
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
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
	@Override
	public String toString() {
		return "FailureID" + id;
	}

	/**
	 * Getter for issueText
	 * 
	 * @return issueText String
	 */
	public String getIssueText() {
		return issueText;
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
