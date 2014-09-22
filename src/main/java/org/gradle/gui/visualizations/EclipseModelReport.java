package org.gradle.gui.visualizations;

import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;

import java.io.PrintWriter;

public class EclipseModelReport extends Report<EclipseProject> {
    @Override
    public String getDisplayName() {
        return "Eclipse model";
    }

    protected void render(EclipseProject project, PrintWriter output) {
        output.println();
        output.println("PROJECT");
        output.format("%s (%s)%n", project.getName(), project);
        output.format("build script: %s%n", project.getGradleProject().getBuildScript().getSourceFile());
        output.println();

        output.println("SOURCE DIRECTORIES");
        for (EclipseSourceDirectory sourceDirectory : project.getSourceDirectories()) {
            output.format("%s -> %s%n", sourceDirectory.getPath(), sourceDirectory.getDirectory());
        }
        output.println();

        output.println("CLASSPATH");
        for (ExternalDependency dependency : project.getClasspath()) {
            output.format("%s -> %s%n", dependency.getGradleModuleVersion(), dependency.getFile());
        }
        output.println();

        output.println("TASKS");
        for (Task task : project.getGradleProject().getTasks()) {
            output.format("%s (%s)%n", task.getName(), task);
        }
        output.println();

        for (EclipseProject childProject : project.getChildren()) {
            render(childProject, output);
        }
    }
}
