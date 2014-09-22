package org.gradle.gui.actions;

import org.gradle.gui.ToolingOperation;
import org.gradle.gui.UIContext;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.gradle.GradleBuild;

public class GetBuildModel implements ToolingOperation<GradleBuild> {
    @Override
    public String getDisplayName(UIContext uiContext) {
        return "fetch projects";
    }

    @Override
    public GradleBuild run(ProjectConnection connection, UIContext uiContext) {
        ModelBuilder<GradleBuild> builder = connection.model(GradleBuild.class);
        uiContext.setup(builder);
        return builder.get();
    }
}
