package org.globaltester.testrunner;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;

public interface TestResourceExecutor {
	public Object execute(List<IResource> resources, Map<?,?> map);
}
