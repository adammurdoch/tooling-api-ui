package org.gradle.sample;

import org.gradle.gui.ConsolePanel;
import org.gradle.gui.MainPanel;
import org.gradle.gui.PathControl;
import org.gradle.gui.UIContext;
import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.eclipse.EclipseSourceDirectory;
import org.gradle.tooling.model.idea.IdeaProject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
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

    private interface ToolingOperation {
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

    private static class RunBuildAction implements ToolingOperation {
        @Override
        public String getDisplayName(UIContext uiContext) {
            return "build using " + uiContext.getCommandLineArgs();
        }

        @Override
        public void run(ProjectConnection connection, UIContext uiContext) {
            BuildLauncher launcher = connection.newBuild();
            uiContext.setup(launcher);
            launcher.run();
        }
    }

    private static class RunBuildActionAction implements ToolingOperation {
        @Override
        public String getDisplayName(UIContext uiContext) {
            return "client action";
        }

        @Override
        public void run(ProjectConnection connection, UIContext uiContext) {
            BuildActionExecuter<MultiModel> executer = connection.action(new ToolingBuildAction());
            uiContext.setup(executer);
            MultiModel result = executer.run();

            PrintStream stdOut = uiContext.getConsoleStdOut();

            GradleProject gradleProject = result.gradleProject;
            stdOut.println("== GRADLE ==");
            stdOut.format("path: %s%n", gradleProject.getPath());
            stdOut.format("name: %s%n", gradleProject.getName());
            stdOut.format("build script: %s%n", gradleProject.getBuildScript().getSourceFile());

            EclipseProject eclipseProject = result.eclipseProject;
            stdOut.println();
            stdOut.println("== ECLIPSE ==");
            stdOut.format("name: %s%n", eclipseProject.getName());
            stdOut.format("project dir: %s%n", eclipseProject.getProjectDirectory());

            IdeaProject ideaProject = result.ideaProject;
            stdOut.println();
            stdOut.println("== IDEA ==");
            stdOut.format("name: %s%n", ideaProject.getName());
            stdOut.format("jdk: %s%n", ideaProject.getJdkName());
            stdOut.format("Java language: %s%n", ideaProject.getLanguageLevel().getLevel());
            stdOut.format("output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getOutputDir());
            stdOut.format("test output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getTestOutputDir());
        }
    }

    private static class MultiModel implements Serializable {
        GradleProject gradleProject;
        EclipseProject eclipseProject;
        IdeaProject ideaProject;
    }

    private static class ToolingBuildAction implements org.gradle.tooling.BuildAction<MultiModel> {
        @Override
        public MultiModel execute(BuildController controller) {
            MultiModel result = new MultiModel();
            result.gradleProject = controller.getModel(GradleProject.class);
            result.eclipseProject = controller.getModel(EclipseProject.class);
            result.ideaProject = controller.getModel(IdeaProject.class);
            return result;
        }
    }

    private static class FetchEclipseModel implements ToolingOperation {
        @Override
        public String getDisplayName(UIContext uiContext) {
            return "fetch Eclipse model";
        }

        @Override
        public void run(ProjectConnection connection, UIContext uiContext) {
            ModelBuilder<EclipseProject> model = connection.model(EclipseProject.class);
            uiContext.setup(model);
            EclipseProject project = model.get();
            show(project, uiContext.getConsoleStdOut());
        }

        private void show(EclipseProject project, PrintStream output) {
            output.println();
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
            output.println();

            for (EclipseProject childProject : project.getChildren()) {
                show(childProject, output);
            }
        }
    }
}

