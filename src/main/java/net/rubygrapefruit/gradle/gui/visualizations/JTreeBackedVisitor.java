package net.rubygrapefruit.gradle.gui.visualizations;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.LinkedList;

public class JTreeBackedVisitor implements TreeVisitor {
    private final DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    private final DefaultTreeModel model = new DefaultTreeModel(root);
    private final JTree tree = new JTree();
    private final LinkedList<DefaultMutableTreeNode> stack = new LinkedList<>();
    private DefaultMutableTreeNode parent;
    private DefaultMutableTreeNode current;

    public JTreeBackedVisitor(String name) {
        root.setUserObject(name);
        tree.setModel(model);
        parent = root;
    }

    public JTree getTree() {
        return tree;
    }

    public void reset() {
        root.removeAllChildren();
        model.reload(root);
        parent = root;
    }

    @Override
    public void node(Object node) {
        current = new DefaultMutableTreeNode(node);
        model.insertNodeInto(current, parent, parent.getChildCount());
        tree.expandPath(new TreePath(parent.getPath()));
    }

    @Override
    public void startChildren() {
        stack.addLast(parent);
        parent = current;
        current = null;
    }

    @Override
    public void endChildren() {
        parent = stack.removeLast();
        current = null;
    }
}
