package net.rubygrapefruit.gradle.gui.actions;

import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationContext;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.connection.ModelResults;

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
        if (uiContext.isComposite()) {
            ModelBuilder<ModelResults<T>> builder = uiContext.createComposite(connection -> connection.models(type));
            return builder.get().iterator().next().getModel();
        } else {
            ModelBuilder<T> builder = uiContext.create(connection -> connection.model(type));
            return builder.get();
        }
    }
}
