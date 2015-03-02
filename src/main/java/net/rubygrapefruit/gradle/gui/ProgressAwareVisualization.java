package net.rubygrapefruit.gradle.gui;

/**
 * All methods are called from the Swing UI thread.
 */
public interface ProgressAwareVisualization<T> extends Visualization<T> {
    void started();

    void failed();
}
