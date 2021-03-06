package org.globaltester.testrunner.ui.views;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.base.ui.UserInteractionImpl;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.RunTests;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.editor.ReportGenerationJob;
import org.globaltester.testrunner.ui.editor.TestExecutionResultViewer;
import org.globaltester.testspecification.testframework.FileTestExecutable;
import org.globaltester.testspecification.testframework.ITestExecutable;
import org.osgi.framework.Bundle;

/**
 * This class shows the test results in a special view
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */

public class ResultView extends ViewPart {
	public static final String VIEW_ID = "org.globaltester.testrunner.ui.views.ResultView";

	// table viewer to show test results
	private TestExecutionResultViewer viewer;

	// some actions defined for this view
	private Action actionClear;
	private Action actionGenerateReport;
	private Action actionRestart;

	public final static int NO = 1;
	public final static int TESTSUITE = 2;
	public final static int TESTCASEID = 3;
	public final static int DESCR = 4;
	public final static int TIME = 5;
	public final static int RESULT = 6;

	public static final String TEST_CASE = "Test Case";
	public static final String LOG_FILE = "Log File";


	/**
	 * Set ArrayList as test result for table viewer
	 * 
	 * @param session
	 *            list with test results
	 */
	public void setInput(AbstractTestExecution newInput) {
		viewer.setInput(newInput);
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TestExecutionResultViewer(parent, this);

		makeActions();
		contributeToActionBars();
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(actionClear);
		manager.add(actionGenerateReport);
		manager.add(actionRestart);
		manager.add(new Separator());
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionClear);
		manager.add(actionGenerateReport);
		manager.add(actionRestart);
	}

	public void reset() {
		setInput(null);
		viewer.refresh();
	}

	/**
	 * Define actions of this view
	 * 
	 */
	private void makeActions() {
		createActionClear();
		createActionGenerateReport();
		createActionRestart();
	}

	private void createActionClear() {
		actionClear = new Action() {
			@Override
			public void run() {
				reset();
			}
		};
		actionClear.setText("Clear list");
		actionClear.setToolTipText("Clear list");
		actionClear.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
	}

	private void createActionGenerateReport() {
		actionGenerateReport = new Action() {
			@Override
			public void run() {

				Job job = new ReportGenerationJob(viewer.getInput(), getSite().getShell());
				job.schedule();

			}
		};
		actionGenerateReport.setText("Generate Report");
		actionGenerateReport.setToolTipText("Generate Report");

		setActionImageDescriptor(actionGenerateReport, "icons//gtGenerateReport.png");
	}

	private static void setActionImageDescriptor(Action action, String imagePath) {
		Bundle bundle = Platform.getBundle("org.globaltester.testrunner.ui"); 
		
		Path path = new Path(imagePath);
		URL fileURL = FileLocator.find(bundle, path, null);

		try {
			Image image = new Image(null, fileURL.openStream());
			action.setImageDescriptor(ImageDescriptor.createFromImage(image));
		} catch (IOException e) {
			GtErrorLogger.log(Activator.PLUGIN_ID, e);
		}

	}

	private void createActionRestart() {

		actionRestart = new Action() {
			@Override
			public void run() {
				restartTestCases();
			}
		};

		actionRestart.setText("Restart");
		actionRestart.setToolTipText("Restart");
		setActionImageDescriptor(actionRestart, "icons//execute.png");

	}

	protected void restartTestCases() {
		
		AbstractTestExecution previousExecution = viewer.getInput();
		List<IResource> resources = new LinkedList<>();
		try {
			resources.addAll(extractResources(previousExecution));
		} catch (CoreException e) {
			String msg = "Unable to restore resources from previous Execution";
			BasicLogger.logException(msg, e, LogLevel.WARN);
			GtUiHelper.openErrorDialog(getViewSite().getShell(), "Running failed: " + msg);
			return;
		}
		
		
		GtRuntimeRequirements runtimeReqs = new GtRuntimeRequirements(new UserInteractionImpl(), RunTests.getLastUsedSampleConfig());
		
		if (!new RunTests(runtimeReqs).execute(resources, TestExecutionCallback.NULL_CALLBACK)){
			GtUiHelper.openErrorDialog(getViewSite().getShell(), "Running failed: No valid execution engine found for your selection.");
		}
	}

	private List<IResource> extractResources(AbstractTestExecution previousExecution) throws CoreException {
		List<IResource> retVal = new LinkedList<>();
		if (previousExecution instanceof TestCampaignExecution) {
			retVal.add(((TestCampaignExecution) previousExecution).getGtTestCampaignProject().getTestCampaignIFile());
		} else if (previousExecution instanceof TestSetExecution) {
			for (IExecution curChild : previousExecution.getChildren()) {
				ITestExecutable curExecutable = curChild.getExecutable();
				if (curExecutable instanceof FileTestExecutable) {
					retVal.add(((FileTestExecutable) curExecutable).getIFile());
				}
			}
		}
		return retVal;
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.setFocus();
	}
}
