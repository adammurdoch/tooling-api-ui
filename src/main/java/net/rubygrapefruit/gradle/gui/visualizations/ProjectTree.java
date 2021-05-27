package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

public class ProjectTree extends Report<GradleBuild> {
    public ProjectTree() {
        super("Project model");
    }

    @Override
    protected void render(GradleBuild gradleBuild, StructureVisitor output) {
        output.struct("Build " + gradleBuild.getBuildIdentifier(), gradleBuild, b1 -> {
            output.struct("Projects", gradleBuild, b2 -> {
                visit(gradleBuild.getRootProject(), output);
            });
            output.collection("Editable builds", gradleBuild.getEditableBuilds(), build -> {
                render(build, output);
            });
            output.collection("Included builds", gradleBuild.getIncludedBuilds(), build -> {
                output.value("Build", build.getBuildIdentifier());
            });
        });
    }

    private void visit(BasicGradleProject project, StructureVisitor output) {
        output.struct(String.format("Project %s (%s)", project.getName(), project.getPath()), project, p1 -> {
            for (BasicGradleProject childProject : project.getChildren()) {
                visit(childProject, output);
            }
        });
    }
}
