package org.gradle.gui.actions;

import org.gradle.gui.ToolingOperation;
import org.gradle.gui.UIContext;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.PrintStream;

public class FetchIdeaModel implements ToolingOperation<Void> {
    @Override
    public String getDisplayName(UIContext uiContext) {
        return "fetch IDEA model";
    }

    @Override
    public Void run(ProjectConnection connection, UIContext uiContext) {
        ModelBuilder<IdeaProject> model = connection.model(IdeaProject.class);
        uiContext.setup(model);
        IdeaProject project = model.get();
        show(project, uiContext.getConsoleStdOut());
        return null;
    }

    private void show(IdeaProject project, PrintStream output) {
        output.println();
        output.println("PROJECT");
        output.format("%s (%s)%n", project.getName(), project);

        for (IdeaModule module : project.getModules()) {
            output.println();
            output.println("MODULE");
            output.format("%s (%s)%n", module.getName(), module);

            output.println();
            output.println("CLASSPATH");
            for (IdeaDependency dependency : module.getDependencies()) {
                output.format("%s%n", dependency);
            }
        }
    }
}
