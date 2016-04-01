package net.rubygrapefruit.gradle.gui;

/**
 * Some asynchronous tooling API operation.
 * @param <T>
 */
public interface ToolingOperation<T> {
    /**
     * Called from UI thread.
     */
    String getDisplayName(ToolingOperationContext uiContext);

    /**
     * Executes this operation and returns some result. Called from a non-UI thread.
     */
    T run(ToolingOperationContext uiContext);
}
