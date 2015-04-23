package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.build.BuildEnvironment;

public class BuildEnvironmentReport extends Report<BuildEnvironment> {
    public BuildEnvironmentReport() {
        super("Build environment");
    }

    @Override
    protected void render(BuildEnvironment environment, StructureVisitor tree) {
        tree.value("Gradle version", environment.getGradle().getGradleVersion());
        tree.value("Java home", environment.getJava().getJavaHome());
        tree.collection("JVM args", environment.getJava().getJvmArguments());
    }
}
