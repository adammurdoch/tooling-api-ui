package net.rubygrapefruit.gradle.gui;

import net.rubygrapefruit.gradle.gui.actions.*;
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
import org.gradle.tooling.model.gradle.ProjectPublications;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.util.GradleVersion;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class UI {

    private static final String DEFAULT_VERSION = "Default";
    private static final String LOCAL_DISTRIBUTION = "Local distribution";
    private static final String PROJECT_DIR_PROP = "project.dir";
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
    private final Executor executionPool = Executors.newCachedThreadPool();
    private final JLabel tapiVersion;
    private final File workspaceFile;
    private final Properties properties;
    private final BuildInvocation runBuildInvocation;
    private JComboBox selectedInvocation;
    private final List<BuildInvocation> invocations;

    public UI() {
        originalStdOut = System.out;
        originalStdErr = System.err;
        runBuild = new JButton("Run");
        cancel = new JButton("Cancel");
        selectedInvocation = new JComboBox();
        VisualizationPanel<GradleBuild> builds = new VisualizationPanel<>(new FetchModelOperation<>(GradleBuild.class), new ProjectTree(), executer);
        VisualizationPanel<BuildInvocations> tasks = new VisualizationPanel<>(new FetchModelOperation<>(BuildInvocations.class), new TasksTable(), executer);
        VisualizationPanel<BuildEnvironment> buildEnvironment = new VisualizationPanel<>(new FetchModelOperation<>(BuildEnvironment.class), new BuildEnvironmentReport(), executer);
        VisualizationPanel<EclipseProject> eclipseProject = new VisualizationPanel<>(new FetchModelOperation<>(EclipseProject.class), new EclipseModelReport(), executer);
        VisualizationPanel<IdeaProject> ideaProject = new VisualizationPanel<>(new FetchModelOperation<>(IdeaProject.class), new IdeaModelReport(), executer);
        VisualizationPanel<List<ProjectPublications>> publications = new VisualizationPanel<>(new FetchModelPerProjectOperation<>(ProjectPublications.class), new PublicationsTable(), executer);
        VisualizationPanel<MultiModel> multiModel = new VisualizationPanel<>(new RunBuildActionOperation(), new MultiModelReport(), executer);
        VisualizationPanel<String> emptyModel = new VisualizationPanel<>(new RunNoOpBuildActionOperation(), new EmptyModelReport(), executer);
        panels = Arrays.asList(builds, tasks, buildEnvironment, eclipseProject, ideaProject, publications, multiModel, emptyModel);
        buttons = new ArrayList<>();
        buttons.add(runBuild);
        for (VisualizationPanel<?> visualizationPanel : panels) {
            buttons.add(visualizationPanel.getRefreshButton());
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
        tapiVersion = new JLabel(GradleVersion.current().getVersion());
        gradleVersion = new JComboBox<>(new Object[]{LOCAL_DISTRIBUTION, DEFAULT_VERSION, "4.6", "3.4.1", "3.4", "3.3", "3.2", "3.1", "3.0", "2.14.1", "2.13", "2.12", "2.11", "2.10", "2.9", "2.8", "2.7", "2.6", "2.5", "2.4", "2.3", "2.2.1", "2.2", "2.1", "2.0", "1.12", "1.11", "1.0", "1.0-milestone-8", "1.0-milestone-3", "0.9.2", "0.8"});

        invocations = new ArrayList<>();
        runBuildInvocation = new RunBuildInvocation();
        invocations.add(runBuildInvocation);
        for (VisualizationPanel<?> visualizationPanel : panels) {
            invocations.add(new VisualizationInvocation(visualizationPanel));
        }

        properties = new Properties();
        workspaceFile = new File(new File(System.getProperty("user.home")), ".tapi-ui/workspace.properties");
        if (workspaceFile.isFile()) {
            try {
                try (FileInputStream inStream = new FileInputStream(workspaceFile)) {
                    properties.load(inStream);
                }
            } catch (IOException e) {
                originalStdErr.println("Could not load workspace from " + workspaceFile);
                e.printStackTrace(originalStdErr);
            }
        }
        if (!properties.containsKey(PROJECT_DIR_PROP)) {
            properties.setProperty(PROJECT_DIR_PROP, Paths.get(".").toAbsolutePath().normalize().toString());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UI().go());
    }

    void go() {
        JFrame frame = new JFrame("Tooling API Test UI");
        frame.setContentPane(panel);

        SettingsPanel settings = panel.getSettings();
        settings.addControl("Tooling API version", tapiVersion);
        projectDirSelector.setFile(new File(properties.getProperty(PROJECT_DIR_PROP)));
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
        commandLineArgs.addActionListener(runBuildInvocation.getActionListener());
        selectedInvocation.setModel(new DefaultComboBoxModel(invocations.toArray()));
        panel.addToolbarControl(selectedInvocation);
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new StartSelectedInvocation());
        panel.addToolbarControl(cancel);
        cancel.addActionListener(new CancelListener());
        initButtons();
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            properties.setProperty(PROJECT_DIR_PROP, projectDirSelector.getFile().getAbsolutePath());
            workspaceFile.getParentFile().mkdirs();
            try {
                try (OutputStream outputStream = new FileOutputStream(workspaceFile)) {
                    properties.store(outputStream, "workspace");
                }
            } catch (IOException e) {
                originalStdErr.println("Could not write workspace properties to " + workspaceFile);
                e.printStackTrace(originalStdErr);
            }
        }));
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

    private class VisualizationInvocation extends BuildInvocation {
        private final VisualizationPanel<?> visualizationPanel;
        private boolean added;

        VisualizationInvocation(VisualizationPanel<?> visualizationPanel) {
            this.visualizationPanel = visualizationPanel;
        }

        @Override
        public String getDisplayName() {
            return visualizationPanel.getDisplayName();
        }

        @Override
        public void start() {
            if (!added) {
                MainPanel.Tab tab = panel.addTab(visualizationPanel.getDisplayName(), visualizationPanel.getMainComponent());
                tab.show();
                added = true;
            }
            visualizationPanel.start();
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
            AtomicReference<ProjectConnection> projectConnectionRef = new AtomicReference<>();
            final ToolingOperationContext uiContext = new ToolingOperationContext() {
                @Override
                public List<String> getCommandLineArgs() {
                    return Arrays.asList(splitJvmArgs);
                }

                @Override
                public <T extends LongRunningOperation> T create(OperationProvider<T, ProjectConnection> provider) {
                    ProjectConnection connection = projectConnectionRef.get();
                    if (connection == null) {
                        throw new IllegalStateException("Not using ProjectConnection API");
                    }
                    T operation = provider.create(connection);
                    setup(operation);
                    return operation;
                }

                private void setup(LongRunningOperation operation) {
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
            executionPool.execute(() -> {
                final long startTime = System.currentTimeMillis();
                Throwable failure = null;
                T result = null;
                try {
                    System.out.println("Using ProjectConnection API");
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
                        projectConnectionRef.set(connection);
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
            });
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

    private class RunBuildInvocation extends BuildInvocation {
        @Override
        public String getDisplayName() {
            return "Run tasks";
        }

        @Override
        public void start() {
            executer.start(new RunBuildOperation());
        }
    }

    private class StartSelectedInvocation implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ((BuildInvocation) selectedInvocation.getModel().getSelectedItem()).start();
        }
    }
}

