package org.globaltester.testrunner.ui.editor;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.ITextEditor;
import org.globaltester.cardconfiguration.ui.CardConfigSelectionEditor;
import org.globaltester.cardconfiguration.ui.ICardSelectionListener;
import org.globaltester.logging.logger.GTLogger;
import org.globaltester.logging.preferences.PreferenceConstants;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
import org.globaltester.testrunner.testframework.FileTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.PostConditionExecution;
import org.globaltester.testrunner.testframework.PreConditionExecution;
import org.globaltester.testrunner.testframework.TestCampaignElement;
import org.globaltester.testrunner.testframework.TestStepExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.UiImages;

public class TestCampaignEditor extends EditorPart implements ICardSelectionListener {
	public TestCampaignEditor() {
	}

	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;
	private CardConfigSelectionEditor cardConfigManager;
	private TreeViewer treeViewer;
	private boolean dirty = false;
	private Text txtSpecName;
	private Text txtSpecVersion;

	// some actions defined for this view
	private Action actionShowTestCase;
	private Action actionShowLog;
	private Action doubleClickAction;
	

	@Override
	public void doSave(IProgressMonitor monitor) {
		//TODO handle progress in monitor
		
		//save selectedCardConfiguration
		cardConfigManager.doSave();
		
		//flush all changed values to the input
		input.getTestCampaign().setSpecName(txtSpecName.getText());
		input.getTestCampaign().setSpecVersion(txtSpecVersion.getText());
		input.getTestCampaign().setCardConfig(cardConfigManager.getSelectedConfig());
		
		try {
			input.getGtTestCampaignProject().doSave();
		} catch (CoreException e) {
			StatusManager.getManager().handle(e, Activator.PLUGIN_ID);
		}
		setDirty(false);
	}

	@Override
	public void doSaveAs() {
		// SaveAs is not allowed, see isSaveAsAllowed()
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
	throws PartInitException {
		if ((input instanceof FileEditorInput)) {
			try {
				input = new TestCampaignEditorInput(((FileEditorInput) input).getFile());
			} catch (CoreException e) {
				throw new RuntimeException(
				"Wrong Input - No TestCampaignEditorInput can be created from selected resource");
			}
		}
		if (!(input instanceof TestCampaignEditorInput)) {
			throw new RuntimeException("Wrong input");
		}

		this.input = (TestCampaignEditorInput) input;
		setSite(site);
		setInput(input);
		setDirty(false);

		setPartName(this.input.getName());
	}

	@Override
	public boolean isDirty() {
		return dirty || cardConfigManager.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		// savAs is not allowed as the editor reflects state of the complete
		// project instead of only the file
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		// some meta data on top of the editor
		Composite metaDataComp = new Composite(parent, SWT.NONE);
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		metaDataComp.setLayout(new GridLayout(2, false));

		Label lblSpecName = new Label(metaDataComp, SWT.NONE);
		lblSpecName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER,
				false, false, 1, 1));
		lblSpecName.setText("Specification name:");

		txtSpecName = new Text(metaDataComp, SWT.BORDER);
		txtSpecName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtSpecName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
			}
		});
		txtSpecName.setText(input.getTestCampaign().getSpecName());

		Label lblSpecificationVersion = new Label(metaDataComp, SWT.NONE);
		lblSpecificationVersion.setLayoutData(new GridData(SWT.RIGHT,
				SWT.CENTER, false, false, 1, 1));
		lblSpecificationVersion.setText("Specification Version:");

		txtSpecVersion = new Text(metaDataComp, SWT.BORDER);
		txtSpecVersion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1,
				1));
		txtSpecVersion.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setDirty(true);
			}
		});
		txtSpecVersion.setText(input.getTestCampaign().getSpecVersion());
		
		//selection and Editor for CardConfiguration
		Composite cardConfigComp = new Composite(parent, SWT.NONE);
		cardConfigComp.setLayout(new GridLayout(1, false));
		cardConfigComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 1, 1));
		cardConfigManager = new CardConfigSelectionEditor(cardConfigComp, this);
		

		// main part of the editor is occupied by tree view
		Composite treeViewerComp = new Composite(parent, SWT.NONE);
		treeViewerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		treeViewerComp.setLayout(new FillLayout(SWT.HORIZONTAL));
		Tree executionStateTree = new Tree(treeViewerComp, SWT.BORDER
				| SWT.H_SCROLL | SWT.V_SCROLL);
		executionStateTree.setHeaderVisible(true);
		treeViewer = new TreeViewer(executionStateTree);

		TreeColumn columnName = new TreeColumn(executionStateTree, SWT.LEFT);
		executionStateTree.setLinesVisible(true);
		columnName.setAlignment(SWT.LEFT);
		columnName.setText("Testcase/TestStep");
		columnName.setWidth(250);
		TreeColumn columnLastExec = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnLastExec.setAlignment(SWT.LEFT);
		columnLastExec.setText("LastExecuted");
		columnLastExec.setWidth(120);
		TreeColumn columnStatus = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnStatus.setAlignment(SWT.LEFT);
		columnStatus.setText("Status");
		//TODO make column show only Icon and put text in tooltip
		columnStatus.setWidth(120);
		TreeColumn columnComment = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnComment.setAlignment(SWT.LEFT);
		columnComment.setText("Comment");
		columnComment.setWidth(300);

		treeViewer.setContentProvider(new TestCampaignContentProvider());
		treeViewer.setLabelProvider(new TestCampaignTableLabelProvider());
		treeViewer.setInput(input.getGtTestCampaignProject());
		treeViewer.expandAll();
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();

		// below a little button area to control execution and report generation
		Composite buttonAreaComp = new Composite(parent, SWT.NONE);
		buttonAreaComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false, 1, 1));
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));

		Button btnExecute = new Button(buttonAreaComp, SWT.NONE);
		btnExecute.setText("Execute");
		btnExecute.setImage(UiImages.EXECUTE_ICON.getImage());
		btnExecute.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Job job = new Job("Test execution") {
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Execution started...", 10);
						// execute tests
						try {
							input.getTestCampaign().executeTests();
						} catch (CoreException e) {
							StatusManager.getManager().handle(e,
									Activator.PLUGIN_ID);
						}

						monitor.done();
						return Status.OK_STATUS;
					}
				};
				job.setUser(true);
				job.schedule();	

			}
		});

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.setImage(UiImages.RESULT_ICON.getImage());
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// ask for report location
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Please select location to store the report files");
				dialog.setFilterPath(null); // do not filter at all
				String baseDirName = dialog.open();

				if (baseDirName != null) {
					// create report
					TestReport report = new TestReport(input.getTestCampaign(),
							baseDirName);

					try {
						// TODO output XML-Report here, if no pdf is desired

						// output pdf report
						ReportPdfGenerator.writePdfReport(report);
					} catch (IOException ex) {
						IStatus status = new Status(Status.ERROR,
								Activator.PLUGIN_ID,
								"PDF report could not be created", ex);
						StatusManager.getManager().handle(status,
								StatusManager.SHOW);
					}

					// TODO copy relevant logfiles
				}
			}

		});

		//unset dirty flag as input is just loaded from file
		setDirty(false);

	}


	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TestCampaignEditor.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(actionShowTestCase);
		manager.add(actionShowLog);
		
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void openTestCase() {
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection)
		.getFirstElement();
		FileTestExecution fte = null;
		if (obj != null) {
			if(obj instanceof TestCampaignElement){
				fte = ((TestCampaignElement) obj).getLastExecution();
			}
			if((obj instanceof TestStepExecution) || (obj instanceof PreConditionExecution) || (obj instanceof PostConditionExecution)){
				IExecution ie = ((IExecution)obj).getParent();
				fte = (FileTestExecution) ie;
			}
			IFile file = fte.getSpecFile();
			showFile(file, 0);
		}
	}

	private void openLogFile(){
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection)
		.getFirstElement();
		if (obj != null) {
			String logFileName = ((IExecution) obj).getLogFileName();
			int logFileLine = 0;
				logFileLine = ((IExecution) obj).getLogFileLine();
			try{
				showFile(logFileName, logFileLine);
			}
			catch(Exception e){
				//TODO: open dialog that informs about non-existing logfile
			}
		}
	}

	/**
	 * Define actions of this view
	 * 
	 */
	private void makeActions() {

		// show test case:
		actionShowTestCase = new Action() {
			public void run(){
				openTestCase();
			}
		};

		actionShowTestCase.setText("Show test case");
		actionShowTestCase.setToolTipText("Show test case");
		actionShowTestCase.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_PASTE));

		// show log file:
		actionShowLog = new Action() {
			public void run() {
				openLogFile();
			}
		};

		actionShowLog.setText("Show log file");
		actionShowLog.setToolTipText("Show log file");
		actionShowLog.setImageDescriptor(PlatformUI.getWorkbench()
				.getSharedImages().getImageDescriptor(
						ISharedImages.IMG_TOOL_PASTE));

		// double click -> show test case
		doubleClickAction = new Action() {

			public void run(){
				int customizedDoubleClick = Platform.getPreferencesService().getInt(org.globaltester.logging.Activator.PLUGIN_ID,
						PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 0, null);
				if(customizedDoubleClick == 0) {
					openTestCase();
				}
				else{
					openLogFile();
				}
			}
		};
	}

	private void hookDoubleClickAction() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
				
			}
		});
	}


	/**
	 * Show files from local workspace in and editor and highlight special line
	 * 
	 * @param file			the IFile to be opened
	 * @param line			line to be highlighted
	 */
	private void showFile(IFile file, int line) {

		if (file == null)
			return;
		
		IEditorPart editor;
		ITextEditor textEditor = null;
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
		IWorkbenchPage page = window.getActivePage();

		try {
			if (file != null && file.exists()) {
				editor = IDE.openEditor(page, file, true);
				textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
			}
		} catch (Exception ex) {
			GTLogger.getInstance().error(ex);
		}
		
		if (line > 0) {
			try {
				line--; // document starts with 0
				IDocument document = textEditor.getDocumentProvider()
				.getDocument(textEditor.getEditorInput());

				textEditor.selectAndReveal(document.getLineOffset(line),
						document.getLineLength(line));

			} catch (BadLocationException e) {
				// invalid text position -> do nothing
			}
		}
	}

	/**
	 * Show files from local workspace in an editor and highlight special line
	 * 
	 * @param fileName				name of file to be opened
	 * @param line					line to be highlighted
	 */
	private void showFile(String fileName, int line) {

		if (fileName == null)
			return;
		
		IPath path = new Path(fileName);
		// file exists in local workspace
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
		.getFileForLocation(path);

		showFile(file, line);
	}


	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private Shell getShell() {
		return treeViewer.getControl().getShell();
	}

	@Override
	public void cardConfigSelectionChanged() {
		setDirty(true);
	}

	@Override
	public void selectedCardConfigDirty(boolean dirty) {
		if (dirty) {
			setDirty(true);
		}
	}
}
