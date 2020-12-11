package org.globaltester.testrunner.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.globaltester.logging.legacy.logger.GTLogger;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.ui.Activator;

public class ReportingPreferencePage extends
FieldEditorPreferencePage implements IWorkbenchPreferencePage{

	Group testReportGroup;
	BooleanFieldEditor bfeManualPathSettings;
	DirectoryFieldEditor dfeReportDir;
	BooleanFieldEditor bfeAutomaticReport;

	
	public ReportingPreferencePage() {
		super(GRID);
		
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("GlobalTester Preference Page for the TestReports");

	}
	
	

	@Override
	public void init(IWorkbench workbench) {

	}

	@Override
	protected void createFieldEditors() {

		Composite container = new Composite(this.getFieldEditorParent(),
				SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL, GridData.FILL, true, false);
		gd.horizontalSpan = 4;
		
		testReportGroup = new Group(container, SWT.NONE);
		testReportGroup.setText("Customization");
		
		testReportGroup.setLayoutData(gd);
		testReportGroup.setLayout(new GridLayout(4, false));

		//generate reports automatically
				bfeAutomaticReport = new BooleanFieldEditor(
						PreferenceConstants.P_REPORT_AUTOMATIC,
						"Generate reports automatically", testReportGroup);
				addField(bfeAutomaticReport);

				// manual settings of directories 
				bfeManualPathSettings = new BooleanFieldEditor(
						PreferenceConstants.P_REPORT_USE_FIXED_DIR,
						"Fixed directory for report files", testReportGroup);
				addField(bfeManualPathSettings);

				dfeReportDir = new DirectoryFieldEditor(
						PreferenceConstants.P_REPORT_DIR, "Report directory:",
						testReportGroup) {
					@Override
					public boolean checkState() {
						return !getPreferenceStore().getBoolean(PreferenceConstants.P_REPORT_USE_FIXED_DIR) || super.checkState();
					}
				};
				dfeReportDir.setEmptyStringAllowed(false);
				addField(dfeReportDir);

				if (!getPreferenceStore().getBoolean(
						PreferenceConstants.P_REPORT_USE_FIXED_DIR)) {
					dfeReportDir.setEnabled(false, testReportGroup);
				}
				
				addField(dfeReportDir);

	}	
	
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		
		return result;
	}

	protected void performDefaults() {
		super.performDefaults();
		GTLogger
				.getInstance()
				.debug(
						"Switched GT TestManager Preference Page back do default values");

		//enable/disable report directory field
		dfeReportDir.setEnabled(false, testReportGroup);

	}

	public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getProperty().equals(FieldEditor.VALUE)) {

			if (event.getSource() == bfeManualPathSettings) {
				boolean manualPath = ((Boolean) event.getNewValue()).booleanValue();
				if (manualPath) {
					dfeReportDir.setEnabled(true, testReportGroup);
					if (dfeReportDir.isValid() && dfeReportDir.getStringValue() != ""){
						setErrorMessage(null);	
						setValid(true);
					} else {
						setErrorMessage("Use a valid directory!");
						setValid(false);
					}
				} else {
					dfeReportDir.setEnabled(false, testReportGroup);
					setErrorMessage(null);
					setValid(true);
				}
			}
		}
	}
	

	
}
