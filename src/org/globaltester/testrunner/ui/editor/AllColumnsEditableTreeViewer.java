package org.globaltester.testrunner.ui.editor;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class AllColumnsEditableTreeViewer extends TreeViewer {
    
    public AllColumnsEditableTreeViewer(Tree tree) {
		super(tree);
	}

	@Override
    protected Item getItemAt(Point p) {
        Tree tree = getTree();
        TreeItem[] selection = tree.getSelection();
        if( selection.length == 1 ) {
            int columnCount = tree.getColumnCount();
 
            for( int i = 0; i < columnCount; i++ ) {
                if( selection[0].getBounds(i).contains(p) ) {
                    return selection[0];
                }
            }
        }
        
        // TreeItem item = tree.getItem(p); <- this checks only the first column if SWT.FULL_SELECTION is not set
        TreeItem item = searchTreeItem(null, p);
        return item;
    }
    
    private TreeItem searchTreeItem(TreeItem parentItem, Point point) {
        TreeItem[] items = (parentItem == null) ? getTree().getItems() : parentItem.getItems();
        int columnCount = getTree().getColumnCount();
        for (int i = 0; i < items.length; i++) {
            for (int c = 0; c < columnCount; c++) {
                if (items[i].getBounds(c).contains(point))
                    return items[i];
            }
            TreeItem foundItem = null;
            if (items[i].getExpanded() == true)
                foundItem = searchTreeItem(items[i], point);
            if (foundItem != null)
                return foundItem;
        }
        return null;
    }
}