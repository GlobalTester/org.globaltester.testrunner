package org.globaltester.testrunner.testframework;

import org.globaltester.base.util.StringUtil;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.scriptrunner.RuntimeRequirementsProvider;
import org.globaltester.scriptrunner.AssertionFailure;
import org.globaltester.scriptrunner.AssertionWarning;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
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
			return ResultFactory.newFailure(Status.NOT_APPLICABLE, 0, 0, "No script runner available");
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
			return ResultFactory.newFailure(Status.FAILURE,
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

					Status status;
					if (jseo.get("reason", jseo).equals(
							Integer.valueOf(FileTestExecution.STATUS_WARNING))) {
						status = Status.WARNING;
					} else {
						status = Status.FAILURE;
					}
					int scriptLine = jse.lineNumber();
					String expectedValue = (String) jseo.get("expectedValue",
							jseo);
					String receivedValue = (String) jseo.get("receivedValue",
							jseo);
					return ResultFactory.newFailure(status, scriptLine, TestLogger.getLogFileLine(), msg, expectedValue,
							receivedValue);
				} else if (jseo instanceof GPError) {
					GPError gpe = (GPError) jseo;
					String msg = (String) gpe.get("message", gpe);
					// Integer errorID = (Integer)gpe.getProperty(gpe,
					// "reason");

					Status status;
					if (ScriptableObject.getProperty(gpe, "reason").equals(
							Integer.valueOf(FileTestExecution.STATUS_WARNING))) {
						status = Status.WARNING;
					} else {
						status = Status.FAILURE;
					}
					int scriptLine = jse.lineNumber();
					
					return ResultFactory.newFailure(status, scriptLine, TestLogger.getLogFileLine(), msg);

				} else if (jseo instanceof NativeJavaObject) {
					Object nativeJavaObject = ((NativeJavaObject)jseo).unwrap();
					if (nativeJavaObject instanceof AssertionFailure) {
						AssertionFailure error = (AssertionFailure) nativeJavaObject;
						return ResultFactory.newFailure(Status.FAILURE, jse.lineNumber(), TestLogger.getLogFileLine(), error.getMessage());
					} else if (nativeJavaObject instanceof AssertionWarning) {
						AssertionWarning error = (AssertionWarning) nativeJavaObject;
						return ResultFactory.newFailure(Status.WARNING, jse.lineNumber(), TestLogger.getLogFileLine(), error.getMessage());
						}
				} else {
					return ResultFactory.newFailure(Status.FAILURE, 0, TestLogger.getLogFileLine(), jse.toString());
				}
			} else {
				// this exception is thrown e. g. by ECMA exceptions
				// this might be a following error, so handle it as warning
				return ResultFactory.newFailure(Status.WARNING, 0, TestLogger.getLogFileLine(), ex.toString());
			}
		}
		
		//if no error occurred return a new positive result 
		return new Result(Status.PASSED);
	}

}
