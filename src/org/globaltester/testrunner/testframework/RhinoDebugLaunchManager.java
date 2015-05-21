package org.globaltester.testrunner.testframework;


import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.LaunchConfiguration;


/**
 * @author koelzer
 *
 */
public class RhinoDebugLaunchManager extends LaunchManager {

	protected String standardLaunchConfigFileName = "runJSDebugger";
	
	/**
	 * @return the standardLaunchConfigFileName
	 */
	public String getStandardLaunchConfigFileName() {
		return standardLaunchConfigFileName;
	}

	/**
	 * @param standardLaunchConfigFileName the standardLaunchConfigFileName to set
	 */
	public void setStandardLaunchConfigFileName(String standardLaunchConfigFileName) {
		this.standardLaunchConfigFileName = standardLaunchConfigFileName;
	}

	/**
	 * 
	 */
	public RhinoDebugLaunchManager() {
		super();
	}
	
	//TODO which exception class should be thrown?
	public void openLaunchConfiguration() throws Exception {
		
		// path information code was copied from LaunchManager:findLocalLaunchConfigurations()
		IPath containerPath = RhinoDebugLaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		if (containerPath == null) {
			// no path set
			Exception exc = new Exception("No path found for JavaScript debug launch. " + 
					"LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH in class LaunchManager must be set correctly!");
			throw exc;
			
		}

		final File directory = containerPath.toFile();
		if (directory == null) {
			Exception exc = new Exception("Error in retrieving file for path " + containerPath.toOSString() + "!");
			throw exc;			
		}

		ILaunchConfiguration stdConfig = findLocalStandardLaunchConfiguration();
		if (stdConfig == null) {
			// no configuration found
			Exception exc = new Exception("Standard configuration launch file for Rhino debugger missing!\n"
					            + "File was looked for in directory "
								+ directory.getName());
			throw exc;
		}
		
		//getInfo(stdConfig);

	}

	protected ILaunchConfiguration findLocalStandardLaunchConfiguration() {
		
		// retrieve launch configurations from disk and check if our standard config exists there
		List<ILaunchConfiguration> listOfAllConfigs = findLocalLaunchConfigurations();
		for (ILaunchConfiguration curConfig:listOfAllConfigs) {
			if (curConfig.getName() == getStandardLaunchConfigFileName()) {
				return curConfig;
			}
		}
		return null;
	}
}
