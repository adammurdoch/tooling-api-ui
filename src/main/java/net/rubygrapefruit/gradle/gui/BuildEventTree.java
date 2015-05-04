package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.events.*;
import org.gradle.tooling.events.build.BuildProgressEvent;
import org.gradle.tooling.events.build.BuildProgressListener;
import org.gradle.tooling.events.task.TaskProgressEvent;
import org.gradle.tooling.events.task.TaskProgressListener;
import org.gradle.tooling.events.test.TestProgressEvent;
import org.gradle.tooling.events.test.TestProgressListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.HashMap;
import java.util.Map;

public class BuildEventTree extends JTree implements TestProgressListener, TaskProgressListener, BuildProgressListener {
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private final Map<OperationDescriptor, DefaultMutableTreeNode> operations = new HashMap<>();
    private final DefaultTreeModel model = new DefaultTreeModel(rootNode);

    public BuildEventTree() {
        setModel(model);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        ImageIcon icon = new ImageIcon(getClass().getResource("/gradle-icon-16x16.png"));
        renderer.setLeafIcon(icon);
        renderer.setClosedIcon(icon);
        renderer.setOpenIcon(icon);
        setCellRenderer(renderer);
        reset();
    }

    /**
     * Can be invoked from any thread.
     */
    @Override
    public void statusChanged(BuildProgressEvent event) {
        onEvent(event);
    }

    /**
     * Can be invoked from any thread.
     */
    @Override
    public void statusChanged(TaskProgressEvent event) {
        onEvent(event);
    }

    /**
     * Can be invoked from any thread.
     */
    @Override
    public void statusChanged(TestProgressEvent event) {
        onEvent(event);
    }

    private void onEvent(ProgressEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            updateUi(event);
        }
        else {
            SwingUtilities.invokeLater(() -> updateUi(event));
        }
    }

    public void reset() {
        rootNode.setUserObject("<no events>");
        rootNode.removeAllChildren();
        model.reload(rootNode);
    }

    private void updateUi(ProgressEvent event) {
        DefaultMutableTreeNode operationNode;
        if (event instanceof StartEvent) {
            if (operations.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Duplicate start event.");
            }

            rootNode.setUserObject("Build events");
            model.nodeChanged(rootNode);
            DefaultMutableTreeNode parentNode = event.getDescriptor().getParent() == null ? rootNode : operations.get(event.getDescriptor().getParent());
            operationNode = new DefaultMutableTreeNode(event.getDescriptor().getName() + " (running)");
            operations.put(event.getDescriptor(), operationNode);
            model.insertNodeInto(operationNode, parentNode, parentNode.getChildCount());
            setExpandedState(new TreePath(parentNode.getPath()), true);
        } else if (event instanceof FinishEvent) {
            if (!operations.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Unexpected finish event.");
            }
            FinishEvent finishEvent = (FinishEvent) event;
            operationNode = operations.get(event.getDescriptor());
            if (finishEvent.getResult() instanceof FailureResult) {
                operationNode.setUserObject(event.getDescriptor().getName() + " (FAILED)");
            } else if (finishEvent.getResult() instanceof SkippedResult) {
                operationNode.setUserObject(event.getDescriptor().getName() + " (skipped)");
            } else if (finishEvent.getResult() instanceof SuccessResult) {
                operationNode.setUserObject(event.getDescriptor().getName() + " (passed)");
            } else {
                operationNode.setUserObject(event.getDescriptor().getName() + " (unknown result)");
            }
            model.nodeChanged(operationNode);
        } else {
            // Ignore
            return;
        }

        scrollPathToVisible(new TreePath(operationNode.getPath()));
    }
}
