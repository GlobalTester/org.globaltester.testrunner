package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Tree;

public class SilentTextCellEditor extends TextCellEditor {
	public SilentTextCellEditor(Tree tree) {
		super(tree);
	}

	//FIXME: REMOVE! this class and fix the assertion error while selecting for the first time
	@Override
	protected void doSetValue(Object value) {
		if (value != null){
			super.doSetValue(value);
		}
	}
}
