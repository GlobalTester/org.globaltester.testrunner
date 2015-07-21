package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.swt.widgets.Display;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.logging.logger.GtErrorLogger;
import org.globaltester.logging.logger.JSDebugLogger;
import org.globaltester.smartcardshell.RhinoJavaScriptAccess;
import org.globaltester.smartcardshell.ui.RhinoDebugLaunchManager;
import org.globaltester.testrunner.ui.Activator;

/**
 * Subclass of RunTestCommandHandler for activating the debug mode. The Rhino
 * debugging launch is started in the execute method generating a new thread
 * which is connected to the Rhino debugger in case of success.
 * 
 * @see #execute(org.eclipse.core.commands.ExecutionEvent)
 * 
 * @author koelzer
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
	 * Tries to start the Rhino JavaScript debugger launch in an own thread.
	 * Concurrently the execute method of the super class activates
	 * the Rhino debugger thread. This Rhino debugger thread must be started
	 * before the launch thread and the debugger launch thread has to wait for
	 * it, since these two Rhino threads communicate with each other. <br>
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
	 * @param event needed to retrieve information on the currently selected 
	 * 			resource
	 */
	@Override
	protected void startRhinoDebugLaunch(ExecutionEvent event) {

		// TODO this can be deleted as soon as we are sure there are no more thread
		// problems
//		if (Display.findDisplay(Thread.currentThread()) == null) {
//			System.err.println("Thread has no display in DebugTestCommandHandler.execute");
//		} else {
//			System.err.println("Thread HAS a display in DebugTestCommandHandler.execute");
//		}

		final RhinoDebugLaunchManager launchMan = new RhinoDebugLaunchManager();
		try {
			// initialize the standard configuration file and set the port number found
			// there as socket number for the communication between debugger thread.
			// Besides this add the project root to the Rhino source lookup path
			launchMan.initDebugLaunchConfiguration(getSourceLookupRoot(event));
			RhinoJavaScriptAccess.setStandardPortNum(launchMan.getPortNum());
			
		} catch (Exception exc) {
			//log and show error
			String errorMsg = "A problem occurred when trying to access a JavaScript launch configuration.\n"
					+ exc.getLocalizedMessage();
			JSDebugLogger.error(errorMsg);
			GtErrorLogger.log(Activator.PLUGIN_ID, new Exception(errorMsg, exc));
			if (shell != null)
				GtUiHelper.openErrorDialog(shell, errorMsg);
			//e1.printStackTrace();
			return;
		}
	
		// generate the launch thread and override its run method with a waiting
		// loop so that there is some time to establish the connection
		// (There is no extra class declared for this thread, since the run method
		// shall have access to the local variables!)
		Thread rhinoDebugLaunchThread = new Thread() {
			
			/*
			 * FIXME (probably already fixed!? must be observed!) When starting
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
				// TODO this can be deleted as soon as we are sure there are no more thread
				// problems
//				if (Display.findDisplay(Thread.currentThread()) == null) {
//					System.err.println("Thread has no display in run");
//				} else {
//					System.err.println("Thread HAS a display in run");
//				}

				try {
					// TODO amay this waiting is still done in the UI thread. 
					// should this be solved differently?
					int count = 0;
					while (count <= numLoop) {
						// wait for Rhino debugger to be started; the object delivered
						// here is an interface object which can be filled with 
						// functionality in later versions
						System.err.println("Run loop " + count + " of launch thread!");
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

				} catch (Exception exc) { // probably missing debug configuration
					String errorMsg = "DebugTestCommandHandler: "
							+ "Debugger launch could not be started! Canceling debug launching.\n"
							+ "Reason:\n" + exc.getLocalizedMessage();
					JSDebugLogger.error(errorMsg);
					GtErrorLogger.log(Activator.PLUGIN_ID, new Exception(errorMsg, exc));
					if (shell != null)
						GtUiHelper.openErrorDialog(shell, errorMsg);
					//exc.printStackTrace();
				}
			}
		};

		rhinoDebugLaunchThread.setName(rhinoLauncherThreadName); // simplifies retrieving thread in runtime stack
		try {
			// TODO this can be deleted as soon as we are sure there are no more thread
			// problems
//			if (Display.findDisplay(Thread.currentThread()) == null) {
//				System.err.println("Thread has no display in DebugTestCommandHandler.execute before asyncExec");
//			} else {
//				System.err.println("Thread HAS a display in DebugTestCommandHandler.execute before asyncExec");
//			}

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
		}
	}

	/**
	 * Indicates if JavaScript debugging is activated or not. 
	 * @return true since JavaScript debugging is activated for this handler
	 */
	@Override
	public boolean isDebugMode() {
		return true;
	}

}
