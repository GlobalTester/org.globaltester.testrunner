package org.globaltester.testrunner.ui.views;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.globaltester.logging.legacy.logger.GTLogger;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.ui.editor.TestExecutionResultViewer;
import org.osgi.framework.Bundle;

/**
 * This class shows the test results in a special view
 * 
 * @version Release 2.2.0
 * @author Holger Funke
 * 
 */

public class ResultView extends ViewPart {
	//FIXME AAD remove this class if possible
	private static final String SHELL_NAME = "GlobalTester";

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
				//FIXME AAC adapt report generation code below
//				TestSession lastSession = TestSession.getLastTestSession();
//				if (lastSession == null || lastSession.getVirtualSuite() == null) {
//					MessageDialog.openWarning(getShell(), SHELL_NAME,
//							"No last test session available. Report cannot be generated.");
//					return;
//				}
//
//				boolean staticReportDir = store.getBoolean(PreferenceConstants.P_FIXEDDIRSETTINGS); //FIXME move this preference to the testrunner configuration
//
//				//FIXME AAD clean up reporting configuration and consolidate it to a single bundle
//				staticReportDir = false;
//				
//				File destinationDir = null;
//
//				if (!staticReportDir) {
//					// ask for report location
//					DialogOptions dialogOptions = new DialogOptions();
//					dialogOptions.setMessage("Please select location to store the report files");
//					dialogOptions.setFilterPath(null); // do not filter at all
//					String baseDirPath = GtUiHelper.openDirectoryDialog(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), dialogOptions);
//					destinationDir = new File(baseDirPath);
//
//					if ((destinationDir.list().length > 0) &&
//						 !MessageDialog.openQuestion(getShell(), SHELL_NAME, "The target directory is not empty, proceed?")) {
//						return;	
//					}
//				} else {
//					destinationDir = TestReport.getDefaultDestinationDir();
//				}
//				String reportBaseDir = destinationDir.getAbsolutePath();
//				
//				Job job = new Job("Export TestReport") {
//					
//					@Override
//					public IStatus run(IProgressMonitor monitor) {
//
//						monitor.beginTask("Export report", 5);
//
//						monitor.subTask("Prepare reports");
//
//						
//						// create report
//						TestReport report = new TestReport(
//								lastSession.getTestSetExecution(),
//								reportBaseDir);
//
//						monitor.worked(1);
//
//						try {
//							monitor.subTask("Create CSV report");
//							ReportCsvGenerator.writeCsvReport(report);
//							monitor.worked(1);
//							monitor.subTask("Create PDF report");
//							ReportPdfGenerator.writePdfReport(report);
//							monitor.worked(1);
//							monitor.subTask("Create JUnit report");
//							ReportJunitGenerator.writeJUnitReport(report);
//							monitor.worked(1);
//							GtResourceHelper.copyFilesToDir(report.getLogFiles(), report.getReportDir().getAbsolutePath());
//							monitor.worked(1);
//							PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
//
//										@Override
//										public void run() {
//											MessageDialog.openInformation(null, "PDF report", "Report exported successfully.");
//										}
//									});
//
//						} catch (IOException ex) {
//							IStatus status = new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.Status.ERROR,
//									Activator.PLUGIN_ID,
//									"PDF report could not be created",
//									ex);
//							StatusManager.getManager().handle(status,
//									StatusManager.SHOW);
//						}
//
//						monitor.done();
//						MessageDialog.openInformation(getShell(), SHELL_NAME, "The report has been generated");
//						return new org.eclipse.core.runtime.Status(IStatus.OK, Activator.PLUGIN_ID,
//								"Export successfull.");
//					}
//				};
//				job.setUser(true);
//				job.schedule();

			}
		};
		actionGenerateReport.setText("Generate Report");
		actionGenerateReport.setToolTipText("Generate Report");

		setActionImageDescriptor(actionGenerateReport, "icons//gtGenerateReport.png");
	}

	/**
	 * Return the shell of this workbench, mostly used to open dialogs.
	 * 
	 * @return
	 */
	private Shell getShell() {
		return getViewSite().getShell();
	}

	private static void setActionImageDescriptor(Action action, String imagePath) {
//		Bundle bundle = Activator.getDefault().getBundle();
		Bundle bundle = Platform.getBundle("org.globaltester.testmanager.ui"); 
		//FIXME AAB move these required resources as well
		
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
		//FIXME AAB implement restart of TestExecution
//		TestSession testSession = TestSession.getLastTestSession();
//		if (testSession != null) {
//			if (!TestLogger.isInitialized())
//				TestLogger.init();
//			ThreadGroup threadGroup = new ThreadGroup("TestManager test execution by result view thread group " + Calendar.getInstance().getTimeInMillis());
//			TestSession testSessionCopy = TestSession.getTestSessionCopy(testSession);
//			
//			new Thread(threadGroup, new TestManagerExecution(testSessionCopy)).start();
//		} else {
//			MessageDialog.openWarning(getShell(), SHELL_NAME, "No last test session available to restart.");
//		}
	}

	//FIXME AAC see if we can get away without this code
//	private void createActionShowTestcase() {
//		actionShowTestCase = new Action() {
//			@Override
//			public void run() {
//				ISelection selection = viewer.getSelection();
//				Object obj = ((IStructuredSelection) selection).getFirstElement();
//				if (obj != null) {
//					TestResult testResult = (TestResult) obj;
//					String fileName = testResult.getFileName();
//					File file = getAsFile(fileName);
//					showFile(file, 0);
//				}
//			}
//		};
//
//		actionShowTestCase.setText("Show test case");
//		actionShowTestCase.setToolTipText("Show test case");
//		actionShowTestCase.setImageDescriptor(
//				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
//	}
//
//	private void createActionShowLog() {
//		actionShowLog = new Action() {
//			@Override
//			public void run() {
//				ISelection selection = viewer.getSelection();
//				Object obj = ((IStructuredSelection) selection).getFirstElement();
//				if (obj != null) {
//					TestResult testResult = (TestResult) obj;
//					String fileName = testResult.getLogFileName();
//					File file = getAsFile(fileName);
//					showFile(file, 0);
//				}
//			}
//		};
//
//		actionShowLog.setText("Show log file");
//		actionShowLog.setToolTipText("Show log file");
//		actionShowLog.setImageDescriptor(
//				PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
//	}
//
//	/**
//	 * This method returns a File matching the provided file name. If the file
//	 * name is absolute and references an existing file the respective File
//	 * object will be returned. If the file does not exist null will be
//	 * returned. If the file name is relative to the workspace and references an
//	 * existing file the respective File object will be returned. If the file
//	 * does not exist ( e.g. a file outside the workspace) null will be
//	 * returned.
//	 * 
//	 * @param filename
//	 *            the name of the file to return
//	 * @return the file referenced by the filename or null
//	 */
//	private static File getAsFile(String filename) {
//		if (filename == null) {
//			return null;
//		}
//
//		File file = new File(filename);
//
//		if (file.isAbsolute()) {
//			if (!file.exists()) {
//				file = null;
//			}
//		} else {
//			IPath iPath = Path.fromOSString(filename);
//			IFile ifile = ResourcesPlugin.getWorkspace().getRoot().getFile(iPath);
//
//			if (ifile.exists()) {
//				URI uri = ifile.getLocationURI();
//
//				if (ifile.isLinked()) {
//					uri = ifile.getRawLocationURI();
//				}
//
//				try {
//					file = EFS.getStore(uri).toLocalFile(0, new NullProgressMonitor());
//				} catch (CoreException e) {
//					file = null;
//					GtErrorLogger.log(Activator.PLUGIN_ID, e);
//				}
//			} else {
//				file = null;
//			}
//
//		}
//
//		return file;
//	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		viewer.setFocus();
	}
}
