package org.globaltester.testrunner.ui.commands;

import java.io.FileNotFoundException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.logging.logger.JSDebugLogger;
import org.globaltester.smartcardshell.jsinterface.RhinoJavaScriptAccess;
import org.globaltester.smartcardshell.ui.RhinoDebugLaunchManager;
import org.globaltester.testrunner.ui.Activator;

/**
 * Subclass of RunTestCommandHandler for activating the JavaScript debug mode.
 * The Rhino debugging launch is started in the execute method generating a new
 * thread which is connected to the Rhino debugger in case of success.
 * 
 * @see #execute(org.eclipse.core.commands.ExecutionEvent)
 * 
 * @author akoelzer
 * 
 */
public class DebugTestCommandHandler extends RunTestCommandHandler {

	/**
	 * Time which the debug launch thread shall wait in case the connection
	 * could not immediately be established (then try again {@link #numLoop} times).
	 */
	protected static long waitingTime = 2000;
	
	/**
	 * Number of iterations for repeating trial to establish connection in debug
	 * launch thread.
	 */
	protected static int numLoop = 3;
	
	/**
	 * Name for debug launch thread which makes it easier to find this thread 
	 * in the runtime stack.
	 */
	protected static String rhinoLauncherThreadName = "RhinoDebuggerLauncher";
		

	/**
	 * Returns the absolute path for the currently selected resource. This is
	 * currently only used for testing the {@link #ConvertFileReader} routines
	 * and can be used in later versions for access to this resource.
	 * 
	 * @param event
	 *            which delivers the currently selected resource
	 * @return IPath for the currently selected resource, null if
	 *         none could be detected.
	 */
	protected IPath getResource(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window
					.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IFile) {
				// NOTE: usually the launch configuration files contain the project location
				// where the JavaScript files are stored. This does not work for test campaigns, 
				// therefore we take an absolute path in the file system instead ("C:\...")
				IPath path = (((IFile) firstElement)).getLocation();
//				System.out.println("full path " + path);
				return path;
			}
		}
		return null;
	}

	/**
	 * Returns the parent of the full, absolute path of the currently selected
	 * resource. This is needed for setting the source lookup path in launch
	 * configurations.
	 * 
	 * @param event
	 *            which delivers the currently selected resource
	 * @return path for the currently selected resource
	 */
	protected IPath getSourceLookupRoot(ExecutionEvent event) {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		if (window != null) {
			IStructuredSelection selection = (IStructuredSelection) window
					.getSelectionService().getSelection();
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof IFile) {
				// NOTE: usually the launch configuration files contain the project location
				// where the JavaScript files are stored. This does not work for test campaigns, 
				// therefore we take an absolute path in the file system instead ("C:\...")
				IPath path = (((IFile) firstElement).getParent()).getLocation();
//				System.out.println("full path " + path);
				return path;
			}
		}
		return null;
	}
	
	/**
	 * prepares settings for Rhino debugging thread and launch and starts the
	 * launch
	 * @throws RuntimeException
	 *             in case the launch could not be started properly
	 */
	@Override
	protected void setupEnvironment(ExecutionEvent event)  throws RuntimeException {
		startRhinoDebugLaunch(event);
	}

	/**
	 * Tries to start the Rhino JavaScript debugger launch in an own thread.
	 * Concurrently the execute method of the super class activates the Rhino
	 * debugger thread. This Rhino debugger thread must be started before the
	 * launch thread and the debugger launch thread has to wait for it, since
	 * these two Rhino threads communicate with each other. When the debugger
	 * thread has created a debuggerStartedObj-object (which serves as a signal
	 * that it is running) the two threads are connected.<br>
	 * 
	 * NOTE: The debugger thread works with its own timeout if it is started in
	 * suspended mode (concurrently set to the fixed value of 300000 in class
	 * org.eclipse.wst.jsdt.debug.internal.rhino.debugger.DebugSessionManager.
	 * start()). Therefore a deadlock should not occur when the launch thread is
	 * terminated too early. Since the timeout is rather long, this could be
	 * irritating for the user. Maybe there should be a special treatment for
	 * this case!
	 * 
	 * @see org.globaltester.testrunner.ui.commands.RunTestCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 * @param event
	 *            needed to retrieve information on the currently selected
	 *            resource
	 * @throws RuntimeException
	 *             if the launch could not be started
	 */
	protected void startRhinoDebugLaunch(ExecutionEvent event) throws RuntimeException {

		final RhinoDebugLaunchManager launchMan = new RhinoDebugLaunchManager();
		try {
			// initialize the standard configuration file and set the port number found
			// there as socket number for the communication between debugger thread.
			// Besides this add the project root to the Rhino source lookup path
			envSettings.put(RhinoJavaScriptAccess.getRhinoJSFileNameHashKey(), getResource(event));
			envSettings.put(RhinoJavaScriptAccess.getRhinoJSLookupPathHashKey(), getSourceLookupRoot(event));
			launchMan.initDebugLaunchConfiguration(envSettings);
			envSettings.put(RhinoJavaScriptAccess.getRhinoJSPortHashKey(), launchMan.getPortNum());
			
//			// delete from here ...
//			// TODO: Currently only used for testing the {@link #ConvertFileReader} routines.
//			RhinoJavaScriptAccess jsAccess = new RhinoJavaScriptAccess(); 
//			jsAccess.XML2JSConverter(envSettings);
//			if (true)
//				return;
//			// delete to here!
		
		} catch (FileNotFoundException | RuntimeException exc) {	
			//rewrap Exception with additional context description and rethrow it to be handled by calling code
			String errorMsg = "A problem occurred when trying to access a JavaScript launch configuration.\n"
					+ exc.getLocalizedMessage();
			throw new RuntimeException(errorMsg, exc);
		}
	
		// generate the launch thread and override its run method with a waiting
		// loop so that there is some time to establish the connection
		// (There is no extra class declared for this thread, since the run method
		// shall have access to the local variables!)
		Thread rhinoDebugLaunchThread = new Thread() {
			
			/*
			 * NOTE (seems to be fixed - must be observed!) When starting
			 * a new Eclipse launch, build processes are executed in void
			 * org.eclipse.debug.internal.ui.DebugUIPlugin.launchInBackground(
			 * ILaunchConfiguration configuration, String mode). If these
			 * processes have not finished yet, the launch process is set to
			 * wait (a wait variable is set to true). In this case the code
			 * executes the statements if (wait) {
			 * progressService.showInDialog(workbench
			 * .getActiveWorkbenchWindow().getShell(), job); } It sometimes
			 * happens that the getActiveWorkbenchWindow() returns null which
			 * causes a NullPointerException. This usually happens if the
			 * current thread is not a UI thread. It is still unclear how to
			 * ensure the thread concerned to be a UI thread! The problem only
			 * occurs from time to time, also dependent on if breakpoints are
			 * set and where. There exists a bug fix for this described in
			 * http:/
			 * /git.eclipse.org/c/platform/eclipse.platform.debug.git/commit
			 * /?id=a7933cebb9008430f78cb0a48e66007178723c95 which is not part
			 * of the official library org.eclipse.debug.ui.
			 */

			@Override
			public void run() {
				try {
					int count = 0;
					while (count <= numLoop) {
						// wait for Rhino debugger to be started; the object delivered
						// here is an interface object which can be filled with 
						// functionality in later versions
//						System.err.println("Run loop " + count + " of launch thread!");
						if (RhinoJavaScriptAccess.getDebuggerStartedObj() == null) {
							try {
								Thread.sleep(waitingTime);
							} catch (InterruptedException exc) {
								// just continue
							}
							count++;
						} else {
							launchMan.startDebugLaunchConfiguration();
							break;
						}
					}
					if (RhinoJavaScriptAccess.getDebuggerStartedObj() == null) {
						JSDebugLogger.error("DebugTestCommandHandler: "
										+ "No JavaScript debugger start could be detected! Canceling debug launching.");
					}

				} catch (RuntimeException exc) { // probably a missing debug configuration
					String errorMsg = "DebugTestCommandHandler: "
							+ "Debugger launch could not be started! Canceling debug launching.\n"
							+ "Reason:\n" + exc.getLocalizedMessage();
					JSDebugLogger.error(errorMsg);
					GtErrorLogger.log(Activator.PLUGIN_ID, new Exception(errorMsg, exc));
					if (shell != null)
						GtUiHelper.openErrorDialog(shell, errorMsg);
					//exc.printStackTrace();
					throw new RuntimeException(errorMsg, exc);
				}
			}
		};

		rhinoDebugLaunchThread.setName(rhinoLauncherThreadName); // simplifies retrieving thread in runtime stack
		try {
			JSDebugLogger.info("Trying to start debug launch thread ...");
			// make this thread a UI thread; otherwise there will be null pointer
			// exceptions when accessing the active workbench window in 
			// DebugUIPlugin.launchInBackground()
			final Display display = DebugUIPlugin.getStandardDisplay(); 
									// currently same as PlatformUI.getWorkbench().getDisplay()
			display.asyncExec(rhinoDebugLaunchThread); // starts the launch (hopefully) in a UI thread
		} catch  (Exception exc) {
			// this part should usually only be reached, when non-debugging exceptions occur,
			// since debug exceptions are handled elsewhere
			String errorMsg = "DebugTestCommandHandler: " +
					   "Debugger launch could not be started! Canceling debug launching.\n" +
					   "Error message: " + exc.getLocalizedMessage() + "\n";
			JSDebugLogger.error(errorMsg);
			GtErrorLogger.log(Activator.PLUGIN_ID, new Exception(errorMsg, exc));
			//exc.printStackTrace();
			throw new RuntimeException(errorMsg, exc);
		}
	}
}
