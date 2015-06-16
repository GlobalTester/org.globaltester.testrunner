/**
 * 
 */
package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.globaltester.core.ui.GtUiHelper;
import org.globaltester.smartcardshell.RhinoJavaScriptAccess;
import org.globaltester.smartcardshell.ui.RhinoDebugLaunchManager;

/**
 * Subclass of RunTestCommandHandler, but with different default setting for
 * debugMode (true). Besides this the handler organizes the launching of the
 * Rhino debugger.
 * 
 * @see #execute(org.eclipse.core.commands.ExecutionEvent)
 * 
 * @author koelzer
 * 
 */
public class DebugTestCommandHandler extends RunTestCommandHandler {

	public DebugTestCommandHandler() {
		super();
		setDebugMode(true); // enable JavaScript debugging
	}

	/**
	 * Tries to launch the Rhino JavaScript debugger in an own thread and
	 * concurrently executes the execute method of the super class. The debugger
	 * launch thread has to wait until the concurrent thread set up the Rhino
	 * debugging thread since these two threads communicate with each other and
	 * the debugging thread must be started before the launch thread.
	 * 
	 * @see org.globaltester.testrunner.ui.commands.RunTestCommandHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		final RhinoDebugLaunchManager launchMan = new RhinoDebugLaunchManager();
		try {
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

		new Thread() {
			@Override
			public void run() {
				try {
					int count = 0;
					while (count <= 3) {
						// wait for Rhino debugger to be started
						if (RhinoJavaScriptAccess.getDebuggerStartedObj() == null) {
							Thread.sleep(1703);
							count++;
						} else {
							//TODO use fo final object OK??
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
		}.start();

		// concurrently continue standard execution
		return super.execute(event);

	}
}
