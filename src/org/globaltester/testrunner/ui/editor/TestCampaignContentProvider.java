package org.globaltester.testrunner.ui.editor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.globaltester.interfaces.ITreeChangeListener;
import org.globaltester.interfaces.ITreeObservable;
import org.globaltester.testrunner.GtTestCampaignProject;
import org.globaltester.testrunner.testframework.IExecution;
import org.globaltester.testrunner.testframework.TestCampaign;

/**
 * ContentProvider for TreeViewer in TestCampaignEditor. Provides content
 * elements of TestCampaign and its possible children. The returned elements
 * will always represent the most recent IExecution of the given elements.
 * 
 * @author amay
 * 
 */
public class TestCampaignContentProvider implements ITreeContentProvider,
		ITreeChangeListener {
	private Map<ITreeObservable, Set<Viewer>> listenerMapping = new HashMap<ITreeObservable, Set<Viewer>>();

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof GtTestCampaignProject)
			return new Object[] { ((GtTestCampaignProject) parentElement)
					.getTestCampaign() };
		if ((parentElement instanceof IExecution)
				&& ((IExecution) parentElement).hasChildren())
			return ((IExecution) parentElement).getChildren().toArray();
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof TestCampaign)
			return ((TestCampaign) element).getProject();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof GtTestCampaignProject)
			return true;
		if (element instanceof IExecution)
			return ((IExecution) element).hasChildren();
		return false;
	}

	@Override
	public Object[] getElements(Object parent) {
		return getChildren(parent);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// deregister this listener from oldInput
		if (oldInput instanceof ITreeObservable) {
			((ITreeObservable) oldInput).removeTreeChangeListener(this);

			// remove viewer from listenerMapping
			if (listenerMapping.containsKey(oldInput)) {
				Set<Viewer> set = listenerMapping.get(oldInput);
				if ((set != null) && set.contains(viewer)) {
					set.remove(viewer);
				}
				if ((set == null) || set.isEmpty()) {
					listenerMapping.remove(oldInput);
				}
			}
		}

		// register this listener on newInput
		if (newInput instanceof ITreeObservable) {
			((ITreeObservable) newInput).addTreeChangeListener(this);

			// add viewer to listenerMapping to enable propagation of change
			if (listenerMapping.containsKey(newInput)
					&& (listenerMapping.get(newInput) != null)) {
				listenerMapping.get(newInput).add(viewer);
			} else {
				HashSet<Viewer> set = new HashSet<Viewer>();
				set.add(viewer);
				listenerMapping.put((ITreeObservable) newInput, set);
			}
		}
	}

	@Override
	public void notifyTreeChange(Object notifier, boolean structureChanged,
			Object[] changedElements, String[] properties) {
		if (listenerMapping.containsKey(notifier)) {
			Iterator<Viewer> viewerIter = listenerMapping.get(notifier)
					.iterator();

			while (viewerIter.hasNext()) {
				Viewer viewer = (Viewer) viewerIter.next();

				if ((structureChanged) || !(viewer instanceof StructuredViewer)) {
					viewer.refresh();
				} else {
					((StructuredViewer) viewer).update(changedElements,
							properties);
				}
			}
		}

	}
}