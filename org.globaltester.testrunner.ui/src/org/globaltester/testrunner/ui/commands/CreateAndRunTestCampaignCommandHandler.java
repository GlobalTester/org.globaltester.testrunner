package org.globaltester.testrunner.ui.commands;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

/**
 * This {@link IHandler} implementation combines the
 * {@link CreateTestCampaignCommandHandler} with the
 * {@link RunTestCommandHandler} to create a TestCampaign and run it directly
 * afterwards.
 * 
 * @author mboonk
 *
 */
public class CreateAndRunTestCampaignCommandHandler extends CreateTestCampaignCommandHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		super.execute(event);
		return new RunTestCommandHandler().execute(event);
	}
}
