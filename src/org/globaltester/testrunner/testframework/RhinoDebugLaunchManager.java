package org.globaltester.testrunner.testframework;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.internal.core.LaunchConfigurationInfo;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.LaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;


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
	public void startDebugLaunchConfiguration() throws Exception {
		
		// path information code was copied from LaunchManager:findLocalLaunchConfigurations()
		IPath containerPath = RhinoDebugLaunchManager.LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH;
		if (containerPath == null) {
			// no path set
			Exception exc = new Exception("No path found for JavaScript debug launch. " + 
					"LOCAL_LAUNCH_CONFIGURATION_CONTAINER_PATH in class LaunchManager must be set correctly!");
			//TODO AKR use a builtin launch configuration instead!
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
								+ containerPath.toOSString());
			throw exc;
		}
		
		//TODO AKR this cast should usually always work. But maybe there should be some "else" case
		if (stdConfig instanceof LaunchConfiguration) {
			LaunchConfiguration stdLConfig = (LaunchConfiguration) stdConfig;
			//LaunchConfigurationInfo info = getInfo(stdLConfig);
			Map<String, Object> attrs = stdLConfig.getAttributes();
			System.out.println(attrs);
		}
		
		DebugUITools.launch(stdConfig, ILaunchManager.DEBUG_MODE);

	}

	protected ILaunchConfiguration findLocalStandardLaunchConfiguration() {
		
		// retrieve launch configurations from disk and check if our standard config exists there
		List<ILaunchConfiguration> validConfigs = new ArrayList<ILaunchConfiguration>(20);
		List<ILaunchConfiguration> configs = findLocalLaunchConfigurations();
		verifyConfigurations(configs, validConfigs);

		for (ILaunchConfiguration curConfig:validConfigs) {
			if (curConfig.getName().equals(getStandardLaunchConfigFileName())) {
				return curConfig;
			}
		}
		return null;
	}
}
