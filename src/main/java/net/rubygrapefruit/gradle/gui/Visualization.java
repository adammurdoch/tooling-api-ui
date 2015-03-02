package net.rubygrapefruit.gradle.gui;

import javax.swing.*;

/**
 * All methods are invoked from the Swing thread.
 *
 * @param <T> The model type.
 */
public interface Visualization<T> {
    String getDisplayName();

    JComponent getMainComponent();

    void update(T model);
}
