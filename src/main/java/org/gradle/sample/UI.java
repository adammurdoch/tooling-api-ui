package org.gradle.sample;

import org.gradle.gui.ConsolePanel;
import org.gradle.gui.MainPanel;
import org.gradle.gui.PathControl;
import org.gradle.gui.UIContext;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class UI {

    private final JButton eclipseModel;
    private final JButton runBuild;
    private final JButton runAction;
    private final JButton projects;
    private final JButton ideaModel;
    private final MainPanel panel;
    private final List<JButton> buttons;
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
        eclipseModel = new JButton("Eclipse model");
        ideaModel = new JButton("IDEA model");
        runAction = new JButton("Client action");
        runBuild = new JButton("Build");
        projects = new JButton("Projects");
        buttons = Arrays.asList(eclipseModel, ideaModel, projects, runAction, runBuild);
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
        commandLineArgs.addActionListener(new BuildAction<>(new RunBuildAction()));
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new BuildAction<>(new RunBuildAction()));
        panel.addToolbarControl(projects);
        JTree projects = new JTree();
        panel.addTab("Projects", projects);
        this.projects.addActionListener(new BuildAction<>(new GetBuildModel(), new ProjectTree(projects)));
        panel.addToolbarControl(eclipseModel);
        eclipseModel.addActionListener(new BuildAction<>(new FetchEclipseModel()));
        panel.addToolbarControl(ideaModel);
        ideaModel.addActionListener(new BuildAction<>(new FetchIdeaModel()));
        panel.addToolbarControl(runAction);
        runAction.addActionListener(new BuildAction<>(new RunBuildActionAction()));
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void onStartOperation(String displayName) {
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
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
        for (JButton button : buttons) {
            button.setEnabled(true);
        }
    }

    public interface ToolingOperation<T> {
        String getDisplayName(UIContext uiContext);

        /**
         * Executes this operation and returns some result. Called from a non-UI thread.
         */
        T run(ProjectConnection connection, UIContext uiContext);
    }

    public interface UIOperation<T> {
        void failed();

        /**
         * Updates the UI. Called from the AWT event thread.
         */
        void update(T result);
    }

    private static class GetBuildModel implements ToolingOperation<GradleBuild> {
        @Override
        public String getDisplayName(UIContext uiContext) {
            return "fetch projects";
        }

        @Override
        public GradleBuild run(ProjectConnection connection, UIContext uiContext) {
            ModelBuilder<GradleBuild> builder = connection.model(GradleBuild.class);
            uiContext.setup(builder);
            return builder.get();
        }
    }

    private static class ProjectTree implements UIOperation<GradleBuild> {
        JTree tree;

        private ProjectTree(JTree tree) {
            this.tree = tree;
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
        }

        @Override
        public void failed() {
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("FAILED")));
        }

        @Override
        public void update(GradleBuild gradleBuild) {
            tree.setModel(new DefaultTreeModel(toNode(gradleBuild.getRootProject())));
        }

        private MutableTreeNode toNode(BasicGradleProject project) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            node.setUserObject(String.format("Project %s", project.getName()));
            for (BasicGradleProject childProject : project.getChildren()) {
                node.add(toNode(childProject));
            }
            return node;
        }
    }

    private class BuildAction<T> implements ActionListener {
        private final ToolingOperation<T> operation;
        private final UIOperation<T> uiOperation;

        protected BuildAction(ToolingOperation<T> operation) {
            this.operation = operation;
            uiOperation = new UIOperation<T>() {
                @Override
                public void failed() {
                }

                @Override
                public void update(T result) {
                }
            };
        }

        protected BuildAction(ToolingOperation<T> operation, UIOperation<T> uiOperation) {
            this.operation = operation;
            this.uiOperation = uiOperation;
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
                    T result = null;
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
                            result = operation.run(connection, uiContext);
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
                        final T finalResult = result;
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                onFinishOperation(endTime - startTime, finalFailure);
                                if (finalResult != null) {
                                    uiOperation.update(finalResult);
                                } else {
                                    uiOperation.failed();
                                }
                            }
                        });
                    }
                }
            }.start();
        }
    }
}

