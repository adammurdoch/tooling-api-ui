package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.PrintWriter;

public class IdeaModelReport extends Report<IdeaProject> {
    @Override
    public String getDisplayName() {
        return "IDEA model";
    }

    @Override
    protected void render(IdeaProject project, PrintWriter output) {
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
