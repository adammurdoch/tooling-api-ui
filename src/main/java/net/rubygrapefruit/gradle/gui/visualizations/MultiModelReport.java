package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.actions.MultiModel;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.PrintWriter;

public class MultiModelReport extends Report<MultiModel> {
    @Override
    public String getDisplayName() {
        return "Custom build action";
    }

    @Override
    protected void render(MultiModel result, PrintWriter stdOut) {
        GradleProject gradleProject = result.gradleProject;
        stdOut.println("== GRADLE ==");
        stdOut.format("path: %s%n", gradleProject.getPath());
        stdOut.format("name: %s%n", gradleProject.getName());
        stdOut.format("build script: %s%n", gradleProject.getBuildScript().getSourceFile());

        EclipseProject eclipseProject = result.eclipseProject;
        stdOut.println();
        stdOut.println("== ECLIPSE ==");
        stdOut.format("name: %s%n", eclipseProject.getName());
        stdOut.format("project dir: %s%n", eclipseProject.getProjectDirectory());

        IdeaProject ideaProject = result.ideaProject;
        stdOut.println();
        stdOut.println("== IDEA ==");
        stdOut.format("name: %s%n", ideaProject.getName());
        stdOut.format("jdk: %s%n", ideaProject.getJdkName());
        stdOut.format("Java language: %s%n", ideaProject.getLanguageLevel().getLevel());
        stdOut.format("output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getOutputDir());
        stdOut.format("test output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getTestOutputDir());

    }
}
