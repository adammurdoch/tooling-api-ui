package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.UIContext;
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
    public T run(UIContext uiContext) {
        ModelBuilder<T> builder = uiContext.create(projectConnection -> projectConnection.model(type));
        return builder.get();
    }
}
