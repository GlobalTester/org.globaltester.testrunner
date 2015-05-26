package org.globaltester.testrunner.testframework;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
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
	protected LaunchConfiguration stdLConfig = null;
	protected String portNo = "9000";
	protected final String PORT_KEY = "port";
	
	public String getPortNo() {
		return portNo;
	}

	public void setPortNo(String portNo) {
		this.portNo = portNo;
	}

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
	public void readDebugLaunchConfiguration() throws Exception {
		
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
			stdLConfig = (LaunchConfiguration) stdConfig;
			setPortNumFromConfig();

			//just for logging:
			Map<String, Object> attrs = stdLConfig.getAttributes();
			System.out.println("Rhino debug configuration settings:");
			System.out.println(attrs);
		}
		else {
			System.err.println("Something wrong with Rhino debug configuration: wrong type - check this!");
			System.err.println("Connecting debugger to port " + getPortNo() + "!");
			System.err.println("Will try to continue execution nevertheless!");
		}
	}		

	public void startDebugLaunchConfiguration() throws Exception {
		if (stdLConfig == null) {
			// no configuration found
			Exception exc = new Exception("Standard configuration was not correctly initialized!\n");
			throw exc;			
		}
		
		DebugUITools.launch(stdLConfig, ILaunchManager.DEBUG_MODE);

	}

	private void setPortNumFromConfig() {
		if (stdLConfig == null) {
			System.err.println("Variable stdLConfig is unset in class " + getClass().getCanonicalName() + "!");
			System.err.println("Debug configuration settings cannot be stored!");
			return;
		}
		
		try {
			Map<String, String> argumentMap = new HashMap<String, String> ();
			Map<String, String> defaultMap = new HashMap<String, String> ();
			defaultMap.put("", "");
			argumentMap = stdLConfig.getAttribute("argument_map", defaultMap);
			if (argumentMap.equals(defaultMap)) {
				System.err.println("No argument map found for Rhino debug configuration settings!");
				System.err.println("Port number could not be extracted!");	
				return;
			}
			
			if (argumentMap.containsKey(PORT_KEY)) {
				portNo = argumentMap.get(PORT_KEY);
				System.out.println("Port number is " + portNo + "!");
				//TODO should we test if the port number is in a valid range?
			}	
			else {
				System.err.println("Port number could not be extracted from Rhino debug configuration settings!");
				System.err.println("Key " + PORT_KEY + " was not found!");
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
