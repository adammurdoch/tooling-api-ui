package org.gradle.sample;

import org.gradle.gui.UIContext;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;

import java.io.PrintStream;

class FetchEclipseModel implements UI.ToolingOperation<Void> {
    @Override
    public String getDisplayName(UIContext uiContext) {
        return "fetch Eclipse model";
    }

    @Override
    public Void run(ProjectConnection connection, UIContext uiContext) {
        ModelBuilder<EclipseProject> model = connection.model(EclipseProject.class);
        uiContext.setup(model);
        EclipseProject project = model.get();
        show(project, uiContext.getConsoleStdOut());
        return null;
    }

    private void show(EclipseProject project, PrintStream output) {
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
            show(childProject, output);
        }
    }
}
