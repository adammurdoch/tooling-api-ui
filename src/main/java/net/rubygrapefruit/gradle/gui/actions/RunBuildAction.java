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
    public Void run(ProjectConnection connection, UIContext uiContext) {
        BuildLauncher launcher = connection.newBuild();
        uiContext.setup(launcher);
        launcher.run();
        return null;
    }
}
