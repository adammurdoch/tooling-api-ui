package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import javax.swing.*;

public class ProjectTree implements Visualization<GradleBuild> {
    private final JTreeBackedVisitor tree = new JTreeBackedVisitor("Builds");

    @Override
    public String getDisplayName() {
        return "Builds";
    }

    @Override
    public JComponent getMainComponent() {
        return tree.getTree();
    }

    public void update(GradleBuild gradleBuild) {
        tree.reset();
        visit(gradleBuild);
        for (GradleBuild build : gradleBuild.getIncludedBuilds()) {
            visit(build);
        }
    }

    private void visit(GradleBuild gradleBuild) {
        tree.node("Build " + gradleBuild.getBuildIdentifier().getRootDir());
        tree.startChildren();
        visit(gradleBuild.getRootProject());
        tree.endChildren();
    }

    private void visit(BasicGradleProject project) {
        tree.node(String.format("Project %s (%s)", project.getName(), project.getPath()));
        tree.startChildren();
        for (BasicGradleProject childProject : project.getChildren()) {
            visit(childProject);
        }
        tree.endChildren();
    }
}
