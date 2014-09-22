package org.gradle.gui;

import org.gradle.gui.actions.FetchModel;
import org.gradle.gui.actions.MultiModel;
import org.gradle.gui.actions.RunBuildAction;
import org.gradle.gui.actions.RunBuildActionAction;
import org.gradle.gui.visualizations.*;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BuildInvocations;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.gradle.tooling.model.idea.IdeaProject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class UI {

    public static final String DEFAULT_VERSION = "Default";
    public static final String LOCAL_DISTRIBUTION = "Local distribution";
    private final JButton runBuild;
    private final List<VisualizationPanel<?>> panels;
    private final JButton cancel;
    private final MainPanel panel;
    private final List<JButton> buttons;
    private final PathControl projectDirSelector;
    private final PathControl installation;
    private final JTextField commandLineArgs;
    private final JCheckBox embedded;
    private final ConsolePanel console;
    private final ConsolePanel log;
    private final AtomicReference<CancellationTokenSource> token = new AtomicReference<>();
    private final PrintStream originalStdOut;
    private final PrintStream originalStdErr;
    private final JComboBox<Object> gradleVersion;

    public UI() {
        originalStdOut = System.out;
        originalStdErr = System.err;
        runBuild = new JButton("Build");
        cancel = new JButton("Cancel");
        VisualizationPanel<GradleBuild> projects = new VisualizationPanel<>(new FetchModel<>(GradleBuild.class), new ProjectTree());
        VisualizationPanel<BuildInvocations> tasks = new VisualizationPanel<>(new FetchModel<>(BuildInvocations.class), new TasksTable());
        VisualizationPanel<BuildEnvironment> buildEnvironment = new VisualizationPanel<>(new FetchModel<>(BuildEnvironment.class), new BuildEnvironmentReport());
        VisualizationPanel<EclipseProject> eclipseProject = new VisualizationPanel<>(new FetchModel<>(EclipseProject.class), new EclipseModelReport());
        VisualizationPanel<IdeaProject> ideaProject = new VisualizationPanel<>(new FetchModel<>(IdeaProject.class), new IdeaModelReport());
        VisualizationPanel<MultiModel> multiModel = new VisualizationPanel<>(new RunBuildActionAction(), new MultiModelReport());
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
        projectDirSelector = new PathControl();
        installation = new PathControl();
        commandLineArgs = new JTextField();
        embedded = new JCheckBox("Embedded");
        gradleVersion = new JComboBox<>(new Object[]{LOCAL_DISTRIBUTION, DEFAULT_VERSION, "2.1", "2.0", "1.12", "1.0", "1.0-milestone-8", "1.0-milestone-3", "0.9.2"});
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
        settings.addControl(embedded);

        panel.addToolbarControl("Command-line arguments", commandLineArgs);
        commandLineArgs.addActionListener(new BuildAction<>(new RunBuildAction()));
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new BuildAction<>(new RunBuildAction()));
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

    private class VisualizationPanel<T> implements ProgressAwareVisualization<T> {
        private final Visualization<? super T> visualization;
        private final JButton button;
        private final JLayeredPane main;
        private final JLabel overlay;
        private final JPanel overlayPanel;
        private final JScrollPane mainWrapper;

        private VisualizationPanel(ToolingOperation<? extends T> operation, Visualization<? super T> visualization) {
            this.visualization = visualization;
            this.main = new JLayeredPane();

            JComponent mainComponent = visualization.getMainComponent();
            mainWrapper = new JScrollPane(mainComponent);
            main.add(mainWrapper, JLayeredPane.DEFAULT_LAYER);

            overlay = new JLabel();
            overlay.setText(String.format("Click '%s' to load", visualization.getDisplayName()));
            overlay.setFont(overlay.getFont().deriveFont(18f));
            overlayPanel = new JPanel();
            overlayPanel.add(overlay);
            overlayPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
            main.add(overlayPanel, JLayeredPane.MODAL_LAYER);
            main.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    resizeOverlay();
                }
            });
            button = new JButton(visualization.getDisplayName());
            button.addActionListener(new BuildAction<>(operation, this));
            main.add(button, JLayeredPane.DEFAULT_LAYER);
        }

        void resizeOverlay() {
            Dimension buttonSize = button.getPreferredSize();
            button.setLocation(main.getWidth() - buttonSize.width, 0);
            button.setSize(buttonSize);
            mainWrapper.setLocation(0, buttonSize.height + 5);
            mainWrapper.setSize(main.getSize().width, main.getSize().height - mainWrapper.getY());
            overlayPanel.setSize(overlayPanel.getPreferredSize());
            overlayPanel.setLocation((main.getWidth() - overlayPanel.getWidth()) / 2,
                    (main.getHeight() - overlayPanel.getHeight()) / 4);
        }

        @Override
        public String getDisplayName() {
            return visualization.getDisplayName();
        }

        @Override
        public void started() {
            visualization.getMainComponent().setEnabled(false);
            overlay.setText("Loading");
            resizeOverlay();
            overlayPanel.setVisible(true);
        }

        @Override
        public void update(T model) {
            overlayPanel.setVisible(false);
            button.setText("Refresh");
            resizeOverlay();
            visualization.getMainComponent().setEnabled(true);
            visualization.update(model);
        }

        @Override
        public void failed() {
            overlay.setText("Failed");
        }

        public JButton getLaunchButton() {
            return button;
        }

        public JComponent getMainComponent() {
            return main;
        }
    }

    private class BuildAction<T> implements ActionListener {
        private final ToolingOperation<? extends T> operation;
        private final ProgressAwareVisualization<? super T> visualization;

        protected BuildAction(ToolingOperation<T> operation) {
            this.operation = operation;
            visualization = new ProgressAwareVisualization<T>() {
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

                public void update(T result) {
                }
            };
        }

        protected BuildAction(ToolingOperation<? extends T> operation, ProgressAwareVisualization<? super T> visualization) {
            this.operation = operation;
            this.visualization = visualization;
        }

        public void actionPerformed(ActionEvent actionEvent) {
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

            final boolean isEmbedded = embedded.isSelected();
            final String[] commandLine = commandLineArgs.getText().trim().split("\\s+");
            final CancellationTokenSource tokenSource = DefaultGradleConnector.newCancellationTokenSource();
            token.set(tokenSource);
            final UIContext uiContext = new UIContext(projectDir, distribution, isEmbedded, console.getOutput(), Arrays.asList(commandLine)) {
                @Override
                public void setup(LongRunningOperation operation) {
                    operation.withCancellationToken(tokenSource.token());
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
            visualization.started();
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
                        if (version != null) {
                            connector.useGradleVersion(version);
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
                                    visualization.update(finalResult);
                                } else {
                                    visualization.failed();
                                }
                            }
                        });
                    }
                }
            }.start();
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
}

