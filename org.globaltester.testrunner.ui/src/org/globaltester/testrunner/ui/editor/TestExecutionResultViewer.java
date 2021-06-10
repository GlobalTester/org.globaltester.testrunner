package org.globaltester.testrunner.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
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
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GTLogger;
import org.globaltester.logging.logfileeditor.ui.editors.LogfileEditor;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.ActionStepExecution;
import org.globaltester.testrunner.testframework.FileTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result;
import org.globaltester.testrunner.testframework.Result.Status;
import org.globaltester.testrunner.testframework.ResultChangeListener;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;

public class TestExecutionResultViewer implements SelectionListener, ResultChangeListener {

	private IWorkbenchPart part;
	
	private Tree executionStateTree;
	private TreeViewer treeViewer;
	
	private Action actionShowSpec;
	private Action actionShowLog;
	private Action doubleClickAction;

	private boolean autoExpandNonPassed = Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_EXPAND_NON_PASSED, org.globaltester.testrunner.preferences.PreferenceConstants.P_AUTO_EXPAND_NON_PASSED_DEFAULT));

	private boolean filterPassed = Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(org.globaltester.testrunner.Activator.PLUGIN_ID, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_PASSED, org.globaltester.testrunner.preferences.PreferenceConstants.P_FILTER_PASSED_DEFAULT));

	private TreeColumn columnName;

	private boolean autoScroll = true;
	


	public TestExecutionResultViewer(Composite parent, IWorkbenchPart part) {
		this.part = part;
		createPartControl(parent);
	}

	private void createPartControl(Composite parent) {
		executionStateTree = new Tree(parent, SWT.BORDER
				| SWT.FULL_SELECTION);
		executionStateTree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		executionStateTree.setHeaderVisible(true);
		
		treeViewer = new TreeViewer(executionStateTree);
		
		columnName = new TreeColumn(executionStateTree, SWT.LEFT);
		executionStateTree.setLinesVisible(true);
		columnName.setAlignment(SWT.LEFT);
		
		updateColumnName();
		
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
					if (part instanceof TestCampaignEditor){
						((TestCampaignEditor) part).setDirty(true);
					}
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
		parent.setLayout( execStateTreeLayout );
		execStateTreeLayout.setColumnData( columnName, new ColumnWeightData( 50 ) );
		execStateTreeLayout.setColumnData( columnLastExec, new ColumnPixelData( 120 ) );
		execStateTreeLayout.setColumnData( columnStatus, new ColumnPixelData( 100 ) );
		execStateTreeLayout.setColumnData( columnComment, new ColumnWeightData( 100 ) );

		treeViewer.setContentProvider(new TestExecutionResultContentProvider());
		treeViewer.setLabelProvider(new TestExecutionResultLabelProvider());

		executionStateTree.addSelectionListener(this);

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
	}
	
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				TestExecutionResultViewer.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		part.getSite().registerContextMenu(menuMgr, treeViewer);
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
			GtUiHelper.openErrorDialog(part.getSite().getShell(),
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
			GtUiHelper.openErrorDialog(part.getSite().getShell(),
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
				GtUiHelper.openErrorDialog(part.getSite().getShell(),
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

			} catch (BadLocationException e) { //NOSONAR
				//invalid text position -> do nothing
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
			GtUiHelper.openErrorDialog(part.getSite().getShell(),
					"No file name given, thus file can not be displayed.");
			return;
		}
		
		IPath path = new Path(fileName);
		// file exists in local workspace
		IFile file = ResourcesPlugin.getWorkspace().getRoot()
		.getFileForLocation(path);

		showFile(file, line, LogfileEditor.EDITOR_ID);
	}

	public void addSelectionListener(SelectionListener listener) {
		executionStateTree.addSelectionListener(listener);
		
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
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		//nothing to do here
	}

	public void setInput(AbstractTestExecution newInput) {
		Object formerInput = treeViewer.getInput();
		if (formerInput instanceof AbstractTestExecution) {
			((AbstractTestExecution) formerInput).removeResultListener(this);
		}
		
		treeViewer.setInput(newInput);
		if (newInput != null) {
			newInput.addResultListener(this);
			
			applyVisualModifiers(newInput);
		}
	}
	
	public AbstractTestExecution getInput() {
		return (AbstractTestExecution) treeViewer.getInput();
	}

	public void expandAll() {
		treeViewer.expandAll();
		
	}

	public void addListener(int eventType, Listener listener) {
		executionStateTree.addListener(eventType, listener);
		
	}

	public void setFocus() {
		executionStateTree.setFocus();
		
	}

	public void refresh() {
		treeViewer.refresh();
	}

	public void applyVisualModifiers(IExecution execution) {		
		
		for (IExecution child : execution.getChildren()) {
			applyVisualModifiers(child);
		}
		

		if (autoScroll ) {
			for (TreeItem item : treeViewer.getTree().getItems()) {
				if (execution.equals(item.getData())) {
					treeViewer.getTree().setTopItem(item);
				}
			}
		}
		
		if (execution instanceof TestCaseExecution && autoExpandNonPassed && !execution.getResult().getStatus().equals(Status.PASSED)) {
			treeViewer.setExpandedState(execution, true);
		}
	}
	
	@Override
	public void resultChanged(final IExecution changedObject) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!executionStateTree.isDisposed()) {
					if (changedObject != null) {
						applyVisualModifiers(changedObject);
					}
					refresh();
				}
			}
		});
	}

	public void toggleExpandNonPassed() {
		this.autoExpandNonPassed = !this.autoExpandNonPassed;
		updateColumnName();
	}

	private void updateColumnName() {
		String text = "Test case";
		if (filterPassed) {
			text += " (filter PASSED)";
		}
		columnName.setText(text);
	}

	public void toggleFilterPassed() {
		this.filterPassed = !this.filterPassed;
		updateColumnName();
	}

	public void addFilter(ViewerFilter filter) {
		treeViewer.addFilter(filter);
	}

	public void removeFilter(ViewerFilter filter) {
		treeViewer.removeFilter(filter);
	}

	public void expandNonPassed() {
		for (TreeItem item : treeViewer.getTree().getItems()) {
			expandNonPassed(item);
		}
		treeViewer.refresh();
	}

	private void expandNonPassed(TreeItem item) {
		if (item.getData() instanceof TestCaseExecution && !((TestCaseExecution)item.getData()).getResult().getStatus().equals(Status.PASSED)) {
			treeViewer.setExpandedState(item.getData(), true);
		}
		for (TreeItem subItem : item.getItems()) {
			expandNonPassed(subItem);
		}
	}

	public void collapseAll() {
		treeViewer.collapseAll();
	}

	public void setAutoExpandNonPassed(boolean checked) {
		autoExpandNonPassed = checked;
	}

	public void setAutoScroll(boolean checked) {
		autoScroll = checked;
	}
	
	

}
