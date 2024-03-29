package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.testframework.AbstractTestExecution;
import org.globaltester.testrunner.testframework.ActionStepExecution;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.Result;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignExecution;
import org.globaltester.testrunner.testframework.TestCaseExecution;
import org.globaltester.testrunner.ui.NonUiImages;
import org.globaltester.testrunner.ui.UiImages;

public class TestExecutionResultLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof TestCampaignExecution)
				return UiImages.CAMPAIGN_TESTSUITE_ICON.getImage();
			if (element instanceof TestCampaign)
				return UiImages.CAMPAIGN_TESTSUITE_ICON.getImage();
			if (element instanceof TestCaseExecution)
				return UiImages.CAMPAIGN_TESTCASE_ICON.getImage();
			if (element instanceof ActionStepExecution)
				return UiImages.CAMPAIGN_TESTSTEP_ICON.getImage();
			break;
		case 2: // Status
			if (element instanceof AbstractTestExecution)
				return NonUiImages.valueOf(
						((AbstractTestExecution) element).getResult().getStatus())
						.getImage();
			break;
		}

		return null;
	}
	
	public static String findFirstComment(Result result) {
		if (result.getComment() != null && !result.getComment().isEmpty()) {
			return result.getComment();
		}
		for (Result s : result.getSubResults()) {
			String r = findFirstComment(s);
			if (r != null && !r.isEmpty()) return r;
		}
		return "";
	}

	public String getColumnText(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof IExecution)
				return ((IExecution) element).getId();
			return element.toString();
		case 1: // Last executed
			if (element instanceof AbstractTestExecution) {
				return ((AbstractTestExecution) element).getLastExecutionStartTimeAsString();
			}
			break;
		case 2: // Status
			if (element instanceof AbstractTestExecution)
				return ((AbstractTestExecution) element).getResult().getStatus()
						.toString();
			break;
		case 3: // Comment
			if (element instanceof AbstractTestExecution) {
				return findFirstComment(((AbstractTestExecution) element).getResult());
			}
			break;
		}
		return null;
	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
	}
}
