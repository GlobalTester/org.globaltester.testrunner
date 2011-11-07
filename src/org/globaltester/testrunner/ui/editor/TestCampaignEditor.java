package org.globaltester.testrunner.ui.editor;

import java.io.IOException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
import org.globaltester.testrunner.ui.Activator;

public class TestCampaignEditor extends EditorPart {
	public TestCampaignEditor() {
	}

	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;
	private TreeViewer treeViewer;
	private boolean dirty = false;
	private Text txtSpecName;
	private Text txtSpecVersion;

	@Override
	public void doSave(IProgressMonitor monitor) {
		//flush all changed values to the input
		input.getTestCampaign().setSpecName(txtSpecName.getText());
		input.getTestCampaign().setSpecVersion(txtSpecVersion.getText());
		//TODO handle progress in monitor
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
				input = new TestCampaignEditorInput((FileEditorInput) input);
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
		parent.setLayout(new GridLayout(1, false));

		// some meta data on top of the editor
		Composite metaDataComp = new Composite(parent, SWT.NONE);
		metaDataComp.setLayout(new GridLayout(2, false));
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false, 1, 1));

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

		// main part of the editor is occupied by tree view
		Composite treeViewerComp = new Composite(parent, SWT.NONE);
		treeViewerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
				true, 1, 1));
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

		// below a little button area to control execution and report generation
		Composite buttonAreaComp = new Composite(parent, SWT.NONE);
		buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));
		buttonAreaComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true,
				false, 1, 1));

		Button btnExecute = new Button(buttonAreaComp, SWT.NONE);
		btnExecute.setText("Execute");
		btnExecute.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					// execute tests
					input.getTestCampaign().executeTests();
					
					// flag the editor as dirty, so that changes can be saved
					setDirty(true);
					
					// refresh the workspace
					ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
					
				} catch (CoreException ex) {
					StatusManager.getManager().handle(ex, Activator.PLUGIN_ID);
				}
				
			}
		});

		Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
		btnGenerateReport.setText("Generate Report");
		btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// ask for report location
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Please select location to store the report files");
				dialog.setFilterPath(null); // do not filter at all
			    String baseDirName = dialog.open();
			    
			    
//				String baseDirName = "C:/tmp/report"
//						+ GtDateHelper.getCurrentTimeString() + "/";
				TestReport report = new TestReport(input.getTestCampaign(),
						baseDirName);

				try {
					// TODO output XML-Report here, if no pdf is desired

					// output pdf report
					ReportPdfGenerator.writePdfReport(report);
				} catch (IOException ex) {
					IStatus status = new Status(Status.ERROR, Activator.PLUGIN_ID, "PDF report could not be created", ex);
					StatusManager.getManager().handle(status, StatusManager.SHOW);
				}
				
				//TODO copy relevant logfiles
			}

		});
		
		//unset dirty flag as input is just loaded from file
		setDirty(false);

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
	
}
