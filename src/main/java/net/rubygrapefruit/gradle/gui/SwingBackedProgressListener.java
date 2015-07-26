package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;

import javax.swing.*;

public class SwingBackedProgressListener implements ProgressListener {
    private final ProgressListener delegate;

    public SwingBackedProgressListener(ProgressListener delegate) {
        this.delegate = delegate;
    }

    /**
     * Can be invoked from any thread.
     */
    @Override
    public void statusChanged(ProgressEvent event) {
        if (SwingUtilities.isEventDispatchThread()) {
            delegate.statusChanged(event);
        } else {
            SwingUtilities.invokeLater(() -> delegate.statusChanged(event));
        }
    }
}
