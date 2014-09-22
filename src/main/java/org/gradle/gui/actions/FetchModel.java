package org.gradle.gui.actions;

import org.gradle.gui.ToolingOperation;
import org.gradle.gui.UIContext;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

public class FetchModel<T> implements ToolingOperation<T> {
    private final Class<T> type;

    public FetchModel(Class<T> type) {
        this.type = type;
    }

    @Override
    public String getDisplayName(UIContext uiContext) {
        return "fetch " + type.getSimpleName();
    }

    @Override
    public T run(ProjectConnection connection, UIContext uiContext) {
        ModelBuilder<T> builder = connection.model(type);
        uiContext.setup(builder);
        return builder.get();
    }
}
