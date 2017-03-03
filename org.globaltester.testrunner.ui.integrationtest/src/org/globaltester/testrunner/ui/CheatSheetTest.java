package org.globaltester.testrunner.ui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.globaltester.junit.JUnitHelper;
import org.globaltester.swtbot.Strings;
import org.globaltester.swtbot.uihelper.SampleConfigWizardUiHelper;
import org.globaltester.swtbot.uihelper.GlobalTesterUiHelper;
import org.globaltester.swtbot.uihelper.LogFileEditorUiHelper;
import org.globaltester.swtbot.uihelper.NavigatorViewUiHelper;
import org.globaltester.swtbot.uihelper.TestCampaignEditorUiHelper;
import org.globaltester.swtbot.uihelper.TestSpecificationImportWizardUiHelper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
/**
 * Test the workflows that are given as eclipse cheat sheet.
 * 
 * @author mboonk
 *
 */
public class CheatSheetTest {
	private String sampleProject = "GlobalTester Sample TestSpecification";
	
	@Before
	public void prepare() throws CoreException{
		GlobalTesterUiHelper.init();
	}
	
	private void importTestSpecification(){
		TestSpecificationImportWizardUiHelper importWizard = GlobalTesterUiHelper.openImportWizardByMenu().openTestSpecificationImportWizard();
		importWizard.selectProject(sampleProject);
		importWizard.setProjectName(sampleProject);
		importWizard.finish();
	}
	
	private void createSampleConfig(){
		SampleConfigWizardUiHelper sampleConfig = GlobalTesterUiHelper.openNewWizardByMenu().selectSampleConfiguration();
		sampleConfig.setProjectName("SampleConfigProject");
		sampleConfig.setDescription("TestDescription");
		sampleConfig.setPin("1234");
		sampleConfig.setMrz("P<D<<MUSTERMANN<<ERIKA<<<<<<<<<<<<<<<<<<<<<<", "C11T002JM4D<<9608122F2310314<<<<<<<<<<<<<<<4", "");
		sampleConfig.finish();
	}
	
	@Test
	@Ignore
	public void executeTestCases() throws IOException, InterruptedException{
		importTestSpecification();
		createSampleConfig();
		NavigatorViewUiHelper navigator = GlobalTesterUiHelper.focusNavigatorView();
		String [] path1 = new String [] {sampleProject, "TestCases", "ePassport", "ePassport_Application.gt"};
		String [] path2 = new String [] {sampleProject, "TestCases", "nPA", "nPA_Application.gt"};
		navigator.expandAndSelect(path1);
		navigator.expandAndSelect(path2);
		GlobalTesterUiHelper.createAndStartTestCampaignByToolBar();
		SWTBotShell executionDialog = GlobalTesterUiHelper.getBot().shell(Strings.DIALOG_TITLE_TEST_EXECUTION);
		assertNotNull("Execution progress dialog did not open", executionDialog);
		GlobalTesterUiHelper.getBot().waitUntil(Conditions.shellCloses(executionDialog));
		TestCampaignEditorUiHelper editor = GlobalTesterUiHelper.focusTestCampaignEditor();
		File temp = JUnitHelper.createTemporaryFolder();
		editor.generateReport(temp);
		assertTrue("Report folder should contain report pdf", temp.list(new FilenameFilter() {	
			@Override
			public boolean accept(File dir, String name) {
				if (name.contains("TestCampaign") && name.endsWith(".pdf")){
					return true;
				}
				return false;
			}
		}).length == 1);
		LogFileEditorUiHelper logFile = editor.openTestCaseLogFile(0);
		assertTrue("log file editor should be active", logFile.isActive());
	}
}
