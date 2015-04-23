package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.eclipse.EclipseProject;

import javax.swing.*;

public class EclipseModelReport implements Visualization<EclipseProject> {
    private final JTreeBackedStructureVisitor tree = new JTreeBackedStructureVisitor("Eclipse model");

    @Override
    public String getDisplayName() {
        return "Eclipse model";
    }

    @Override
    public JComponent getMainComponent() {
        return tree.getTree();
    }

    @Override
    public void update(EclipseProject project) {
        tree.reset();
        render(project);
    }

    private void render(EclipseProject project) {
        tree.struct("Project", project.getName(), () -> {
            tree.collection("Dependencies", project.getProjectDependencies(), dependency -> {
                tree.value(dependency.getTargetProject().getName());
            });
            tree.collection("Classpath", project.getClasspath(), entry -> {
                tree.value(entry.getGradleModuleVersion());
            });
            tree.collection("Children", project.getChildren(), child -> render(child));
        });
    }
}
