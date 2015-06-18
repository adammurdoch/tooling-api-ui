package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.build.GradleEnvironment;
import org.gradle.tooling.model.build.JavaEnvironment;

public class BuildEnvironmentReport extends Report<BuildEnvironment> {
    public BuildEnvironmentReport() {
        super("Build environment");
    }

    @Override
    protected void render(BuildEnvironment environment, StructureVisitor tree) {
        tree.struct("Gradle", environment.getGradle(), (GradleEnvironment gradleEnvironment) -> {
            tree.value("Gradle version", gradleEnvironment.getGradleVersion());
            tree.value("Gradle user home", gradleEnvironment.getGradleUserHome());
        });
        tree.struct("Java", environment.getJava(), (JavaEnvironment javaEnvironment) -> {
            tree.value("Java home", javaEnvironment.getJavaHome());
            tree.collection("JVM args", javaEnvironment.getJvmArguments());
        });
    }
}
