package org.globaltester.testrunner.ui.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.Activator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * Use this to store plugin preferences For meaning of each preference look
	 * at PreferenceConstants.java
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				Activator.PLUGIN_ID);
		
        store.setDefault(PreferenceConstants.P_REPORT_AUTOMATIC, false);
        store.setDefault(PreferenceConstants.P_REPORT_USE_FIXED_DIR, false);
        store.setDefault(PreferenceConstants.P_REPORT_DIR, System.getProperty("user.home"));
	}

}
