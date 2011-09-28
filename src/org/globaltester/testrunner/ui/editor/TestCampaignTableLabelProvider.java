package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.testframework.TestCampaign;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor.DummyResult;
import org.globaltester.testspecification.testframework.TestCase;

public class TestCampaignTableLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			if (element instanceof TestCampaign)
				return ((TestCampaign) element).getName();
			if (element instanceof TestCase)
				return ((TestCase) element).getTestCaseID();
			return element.toString();
		case 1:
			if (element instanceof DummyResult)
				return ((DummyResult) element).getResult();
			break;
		case 2:
			if (element instanceof DummyResult)
				return ((DummyResult) element).getRemarks();
			if (element instanceof TestCase)
				return ((TestCase) element).getIFile().getFullPath().toOSString();
		}
		return null;
	}

	public void addListener(ILabelProviderListener listener) {
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
	}
}
