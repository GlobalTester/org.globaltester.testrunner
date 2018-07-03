package org.globaltester.testrunner.ui.editor;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;
import org.globaltester.base.resources.GtResourceHelper;
import org.globaltester.base.ui.DialogOptions;
import org.globaltester.base.ui.GtUiHelper;
import org.globaltester.testrunner.preferences.PreferenceConstants;
import org.globaltester.testrunner.report.ReportCsvGenerator;
import org.globaltester.testrunner.report.ReportJunitGenerator;
import org.globaltester.testrunner.report.ReportPdfGenerator;
import org.globaltester.testrunner.report.TestReport;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestSetExecution;
import org.globaltester.testrunner.ui.Activator;

public class ReportGenerationJob extends Job {

	private AbstractTestExecution execution;
	private String reportDir = null;
	protected Shell shell;

	public ReportGenerationJob(AbstractTestExecution execution, Shell shell) {
		super("Generate TestReport");
		this.execution = execution;
		this.shell = shell;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {

		monitor.beginTask("Export report", 6);

		monitor.subTask("Select directory");
		String reportDir = getReportDir();
		if (reportDir == null) {
			IStatus status = new Status(Status.OK, Activator.PLUGIN_ID,
					"User aborted report generation");
			return status;
		}
		monitor.worked(1);

		monitor.subTask("Prepare report data");
		TestReport report = null;
		if (execution instanceof TestCampaignExecution) {
			report = new TestReport((TestCampaignExecution) execution, reportDir);
		} else if (execution instanceof TestSetExecution) {
			report = new TestReport((TestSetExecution) execution, reportDir);
		} else {
			IStatus status = new Status(Status.ERROR, Activator.PLUGIN_ID,
					"TestReport could not be created from this type of execution");
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return status;
		}
		monitor.worked(1);

		try {
			monitor.subTask("Create CSV report");
			ReportCsvGenerator.writeCsvReport(report);
			monitor.worked(1);
			monitor.subTask("Create PDF report");
			ReportPdfGenerator.writePdfReport(report);
			monitor.worked(1);
			monitor.subTask("Create JUnit report");
			ReportJunitGenerator.writeJUnitReport(report);
			monitor.worked(1);
			GtResourceHelper.copyFilesToDir(report.getLogFiles(), report.getReportDir().getAbsolutePath());
			monitor.worked(1);

			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openInformation(null, "TestReport", "TestReport exported successfully.");
				}
			});

		} catch (IOException ex) {
			IStatus status = new Status(Status.ERROR, Activator.PLUGIN_ID, "TestReport could not be created", ex);
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return status;
		}

		monitor.done();
		return new Status(IStatus.OK, Activator.PLUGIN_ID, "Export successfull.");
	}

	public String getReportDir() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		boolean staticReportDir = store.getBoolean(PreferenceConstants.P_REPORT_USE_FIXED_DIR);
		
		if (staticReportDir) {
			reportDir = store.getString(PreferenceConstants.P_REPORT_DIR);
		}else {
			reportDir = getReportDirFromUser();
		}
		
		return reportDir;
	}

	public String getReportDirFromUser() {
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
			
			@Override
			public void run() {
				if (shell == null) {
					shell = PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
				}
				
				// ask user for report location
				DialogOptions dialogOptions = new DialogOptions();
				dialogOptions.setMessage("Please select location to store the report files");
				dialogOptions.setFilterPath(null); // do not filter at all
				reportDir  = GtUiHelper.openDirectoryDialog(shell, dialogOptions);

				if (reportDir != null) {
					// check if file exists
					File baseDir = new File(reportDir);
					if (baseDir.list().length > 0) {
						String message = "The selected destination folder is not empty, proceed?";
						if (!MessageDialog.openConfirm(shell, "Warning", message)) {
							reportDir = null;
						}
					}

				}				
			}
		});
		return reportDir;
	}
}
