package org.globaltester.testrunner.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.globaltester.testrunner.Activator;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * This class stores the properties of the plug-in Class used to initialize
 * default preference values.
 * 
 * @author lvelten
 * 
 */

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * Use this to store plugin preferences For meaning of each preference look
	 * at PreferenceConstants.java
	 */
	public void initializeDefaultPreferences() {

		Preferences preferences = new DefaultScope()
				.getNode(Activator.PLUGIN_ID);
		
		
		//if TestCase or log file should open on double click
		preferences.putInt(PreferenceConstants.P_DOUBLECLICKRESULTVIEW, 0);
		
		// Force the application to save the preferences
		try {
			preferences.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

	}

}
