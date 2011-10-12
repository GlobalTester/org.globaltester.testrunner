package org.globaltester.testrunner.testframework;

import org.globaltester.logging.logger.TestLogger;
import org.globaltester.smartcardshell.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;

public class TestStepExecutor {

	private ScriptRunner scriptRunner;
	private Context context;
	
	public TestStepExecutor(ScriptRunner sr, Context cx) {
		scriptRunner= sr;
		context = cx;
	}

	public Result execute(String code, String sourceName) {
		//get and execute the code
		TestLogger.trace("Code block to be executed: \n" + code);
		try {
		scriptRunner.executeCommand(context, code, sourceName, -1);
//		} catch (EvaluatorException ex) {
//			// this exception is thrown e. g. by asserts
//			if (ex instanceof WrappedException) {
//				return generateFailure(TestCase.STATUS_FAILURE, ex.getCause()
//						.toString(), 0);
//			} else {
//				generateFailure(TestCase.STATUS_FAILURE, ex.toString(), 0);
//			}
		} catch (Exception ex) {

			if (ex instanceof JavaScriptException) {
				JavaScriptException jse = (JavaScriptException) ex;
				// Some form of JavaScript error.
				Scriptable jseo = (Scriptable) jse.getValue();

//				if (jseo.getClassName() == "AssertionError") {
//					String msg = (String) jseo.get("message", jseo);
//					// Integer errorID = (Integer)gpe.getProperty(gpe,
//					// "reason");
//
//					int rating;
//					if (jseo.get("reason", jseo).equals(
//							Integer.valueOf(TestCase.ID_WARNING))) {
//						rating = TestCase.STATUS_WARNING;
//					} else {
//						rating = TestCase.STATUS_FAILURE;
//					}
//					int scriptLine = jse.lineNumber();
//					String expectedValue = (String) jseo.get("expectedValue",
//							jseo);
//					String receivedValue = (String) jseo.get("receivedValue",
//							jseo);
//					generateFailure(rating, msg, scriptLine, expectedValue,
//							receivedValue);
//					return true;
//				}
//
//				if (jseo instanceof GPError) {
//					GPError gpe = (GPError) jseo;
//					String msg = (String) gpe.get("message", gpe);
//					// Integer errorID = (Integer)gpe.getProperty(gpe,
//					// "reason");
//
//					int rating;
//					if (ScriptableObject.getProperty(gpe, "reason").equals(
//							Integer.valueOf(TestCase.ID_WARNING))) {
//						rating = TestCase.STATUS_WARNING;
//					} else {
//						rating = TestCase.STATUS_FAILURE;
//					}
//					int scriptLine = jse.lineNumber();
//					generateFailure(rating, msg, scriptLine);
//
//					if (msg
//							.startsWith("Card communication error: Pcsc10CardTerminal")) {
//
//						String[] buttons = { "Resume", "Skip test",
//								"Abort session" };
//						MessageDialog md = new MessageDialog(Activator
//								.getDefault().getWorkbench()
//								.getActiveWorkbenchWindow().getShell(),
//								"Card communication error", null, msg,
//								MessageDialog.ERROR, buttons, ABORT_ID);
//
//						int ret = md.open();
//
//						if (ret == RESUME_ID) {
//							TestLogger.info("Test case resumed by user");
//							status = TestCase.STATUS_RESUMED;
//							return true;
//						}
//						if (ret == SKIP_ID) {
//							TestLogger.info("Test case skipped by user");
//							status = TestCase.STATUS_SKIPPED;
//							return false;
//
//						}
//						if (ret == ABORT_ID) {
//							TestLogger.info("Test session interrupted");
//							status = TestCase.STATUS_FAILURE;
//							return false;
//						}
//
//						return false;
//
//					}
//					
//					//TODO handle interactive errors
//					/*
//					if (msg == "No card in reader or mute card.") {
//						Shell shell = Activator.getDefault().getWorkbench()
//								.getActiveWorkbenchWindow().getShell();
//						MessageDialog.openError(shell, "GlobalTester",
//								"No card reader available or no card present!");
//						// sr.close();
//						return false;
//					}
//					*/
//				} else { // if (jseo instanceof GPError)
					return generateFailure(FileTestExecution.STATUS_FAILURE, jse.toString(), 0);
//				}
//				TestLogger.info("");
			} else {
				// this execption is thrown e. g. by ECMA exceptions
				// this migtht be a following error, so handle it as warning
				generateFailure(FileTestExecution.STATUS_WARNING, ex.toString(), 0);
			}
		}
		
		//if no error occurred return a new positive result 
		return new Result(Status.PASSED);
	}
		


	/**
	 * Generates a new Failure
	 * 
	 * @param rating
	 *            rating of the corresponding failure (should be one of
	 *            TestCase.STATUS_)
	 * @param failureText
	 *            text describing the failure
	 * @param scriptLine
	 *            line in script where the failure occured
	 */
	private Failure generateFailure(int rating, String failureText, int scriptLine) {
		Failure failure = ResultFactory.newFailure(rating, scriptLine, 0,
				failureText);
//		failureList.addLast(failure);
//		if (rating == TestCase.STATUS_WARNING)
//			warnings++;
//		else
//			failures++;
		// log.info("Current directory: "+sr.getWorkingDirectory());
		return failure;
	}

}
