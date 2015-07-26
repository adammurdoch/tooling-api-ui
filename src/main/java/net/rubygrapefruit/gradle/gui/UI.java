package net.rubygrapefruit.gradle.gui;

import net.rubygrapefruit.gradle.gui.actions.FetchModelOperation;
import net.rubygrapefruit.gradle.gui.actions.MultiModel;
import net.rubygrapefruit.gradle.gui.actions.RunBuildActionOperation;
import net.rubygrapefruit.gradle.gui.actions.RunBuildOperation;
import net.rubygrapefruit.gradle.gui.visualizations.*;
import org.gradle.tooling.CancellationTokenSource;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.IdeaProject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class UI {

    public static final String DEFAULT_VERSION = "Default";
    public static final String LOCAL_DISTRIBUTION = "Local distribution";
    private final JButton runBuild;
    private final List<VisualizationPanel<?>> panels;
    private final JButton cancel;
    private final JButton shutdown;
    private final MainPanel panel;
    private final BuildEventTree buildEventTree;
    private final TestsView testsView;
    private final List<JButton> buttons;
    private final PathControl projectDirSelector;
    private final PathControl installation;
    private final PathControl userHomeDir;
    private final JTextField commandLineArgs;
    private final JTextField jvmArgs;
    private final JCheckBox color;
    private final JCheckBox embedded;
    private final JCheckBox verboseLogging;
    private final ConsolePanel console;
    private final ConsolePanel log;
    private final AtomicReference<CancellationTokenSource> token = new AtomicReference<>();
    private final PrintStream originalStdOut;
    private final PrintStream originalStdErr;
    private final JComboBox<Object> gradleVersion;
    private final OperationExecuter executer = new OperationExecuter();

    public UI() {
        originalStdOut = System.out;
        originalStdErr = System.err;
        runBuild = new JButton("Build");
        cancel = new JButton("Cancel");
        VisualizationPanel<GradleBuild> projects = new VisualizationPanel<>(new FetchModelOperation<>(GradleBuild.class), new ProjectTree(), executer);
        VisualizationPanel<BuildInvocations> tasks = new VisualizationPanel<>(new FetchModelOperation<>(BuildInvocations.class), new TasksTable(), executer);
        VisualizationPanel<BuildEnvironment> buildEnvironment = new VisualizationPanel<>(new FetchModelOperation<>(BuildEnvironment.class), new BuildEnvironmentReport(), executer);
        VisualizationPanel<EclipseProject> eclipseProject = new VisualizationPanel<>(new FetchModelOperation<>(EclipseProject.class), new EclipseModelReport(), executer);
        VisualizationPanel<IdeaProject> ideaProject = new VisualizationPanel<>(new FetchModelOperation<>(IdeaProject.class), new IdeaModelReport(), executer);
        VisualizationPanel<MultiModel> multiModel = new VisualizationPanel<>(new RunBuildActionOperation(), new MultiModelReport(), executer);
        panels = Arrays.asList(projects, tasks, buildEnvironment, eclipseProject, ideaProject, multiModel);
        buttons = new ArrayList<>();
        buttons.add(runBuild);
        for (VisualizationPanel<?> visualizationPanel : panels) {
            buttons.add(visualizationPanel.getLaunchButton());
        }
        panel = new MainPanel();
        console = panel.getConsole();
        log = panel.getLog();
        System.setOut(log.getOutput());
        System.setErr(log.getError());
        buildEventTree = new BuildEventTree();
        testsView = new TestsView(executer);
        projectDirSelector = new PathControl();
        installation = new PathControl();
        userHomeDir = new PathControl();
        commandLineArgs = new JTextField();
        jvmArgs = new JTextField();
        color = new JCheckBox("Color output");
        embedded = new JCheckBox("Run build in-process (internal)");
        verboseLogging = new JCheckBox("Verbose logging (internal)");
        shutdown = new JButton("Shutdown tooling API");
        gradleVersion = new JComboBox<>(new Object[]{LOCAL_DISTRIBUTION, DEFAULT_VERSION, "2.4", "2.3", "2.2.1", "2.2", "2.1", "2.0", "1.12", "1.11", "1.0", "1.0-milestone-8", "1.0-milestone-3", "0.9.2", "0.8"});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UI().go());
    }

    void go() {
        JFrame frame = new JFrame("Tooling API Test UI");
        frame.setContentPane(panel);

        SettingsPanel settings = panel.getSettings();
        projectDirSelector.setFile(new File("/Users/adam/gradle/test-projects/tooling"));
        settings.addControl("Project directory", projectDirSelector);
        settings.addControl("Gradle version", gradleVersion);
        installation.setFile(new File("/Users/adam/gradle/current"));
        settings.addControl("Distribution", installation);
        settings.addControl("JVM args", jvmArgs);
        settings.addControl("User home directory", userHomeDir);
        color.setSelected(true);
        settings.addControl(color);
        settings.addControl(embedded);
        settings.addControl(verboseLogging);
        settings.addControl(shutdown);
        shutdown.addActionListener(new ShutdownListener());

        panel.addTab("Build events", buildEventTree);
        panel.addTab("Tests", testsView);

        panel.addToolbarControl("Command-line arguments", commandLineArgs);
        commandLineArgs.addActionListener(new BuildAction<>(new RunBuildOperation()));
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new BuildAction<>(new RunBuildOperation()));
        panel.addToolbarControl(cancel);
        cancel.addActionListener(new CancelListener());
        for (VisualizationPanel visualizationPanel : panels) {
            panel.addTab(visualizationPanel.getDisplayName(), visualizationPanel.getMainComponent());
        }
        initButtons();
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    private void onStartOperation(String displayName) {
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
        cancel.setEnabled(true);
        console.clearOutput();
        panel.onProgress("");
        buildEventTree.reset();
        testsView.reset();
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
        commandLineArgs.requestFocusInWindow();
        commandLineArgs.selectAll();
        initButtons();
    }

    private void initButtons() {
        for (JButton button : buttons) {
            button.setEnabled(true);
        }
        cancel.setEnabled(false);
    }

    private class BuildAction<T> implements ActionListener {
        private final ToolingOperation<? extends T> operation;

        protected BuildAction(ToolingOperation<T> operation) {
            this.operation = operation;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            executer.start(operation);
        }
    }

    private class OperationExecuter implements ToolingOperationExecuter {
        @Override
        public void start(ToolingOperation<?> operation) {
            start(operation, new ProgressAwareVisualization<Object>() {
                        public String getDisplayName() {
                            return null;
                        }

                        public void started() {
                            panel.showConsole();
                        }

                        public JComponent getMainComponent() {
                            return null;
                        }

                        public void failed() {
                        }

                        public void update(Object result) {
                        }
                    }
            );
        }

        @Override
        public <T> void start(ToolingOperation<T> operation, ProgressAwareVisualization<? super T> visualization) {
            final File userHome = userHomeDir.getFile();
            final File projectDir = projectDirSelector.getFile();
            final File distribution;
            final String version;
            if (gradleVersion.getSelectedItem().equals(LOCAL_DISTRIBUTION)) {
                distribution = installation.getFile();
                version = null;
            } else if (gradleVersion.getSelectedItem().equals(DEFAULT_VERSION)) {
                distribution = null;
                version = null;
            } else {
                distribution = null;
                version = gradleVersion.getSelectedItem().toString();
            }

            final boolean isColor = color.isSelected();
            final boolean isEmbedded = embedded.isSelected();
            final boolean isVerbose = verboseLogging.isSelected();
            final String[] commandLine = args(commandLineArgs.getText());
            final String[] splitJvmArgs = args(jvmArgs.getText());
            final CancellationTokenSource tokenSource = GradleConnector.newCancellationTokenSource();
            token.set(tokenSource);
            AtomicReference<ProjectConnection> connectionRef = new AtomicReference<>();
            final ToolingOperationContext uiContext = new ToolingOperationContext() {
                @Override
                public List<String> getCommandLineArgs() {
                    return Arrays.asList(splitJvmArgs);
                }

                @Override
                public <T extends LongRunningOperation> T create(OperationProvider<T> provider) {
                    T operation = provider.create(connectionRef.get());
                    setup(operation);
                    return operation;
                }

                public void setup(LongRunningOperation operation) {
                    operation.withCancellationToken(tokenSource.token());
                    operation.addProgressListener((org.gradle.tooling.ProgressEvent event) -> {
                        log.getOutput().println("[progress: " + event.getDescription() + "]");
                        panel.onProgress(event.getDescription());
                    });
                    operation.addProgressListener(new SwingBackedProgressListener(buildEventTree));
                    operation.addProgressListener(new SwingBackedProgressListener(testsView), Collections.singleton(OperationType.TEST));
                    operation.setColorOutput(isColor);
                    operation.setStandardOutput(console.getOutput());
                    operation.setStandardError(console.getError());
                    if (commandLine.length > 0) {
                        operation.withArguments(commandLine);
                    }
                    if (splitJvmArgs.length > 0) {
                        operation.setJvmArguments(splitJvmArgs);
                    }
                }
            };
            onStartOperation(operation.getDisplayName(uiContext));
            panel.onProgress("building for project dir: " + projectDir);
            visualization.started();
            new Thread() {
                @Override
                public void run() {
                    final long startTime = System.currentTimeMillis();
                    Throwable failure = null;
                    T result = null;
                    try {
                        DefaultGradleConnector connector = (DefaultGradleConnector) GradleConnector.newConnector();
                        if (userHome != null) {
                            connector.useGradleUserHomeDir(userHome);
                        }
                        if (distribution != null) {
                            if (distribution.isDirectory()) {
                                connector.useInstallation(distribution);
                            } else {
                                connector.useDistribution(distribution.toURI());
                            }
                        }
                        if (version != null) {
                            connector.useGradleVersion(version);
                        }
                        if (isEmbedded) {
                            connector.embedded(true);
                        }
                        if (isVerbose) {
                            connector.setVerboseLogging(true);
                        }
                        ProjectConnection connection = connector.forProjectDirectory(projectDir).connect();
                        try {
                            connectionRef.set(connection);
                            result = operation.run(uiContext);
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
                        SwingUtilities.invokeLater(() -> {
                            onFinishOperation(endTime - startTime, finalFailure);
                            if (finalResult != null) {
                                visualization.update(finalResult);
                            } else {
                                visualization.failed();
                            }
                        });
                    }
                }
            }.start();
        }

        private String[] args(String source) {
            String trimmed = source.trim();
            if (trimmed.isEmpty()) {
                return new String[0];
            }
            return trimmed.split("\\s+");
        }
    }

    private class CancelListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO - do this in the background
            System.out.println("Cancelling...");
            token.get().cancel();
        }
    }

    private class ShutdownListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO - do this in the background
            System.out.println("Shutdown...");
            DefaultGradleConnector.close();
        }
    }
}

