package org.globaltester.testrunner.ui.editor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.globaltester.testrunner.ui.Activator;

public class TestCampaignEditor extends EditorPart {
	public TestCampaignEditor() {
	}

	public static final String ID = "org.globaltester.testrunner.ui.testcampaigneditor";
	private TestCampaignEditorInput input;

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if ((input instanceof FileEditorInput)) {
			try {
				input = new TestCampaignEditorInput((FileEditorInput) input);
			} catch (CoreException e) {
				throw new RuntimeException("Wrong Input - No TestCampaignEditorInput can be created from selected resource");	
			}
		}
		if (!(input instanceof TestCampaignEditorInput)) {
			throw new RuntimeException("Wrong input");
		}

		this.input = (TestCampaignEditorInput) input;
		setSite(site);
		setInput(input);
		
		setPartName(this.input.getName());
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		//GridLayout layout = new GridLayout();
		//layout.numColumns = 2;
		//parent.setLayout(layout);
//		Label label1 = new Label(parent, SWT.NONE);
//		label1.setText("First Name");
//		Text text = new Text(parent, SWT.BORDER);
//		text.setText(input.getName());
//		text.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
//		new Label(parent, SWT.NONE).setText("Last Name");
//		Text lastName = new Text(parent, SWT.BORDER);
//		lastName.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
//				false));
//		lastName.setText(input.getName());
		
		//some meta data on top of the editor
		Composite metaDataComp = new Composite(parent, SWT.NONE);
		metaDataComp.setLayout(new GridLayout(8, false));
		metaDataComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		new Label(metaDataComp, SWT.NONE);
		
		Label lblMetadataHere = new Label(metaDataComp, SWT.NONE);
		lblMetadataHere.setText("MetaData here");
		
		
		//main part of the editor is occupied by tree view
		Composite treeViewerComp = new Composite(parent, SWT.NONE);
		treeViewerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		treeViewerComp.setLayout(new FillLayout(SWT.HORIZONTAL));
		Tree executionStateTree = new Tree(treeViewerComp, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		executionStateTree.setHeaderVisible(true);
      TreeViewer m_treeViewer = new TreeViewer(executionStateTree);
 
      TreeColumn column1 = new TreeColumn(executionStateTree, SWT.LEFT);
      executionStateTree.setLinesVisible(true);
      column1.setAlignment(SWT.LEFT);
      column1.setText("Testcase/TestStep");
      column1.setWidth(160);
      TreeColumn column2 = new TreeColumn(executionStateTree, SWT.RIGHT);
      column2.setAlignment(SWT.LEFT);
      column2.setText("Result");
      column2.setWidth(100);
      TreeColumn column3 = new TreeColumn(executionStateTree, SWT.RIGHT);
      column3.setAlignment(SWT.LEFT);
      column3.setText("Remarks");
      column3.setWidth(100);
 
      m_treeViewer.setContentProvider(new TestCampaignContentProvider());
      m_treeViewer.setLabelProvider(new TestCampaignTableLabelProvider());
      List<DummyTestCase> cities = new ArrayList<DummyTestCase>();
      cities.add(new DummyTestCase("DummyContent1"));
      cities.add(new DummyTestCase("DummyContent2"));
      m_treeViewer.setInput(cities);
      m_treeViewer.expandAll();
      
      //below a little row to control execution and report generation
      Composite buttonAreaComp = new Composite(parent, SWT.NONE);
      buttonAreaComp.setLayout(new FillLayout(SWT.HORIZONTAL));
      buttonAreaComp.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false, 1, 1));
      
      Button btnExecute = new Button(buttonAreaComp, SWT.NONE);
      btnExecute.setText("Execute");
      btnExecute.addSelectionListener(new SelectionAdapter(){
    	  public void widgetSelected(SelectionEvent e) {
    		//TODO integrate testcase execution here
				MessageDialog
				.openWarning(
						Activator.getDefault().getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
						"GlobalTester",
						"Execution of this TestCampaign is support from context menu only at the moment");
    	  }
      });
      
      Button btnGenerateReport = new Button(buttonAreaComp, SWT.NONE);
      btnGenerateReport.setText("Generate Report");
      btnGenerateReport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				//TODO integrate report generation here
				MessageDialog
				.openWarning(
						Activator.getDefault().getWorkbench()
								.getActiveWorkbenchWindow().getShell(),
						"GlobalTester",
						"Report generation is not yet supported in GlobalTester3");
			}

		});
      
		
		
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}
	
	
	class DummyTestCase{
	      DummyTestStep[]	steps	= new DummyTestStep[2];
		private String name;
	 
	      public DummyTestCase(String newName){
	         name = newName;
	    	 for (int i = 0; i < steps.length; i++)
	            steps[i] = new DummyTestStep("DummyStep "+i, this);
	         
	      }
	 
	      public DummyTestStep[] getSteps(){
	         return steps;
	      }
	 
	      public String toString(){
	         return name;
	      }
	   }
	 
	   class DummyTestStep{
	      DummyTestCase	parentTestCase;
	      DummyResult[]	results	= new DummyResult[2];
		private String name;
	 
	      public DummyTestStep(String newName, DummyTestCase parent){
	         this.parentTestCase = parent;
	         this.name = newName;
	         for (int i = 0; i < results.length; i++)
	            results[i] = new DummyResult("DummyResult "+i, this);
	      }
	 
	      public DummyResult[] getResults(){
	         return results;
	      }
	 
	      public String toString(){
	         return name;
	      }
	   }
	 
	 
	   class DummyResult{
	      DummyTestStep	parentStep;
		private String name;
	 
	      public DummyResult(String newName, DummyTestStep parent){
	         this.parentStep = parent;
	         this.name = newName;
	      }
	 
	      public String toString(){
	         return name;
	      }
	 
	      public String getResult(){
	         return toString();
	      }
	 
	      public String getRemarks(){
	         return "some remarks here";
	      }
	   }
	 
	 
	   class TestCampaignContentProvider implements ITreeContentProvider{
	      public Object[] getChildren(Object parentElement){
	         if (parentElement instanceof List)
	            return ((List<?>) parentElement).toArray();
	         if (parentElement instanceof DummyTestCase)
	            return ((DummyTestCase) parentElement).getSteps();
	         if (parentElement instanceof DummyTestStep)
	            return ((DummyTestStep) parentElement).getResults();
	         return new Object[0];
	      }
	 
	      public Object getParent(Object element){
	         if (element instanceof DummyTestStep)
	            return ((DummyTestStep) element).parentTestCase;
	         if (element instanceof DummyResult)
	            return ((DummyResult) element).name;
	         return null;
	      }
	 
	      public boolean hasChildren(Object element){
	         if (element instanceof List)
	            return ((List<?>) element).size() > 0;
	         if (element instanceof DummyTestCase)
	            return ((DummyTestCase) element).getSteps().length > 0;
	         if (element instanceof DummyTestStep)
	            return ((DummyTestStep) element).getResults().length > 0;
	         return false;
	      }
	 
	      public Object[] getElements(Object cities){
	         // cities ist das, was oben in setInput(..) gesetzt wurde.
	         return getChildren(cities);
	      }
	 
	      public void dispose(){
	      }
	 
	      public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
	      }
	   }
	 
	 
	   class TestCampaignTableLabelProvider implements ITableLabelProvider{
	 
	      public Image getColumnImage(Object element, int columnIndex){
	         return null;
	      }
	 
	      public String getColumnText(Object element, int columnIndex){
	         switch (columnIndex){
	            case 0: return element.toString();
	            case 1:
	               if (element instanceof DummyResult)
	                  return ((DummyResult)element).getResult();
	            case 2: 
	               if (element instanceof DummyResult)
	                  return ((DummyResult)element).getRemarks();
	         }
	         return null;
	      }
	 
	      public void addListener(ILabelProviderListener listener){
	      }
	 
	      public void dispose(){
	      }
	 
	      public boolean isLabelProperty(Object element, String property){
	         return false;
	      }
	 
	      public void removeListener(ILabelProviderListener listener){
	      }
	   }
}
