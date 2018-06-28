package org.globaltester.testrunner.ui.views;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.base.ui.UserInteractionImpl;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.legacy.logger.GTLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.scriptrunner.GtRuntimeRequirements;
import org.globaltester.scriptrunner.RunTests;
import org.globaltester.scriptrunner.TestExecutionCallback;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
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
	private Action actionShowTestCase;
	private Action actionShowLog;

	public final static int NO = 1;
	public final static int TESTSUITE = 2;
	public final static int TESTCASEID = 3;
	public final static int DESCR = 4;
	public final static int TIME = 5;
	public final static int RESULT = 6;

	public static final String TEST_CASE = "Test Case";
	public static final String LOG_FILE = "Log File";

	// Preference store of GT Plugin
	//FIXME PreferenceStore
//	IPreferenceStore store = Activator.getDefault().getPreferenceStore();


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
		//FIXME AAC should be provided by TestExecutionResultViewer
//		hookContextMenu();
//		hookDoubleClickAction();
		contributeToActionBars();
	}

//	private void hookContextMenu() {
//		MenuManager menuMgr = new MenuManager("#PopupMenu");
//		menuMgr.setRemoveAllWhenShown(true);
//		menuMgr.addMenuListener(ResultView.this::fillContextMenu);
//		Menu menu = menuMgr.createContextMenu(viewer.getControl());
//		viewer.getControl().setMenu(menu);
//		getSite().registerContextMenu(menuMgr, viewer);
//	}

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
		//FIXME AAC remove if possible
//		manager.add(actionShowTestCase);
//		manager.add(actionShowLog);

	}

//	private void fillContextMenu(IMenuManager manager) {
//		manager.add(actionClear);
//		manager.add(actionGenerateReport);
//		manager.add(actionRestart);
//		manager.add(actionShowTestCase);
//
//		IPreferenceStore logStore = org.globaltester.logging.legacy.Activator.getDefault().getPreferenceStore();
//		boolean plainLogging = logStore
//				.getBoolean(org.globaltester.logging.legacy.preferences.PreferenceConstants.P_TEST_PLAINLOGGING);
//		if (plainLogging) {
//			manager.add(actionShowLog);
//		}
//
//		// Other plug-ins can contribute there actions here
//		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
//	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(actionClear);
		manager.add(actionGenerateReport);
		manager.add(actionRestart);
	}

	/**
	 * Show files in editor
	 * 
	 * @param file
	 *            the file to show
	 * @return Editor the editor used to show the file
	 */
	private ITextEditor showFile(File file) {

		ITextEditor textEditor = null;

		if (file.exists() && file.isFile()) {
			IFileStore fileStore = EFS.getLocalFileSystem().getStore(file.toURI());
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			try {
				IEditorPart editor = IDE.openEditorOnFileStore(page, fileStore);
				textEditor = editor.getAdapter(ITextEditor.class);
			} catch (PartInitException e) {
				GTLogger.getInstance().error(e);
			}
		}

		return textEditor;

	}

	/**
	 * Show file in editor and highlight special line
	 * 
	 * @param file
	 *            the file to show
	 * @param line
	 *            line to be highlighted
	 */
	private void showFile(File file, int line) {

		if (file == null)
			return;

		ITextEditor textEditor = showFile(file);

		if (line > 0 && textEditor != null) {
			try {
				int curLine = line - 1; // document starts with 0
				IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

				textEditor.selectAndReveal(document.getLineOffset(curLine), document.getLineLength(curLine));

			} catch (BadLocationException e) {
				// invalid text position -> do nothing but log
				GtErrorLogger.log(Activator.PLUGIN_ID, e);
			}
		}
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
		//FIXME AAC remove if possible
//		createActionShowTestcase();
//		createActionShowLog();
//		createActionDoubleClick();
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
//		Bundle bundle = Activator.getDefault().getBundle();
		Bundle bundle = Platform.getBundle("org.globaltester.testmanager.ui"); 
		//FIXME AAB UI - move these required resources as well
		
//		Path path = new Path(imagePath);
//		URL fileURL = FileLocator.find(bundle, path, null);
//
//		try {
//			Image image = new Image(null, fileURL.openStream());
//			action.setImageDescriptor(ImageDescriptor.createFromImage(image));
//		} catch (IOException e) {
//			GtErrorLogger.log(Activator.PLUGIN_ID, e);
//		}

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
