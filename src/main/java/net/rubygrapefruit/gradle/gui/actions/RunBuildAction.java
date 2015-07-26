package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.UIContext;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;

public class RunBuildAction implements ToolingOperation<Void> {
    @Override
    public String getDisplayName(UIContext uiContext) {
        return "build using " + uiContext.getCommandLineArgs();
    }

    @Override
    public Void run(UIContext uiContext) {
        BuildLauncher launcher = uiContext.create(projectConnection -> projectConnection.newBuild());
        launcher.run();
        return null;
    }
}
