package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor.DummyResult;

public class TestCampaignTableLabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {
		switch (columnIndex) {
		case 0:
			return element.toString();
		case 1:
			if (element instanceof DummyResult)
				return ((DummyResult) element).getResult();
		case 2:
			if (element instanceof DummyResult)
				return ((DummyResult) element).getRemarks();
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
