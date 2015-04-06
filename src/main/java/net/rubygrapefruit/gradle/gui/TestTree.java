package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.Map;

public class TestTree extends JTree implements TestProgressListener {
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("<no tests>");
    private final Map<TestDescriptor, DefaultMutableTreeNode> tests = new HashMap<>();
    private final DefaultTreeModel model = new DefaultTreeModel(rootNode);

    public TestTree() {
        setModel(model);
    }

    /**
     * Can be invoked from any thread.
     */
    @Override
    public void statusChanged(TestProgressEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            onTestEvent(event);
        }
        else {
            SwingUtilities.invokeLater(() -> onTestEvent(event));
        }
    }

    public void reset() {
        rootNode.setUserObject("<no tests>");
        rootNode.removeAllChildren();
        model.reload(rootNode);
    }

    private void onTestEvent(TestProgressEvent event) {
        DefaultMutableTreeNode testNode;
        if ((event instanceof TestStartedEvent) || (event instanceof TestSuiteStartedEvent)) {
            if (tests.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Duplicate test start event.");
            }

            rootNode.setUserObject("Tests");
            model.nodeChanged(rootNode);
            DefaultMutableTreeNode parentNode = event.getDescriptor().getParent() == null ? rootNode : tests.get(
                    event.getDescriptor().getParent());
            testNode = new DefaultMutableTreeNode(event.getDescriptor().getName() + " (running)");
            tests.put(event.getDescriptor(), testNode);
            model.insertNodeInto(testNode, parentNode, parentNode.getChildCount());
            setExpandedState(new TreePath(parentNode.getPath()), true);
        } else {
            if (!tests.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Unexpected test event.");
            }
            testNode = tests.get(event.getDescriptor());
            if (event instanceof TestFailedEvent || event instanceof TestSuiteFailedEvent) {
                testNode.setUserObject(event.getDescriptor().getName() + " (FAILED)");
            } else if (event instanceof TestSkippedEvent || event instanceof TestSuiteSkippedEvent) {
                testNode.setUserObject(event.getDescriptor().getName() + " (skipped)");
            } else {
                testNode.setUserObject(event.getDescriptor().getName() + " (passed)");
            }
            model.nodeChanged(testNode);
        }

        scrollPathToVisible(new TreePath(testNode.getPath()));
    }
}
