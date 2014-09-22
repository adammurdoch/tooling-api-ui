package org.gradle.gui;

import org.gradle.gui.actions.*;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.gradle.GradleBuild;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

public class UI {

    private final JButton eclipseModel;
    private final JButton runBuild;
    private final JButton runAction;
    private final VisualizationPanel<GradleBuild> projects;
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
        projects = new VisualizationPanel<>(new GetBuildModel(), new ProjectTree());
        buttons = Arrays.asList(eclipseModel, ideaModel, projects.getLaunchButton(), runAction, runBuild);
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
        panel.addToolbarControl(projects.getLaunchButton());
        panel.addTab(projects.visualization.getDisplayName(), projects.getMainComponent());
        panel.addToolbarControl(eclipseModel);
        eclipseModel.addActionListener(new BuildAction<>(new FetchEclipseModel()));
        panel.addToolbarControl(ideaModel);
        ideaModel.addActionListener(new BuildAction<>(new FetchIdeaModel()));
        panel.addToolbarControl(runAction);
        runAction.addActionListener(new BuildAction<>(new RunBuildActionAction()));
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

    private class VisualizationPanel<T> implements ProgressAwareVisualization<T> {
        private final Visualization<? super T> visualization;
        private final JButton button;
        private final JLayeredPane main;
        private final JLabel overlay;
        private final JPanel overlayPanel;

        private VisualizationPanel(ToolingOperation<? extends T> operation, Visualization<? super T> visualization) {
            this.visualization = visualization;
            this.main = new JLayeredPane();
            JComponent mainComponent = visualization.getMainComponent();
            main.add(mainComponent, JLayeredPane.DEFAULT_LAYER);
            overlay = new JLabel();
            overlayPanel = new JPanel();
            overlayPanel.add(overlay);
            overlayPanel.setVisible(false);
            main.add(overlayPanel, JLayeredPane.MODAL_LAYER);
            main.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    mainComponent.setLocation(0, 0);
                    mainComponent.setSize(main.getSize());
                    resizeOverlay();
                }
            });
            button = new JButton(visualization.getDisplayName());
            button.addActionListener(new BuildAction<>(operation, this));
        }

        void resizeOverlay() {
            overlayPanel.setSize(overlayPanel.getPreferredSize());
            overlayPanel.setLocation((main.getWidth() - overlayPanel.getWidth()) / 2,
                    (main.getHeight() - overlayPanel.getHeight()) / 2);
        }

        @Override
        public String getDisplayName() {
            return visualization.getDisplayName();
        }

        @Override
        public void started() {
            visualization.getMainComponent().setEnabled(false);
            overlay.setText("Fetching");
            resizeOverlay();
            overlayPanel.setVisible(true);
        }

        @Override
        public void update(T model) {
            overlayPanel.setVisible(false);
            visualization.getMainComponent().setEnabled(true);
            visualization.update(model);
        }

        @Override
        public void failed() {
            overlayPanel.setVisible(false);
            visualization.failed();
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
}

