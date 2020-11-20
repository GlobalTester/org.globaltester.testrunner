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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.base.ui.UserInteractionImpl;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.RunTests;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.scriptrunner.TestExecutor;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
import org.globaltester.testrunner.testframework.Result.Status;
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
	private Action actionFilterPassed;
	private Action actionExpandNonPassed;
	private Action actionExpandAll;
	private Action actionCollapseAll;
	private Action actionGenerateReport;
	private Action actionRestart;
	private Action actionAutoExpandNonPassed;

	private Action actionAutoScroll;

	private IAction actionFilterUndefined;

	private Action actionStop;

	
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
		manager.add(actionAutoScroll);
		manager.add(actionAutoExpandNonPassed);
		IMenuManager filter = new MenuManager("Filter");
		filter.add(actionFilterPassed);
		filter.add(actionFilterUndefined);
		manager.add(filter);
		IMenuManager expand = new MenuManager("Expand");
		expand.add(actionExpandNonPassed);
		expand.add(actionExpandAll);
		expand.add(actionCollapseAll);
		manager.add(expand);
		
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionCollapseAll);
		manager.add(new Separator());
		manager.add(actionClear);
		manager.add(actionGenerateReport);
		manager.add(new Separator());
		manager.add(actionRestart);
		manager.add(actionStop);
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
		createActionFilterPassed();
		createActionExpandNonPassed();
		createActionAutoExpandNonPassed();
		createActionCollapseAll();
		createActionExpandAll();
		createActionAutoScroll();
		createActionStop();
		createActionFilterUndefined();
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

	private void createActionFilterPassed() {
		Status status = Status.PASSED;
		ViewerFilter filter = createFilter(status);
		actionFilterPassed = new Action("Hide " + status.getTextualRepresentation(), Action.AS_CHECK_BOX) {
			
			@Override
			public void run() {
				if (actionFilterPassed.isChecked()) {
					viewer.addFilter(filter);
				} else {
					viewer.removeFilter(filter);
				}
			}
		};
		actionFilterPassed.setChecked(Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_PASSED, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_PASSED_DEFAULT)));
		actionFilterPassed.setToolTipText("Hide all " + status.getTextualRepresentation() + " elements");
		actionFilterPassed.run();
	}

	private void createActionFilterUndefined() {
		Status status = Status.UNDEFINED;
		ViewerFilter filter = createFilter(status);
		actionFilterUndefined = new Action("Hide " + status.getTextualRepresentation(), Action.AS_CHECK_BOX) {
			
			@Override
			public void run() {
				if (actionFilterUndefined.isChecked()) {
					viewer.addFilter(filter);
				} else {
					viewer.removeFilter(filter);
				}
			}
		};
		actionFilterUndefined.setChecked(Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_UNDEFINED, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_UNDEFINED_DEFAULT)));
		actionFilterUndefined.setToolTipText("Hide all " + status.getTextualRepresentation() + " elements");
		actionFilterUndefined.run();
	}

	private ViewerFilter createFilter(Status status) {
		return new ViewerFilter() {

			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof TestCaseExecution) {
					return ((TestCaseExecution) element).getResult().getStatus() != status;
				}
				return true;
			}
			
		};
	}

	private void createActionAutoExpandNonPassed() {
		actionAutoExpandNonPassed = new Action("Automatically expand non-PASSED", Action.AS_CHECK_BOX) {
			
			@Override
			public void run() {
				viewer.setAutoExpandNonPassed(actionAutoExpandNonPassed.isChecked());
			}
		};
		actionAutoExpandNonPassed.setChecked(Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_EXPAND_NON_PASSED, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_EXPAND_NON_PASSED_DEFAULT)));
		actionAutoExpandNonPassed.setToolTipText("Automatically expand all non-PASSED elements");
		actionAutoExpandNonPassed.run();
	}

	private void createActionAutoScroll() {
		actionAutoScroll = new Action("Automatically scroll", Action.AS_CHECK_BOX) {
			
			@Override
			public void run() {
				viewer.setAutoExpandNonPassed(actionAutoScroll.isChecked());
			}
		};
		actionAutoScroll.setChecked(Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_SCROLL, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_SCROLL_DEFAULT)));
		actionAutoScroll.setToolTipText("Automatically scroll");
		actionAutoScroll.run();
	}

	private void createActionExpandNonPassed() {
		actionExpandNonPassed = new Action("Expand non-PASSED") {
			
			@Override
			public void run() {
				viewer.expandNonPassed();
			}
		};
		actionExpandNonPassed.setToolTipText("Expand all non-PASSED elements");
	}

	private void createActionExpandAll() {
		actionExpandAll = new Action("Expand all elements") {
			
			@Override
			public void run() {
				viewer.expandAll();
			}
		};
		actionExpandAll.setToolTipText("Expand all elements");
	}

	private void createActionCollapseAll() {
		actionCollapseAll = new Action("Collapse all elements") {
			
			@Override
			public void run() {
				viewer.collapseAll();
			}
		};
		actionCollapseAll.setToolTipText("Collapse all elements");
		actionCollapseAll.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL));
	}

	private void createActionStop() {
		actionStop = new Action("Stop all currently running tests") {
			
			@Override
			public void run() {
				Job.getJobManager().cancel(TestExecutor.FAMILY);
			}
		};
		actionStop.setToolTipText("Stop all currently running tests");
		actionStop.setImageDescriptor(
				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP));
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
