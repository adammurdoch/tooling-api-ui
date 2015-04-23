package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

public class ProjectTree implements Visualization<GradleBuild> {
    private final JTreeBackedVisitor<String> tree = new JTreeBackedVisitor<>("Projects");

    @Override
    public String getDisplayName() {
        return "Projects";
    }

    @Override
    public JComponent getMainComponent() {
        return tree.getTree();
    }

    public void update(GradleBuild gradleBuild) {
        tree.reset();
        visit(gradleBuild.getRootProject());
    }

    private void visit(BasicGradleProject project) {
        tree.node(String.format("Project %s", project.getName()));
        tree.startChildren();
        for (BasicGradleProject childProject : project.getChildren()) {
            visit(childProject);
        }
        tree.endChildren();
    }
}
