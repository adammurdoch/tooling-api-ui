package org.gradle.gui;

import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class ProjectTree implements Visualization<GradleBuild> {
    JTree tree;

    public ProjectTree() {
        this.tree = new JTree();
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
    }

    @Override
    public String getDisplayName() {
        return "Projects";
    }

    @Override
    public JComponent getMainComponent() {
        return tree;
    }

    public void update(GradleBuild gradleBuild) {
        tree.setModel(new DefaultTreeModel(toNode(gradleBuild.getRootProject())));
    }

    private MutableTreeNode toNode(BasicGradleProject project) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode();
        node.setUserObject(String.format("Project %s", project.getName()));
        for (BasicGradleProject childProject : project.getChildren()) {
            node.add(toNode(childProject));
        }
        return node;
    }
}
