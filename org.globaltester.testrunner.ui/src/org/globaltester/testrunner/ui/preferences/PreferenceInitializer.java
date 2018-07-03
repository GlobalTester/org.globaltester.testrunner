package org.globaltester.testrunner.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.ui.Activator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * Use this to store plugin preferences For meaning of each preference look
	 * at PreferenceConstants.java
	 */
	public void initializeDefaultPreferences() {

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		
		//if TestCase or log file should open on double click
		store.setDefault(PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 0);
		
		//do not ignore checksum results by default
		store.setDefault(PreferenceConstants.P_IGNORECHECKSUMRESULT, false);
		

		
		
        store.setDefault(PreferenceConstants.P_REPORT_AUTOMATIC, false);
        store.setDefault(PreferenceConstants.P_REPORT_USE_FIXED_DIR, false);
        store.setDefault(PreferenceConstants.P_REPORT_DIR, System.getProperty("user.home"));
		
	}

}
