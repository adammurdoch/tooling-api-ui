package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.events.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BuildEventTree extends JPanel implements ProgressListener {
    private final DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    private final Map<OperationDescriptor, DefaultMutableTreeNode> operations = new HashMap<>();
    private final DefaultTreeModel model = new DefaultTreeModel(rootNode);
    private final SettingsPanel detail = new SettingsPanel();
    private final JLabel eventDisplayName = new JLabel();
    private final JLabel operationName = new JLabel();
    private final JLabel operationDisplayName = new JLabel();
    private final JTree tree = new JTree();

    public BuildEventTree() {
        setLayout(new BorderLayout());

        tree.setModel(model);
        tree.setCellRenderer(new Renderer());
        reset();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.addTreeSelectionListener(e -> {
            if (e.isAddedPath()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
                Object object = node.getUserObject();
                if (object instanceof StartEvent) {
                    StartEvent event = (StartEvent) object;
                    eventDisplayName.setText(event.getDisplayName());
                    operationName.setText(event.getDescriptor().getName());
                    operationDisplayName.setText(event.getDescriptor().getDisplayName());
                    return;
                } else if (object instanceof FinishEvent) {
                    FinishEvent event = (FinishEvent) object;
                    eventDisplayName.setText(event.getDisplayName());
                    operationName.setText(event.getDescriptor().getName());
                    operationDisplayName.setText(event.getDescriptor().getDisplayName());
                    return;
                }
            }
            eventDisplayName.setText("");
            operationName.setText("");
            operationDisplayName.setText("");
        });

        detail.addControl("Event display name", eventDisplayName);
        detail.addControl("Operation name", operationName);
        detail.addControl("Operation display name", operationDisplayName);

        add(new JScrollPane(tree), BorderLayout.CENTER);
        add(detail, BorderLayout.SOUTH);
    }

    /**
     * Can be invoked from Swing thread only.
     */
    @Override
    public void statusChanged(ProgressEvent event) {
        updateUi(event);
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
            operationNode = new DefaultMutableTreeNode(event);
            operations.put(event.getDescriptor(), operationNode);
            model.insertNodeInto(operationNode, parentNode, parentNode.getChildCount());
            tree.expandPath(new TreePath(parentNode.getPath()));
        } else if (event instanceof FinishEvent) {
            if (!operations.containsKey(event.getDescriptor())) {
                throw new RuntimeException("Unexpected finish event.");
            }
            FinishEvent finishEvent = (FinishEvent) event;
            operationNode = operations.get(event.getDescriptor());
            operationNode.setUserObject(finishEvent);
            model.nodeChanged(operationNode);
        } else {
            // Ignore
            return;
        }

        tree.scrollPathToVisible(new TreePath(operationNode.getPath()));
    }

    private static class Renderer extends DefaultTreeCellRenderer {
        public Renderer() {
            ImageIcon icon = new ImageIcon(getClass().getResource("/gradle-icon-16x16.png"));
            setLeafIcon(icon);
            setClosedIcon(icon);
            setOpenIcon(icon);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object object = node.getUserObject();
                if (object instanceof StartEvent) {
                    StartEvent event = (StartEvent) object;
                    setText(event.getDescriptor().getName() + " (running)");
                } else if (object instanceof FinishEvent) {
                    FinishEvent event = (FinishEvent) object;
                    if (event.getResult() instanceof FailureResult) {
                        setText(event.getDescriptor().getName() + " (FAILED)");
                    } else if (event.getResult() instanceof SkippedResult) {
                        setText(event.getDescriptor().getName() + " (skipped)");
                    } else if (event.getResult() instanceof SuccessResult) {
                        setText(event.getDescriptor().getName());
                    } else {
                        setText(event.getDescriptor().getName() + " (unknown result)");
                    }
                }
            }
            return this;
        }
    }
}
