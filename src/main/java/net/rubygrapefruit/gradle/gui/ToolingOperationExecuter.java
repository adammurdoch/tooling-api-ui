package net.rubygrapefruit.gradle.gui;

public interface ToolingOperationExecuter {
    /**
     * Starts running the given operation. Discards the result.
     */
    void start(ToolingOperation<?> operation);

    /**
     * Starts running the given operation, passing the result to the given visualisation.
     */
    <T> void start(ToolingOperation<T> operation, ProgressAwareVisualization<? super T> visualization);
}
