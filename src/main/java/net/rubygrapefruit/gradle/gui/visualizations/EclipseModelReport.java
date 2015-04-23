package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.eclipse.EclipseProject;

public class EclipseModelReport extends Report<EclipseProject> {
    public EclipseModelReport() {
        super("Eclipse model");
    }

    @Override
    protected void render(EclipseProject project, StructureVisitor tree) {
        tree.struct("Project", project.getName(), () -> {
            tree.collection("Dependencies", project.getProjectDependencies(), dependency -> {
                tree.value(dependency.getTargetProject().getName());
            });
            tree.collection("Classpath", project.getClasspath(), entry -> {
                tree.value(entry.getGradleModuleVersion());
            });
            tree.collection("Children", project.getChildren(), child -> render(child, tree));
        });
    }
}
