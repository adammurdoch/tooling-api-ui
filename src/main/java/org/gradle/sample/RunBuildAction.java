package org.gradle.sample;

import org.gradle.gui.UIContext;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.ProjectConnection;

class RunBuildAction implements UI.ToolingOperation<Void> {
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
