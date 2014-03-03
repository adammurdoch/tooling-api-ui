package org.gradle.gui;

import org.gradle.tooling.LongRunningOperation;

import java.io.File;
import java.io.PrintStream;
import java.util.List;

public abstract class UIContext {
    private final File projectDir;
    private final File distribution;
    private final boolean embedded;
    private final PrintStream consoleStdOut;
    private final List<String> commandLineArgs;

    public UIContext(File projectDir, File distribution, boolean embedded, PrintStream consoleStdOut, List<String> commandLineArgs) {
        this.projectDir = projectDir;
        this.distribution = distribution;
        this.embedded = embedded;
        this.consoleStdOut = consoleStdOut;
        this.commandLineArgs = commandLineArgs;
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

    public List<String> getCommandLineArgs() {
        return commandLineArgs;
    }

    public abstract void setup(LongRunningOperation operation);
}
