package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.build.BuildEnvironment;

public class BuildEnvironmentReport extends Report<BuildEnvironment> {
    public BuildEnvironmentReport() {
        super("Build environment");
    }

    @Override
    protected void render(BuildEnvironment environment, StructureVisitor tree) {
        tree.value("Gradle version", environment.getGradle().getGradleVersion());
        tree.value("Gradle user home", environment.getGradle().getGradleUserHome());
        tree.value("Java home", environment.getJava().getJavaHome());
        tree.collection("Requested JVM args", environment.getJava().getRequestedJvmArguments());
        tree.collection("JVM args", environment.getJava().getJvmArguments());
        tree.collection("All JVM args", environment.getJava().getAllJvmArguments());
        tree.map("Requested system properties", environment.getJava().getRequestedSystemProperties());
        tree.map("System properties", environment.getJava().getSystemProperties());
    }
}
