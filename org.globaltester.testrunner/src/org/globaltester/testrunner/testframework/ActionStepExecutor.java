package org.globaltester.testrunner.testframework;

import org.globaltester.base.SeverityLevel;
import org.globaltester.base.UserInteraction;
import org.globaltester.base.util.StringUtil;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.platform.ExecutionRequirementsException;
import org.globaltester.scriptrunner.AssertionFailure;
import org.globaltester.scriptrunner.AssertionWarning;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.testrunner.testframework.Result.Status;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrappedException;

import de.cardcontact.scdp.gp.GPError;

public class ActionStepExecutor {

	private GtRuntimeRequirements runtimeReqs;
	private boolean ignoreExecutionRequirements;
	
	public ActionStepExecutor(GtRuntimeRequirements provider, boolean ignoreExecutionRequirements) {
		this.runtimeReqs = provider;
		this.ignoreExecutionRequirements = ignoreExecutionRequirements;
	}

	public Result execute(String code, String sourceName) {
		ScriptRunner scriptRunner = runtimeReqs.get(ScriptRunner.class);
		if (scriptRunner == null) {
			return new Result(Status.UNDEFINED, "No script runner available");
		}
		
		//unindent code
		code = StringUtil.formatCode(code);
		
		//get and execute the code
		TestLogger.trace("Code:\n" + code);
		try {
			scriptRunner.exec(code, sourceName, -1);
		} catch (EvaluatorException ex) {
			// this exception is thrown e. g. by asserts
			
			Throwable unwrappedEx = ex;
			if (ex instanceof WrappedException) {
				unwrappedEx = ex.getCause();
				if (unwrappedEx instanceof ExecutionRequirementsException) {
					if (!ignoreExecutionRequirements) {
						if (runtimeReqs.containsKey(UserInteraction.class)) {
							runtimeReqs.get(UserInteraction.class).notify(SeverityLevel.ERROR, ((ExecutionRequirementsException) unwrappedEx).getUserMessage());
						}
						return ResultFactory.newFailure(Status.REQUIREMENT_MISSING,
								0, TestLogger.getLogFileLine(), unwrappedEx.toString());	
					}
				}
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
					int rating = (Integer) jseo.get("reason", jseo);
					int scriptLine = jse.lineNumber();
					String expectedValue = (String) jseo.get("expectedValue", jseo);
					String receivedValue = (String) jseo.get("receivedValue", jseo);
					return ResultFactory.newFailure(rating, scriptLine, TestLogger.getLogFileLine(), msg, expectedValue, receivedValue);
				} else if (jseo instanceof GPError) {
					GPError gpe = (GPError) jseo;
					String msg = (String) gpe.get("message", gpe);
					int scriptLine = jse.lineNumber();
					return ResultFactory.newFailure(Status.FAILURE, scriptLine, TestLogger.getLogFileLine(), msg);
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
