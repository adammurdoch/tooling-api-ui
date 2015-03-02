package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.build.BuildEnvironment;

import java.io.PrintWriter;

public class BuildEnvironmentReport extends Report<BuildEnvironment> {
    @Override
    public String getDisplayName() {
        return "Build environment";
    }

    @Override
    protected void render(BuildEnvironment project, PrintWriter output) {
        output.format("Gradle version: %s%n", project.getGradle().getGradleVersion());
        output.format("Java version: %s%n", project.getJava().getJavaHome());
        output.format("JVM args: %s%n", project.getJava().getJvmArguments());
    }
}
