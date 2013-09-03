package org.gradle.gui;

import org.gradle.tooling.LongRunningOperation;

import java.io.File;
import java.io.PrintStream;

public abstract class UIContext {
    private final File projectDir;
    private final File distribution;
    private final boolean embedded;
    private final PrintStream consoleStdOut;

    public UIContext(File projectDir, File distribution, boolean embedded, PrintStream consoleStdOut) {
        this.projectDir = projectDir;
        this.distribution = distribution;
        this.embedded = embedded;
        this.consoleStdOut = consoleStdOut;
    }

    public File getProjectDir() {
        return projectDir;
    }

    public File getDistribution() {
        return distribution;
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public PrintStream getConsoleStdOut() {
        return consoleStdOut;
    }

    public abstract void setup(LongRunningOperation operation);
}
