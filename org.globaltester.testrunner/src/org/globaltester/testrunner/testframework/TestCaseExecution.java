package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringJoiner;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.UserInteraction;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.TestLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.sampleconfiguration.SampleConfig;
import org.globaltester.sampleconfiguration.profiles.ProfileMapper;
import org.globaltester.sampleconfiguration.profiles.expressions.AndProfileExpression;
import org.globaltester.sampleconfiguration.profiles.expressions.ProfileEvaluationException;
import org.globaltester.sampleconfiguration.profiles.expressions.ProfileExpression;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.ScriptRunner;
import org.globaltester.scriptrunner.ScshScope;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.globaltester.testspecification.testframework.ParameterGenerationFailedException;
import org.globaltester.testspecification.testframework.ParameterGenerator;
import org.globaltester.testspecification.testframework.PostCondition;
import org.globaltester.testspecification.testframework.PreCondition;
import org.globaltester.testspecification.testframework.TestCase;
import org.globaltester.testspecification.testframework.TestCaseParameter;
import org.globaltester.testspecification.testframework.TestStep;

public class TestCaseExecution extends FileTestExecution {

	public static final String XML_ELEMENT = "TestCaseExecution";
	private TestCaseParameter testCaseParameter = null;

	protected TestCaseExecution(IFile iFile) throws CoreException {
		super(iFile);
	}

	protected TestCaseExecution(IFile iFile, TestCase testCase)
			throws CoreException {
		super(iFile);
		
		specFile = testCase.getIFile();
		
		initFromTestCase(testCase);
		
		//store this configuration
		doSave();
	}

	private TestCaseExecution(IFile iFile, TestCase testCase, TestCaseParameter param)
				throws CoreException {
		this(iFile, testCase);
		testCaseParameter = param;

		setId(testCase.getName() + " " + param.getIdSuffix());

		//store this configuration
		doSave();
	}

	/**
	 * create all required execution instances from test case. E.g. TestStepExecutions
	 */
	private void initFromTestCase(TestCase testCase) {
		setId(testCase.getName());
		setDescription(testCase.getTestCasePurpose());
	}

	private void createChildrenFromActionSteps(TestCase testCase) {
		int childIndex = 0;
		for (ITestExecutable curChild : testCase.getChildren()) {

			if (curChild instanceof PreCondition) {
				addChildExecution(new PreConditionExecution(this, childIndex++));
			} else if (curChild instanceof TestStep) {
				addChildExecution(new TestStepExecution(this, childIndex++));
			} else if (curChild instanceof PostCondition) {
				addChildExecution(new PostConditionExecution(this, childIndex++));
			} else {
				BasicLogger.log(getClass(), "Found unknown child in TestCase, ignored.");
			}
		}
	}

	private void createChildrenFromParameters(TestCase testCase, GtRuntimeRequirements runtimeReqs) throws ParameterGenerationFailedException {
		ParameterGenerator generator = testCase.getParameterGenerator();
		
		for (TestCaseParameter curParameter : generator.generateParameters(runtimeReqs.get(SampleConfig.class))) {
			try {
				addChildExecution(new TestCaseExecution(getNewStateFile(), testCase, curParameter));
			} catch (CoreException e) {
				throw new ParameterGenerationFailedException(e);
			}
		}
		if (childExecutions.isEmpty()) {
			result.status = Status.NOT_APPLICABLE;
			result.comment = "Parameter generation resulted in no applicable test case instances";
			TestLogger.warn(result.comment);
		} 
	}

	private IFile getNewStateFile() throws CoreException {
		if (iFile != null) {
			GtTestCampaignProject campaignProject = getGtTestCampaignProject();
			if (campaignProject != null) return campaignProject.getNewStateIFile(getExecutable());
		}
		
		return null;
	}

	@Override
	protected void execute(GtRuntimeRequirements runtimeReqs, boolean forceExecution, boolean reExecution, IProgressMonitor monitor) {
		if (monitor == null){
			monitor = new NullProgressMonitor();
		}
		ScriptRunner sr = null;
		try {
			
		
		TestCase testCase = (TestCase) getExecutable();
		if (!testCase.isParameterized() ) {
			testCaseParameter = TestCaseParameter.UNPARAMETERIZED;
		}
		
		monitor.beginTask("Execute TestCase "+getId() , getChildren().size());
		
		//make sure that failures are counted for each test case separately
		ResultFactory.reset();
		
		// IMPL use variable forceExecution
		
		// dump execution information to logfile
		TestLogger.initTestCase(getId());
		// Put info of new TestExecutable into log file(s)
		TestLogger.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		TestLogger.info("Starting new test executable" + getId());
		
		//set the log file
		setLogFileName(TestLogger.getTestCaseLogFileName());
		setLogFileLine(TestLogger.getLogFileLine());
		
		testCase.dumpTestCaseInfos();
		
		// check provider capabilities
		if (!runtimeReqs.containsKey(SampleConfig.class)) {
			result.status = Status.NOT_APPLICABLE;
			result.comment = "Runtime requirements not fulfilled.";
			TestLogger.info(result.comment);
			return;
		}
		
		SampleConfig config = runtimeReqs.get(SampleConfig.class);
		TestLogger.trace(config.dump());
		
		// check if test case is applicable
		TestLogger.info("Check test case profiles");
		


		ProfileExpression profileExpression = testCase.getProfileExpression();
		if (testCaseParameter != null) {
			Object profileParam = testCaseParameter.get("profile");
			if (profileParam != null ) {
				if (profileParam instanceof String) {
					profileExpression = new AndProfileExpression(profileExpression, testCase.getProfileExpression((String) profileParam));
				} else if (profileParam instanceof ArrayList<?>) {
					ProfileExpression [] params = new ProfileExpression [((ArrayList<?>) profileParam).size() + 1];
					params[0] = new AndProfileExpression(profileExpression);
					for (int i=0; i<params.length-1; i++){
						ProfileExpression tmpExpr = ProfileMapper.parse((String) (((ArrayList<?>) profileParam).get(i)), testCase.getPropertyFiles());
						params[i+1] = new AndProfileExpression(tmpExpr);
					}
					profileExpression = new AndProfileExpression(params);
				} else {
					result.status = Status.FAILURE;
					result.comment = "Profile expression from TestCaseParameter not parsable";
					TestLogger.info(result.comment);
					this.putAdditionalInfo("Profile details", "Profile not parseable");
					return;
				}
			}
		}

		boolean askUser = Boolean.parseBoolean(PreferenceHelper.getPreferenceValue("org.globaltester.testrunner",
			PreferenceConstants.P_ASK_USER_FOR_GENERATED_TESTS, "false"));



		
		try {
			String paramProfiles = "";
			
			if (testCaseParameter != null) {
				Object profileParam = testCaseParameter.get("profile");
				if (profileParam != null ) {
					if (profileParam instanceof String) {
						paramProfiles += " and \"" + testCaseParameter.get("profile") + "\"";
					} else if (profileParam instanceof ArrayList<?>) {
						ArrayList<?> params = (ArrayList<?>) profileParam;
						StringJoiner joiner = new StringJoiner("\" and \"");
						for (Object c : params){
							joiner.add(c.toString());
						}
						paramProfiles += " and \"" + joiner.toString() + "\"";
					}
				}
			}
			
			String comment = "";
			
			if (!profileExpression.evaluate(runtimeReqs.get(SampleConfig.class))){
				result.status = Status.NOT_APPLICABLE;				
				comment = "Profiles \"" + testCase.getProfileString() + "\"";

				comment += paramProfiles;
				
				comment += " not fulfilled. Checked expression was \'" + profileExpression + "\'";
				result.comment = comment;
				TestLogger.info("Test case not applicable - " + this.getId() + " - " + comment);
				this.putAdditionalInfo("Profile details", comment.replaceAll("\"", "").replaceAll("'", ""));
				return;
			} else {
				comment = "Profiles \"" + testCase.getProfileString() + "\"";
				
				comment += paramProfiles;
				
				comment += " fulfilled. Checked expression was \'" + profileExpression + "\'";
				TestLogger.debug("Test case applicable - " + this.getId() + " - " + comment);
				this.putAdditionalInfo("Profile details", comment.replaceAll("\"", "").replaceAll("'", ""));
			}
			
			
		} catch (ProfileEvaluationException e) {
			BasicLogger.logException(e.getMessage(), e, LogLevel.WARN);
			ResultFactory.newFailure(Status.FAILURE, 0, TestLogger.getLogFileLine(), e.getMessage());
			result.status = Status.FAILURE;
			result.comment = e.getMessage();
			
			return;
		}
		
		if (testCaseParameter != null) {
			runtimeReqs.put(TestCaseParameter.class, testCaseParameter);
			createChildrenFromActionSteps(testCase);
		} else {
			try {
				createChildrenFromParameters(testCase, runtimeReqs);
			} catch (RuntimeException | ParameterGenerationFailedException e) {
				BasicLogger.logException(getClass(), "Unable to generate parametrized TestcaseExecutions", e);
				result.status = Status.FAILURE;
				result.comment = ParameterGenerationFailedException.DEFAULT_MSG;
				TestLogger.error(ParameterGenerationFailedException.DEFAULT_MSG);
				return;
			}

		}
		
		if (!childExecutions.stream().anyMatch(a -> TestCaseExecution.class.isInstance(a)) && askUser){
			UserInteraction interaction = runtimeReqs.get(UserInteraction.class);
			switch (interaction.select("Test id: " + this.getId() + "\n\nExecute test case?", null, "Ok", "Skip")) {
				case 0:
					TestLogger.debug("Test case applicable (user input).");
					break;
				case 1:
					result.status = Status.NOT_APPLICABLE;
					result.comment = "User selected skip";
					TestLogger.info("Test case not applicable (user input).");
					this.putAdditionalInfo("User input", "Test selected as not applicable by user.");
					return;
				default:
			}
		}
		
		IContainer scriptRoot = ResourcesPlugin.getWorkspace().getRoot();
		String workingDir = testCase.getIFile().getParent().getLocation().toOSString();
		sr = new ScriptRunner(scriptRoot, workingDir, runtimeReqs);
		sr.init(new ScshScope(sr));
		runtimeReqs.put(ScriptRunner.class, sr);
		

		runtimeReqs.put(TestCaseExecution.class, this);

		// iterate over all ActionSteps
		TestLogger.info("Running ActionSteps");
		for (Iterator<IExecution> childIter = getChildren().iterator(); childIter
				.hasNext() && !monitor.isCanceled();) {
			IExecution curStepExec = childIter.next();
			curStepExec.execute(runtimeReqs, forceExecution, new NullProgressMonitor());
			
			result.addSubResult(curStepExec.getResult());
			monitor.worked(1);
		}
		} finally {
			if (sr != null){
				sr.close();
			}
			TestLogger.info("End execution of " + getId() +", result: "+ result.getStatus());
			TestLogger.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< -");
			TestLogger.shutdownTestCase();
			monitor.done();
		}
		
	}

	@Override
	public String getXmlRootElementName() {
		return XML_ELEMENT;
	}

}
