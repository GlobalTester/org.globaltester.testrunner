package org.globaltester.testrunner.ui.editor;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor.DummyResult;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor.DummyTestCase;
import org.globaltester.testrunner.ui.editor.TestCampaignEditor.DummyTestStep;

public class TestCampaignContentProvider implements ITreeContentProvider {
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof List)
			return ((List<?>) parentElement).toArray();
		if (parentElement instanceof DummyTestCase)
			return ((DummyTestCase) parentElement).getSteps();
		if (parentElement instanceof DummyTestStep)
			return ((DummyTestStep) parentElement).getResults();
		return new Object[0];
	}

	public Object getParent(Object element) {
		if (element instanceof DummyTestStep)
			return ((DummyTestStep) element).parentTestCase;
		if (element instanceof DummyResult)
			return ((DummyResult) element).name;
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof List)
			return ((List<?>) element).size() > 0;
		if (element instanceof DummyTestCase)
			return ((DummyTestCase) element).getSteps().length > 0;
		if (element instanceof DummyTestStep)
			return ((DummyTestStep) element).getResults().length > 0;
		return false;
	}

	public Object[] getElements(Object cities) {
		return getChildren(cities);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}