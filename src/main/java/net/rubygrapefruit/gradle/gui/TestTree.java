package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.events.*;
import org.gradle.tooling.events.test.TestProgressEvent;
import org.gradle.tooling.events.test.TestProgressListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.Map;

public class TestTree extends JTree implements TestProgressListener {
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("<no tests>");
    private final Map<OperationDescriptor, DefaultMutableTreeNode> tests = new HashMap<>();
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

    private void onTestEvent(ProgressEvent event) {
        DefaultMutableTreeNode testNode;
        if (event instanceof StartEvent) {
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
        } else if (event instanceof FinishEvent) {
            if (!tests.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Unexpected test event.");
            }
            FinishEvent finishEvent = (FinishEvent) event;
            testNode = tests.get(event.getDescriptor());
            if (finishEvent.getResult() instanceof FailureResult) {
                testNode.setUserObject(event.getDescriptor().getName() + " (FAILED)");
            } else if (finishEvent.getResult() instanceof SkippedResult) {
                testNode.setUserObject(event.getDescriptor().getName() + " (skipped)");
            } else if (finishEvent.getResult() instanceof SuccessResult) {
                testNode.setUserObject(event.getDescriptor().getName() + " (passed)");
            } else {
                testNode.setUserObject(event.getDescriptor().getName() + " (unknown result)");
            }
            model.nodeChanged(testNode);
        } else {
            // Ignore
            return;
        }

        scrollPathToVisible(new TreePath(testNode.getPath()));
    }
}
