package org.globaltester.testrunner.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.globaltester.base.PreferenceHelper;
import org.globaltester.testrunner.Activator;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * Use this to store plugin preferences For meaning of each preference look
	 * at PreferenceConstants.java
	 */
	public void initializeDefaultPreferences() {
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_DOUBLECLICKRESULTVIEW, Integer.toString(0));
		
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_IGNORECHECKSUMRESULT, Boolean.toString(false));
		
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_REPORT_AUTOMATIC, Boolean.toString(false));
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_REPORT_USE_FIXED_DIR, Boolean.toString(false));
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_REPORT_DIR, System.getProperty("user.home"));

		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_AUTO_EXPAND_NON_PASSED, PreferenceConstants.P_AUTO_EXPAND_NON_PASSED_DEFAULT);
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_AUTO_SCROLL, PreferenceConstants.P_AUTO_SCROLL_DEFAULT);
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_FILTER_PASSED, PreferenceConstants.P_FILTER_PASSED_DEFAULT);
		PreferenceHelper.setPreferenceDefaultValue(Activator.PLUGIN_ID, PreferenceConstants.P_FILTER_UNDEFINED, PreferenceConstants.P_FILTER_UNDEFINED_DEFAULT);
		
	}

}
