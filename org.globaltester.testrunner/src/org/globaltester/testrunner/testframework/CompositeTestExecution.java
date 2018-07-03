package org.globaltester.testrunner.testframework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.globaltester.testrunner.testframework.Result.Status;
import org.jdom.Element;

public abstract class CompositeTestExecution extends AbstractTestExecution implements ResultChangeListener {
	
	private static final String XML_CHILD_CHILDREN = "Children";
	private static final String XML_CHILD_CHILDREF = "Reference";
	private static final String XML_CHILD_CHILDINLINE = "Inline";

	protected List<IExecution> childExecutions = new ArrayList<>();

	public CompositeTestExecution() {
		super();
	}

	public void addChildExecution(IExecution tcExecution) {
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
		for (Iterator<IExecution> execIter = childExecutions.iterator(); execIter
				.hasNext();) {
			duration += execIter.next().getDuration();
		}

		return duration;
	}
	
	@Override
	public void resultChanged(IExecution changedObject) {
		notifyResultChangeListeners(changedObject);
	}
	
	@Override
	public void extractFromXml(Element root) throws CoreException {
		super.extractFromXml(root);
		

		//extract children
		Element fileNames = root.getChild(XML_CHILD_CHILDREN);
		if (fileNames != null) {
			@SuppressWarnings("unchecked")
			List<Element> children = fileNames.getChildren();
			
			int childIndex = 0;
			for (Element curChildElem : children) {
				IExecution exec = null;
				
				if  (XML_CHILD_CHILDREF.equals(curChildElem.getName())) {
					exec = extractChildReference(curChildElem);
				} else if  (XML_CHILD_CHILDINLINE.equals(curChildElem.getName())) {
					exec = extractChildInline(curChildElem, childIndex++);
				}
				
				if (exec != null) {
					addChildExecution(exec);
					result.addSubResult(exec.getResult());
				}

			}
			
			Element status = root.getChild("LastExecutionResult").getChild("Status");
			result.status = Result.Status.get(status.getTextTrim());
		}
		
	}

	private IExecution extractChildReference(Element curChildElem) throws CoreException {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IPath fileName = new Path(curChildElem.getTextTrim());
		IFile curChildIFile = workspaceRoot.getFile(fileName);
		
		IExecution exec = FileTestExecutionFactory
				.getInstance(curChildIFile);
		return exec;
	}

	private IExecution extractChildInline(Element curChildElem, int childIndex) throws CoreException {
		return factoryMethod((Element) curChildElem.getChildren().iterator().next(), childIndex);
	}
	
	private IExecution factoryMethod(Element xmlElement, int childIndex) throws CoreException {
		switch (xmlElement.getName()) {
		case PreConditionExecution.XML_ELEMENT:
			return new PreConditionExecution(this, childIndex, xmlElement);
		case PostConditionExecution.XML_ELEMENT:
			return new PostConditionExecution(this, childIndex, xmlElement);
		case TestStepExecution.XML_ELEMENT:
			return new TestStepExecution(this, childIndex, xmlElement);

		default:
			return null;
		}
	}

	@Override
	public void dumpToXml(Element root) {
		super.dumpToXml(root);
		
		// dump references to children
		Element xmlChildren = new Element(XML_CHILD_CHILDREN);
		root.addContent(xmlChildren);

		Iterator<IExecution> iter = this.getChildren().iterator();
		while (iter.hasNext()) {
			IExecution curChild = iter.next();
			if (curChild instanceof FileTestExecution) {
				Element xmlChildRef = new Element(XML_CHILD_CHILDREF);
				xmlChildRef.addContent(((FileTestExecution) curChild).getIFile().getFullPath().toString());
				xmlChildren.addContent(xmlChildRef);
			} else {
				Element xmlChildInline = new Element(XML_CHILD_CHILDINLINE);
				xmlChildren.addContent(xmlChildInline);
				
				Element xmlChild = new Element(curChild.getXmlRootElementName());
				curChild.dumpToXml(xmlChild);
				xmlChildInline.addContent(xmlChild);
			}

		}

	}

	public void doSaveChildren() {
		for (IExecution curElemExecution : childExecutions) {
			if (curElemExecution instanceof FileTestExecution) {
				((FileTestExecution) curElemExecution).doSave();
			}
		}
	}

	public long getNumberOfExecutedTests() {
		return childExecutions.stream().filter(IExecution::isExecuted).count();
	}

	public long getNumberOfTestsWithStatus(Status expectedStatus) {
		return childExecutions.stream().filter(exec -> exec.getStatus() == expectedStatus).count();
	}

}