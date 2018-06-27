package org.globaltester.testrunner.ui.editor;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.base.ui.DialogOptions;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.sampleconfiguration.ui.SampleConfigEditorWidget;
import org.globaltester.testrunner.report.ReportCsvGenerator;
import org.globaltester.testrunner.report.ReportJunitGenerator;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
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
	private TestExecutionResultViewer resultViewer;
	
	private boolean dirty = false;
	private Text txtSpecName;
	private Text txtSpecVersion;

	private Button btnOldest;
	private Button btnStepBack;
	private Combo cmbExecutionSelector;
	private Button btnStepForward;
	private Button btnNewest;
	
	
	private String baseDirName;
	private boolean writeReport;

	@Override
	public void doSave(IProgressMonitor monitor) {
		//IMPL handle progress in monitor
		
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
		GridData gdSampleConfigComp = new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1);
		gdSampleConfigComp.heightHint = 230;
		gdSampleConfigComp.widthHint = 120;
		sampleConfigComp.setLayoutData(gdSampleConfigComp);
		sampleConfigComp.setLayout(new GridLayout(1, false));
		sampleConfigViewer = new SampleConfigEditorWidget(sampleConfigComp);
		sampleConfigViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		sampleConfigViewer.setActive(false);
		
		Composite execStateTreeComp = new Composite(grpExecutionresults, SWT.NONE);
		execStateTreeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		
		//show the ExecutionResults of the currently selected TestCampaignExecution
		resultViewer = new TestExecutionResultViewer(execStateTreeComp, this);
		resultViewer.setInput(input.getCurrentTestCampaignExecution());
		resultViewer.expandAll();
		
		//Allow scrolling in the scrolled composite when an entry in the Tree is selected
		resultViewer.addListener(SWT.MouseWheel, new Listener() {
			@Override
			public void handleEvent(Event event) {
				scrolledComposite.setFocus();
			}
		});


		

		// below a little button area to report generation and maybe later other
		// tasks
		Composite buttonAreaComp = new Composite(grpExecutionresults, SWT.NONE);
		buttonAreaComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false, 1, 1));
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.setImage(UiImages.RESULT_ICON.getImage());
		
		TestCampaignEditor editor = this;
		
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// check dirty flag
				if(isDirty()) {
					int returnCode = openSaveDialog();
					if(returnCode == SWT.YES) {
						editor.doSave(new NullProgressMonitor());
					}
				}
				
				//check whether an actual execution exists and abort if not
				final TestCampaignExecution currentExecution = input.getCurrentlyDisplayedTestCampaignExecution();
				if (currentExecution == null) {
					PlatformUI.getWorkbench().getDisplay()
					.syncExec(new Runnable() {

						@Override
						public void run() {
							String message = "No current TestCampaignExecution selected. Please select a valid TestCampaignExecution and try again.";
							MessageDialog.openError(null,
											"Error",
											message);
						}
					});
					return;
				}
				
				// ask for report location
				DialogOptions dialogOptions = new DialogOptions();
				dialogOptions.setMessage("Please select location to store the report files");
				dialogOptions.setFilterPath(null); // do not filter at all
				baseDirName = GtUiHelper.openDirectoryDialog(getSite().getShell(), dialogOptions);
				
				if (baseDirName != null){
					writeReport = true;
					// check if file exists
					File baseDir = new File(baseDirName);
					if (baseDir.list().length > 0) {

						PlatformUI.getWorkbench().getDisplay()
								.syncExec(new Runnable() {

									@Override
									public void run() {
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

								monitor.beginTask("Export report", 5);

								monitor.subTask("Prepare reports");

								// create report
								TestReport report = new TestReport(
										currentExecution,
										baseDirName);

								monitor.worked(1);

								try {
									monitor.subTask("Create CSV report");
									ReportCsvGenerator.writeCsvReport(report);
									monitor.worked(1);
									monitor.subTask("Create PDF report");
									ReportPdfGenerator.writePdfReport(report);
									monitor.worked(1);
									monitor.subTask("Create JUnit report");
									ReportJunitGenerator.writeJUnitReport(report);
									monitor.worked(1);
									GtResourceHelper.copyFilesToDir(report.getLogFiles(), report.getReportDir().getAbsolutePath());
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


	@Override
	public void setFocus() {
		txtSpecName.setFocus();
	}

	void setDirty(boolean isDirty) {
		this.dirty = isDirty;
		input.setDirty(isDirty);
		firePropertyChange(IEditorPart.PROP_DIRTY);
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
					resultViewer.setInput(toDisplay);
					resultViewer.expandAll();
					if (toDisplay.getTestSetExecution() != null) {
					sampleConfigViewer.setInput(toDisplay.getTestSetExecution().getSampleConfig());
					}					
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
	
	private int openSaveDialog() {
		MessageBox mb = new MessageBox(PlatformUI.getWorkbench().getModalDialogShellProvider().getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		mb.setText("Save " + this.getTitle());
		mb.setMessage("'"+this.getEditorInput().getName()+"' has been modified.\nThe report will only contain changes since the last save.\n\nSave changes?");
		return mb.open();
	}

	@Override
	public void dispose() {
		if (input != null){
			input.disconnect();
		}
		super.dispose();
		
	}
}
