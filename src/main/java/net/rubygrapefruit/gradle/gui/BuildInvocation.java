package net.rubygrapefruit.gradle.gui;

import java.awt.event.ActionListener;

public abstract class BuildInvocation {
    abstract String getDisplayName();

    @Override
    public String toString() {
        return getDisplayName();
    }

    public ActionListener getActionListener() {
        return e -> start();
    }

    // Called from the UI thread
    abstract void start();
}
