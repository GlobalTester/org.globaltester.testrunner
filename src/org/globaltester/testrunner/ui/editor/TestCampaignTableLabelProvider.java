package org.globaltester.testrunner.ui.editor;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.FileTestExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignElement;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.testframework.TestStepExecution;
import org.globaltester.testrunner.ui.NonUiImages;
import org.globaltester.testrunner.ui.UiImages;

public class TestCampaignTableLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof TestCampaign)
				return UiImages.CAMPAIGN_TESTSUITE_ICON.getImage();
			if (element instanceof TestCampaignElement)
				element = ((TestCampaignElement) element).getLastExecution();
			if (element instanceof TestCaseExecution)
				return UiImages.CAMPAIGN_TESTCASE_ICON.getImage();
			if (element instanceof TestStepExecution)
				return UiImages.CAMPAIGN_TESTSTEP_ICON.getImage();
			break;
		case 2: // Status
			if (element instanceof TestCampaignElement)
				element = ((TestCampaignElement) element).getLastExecution();
			if (element instanceof FileTestExecution)
				return NonUiImages.valueOf(((FileTestExecution)element).getResult().getStatus()).getImage();
			break;
		}
		   
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof IExecution)
				return ((IExecution) element).getName();
			return element.toString();
		case 1: // Last executed
			if (element instanceof TestCampaignElement) {
				AbstractTestExecution lastExec = ((TestCampaignElement) element)
						.getLastExecution();
				if (lastExec != null) {
					return DateFormat.getDateTimeInstance().format(new Date(lastExec.getLastExecutionStartTime()));
				}
			}
			break;
		case 2: // Status
			if (element instanceof TestCampaignElement)
				element = ((TestCampaignElement) element).getLastExecution();
			if (element instanceof FileTestExecution)
				return ((FileTestExecution)element).getResult().getStatus().toString();
			break;
		case 3: // Comment
			if (element instanceof TestCampaignElement)
				element = ((TestCampaignElement) element).getLastExecution();
			if (element instanceof FileTestExecution)
				return ((FileTestExecution)element).getResult().getComment();
			break;
		}
		return null;
	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		if ((element instanceof TestCampaignElement)&&("lastExecution".equals(property))) return true;
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}
}
