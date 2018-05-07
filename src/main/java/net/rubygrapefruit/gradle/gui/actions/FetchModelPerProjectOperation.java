package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.util.ArrayList;
import java.util.List;

public class FetchModelPerProjectOperation<T> implements ToolingOperation<List<T>> {
    private final Class<T> type;

    public FetchModelPerProjectOperation(Class<T> type) {
        this.type = type;
    }

    @Override
    public String getDisplayName(ToolingOperationContext uiContext) {
        return "fetch " + type.getSimpleName();
    }

    @Override
    public List<T> run(ToolingOperationContext uiContext) {
        BuildActionExecuter<List<T>> executer = uiContext.create(connection -> connection.action(new ActionImpl<>(type)));
        return executer.run();
    }

    private static class ActionImpl<T> implements BuildAction<List<T>> {
        private final Class<T> type;

        ActionImpl(Class<T> type) {
            this.type = type;
        }

        @Override
        public List<T> execute(BuildController controller) {
            List<T> result = new ArrayList<>();
            GradleBuild build = controller.getBuildModel();
            for (BasicGradleProject project : build.getProjects()) {
                System.out.println("PROJECT: " + project);
                result.add(controller.getModel(project, type));
            }
            return result;
        }
    }
}
