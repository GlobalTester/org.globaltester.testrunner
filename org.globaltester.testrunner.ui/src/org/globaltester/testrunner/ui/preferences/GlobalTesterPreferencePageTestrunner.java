package org.globaltester.testrunner.ui.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.globaltester.testrunner.Activator;
import org.globaltester.testrunner.preferences.PreferenceConstants;

/**
 * This class implements the Preference Page for the plug-in testrunner
 * @author lvelten
 *
 */

public class GlobalTesterPreferencePageTestrunner extends
FieldEditorPreferencePage implements IWorkbenchPreferencePage{

	Group customizationGroup;
	private BooleanFieldEditor bfeIntegrityWarningDialog;
	private BooleanFieldEditor bfeUserInteractionForGeneratedTests;
	
	
	public GlobalTesterPreferencePageTestrunner() {
		super(GRID);
		
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Activator.PLUGIN_ID);
		setPreferenceStore(store);
		setDescription("GlobalTester Preference Page for the TestRunner");

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
		
		customizationGroup = new Group(container, SWT.NONE);
		customizationGroup.setText("Customization");
		
		customizationGroup.setLayoutData(gd);
		customizationGroup.setLayout(new GridLayout(4, false));

		// disable integrity warning dialog 
		bfeIntegrityWarningDialog = new BooleanFieldEditor(
				PreferenceConstants.P_IGNORECHECKSUMRESULT,
				"Ignore integrity checks testcases", customizationGroup);
		addField(bfeIntegrityWarningDialog);
		
		String doubleClicks[][] = new String[2][2];
		doubleClicks[0][0] = PreferenceConstants.TEST_CASE;
		doubleClicks[0][1] = "0";
		doubleClicks[1][0] = PreferenceConstants.LOG_FILE;
		doubleClicks[1][1] = "1";
		ComboFieldEditor cfeDoubleClickResultView = new ComboFieldEditor(PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 
				"Double click in test results shows: ", doubleClicks, customizationGroup);
		addField(cfeDoubleClickResultView);

		// enable user interaction for generated test cases
		bfeUserInteractionForGeneratedTests = new BooleanFieldEditor(
				PreferenceConstants.P_ASK_USER_FOR_GENERATED_TESTS,
				"Ask user for execution of generated test cases", customizationGroup);
		addField(bfeUserInteractionForGeneratedTests);

	}
	
	
	@Override
	public boolean performOk() {
		boolean result = super.performOk();
		
		return result;
	}
	

	
}
