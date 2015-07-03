package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.smartcardshell.RhinoJavaScriptAccess;
import org.globaltester.smartcardshell.ui.RhinoDebugLaunchManager;

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
	protected static int numLoop = 7;
	
	/**
	 * Name for debug launch thread which makes it easier to find this thread 
	 * in the runtime stack.
	 */
	protected static String rhinoLauncherThreadName = "RhinoDebuggerLauncher";
	
	/**
	 * Calls the super constructor and initializes debug mode with true.
	 */
	public DebugTestCommandHandler() {
		super();
	}

	/**
	 * Tries to start the Rhino JavaScript debugger launch in an own thread and
	 * concurrently starts the execute method of the super class which activates
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
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (Display.getCurrent() == null) {
			System.out.println("Display is null in execute");
		} else {
			System.out.println("Display is not null in execute");
		}
		if (Display.findDisplay(Thread.currentThread()) == null) {
			System.out.println("Thread is not UI in execute");
		} else {
			System.out.println("Thread is UI in execute");
		}

		final RhinoDebugLaunchManager launchMan = new RhinoDebugLaunchManager();
		try {
			// read the standard configuration file and set the port number found
			// there as socket number for the communication between debugger thread
			launchMan.readDebugLaunchConfiguration();
			RhinoJavaScriptAccess.setStandardPortNum(launchMan.getPortNo());
		} catch (Exception e1) {
			//e1.printStackTrace();
			Shell shell = HandlerUtil.getActiveWorkbenchWindow(event)
					.getShell();
			//TODO how show errors correctly?
			GtUiHelper
					.openErrorDialog(
							shell,
							"A problem occurred when trying to read the JavaScript debug launch configuration."
									+ e1.getLocalizedMessage());
			return null;
		}
	
		// generate the launch thread and override its run method with a waiting
		// loop so that there is some time to establish the connection
		Thread rhinoDebugLaunchThread = new Thread() {
			
		/*
			FIXME When starting a new Eclipse launch, build processes are executed in
			void org.eclipse.debug.internal.ui.DebugUIPlugin.launchInBackground(ILaunchConfiguration 
			configuration, String mode).
			If these processes have not finished yet the launch process is set to wait
			(a wait variable is set to true). In this case the code executes the statements
			if (wait) {
				progressService.showInDialog(workbench.getActiveWorkbenchWindow().getShell(), job);
			}
			It sometimes happens that the getActiveWorkbenchWindow() returns null which causes
			a NullPointerException. This usually happens if the current thread is not a UI thread.
			It is still unclear how to make the thread concerned a UI thread!
			There exists a bug fix for this described in 
			http://git.eclipse.org/c/platform/eclipse.platform.debug.git/commit/?id=a7933cebb9008430f78cb0a48e66007178723c95
			which is not part of the official library org.eclipse.debug.ui.
		*/

			@Override
			public void run() {
				try {
					int count = 0;
					while (count <= numLoop) {
						// wait for Rhino debugger to be started; the object delivered
						// here is an interface object which can be filled with 
						// functionality in later versions
						System.out.println("Run loop " + count + "of launch thread!");
						if (RhinoJavaScriptAccess.getDebuggerStartedObj() == null) {
							Thread.sleep(waitingTime);
							count++;
						} else {
							if (Display.getCurrent() == null) {
								System.out.println("Display is null");
							}
							if (Display.findDisplay(Thread.currentThread()) == null) {
								System.out.println("Thread is not UI in run");
							} else {
								System.out.println("Thread is UI in run");
							}
							launchMan.startDebugLaunchConfiguration();
							break;
						}
					}
					if (RhinoJavaScriptAccess.getDebuggerStartedObj() == null) {
						System.err
								.println("DebugTestCommandHandler: "
										+ "Debugger start could not be detected! Canceling debug launching.");
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		rhinoDebugLaunchThread.setName(rhinoLauncherThreadName); // simplifies retrieving thread in runtime stack
		try {
			System.out.println("Starting debug launch thread ...");
			//final Display display = DebugUIPlugin.getStandardDisplay();
			//display.asyncExec(rhinoDebugLaunchThread);
			//PlatformUI.getWorkbench().getDisplay().asyncExec(rhinoDebugLaunchThread);
			rhinoDebugLaunchThread.start();
		} catch  (Exception e) {
			System.err
			.println("DebugTestCommandHandler: "
					+ "Debugger launch could not be started! Canceling debug launching. Reason: " + e.getMessage());
			e.printStackTrace();
		}

		// concurrently continue standard execution, which starts the debugger 
		// thread and evaluates the JavaScript code for this test case or campaign
		System.out.println("Starting execute of handler!");
		return super.execute(event);

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
