package org.globaltester.testrunner;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	public static final String PLUGIN_ID = "org.globaltester.testrunner";
	public static final String VERSION = "Release " + Platform.getBundle(PLUGIN_ID).getVersion();
	
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}
	
	private static void setContext(BundleContext bundleContext) {
		context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.setContext(bundleContext);
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.setContext(null);
	}
	
	/**
	 * Returns the current path of GlobalTester_Plugin
	 */
	public static IPath getPluginDir() {
		URL url = context.getBundle().getEntry("/");
		
		IPath pluginDir = null;
		try {
			pluginDir = new Path(FileLocator.toFileURL(url).getPath());
			return pluginDir;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

}
