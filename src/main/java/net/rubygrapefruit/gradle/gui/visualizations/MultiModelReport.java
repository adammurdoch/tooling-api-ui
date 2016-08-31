package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.actions.MultiModel;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

public class MultiModelReport extends Report<MultiModel> {
    public MultiModelReport() {
        super("Custom build action");
    }

    @Override
    protected void render(MultiModel model, StructureVisitor tree) {
        GradleProject gradleProject = model.gradleProject;
        tree.struct("Gradle", gradleProject.getName(), () -> {
            tree.value("Path", gradleProject.getPath());
            tree.value("Build script", gradleProject.getBuildScript().getSourceFile());
        });

        EclipseProject eclipseProject = model.eclipseProject;
        tree.struct("Eclipse", eclipseProject.getName(), () -> {
            new EclipseModelReport().render(eclipseProject, tree);
        });

        IdeaProject ideaProject = model.ideaProject;
        tree.struct("IDEA", ideaProject.getName(), () -> {
            new IdeaModelReport().render(ideaProject, tree);
        });
    }
}
