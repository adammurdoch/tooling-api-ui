package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.ModelBuilder;

public class FetchModelOperation<T> implements ToolingOperation<T> {
    private final Class<T> type;

    public FetchModelOperation(Class<T> type) {
        this.type = type;
    }

    @Override
    public String getDisplayName(ToolingOperationContext uiContext) {
        return "fetch " + type.getSimpleName();
    }

    @Override
    public T run(ToolingOperationContext uiContext) {
        ModelBuilder<T> builder = uiContext.create(projectConnection -> projectConnection.model(type));
        return builder.get();
    }
}
