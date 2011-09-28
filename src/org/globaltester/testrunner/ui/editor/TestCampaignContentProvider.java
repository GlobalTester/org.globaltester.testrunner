package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.TestCampaign;

public class TestCampaignContentProvider implements ITreeContentProvider {
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GtTestCampaignProject)
			return new Object[]{((GtTestCampaignProject)parentElement).getTestCampaign()};
		if (parentElement instanceof TestCampaign)
			return ((TestCampaign)parentElement).getTestExecutables().toArray();
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof TestCampaign)
			return ((TestCampaign) element).getProject();
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof GtTestCampaignProject)
			return true;
		if (element instanceof TestCampaign)
			return !((TestCampaign)element).getTestExecutables().isEmpty();
		return false;
	}

	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}