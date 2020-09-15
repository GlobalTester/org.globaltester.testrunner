package org.globaltester.testrunner;

import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;

import javax.crypto.Cipher;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.smartcardshell.preferences.SmartCardShellInfo;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.CompositeTestExecution;
import org.globaltester.testrunner.testframework.Result.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * This helper class provides utility methods to dump TestRun information into
 * the TestLogger.
 * 
 * @author amay
 *
 */
public final class TestLogHelper {

	/**
	 * This class does not need to be instantiated as it provides only static
	 * methods
	 */
	private TestLogHelper() {

	}

	public static void dumpLogfileHeaderToTestLogger() {
		Date now = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();

		TestLogger.info("GlobalTester TestRunner " + Activator.VERSION);
		TestLogger.info("Copyright secunet Security Networks AG (Germany)   www.globaltester.org");
		TestLogger.info("Starting new test session at " + df.format(now));
		TestLogger.info("Test executed by: " + System.getProperty("user.name"));

		if (!TestLogHelper.checkJavaVersion()) {
			return;
		}

		TestLogHelper.checkJCEUnlimitedPolicy();

		TestLogger.debug("Operating system: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
				+ "  (" + System.getProperty("os.arch") + ")");

		TestLogger.debug("User directory: " + System.getProperty("user.dir"));

		TestLogHelper.findAndLogGtPlugins();

		SmartCardShellInfo.checkCardReader();
	}

	/**
	 * Find and log available GlobalTester plugins
	 */
	public static void findAndLogGtPlugins() {
		TestLogger.debug("Check for other relevant GlobalTester Plugins...");

		BundleContext context = Activator.getContext();
		for (Bundle currentBundle : context.getBundles()) {
			String name = currentBundle.getSymbolicName();
			if (name.startsWith("com.secunet.globaltester") || name.startsWith("org.globaltester")
					|| name.startsWith("de.persosim")) {
				TestLogger.debug("Plugin available: " + name + " " + currentBundle.getVersion());
			}
		}
	}

	/**
	 * Checks for Java Cryptography Extension (JCE) Unlimited Strength Policy
	 * Files
	 */
	public static void checkJCEUnlimitedPolicy() {
		try {
			int maxKeyLenAES = Cipher.getMaxAllowedKeyLength("AES");
			TestLogger.debug("Maximum key length for AES (permitted by policy): " + maxKeyLenAES + " bit");
			if (maxKeyLenAES < 128) {
				TestLogger.info("WARNING: Unknown key length for AES!");
			}
			if (maxKeyLenAES == 128) {
				TestLogger.info("WARNING: Please install JCE Unlimited Strength Jurisdiction Policy Files!");
			}
			if (maxKeyLenAES > 128) {
				TestLogger.info("Java Cryptography Extension (JCE) Unlimited Strength Policy available");
			}

		} catch (NoSuchAlgorithmException e) {
			BasicLogger.logException(TestLogHelper.class, e);
			TestLogger.info("WARNING: AES Cipher unknown!");
		}
	}

	/**
	 * Checks the required java version, returns true iff at least Java 8
	 */
	public static boolean checkJavaVersion() {

		String javaVersion = System.getProperty("java.version");
		TestLogger.debug("Java version: " + javaVersion + "   (" + System.getProperty("java.vendor") + ")");

		String[] version = javaVersion.split("[.]");

		if (version.length > 1) {
			int major = Integer.parseInt(version[0]);
			int minor = Integer.parseInt(version[1]);
			
			//JAVA 8 is encoded as 1.8 in some VMs
			if (major == 1 || minor > 8) {
				return true;
			}
			
			//otherwise, if major version is at least 8, we should be good
			if (major >= 8) {
				return true;
			}
		}
		
		TestLogger
		.error("Java version is too old!\nAt least java version 1.8 is required. You are using version"
				+ javaVersion);
		return false;
	}

	public static void dumpLogfileFooterToTestLogger(AbstractTestExecution execution) {
		double testTime = execution.getLastExecutionDuration() / 1000.;

		TestLogger.info("");
		TestLogger.info("---------------------------------");
		TestLogger.info("Test Summary");
		TestLogger.info("");
		TestLogger.info("Time for complete session: " + testTime + " sec");
		TestLogger.info("Overall result: " + execution.getStatus());
		if (execution instanceof CompositeTestExecution) {
			TestLogger.info("Number of test cases: " + execution.getChildren().size());
			TestLogger.info("Session failures: "
					+ ((CompositeTestExecution) execution).getNumberOfTestsWithStatus(Status.FAILURE));
			TestLogger.info("Session warnings: "
					+ ((CompositeTestExecution) execution).getNumberOfTestsWithStatus(Status.WARNING));
		}

	}

}
