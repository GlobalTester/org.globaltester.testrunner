package org.globaltester.testrunner.ui;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.logging.legacy.logger.GtErrorLogger;
import org.globaltester.scriptrunner.ui.TestResourceExecutorUi;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.osgi.framework.Bundle;

public class TestRunnerExecutorUi implements TestResourceExecutorUi {

	@Override
	public void show(List<IResource> resources) {
		// open the new TestCampaign in the Test Campaign Editor
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				try {
					//clean the result view
					IViewPart resultView = null;
					IViewReference viewReferences[] = PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage().getViewReferences();
					for (int i = 0; i < viewReferences.length; i++) {
						if ("org.globaltester.testmanager.views.ResultView".equals(viewReferences[i].getId())) {
							resultView = viewReferences[i].getView(false);
						}
					}
					if(resultView != null) {
						try {
							Bundle testmanagerBundle = Platform.getBundle("org.globaltester.testmanager");
							Class<?> resultViewClass = testmanagerBundle.loadClass("org.globaltester.testmanager.views.ResultView");
							Method reset = resultViewClass.getDeclaredMethod("reset");
							reset.invoke(resultView);
						} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
							e.printStackTrace();
						}
					}
					
					//actually open campaign in editor
					GtTestCampaignProject campaign = GtTestCampaignProject
							.getProjectForResource(resources.iterator().next());
					GtUiHelper.openInEditor(campaign.getTestCampaignIFile());
				} catch (CoreException e) {
					// log Exception to eclipse log
					GtErrorLogger.log(Activator.PLUGIN_ID, e);

					// users most probably will ignore this
					// behavior and open editor manually, so do
					// not open annoying dialog
				}
			}
		});

	}

	@Override
	public boolean canShow(List<IResource> resources) {
		return new TestRunnerExecutor().canExecute(resources);
	}

}
