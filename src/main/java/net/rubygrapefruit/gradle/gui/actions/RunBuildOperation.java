package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.BuildLauncher;

public class RunBuildOperation implements ToolingOperation<Void> {
    @Override
    public String getDisplayName(ToolingOperationContext uiContext) {
        return "build using " + uiContext.getCommandLineArgs();
    }

    @Override
    public Void run(ToolingOperationContext uiContext) {
        BuildLauncher launcher = uiContext.create(connection -> connection.newBuild());
        launcher.run();
        return null;
    }
}
