package org.globaltester.testrunner.testframework;

import org.globaltester.base.util.StringUtil;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.RuntimeRequirementsProvider;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrappedException;

import de.cardcontact.scdp.gp.GPError;

public class ActionStepExecutor {

	private RuntimeRequirementsProvider provider;
	
	public ActionStepExecutor(RuntimeRequirementsProvider provider) {
		this.provider = provider;
	}

	public Result execute(String code, String sourceName) {
		if (!(provider instanceof ScriptRunnerProvider)){
			return ResultFactory.newFailure(FileTestExecution.STATUS_NOT_APPLICABLE, 0, 0, "No script runner available");
		}
		ScriptRunner scriptRunner = ((ScriptRunnerProvider)provider).getScriptRunner();
		
		//unindent code
		code = StringUtil.formatCode(code);
		
		//get and execute the code
		String codeFormat = String.format(TestLogger.DEFAULTFORMAT, "Code:");
		TestLogger.trace(codeFormat + "\n" + code);
		try {
			scriptRunner.exec(code, sourceName, -1);
		} catch (EvaluatorException ex) {
			// this exception is thrown e. g. by asserts
			
			Throwable unwrappedEx = ex;
			if (ex instanceof WrappedException) {
				unwrappedEx = ex.getCause();
			}
			return ResultFactory.newFailure(FileTestExecution.STATUS_FAILURE,
					0, TestLogger.getLogFileLine(), unwrappedEx.toString());

		} catch (Exception ex) {

			if (ex instanceof JavaScriptException) {
				JavaScriptException jse = (JavaScriptException) ex;
				// Some form of JavaScript error.
				Scriptable jseo = (Scriptable) jse.getValue();

				if (jseo.getClassName() == "AssertionError") {
					String msg = (String) jseo.get("message", jseo);
					// Integer errorID = (Integer)gpe.getProperty(gpe,
					// "reason");

					int rating;
					if (jseo.get("reason", jseo).equals(
							Integer.valueOf(FileTestExecution.STATUS_WARNING))) {
						rating = FileTestExecution.STATUS_WARNING;
					} else {
						rating = FileTestExecution.STATUS_FAILURE;
					}
					int scriptLine = jse.lineNumber();
					String expectedValue = (String) jseo.get("expectedValue",
							jseo);
					String receivedValue = (String) jseo.get("receivedValue",
							jseo);
					return ResultFactory.newFailure(rating, scriptLine, TestLogger.getLogFileLine(), msg, expectedValue,
							receivedValue);
				}

				else if (jseo instanceof GPError) {
					GPError gpe = (GPError) jseo;
					String msg = (String) gpe.get("message", gpe);
					// Integer errorID = (Integer)gpe.getProperty(gpe,
					// "reason");

					int rating;
					if (ScriptableObject.getProperty(gpe, "reason").equals(
							Integer.valueOf(FileTestExecution.STATUS_WARNING))) {
						rating = FileTestExecution.STATUS_WARNING;
					} else {
						rating = FileTestExecution.STATUS_FAILURE;
					}
					int scriptLine = jse.lineNumber();
					
					//IMPL handle interactive errors
					/*
					if (msg
							.startsWith("Card communication error: Pcsc10CardTerminal")) {

						String[] buttons = { "Resume", "Skip test",
								"Abort session" };
						MessageDialog md = new MessageDialog(Activator
								.getDefault().getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
								"Card communication error", null, msg,
								MessageDialog.ERROR, buttons, ABORT_ID);

						int ret = md.open();

						if (ret == RESUME_ID) {
							TestLogger.info("Test case resumed by user");
							status = TestCase.STATUS_RESUMED;
							return true;
						}
						if (ret == SKIP_ID) {
							TestLogger.info("Test case skipped by user");
							status = TestCase.STATUS_SKIPPED;
							return false;

						}
						if (ret == ABORT_ID) {
							TestLogger.info("Test session interrupted");
							status = TestCase.STATUS_FAILURE;
							return false;
						}

						return false;

					}
					
					if (msg == "No card in reader or mute card.") {
						Shell shell = Activator.getDefault().getWorkbench()
								.getActiveWorkbenchWindow().getShell();
						MessageDialog.openError(shell, "GlobalTester",
								"No card reader available or no card present!");
						// sr.close();
						return false;
					}
					*/
					return ResultFactory.newFailure(rating, scriptLine, TestLogger.getLogFileLine(), msg);
					
				} else { // if (jseo instanceof GPError)
					return ResultFactory.newFailure(FileTestExecution.STATUS_FAILURE, 0, TestLogger.getLogFileLine(), jse.toString());
				}
			} else {
				// this exception is thrown e. g. by ECMA exceptions
				// this might be a following error, so handle it as warning
				return ResultFactory.newFailure(FileTestExecution.STATUS_WARNING, 0, TestLogger.getLogFileLine(), ex.toString());
			}
		}
		
		//if no error occurred return a new positive result 
		return new Result(Status.PASSED);
	}

}
