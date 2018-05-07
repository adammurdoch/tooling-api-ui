package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;

public class RunNoOpBuildActionOperation implements ToolingOperation<String> {

    private static class ToolingBuildAction implements org.gradle.tooling.BuildAction<Void> {
        @Override
        public Void execute(BuildController controller) {
            System.out.println("building empty model");
            return null;
        }
    }

    @Override
    public String getDisplayName(ToolingOperationContext uiContext) {
        return "no-op action";
    }

    @Override
    public String run(ToolingOperationContext uiContext) {
        BuildActionExecuter<Void> executer = uiContext.create(projectConnection -> projectConnection.action(new ToolingBuildAction()));
        executer.run();
        return "ok";
    }
}
