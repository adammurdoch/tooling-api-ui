package org.gradle.sample;

import org.gradle.gui.ConsolePanel;
import org.gradle.gui.MainPanel;
import org.gradle.gui.PathControl;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;

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
        commandLineArgs.addActionListener(new RunBuildAction());
        panel.addToolbarControl(runBuild);
        runBuild.addActionListener(new RunBuildAction());
        panel.addToolbarControl(buildModel);
        buildModel.addActionListener(new FetchEclipseModel());
        panel.addToolbarControl(runAction);
        runAction.addActionListener(new RunBuildActionAction());
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

    private void onFinishOperation(long timeMillis) {
        console.getOutput().flush();
        console.getError().flush();
        log.getOutput().flush();
        log.getError().flush();
        panel.onProgress("Finished (" + timeMillis/1000 + " seconds)");
        buildModel.setEnabled(true);
        runBuild.setEnabled(true);
        runAction.setEnabled(true);
    }

    private abstract class BuildAction implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            onStartOperation(getDisplayName());
            final File projectDir = projectDirSelector.getFile();
            final File distribution = useDistribution.isSelected() ? installation.getFile() : null;
            final boolean isEmbedded = embedded.isSelected();
            panel.onProgress("building for project dir: " + projectDir);
            new Thread() {
                @Override
                public void run() {
                    final long startTime = System.currentTimeMillis();
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
                            BuildAction.this.run(connection);
                        } finally {
                            connection.close();
                        }
                    } catch (Throwable t) {
                        log.getError().println("FAILED WITH EXCEPTION");
                        t.printStackTrace(log.getError());
                    } finally {
                        final long endTime = System.currentTimeMillis();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                onFinishOperation(endTime - startTime);
                            }
                        });
                    }
                }
            }.start();
        }

        protected abstract String getDisplayName();

        protected abstract void run(ProjectConnection connection);

        protected void setup(LongRunningOperation operation) {
            operation.addProgressListener(new ProgressListener() {
                public void statusChanged(ProgressEvent event) {
                    log.getOutput().println("[progress: " + event.getDescription() + "]");
                    panel.onProgress(event.getDescription());
                }
            });
            operation.setStandardOutput(console.getOutput());
            operation.setStandardError(console.getError());
            if (!commandLineArgs.getText().trim().isEmpty()) {
                operation.withArguments(commandLineArgs.getText().split("\\s+"));
            }
        }
    }

    private class RunBuildAction extends BuildAction {
        @Override
        protected String getDisplayName() {
            return "build using " + commandLineArgs.getText();
        }

        @Override
        protected void run(ProjectConnection connection) {
            BuildLauncher launcher = connection.newBuild();
            setup(launcher);
            launcher.run();
        }
    }

    private class RunBuildActionAction extends BuildAction {
        @Override
        protected String getDisplayName() {
            return "client action";
        }

        @Override
        protected void run(ProjectConnection connection) {
            BuildActionExecuter<String> executer = connection.action(new ToolingBuildAction());
            setup(executer);
            String result = executer.run();
            console.getOutput().format("result: %s%n", result);
        }

    }

    private static class ToolingBuildAction implements org.gradle.tooling.BuildAction<String> {
        @Override
        public String execute(BuildController controller) {
            return "running in build process!";
        }
    }

    private class FetchEclipseModel extends BuildAction {
        @Override
        protected String getDisplayName() {
            return "fetch Eclipse model";
        }

        @Override
        protected void run(ProjectConnection connection) {
            ModelBuilder<EclipseProject> model = connection.model(EclipseProject.class);
            setup(model);
            EclipseProject project = model.get();
            show(project);
        }

        private void show(EclipseProject project) {
            PrintStream output = console.getOutput();
            output.println("PROJECT");
            output.format("%s (%s)%n", project.getName(), project);
            output.format("build script: %s%n", project.getGradleProject().getBuildScript().getSourceFile());
            output.println();

            output.println("SOURCE DIRECTORIES");
            for (EclipseSourceDirectory sourceDirectory : project.getSourceDirectories()) {
                output.format("%s -> %s%n", sourceDirectory.getPath(), sourceDirectory.getDirectory());
            }
            output.println();

            output.println("CLASSPATH");
            for (ExternalDependency dependency : project.getClasspath()) {
                output.format("%s -> %s%n", dependency.getGradleModuleVersion(), dependency.getFile());
            }
            output.println();

            output.println("TASKS");
            for (Task task : project.getGradleProject().getTasks()) {
                output.format("%s (%s)%n", task.getName(), task);
            }
            for (EclipseProject childProject : project.getChildren()) {
                show(childProject);
            }
        }
    }
}

