package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public abstract class CompositeTestExecution extends AbstractTestExecution implements ResultChangeListener {

	protected List<FileTestExecution> childExecutions = new ArrayList<>();

	public CompositeTestExecution() {
		super();
	}

	public void addChildExecution(FileTestExecution tcExecution) {
		childExecutions.add(tcExecution);
		tcExecution.addResultListener(this);
	}

	@Override
	public boolean hasChildren() {
		return !childExecutions.isEmpty();
	}

	@Override
	public Collection<IExecution> getChildren() {
		ArrayList<IExecution> children = new ArrayList<IExecution>();
		children.addAll(childExecutions);
		return children;
	}

	@Override
	public long getDuration() {
		long duration = 0;
		for (Iterator<FileTestExecution> execIter = childExecutions.iterator(); execIter
				.hasNext();) {
			duration += execIter.next().getDuration();
		}

		return duration;
	}
	
	@Override
	public void resultChanged() {
		notifyResultChangeListeners();
	}

}