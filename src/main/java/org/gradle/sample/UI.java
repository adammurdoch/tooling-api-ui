package org.gradle.sample;

import org.gradle.gui.ConsolePanel;
import org.gradle.gui.MainPanel;
import org.gradle.gui.PathControl;
import org.gradle.gui.UIContext;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;

public class UI {

    private final JButton buildModel;
    private final JButton runBuild;
    private final JButton runAction;
    private final MainPanel panel;
    private final PathControl projectDirSelector;
    private final JRadioButton useDistribution;
    private final PathControl installation;
    private final JTextField commandLineArgs;
    private final JCheckBox embedded;
    private final ConsolePanel console;
    private final ConsolePanel log;
    private final PrintStream originalStdOut;
    private final PrintStream originalStdErr;

    public UI() {
        originalStdOut = System.out;
        originalStdErr = System.err;
        buildModel = new JButton("Eclipse model");
        runAction = new JButton("Client action");
        runBuild = new JButton("Build");
        panel = new MainPanel();
        console = panel.getConsole();
        log = panel.getLog();
        System.setOut(log.getOutput());
        System.setErr(log.getError());
        projectDirSelector = new PathControl();
        useDistribution = new JRadioButton("Use distribution");
        installation = new PathControl();
        commandLineArgs = new JTextField();
        embedded = new JCheckBox("Embedded");
    }

    public static void main(String[] args) {
        new UI().go();
    }

    void go() {
        JFrame frame = new JFrame("Tooling API Test UI");
        frame.setContentPane(panel);
        projectDirSelector.setFile(new File("/Users/adam/gradle/test-projects/minimal"));
        panel.addControl("Project directory", projectDirSelector);
        JRadioButton useDefaultDir = new JRadioButton("Use default distribution");
        panel.addControl(useDefaultDir);
        panel.addControl(useDistribution);
        installation.setFile(new File("/Users/adam/gradle/current"));
        panel.addControl("Distribution", installation);
        ButtonGroup distSelector = new ButtonGroup();
        distSelector.add(useDefaultDir);
        distSelector.add(useDistribution);
        useDistribution.setSelected(true);
        panel.addControl(embedded);
        panel.addToolbarControl("Command-line arguments", commandLineArgs);
        commandLineArgs.addActionListener(new BuildAction(new RunBuildAction()));
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new BuildAction(new RunBuildAction()));
        panel.addToolbarControl(buildModel);
        buildModel.addActionListener(new BuildAction(new FetchEclipseModel()));
        panel.addToolbarControl(runAction);
        runAction.addActionListener(new BuildAction(new RunBuildActionAction()));
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void onStartOperation(String displayName) {
        buildModel.setEnabled(false);
        runBuild.setEnabled(false);
        runAction.setEnabled(false);
        console.clearOutput();
        panel.onProgress("");
        log.getOutput().println("================");
        log.getOutput().print("Starting ");
        log.getOutput().println(displayName);
        log.getOutput().println("================");
    }

    private void onFinishOperation(long timeMillis, Throwable failure) {
        console.getOutput().flush();
        console.getError().flush();
        log.getOutput().flush();
        log.getError().flush();
        panel.onProgress((failure == null ? "Finished" : "Failed") + " (" + timeMillis / 1000 + " seconds)");
        buildModel.setEnabled(true);
        runBuild.setEnabled(true);
        runAction.setEnabled(true);
    }

    public interface ToolingOperation {
        String getDisplayName(UIContext uiContext);

        /**
         * Executes this operation. Called from a non-UI thread.
         */
        void run(ProjectConnection connection, UIContext uiContext);
    }

    private class BuildAction implements ActionListener {
        private final ToolingOperation operation;

        protected BuildAction(ToolingOperation operation) {
            this.operation = operation;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            final File projectDir = projectDirSelector.getFile();
            final File distribution = useDistribution.isSelected() ? installation.getFile() : null;
            final boolean isEmbedded = embedded.isSelected();
            final String[] commandLine = commandLineArgs.getText().trim().split("\\s+");
            final UIContext uiContext = new UIContext(projectDir, distribution, isEmbedded, console.getOutput(), Arrays.asList(commandLine)) {
                @Override
                public void setup(LongRunningOperation operation) {
                    operation.addProgressListener(new ProgressListener() {
                        public void statusChanged(ProgressEvent event) {
                            log.getOutput().println("[progress: " + event.getDescription() + "]");
                            panel.onProgress(event.getDescription());
                        }
                    });
                    operation.setStandardOutput(console.getOutput());
                    operation.setStandardError(console.getError());
                    if (commandLine.length > 0) {
                        operation.withArguments(commandLine);
                    }
                }
            };
            onStartOperation(operation.getDisplayName(uiContext));
            panel.onProgress("building for project dir: " + projectDir);
            new Thread() {
                @Override
                public void run() {
                    final long startTime = System.currentTimeMillis();
                    Throwable failure = null;
                    try {
                        DefaultGradleConnector connector = (DefaultGradleConnector) GradleConnector.newConnector();
                        if (distribution != null) {
                            if (distribution.isDirectory()) {
                                connector.useInstallation(distribution);
                            } else {
                                connector.useDistribution(distribution.toURI());
                            }
                        }
                        if (isEmbedded) {
                            connector.embedded(true);
                        }
                        ProjectConnection connection = connector.forProjectDirectory(projectDir).connect();
                        try {
                            operation.run(connection, uiContext);
                        } finally {
                            connection.close();
                        }
                    } catch (Throwable t) {
                        log.getError().println("FAILED WITH EXCEPTION");
                        t.printStackTrace(log.getError());
                        failure = t;
                    } finally {
                        final long endTime = System.currentTimeMillis();
                        final Throwable finalFailure = failure;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                onFinishOperation(endTime - startTime, finalFailure);
                            }
                        });
                    }
                }
            }.start();
        }
    }
}

