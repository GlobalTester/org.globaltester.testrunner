package org.globaltester.testrunner.ui.editor;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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
import org.globaltester.base.ui.DialogOptions;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GTLogger;
import org.globaltester.logging.ui.editors.LogfileEditor;
import org.globaltester.sampleconfiguration.ui.SampleConfigEditorWidget;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.ActionStepExecution;
import org.globaltester.testrunner.testframework.FileTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.ui.Activator;
import org.globaltester.testrunner.ui.UiImages;

public class TestCampaignEditor extends EditorPart implements SelectionListener, IResourceChangeListener {
	public TestCampaignEditor() {
	}
	
	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;
	private ScrolledComposite scrolledComposite;
	private Composite scrolledContent;
	
	private SampleConfigEditorWidget sampleConfigViewer;
	private Tree executionStateTree;
	private TreeViewer treeViewer;
	private boolean dirty = false;
	private Text txtSpecName;
	private Text txtSpecVersion;

	// some actions defined for this view
	private Action actionShowSpec;
	private Action actionShowLog;
	private Action doubleClickAction;
	private Button btnOldest;
	private Button btnStepBack;
	private Combo cmbExecutionSelector;
	private Button btnStepForward;
	private Button btnNewest;
	
	
	private String baseDirName;
	private boolean writeReport;

	@Override
	public void doSave(IProgressMonitor monitor) {
		//TODO handle progress in monitor
		
		//save selectedSampleConfiguration
		sampleConfigViewer.doSave();
		
		//flush all changed values to the input
		input.getTestCampaign().setSpecName(txtSpecName.getText());
		input.getTestCampaign().setSpecVersion(txtSpecVersion.getText());
		
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
		String exceptionReason = "";
		if ((input instanceof FileEditorInput)) {
			try {
				IFile file = ((FileEditorInput) input).getFile();
				if ((file == null) | (!file.exists())) {
					exceptionReason = "File does not exist";
				}
				input = new TestCampaignEditorInput(file);
			} catch (CoreException e) {
				input = null;
				exceptionReason = "Selected resource does not represent a TestCampaign.";
				String eMsg = e.getMessage();
				if (eMsg != null && eMsg.trim().length() > 0){
					exceptionReason += "( Caused by CoreException:" + e.getMessage() + ")";
				}
				throw new PartInitException(
				"Wrong Input - No TestCampaignEditorInput can be created from selected resource");
			}
		}
		if (!(input instanceof TestCampaignEditorInput)) {
			throw new PartInitException("Wrong input - "+exceptionReason);
		}

		this.input = (TestCampaignEditorInput) input;
		this.input.connect();
		setSite(site);
		setInput(input);
		setDirty(false);
		setPartName(this.input.getName());
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// savAs is not allowed as the editor reflects state of the complete
		// project instead of only the file
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		if ((input == null)){
			//close the editor when the editor input was deleted
			PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					getSite().getPage().closeEditor(TestCampaignEditor.this, false); 
				}
			});
		}
		
		
		parent.setLayout(new FillLayout());
		
		scrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
	    scrolledComposite.setExpandVertical(true);
	    
		scrolledContent = new Composite(scrolledComposite, SWT.NONE);
		scrolledContent.setLayout(new GridLayout(1, false));
		scrolledComposite.setContent(scrolledContent);

		// some meta data on top of the editor
		Composite metaDataComp = new Composite(scrolledContent, SWT.NONE);
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		metaDataComp.setLayout(new GridLayout(2, false));

		Label lblSpecName = new Label(metaDataComp, SWT.NONE);
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
		lblSpecificationVersion.setText("Specification version:");

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
		
		Group grpExecutionresults = new Group(scrolledContent, SWT.NONE);
		grpExecutionresults.setLayout(new GridLayout(1, false));
		grpExecutionresults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		grpExecutionresults.setText("Execution results");
		
		// history
		Composite historyComp = new Composite(grpExecutionresults, SWT.NONE);
		historyComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		historyComp.setLayout(new GridLayout(5, false));
		btnOldest = new Button(historyComp, SWT.NONE);
		btnOldest.setText("|<<");
		btnStepBack = new Button(historyComp, SWT.NONE);
		btnStepBack.setText("<");
		cmbExecutionSelector = new Combo(historyComp, SWT.NONE);
		cmbExecutionSelector.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		cmbExecutionSelector.addSelectionListener(this);
		btnStepForward = new Button(historyComp, SWT.NONE);
		btnStepForward.setText(">");
		btnStepForward.addSelectionListener(this);
		
		btnStepForward.setEnabled(false);
		btnStepBack.addSelectionListener(this);
		btnNewest = new Button(historyComp, SWT.NONE);
		btnNewest.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		btnNewest.setText(">>|");
		btnNewest.addSelectionListener(this);		
		btnNewest.setEnabled(false);
		
		//selection and Editor for SampleConfiguration
		Composite sampleConfigComp = new Composite(grpExecutionresults, SWT.NONE);
		sampleConfigComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
		sampleConfigComp.setLayout(new GridLayout(1, false));
		sampleConfigViewer = new SampleConfigEditorWidget(sampleConfigComp);
		sampleConfigViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		sampleConfigViewer.setEditable(false);
		
		Composite execStateTreeComp = new Composite(grpExecutionresults, SWT.NONE);
		execStateTreeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		executionStateTree = new Tree(execStateTreeComp, SWT.BORDER
				| SWT.FULL_SELECTION | SWT.NO_SCROLL);
		executionStateTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		executionStateTree.setHeaderVisible(true);
		executionStateTree.addSelectionListener(this);
		treeViewer = new TreeViewer(executionStateTree);
		
		TreeColumn columnName = new TreeColumn(executionStateTree, SWT.LEFT);
		executionStateTree.setLinesVisible(true);
		columnName.setAlignment(SWT.LEFT);
		columnName.setText("Test case");
		TreeColumn columnLastExec = new TreeColumn(executionStateTree,
				SWT.RIGHT);
		columnLastExec.setAlignment(SWT.LEFT);
		columnLastExec.setText("Last executed");
		TreeColumn columnStatus = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnStatus.setAlignment(SWT.LEFT);
		columnStatus.setText("Status");
		TreeColumn columnComment = new TreeColumn(executionStateTree, SWT.RIGHT);
		columnComment.setAlignment(SWT.LEFT);
		columnComment.setText("Comment");
		
		//make comment column editable
		TreeViewerColumn viewerColumnComment = new TreeViewerColumn(treeViewer, columnComment);
		viewerColumnComment.setEditingSupport(new EditingSupport(treeViewer) {
			
			@Override
			protected void setValue(Object element, Object value) {
				if (element instanceof AbstractTestExecution && value instanceof String){
					Result result = ((AbstractTestExecution) element).getResult();
					if (!result.getComment().equals(value)){
						result.setComment((String)value);
						treeViewer.refresh();
					}
				}
			}
			
			@Override
			protected Object getValue(Object element) {
				if (element instanceof AbstractTestExecution)
					return ((AbstractTestExecution) element).getResult().getComment();
				return null;
			}
			
			@Override
			protected CellEditor getCellEditor(Object element) {
				if (element instanceof AbstractTestExecution){
					// set editor dirty if cell is going to be edited
					setDirty(true);
					return new TextCellEditor(treeViewer.getTree());
				}
				return null;
			}
			
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});
		
		//set column widths
		TreeColumnLayout execStateTreeLayout = new TreeColumnLayout();
		execStateTreeComp.setLayout( execStateTreeLayout );
		execStateTreeLayout.setColumnData( columnName, new ColumnWeightData( 50 ) );
		execStateTreeLayout.setColumnData( columnLastExec, new ColumnPixelData( 120 ) );
		execStateTreeLayout.setColumnData( columnStatus, new ColumnPixelData( 100 ) );
		execStateTreeLayout.setColumnData( columnComment, new ColumnWeightData( 100 ) );

		treeViewer.setContentProvider(new TestCampaignContentProvider());
		treeViewer.setLabelProvider(new TestCampaignTableLabelProvider());
		treeViewer.setInput(input.getCurrentTestCampaignExecution());
		treeViewer.expandAll();

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();

		// below a little button area to report generation and maybe later other
		// tasks
		Composite buttonAreaComp = new Composite(grpExecutionresults, SWT.NONE);
		buttonAreaComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false, 1, 1));
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.setImage(UiImages.RESULT_ICON.getImage());
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// ask for report location
				DialogOptions dialogOptions = new DialogOptions();
				dialogOptions.setMessage("Please select location to store the report files");
				dialogOptions.setFilterPath(null); // do not filter at all
				baseDirName = GtUiHelper.openDirectoryDialog(getShell(), dialogOptions);
				
				if (baseDirName != null){
					writeReport = true;
					// check if file exists
					File baseDir = new File(baseDirName);
					if (baseDir.list().length > 0) {

						PlatformUI.getWorkbench().getDisplay()
								.syncExec(new Runnable() {

									@Override
									public void run() {
										// TODO Auto-generated method
										// stub
										String message = "The selected destination folder is not empty, proceed?";
										writeReport = MessageDialog
												.openConfirm(null,
														"Warning",
														message);
									}
								});
					}
					if (writeReport){
						Job job = new Job("PDF export") {
							
							@Override
							public IStatus run(IProgressMonitor monitor) {

								monitor.beginTask("Export PDF report", 2);

								monitor.subTask("Create report");

								// create report
								TestReport report = new TestReport(
										input.getCurrentlyDisplayedTestCampaignExecution(),
										baseDirName);

								monitor.worked(1);
								monitor.subTask("Create PDF");

								try {
									// TODO output XML-Report here, if no pdf is
									// desired

									// output pdf report
									ReportPdfGenerator.writePdfReport(report);
									monitor.worked(1);
									PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

												@Override
												public void run() {
													MessageDialog.openInformation(null, "PDF report", "Report exported successfully.");
												}
											});

								} catch (IOException ex) {
									IStatus status = new Status(Status.ERROR,
											Activator.PLUGIN_ID,
											"PDF report could not be created",
											ex);
									StatusManager.getManager().handle(status,
											StatusManager.SHOW);
								}

								// TODO copy relevant logfiles

								monitor.done();
								return new Status(IStatus.OK, Activator.PLUGIN_ID,
										"Export successfull.");
							}
						};
						job.setUser(true);
						job.schedule();
					}
					
				}
				

			}
		});

		updateEditor();

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
		manager.add(actionShowSpec);
		manager.add(actionShowLog);
		
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void openTestCase() {
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection)
		.getFirstElement();
		FileTestExecution fte = null;
		
		if (obj instanceof TestCampaign) {
			GtUiHelper.openErrorDialog(getShell(),
					"Open TestCase is not available for TestCampaigns");
		} else if (obj != null) {
			if (obj instanceof FileTestExecution) {
				fte = (FileTestExecution) obj;
			} else if (obj instanceof ActionStepExecution) {
				IExecution ie = ((ActionStepExecution) obj).getParent();
				if (ie instanceof FileTestExecution) {
					fte = (FileTestExecution) ie;
				}
			}
			if (fte != null) {
				IFile file = fte.getSpecFile();
				showFile(file, 0, null);
			}
		}
	}

	private void openLogFile() {
		ISelection selection = treeViewer.getSelection();
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof IExecution) {
			String logFileName = ((IExecution) obj).getLogFileName();
			int logFileLine = ((IExecution) obj).getLogFileLine();
			openFileOrShowErrorMessage(logFileName, logFileLine);
		} else {
			GtUiHelper.openErrorDialog(getShell(),
					"Selected element is not an IExecution");
		}
	}

	/**
	 * Define actions of this view
	 * 
	 */
	private void makeActions() {

		// show test case:
		actionShowSpec = new Action() {
			public void run(){
				openTestCase();
			}
		};

		actionShowSpec.setText("Show specification");
		actionShowSpec.setToolTipText("Show test case");
		actionShowSpec.setImageDescriptor(PlatformUI.getWorkbench()
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
				int customizedDoubleClick = Platform.getPreferencesService().getInt(org.globaltester.testrunner.Activator.PLUGIN_ID,
						org.globaltester.testrunner.preferences.PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 0, null);
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
	 * Show files from local workspace in and editor and select given line
	 * 
	 * @param file
	 *            the IFile to be opened
	 * @param line
	 *            line to be highlighted
	 * @param editorID
	 *            ID of Editor to use, if null the method tries to resolve the
	 *            editor based on content-type bindings as well as traditional
	 *            name/extension bindings.
	 */
	private void showFile(IFile file, int line, String editorID) {

		IEditorPart editor;
		ITextEditor textEditor = null;
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchWindow window = workbench.getWorkbenchWindows()[0];
		IWorkbenchPage page = window.getActivePage();

		try {
			if (file != null && file.exists()) {
				if (editorID != null) {
					editor = IDE.openEditor(page, file, editorID);
				} else {
					editor = IDE.openEditor(page, file, true);
				}
				textEditor = (ITextEditor) editor.getAdapter(ITextEditor.class);
			} else {
				GtUiHelper.openErrorDialog(getShell(),
						"File does not exist, thus can not be displayed.");
				return;
			}
		} catch (PartInitException ex) {
			GTLogger.getInstance().error(ex);
		}

		if ((line > 0) && (textEditor != null)) {
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
	 * Show files from local workspace in an editor and select given line
	 * 
	 * @param fileName				name of file to be opened
	 * @param line					line to be highlighted
	 */
	private void openFileOrShowErrorMessage(String fileName, int line) {

		if ((fileName == null) || (fileName == "")) {
			GtUiHelper.openErrorDialog(getShell(),
					"No file name given, thus file can not be displayed.");
			return;
		}
		
		IPath path = new Path(fileName);
		// file exists in local workspace
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
		.getFileForLocation(path);

		showFile(file, line, LogfileEditor.EDITOR_ID);
	}


	@Override
	public void setFocus() {
		txtSpecName.setFocus();
	}

	private void setDirty(boolean isDirty) {
		this.dirty = isDirty;
		input.setDirty(isDirty);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	private Shell getShell() {
		return treeViewer.getControl().getShell();
	}

	/**
	 * Updates the editor with the current displayed TestCampaignExecution using the display thread.
	 */
	private void updateEditor() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				//do nothing if widgets are already disposed
				if ((scrolledComposite == null)|| (scrolledComposite.isDisposed())){
					return;
				}
				
				//update inputs
				TestCampaignExecution toDisplay = input.getCurrentlyDisplayedTestCampaignExecution();
				if (toDisplay != null) {
					sampleConfigViewer.setInput(toDisplay.getSampleConfig());
					treeViewer.setInput(input);
					treeViewer.expandAll();
				}
				
				// set buttons according to displayed TestCampaignExecution
				btnOldest.setEnabled(input.isStepBackwardsPossible());
				btnStepBack.setEnabled(input.isStepBackwardsPossible());
				btnStepForward.setEnabled(input.isStepForwardsPossible());
				btnNewest.setEnabled(input.isStepForwardsPossible());
				
				//set input for Combo and select current displayed
				cmbExecutionSelector.setItems(input.getArrayOfTestCampaignExecutions());
				cmbExecutionSelector.select(input.getIndexOfCurrentlyDisplayedTestCampaignExecution());

				//recalculate size
				Point calculatedSize = scrolledContent.computeSize(scrolledComposite.getBounds().width, SWT.DEFAULT);
				scrolledComposite.setMinSize(calculatedSize);
				scrolledContent.pack();
				scrolledContent.layout();

			}
		});		
		
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == executionStateTree){
			boolean enableLogAction = false;
			boolean enableSpecAction = false;
			if ( e.item instanceof TreeItem) {
				TreeItem treeItem = (TreeItem) e.item;
				Object data = treeItem.getData();
				if (data instanceof AbstractTestExecution) {
					String logFileName = ((AbstractTestExecution)data).getLogFileName();
					enableLogAction = (logFileName != null) && (logFileName.trim().length() > 0);
					enableSpecAction = !(data instanceof TestCampaignExecution);
				}
			}
			actionShowLog.setEnabled(enableLogAction);
			actionShowSpec.setEnabled(enableSpecAction);
			return;
		}
		if (e.getSource() == btnNewest){
			input.stepToNewest();
		} else if (e.getSource() == btnOldest){
			input.stepToOldest();
		} else if (e.getSource() == btnStepBack){
			input.stepBackward();
		} else if (e.getSource() == btnStepForward){
			input.stepForward();
		} else if (e.getSource() == cmbExecutionSelector){
			input.stepToIndex(cmbExecutionSelector.getSelectionIndex());
		} 
		updateEditor();
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		if (rootDelta != null) {
			TestCampaignExecution execution = input
					.getCurrentTestCampaignExecution();
			if (execution != null) {
				// find delta for the current TestCampaignExecution
				IResourceDelta campaignExecutionDelta = rootDelta
						.findMember(input.getCurrentTestCampaignExecution()
								.getIFile().getFullPath());
				
				if (campaignExecutionDelta != null) {
					if (campaignExecutionDelta.getKind() == IResourceDelta.REMOVED) {
						//close the editor when the TestCampaign is deleted
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								getSite().getPage().closeEditor(TestCampaignEditor.this, false); 
							}
						});
					} else {
						// update the editor to reflect resource changes
						input.stepToNewest();
						updateEditor();
					}
				}
			}
		}
	}

	@Override
	public void dispose() {
		if (input != null){
			input.disconnect();
		}
		super.dispose();
		
	}
}
