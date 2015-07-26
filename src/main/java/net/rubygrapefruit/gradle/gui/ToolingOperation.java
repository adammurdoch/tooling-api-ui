package net.rubygrapefruit.gradle.gui;

public interface ToolingOperation<T> {
    String getDisplayName(UIContext uiContext);

    /**
     * Executes this operation and returns some result. Called from a non-UI thread.
     */
    T run(UIContext uiContext);
}
