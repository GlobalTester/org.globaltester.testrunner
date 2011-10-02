package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.testframework.TestCampaignElement;
import org.globaltester.testrunner.testframework.TestExecution;

public class TestCampaignTableLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof TestCampaign)
				return ((TestCampaign) element).getName();
			if (element instanceof TestCampaignElement)
				return ((TestCampaignElement) element).getExecutable()
						.getName();
			return element.toString();
		case 1: // Result
			break;
		case 2: // Last executed
			if (element instanceof TestCampaignElement) {
				TestExecution lastExec = ((TestCampaignElement) element)
						.getLastExecution();
				if (lastExec != null) {
					return lastExec.getTime();
				}
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
		if ((element instanceof TestCampaignElement)&&("lastExecution".equals(property))) return true;
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}
}
