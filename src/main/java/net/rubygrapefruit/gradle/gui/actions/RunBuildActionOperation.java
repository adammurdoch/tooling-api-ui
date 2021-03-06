package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

public class RunBuildActionOperation implements ToolingOperation<MultiModel> {

    private static class ToolingBuildAction implements org.gradle.tooling.BuildAction<MultiModel> {
        @Override
        public MultiModel execute(BuildController controller) {
            MultiModel result = new MultiModel();
            result.gradleProject = controller.getModel(GradleProject.class);
            result.eclipseProject = controller.getModel(EclipseProject.class);
            result.ideaProject = controller.getModel(IdeaProject.class);
            return result;
        }
    }

    @Override
    public String getDisplayName(ToolingOperationContext uiContext) {
        return "client action";
    }

    @Override
    public MultiModel run(ToolingOperationContext uiContext) {
        BuildActionExecuter<MultiModel> executer = uiContext.create(projectConnection -> projectConnection.action(new ToolingBuildAction()));
        return executer.run();
    }
}
