package org.gradle.sample;

import org.gradle.tooling.*;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.gradle.tooling.model.Task;
import org.gradle.tooling.model.eclipse.EclipseProject;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    public UI() {
        buildModel = new JButton("Eclipse model");
        runAction = new JButton("Client action");
        runBuild = new JButton("Build");
        panel = new MainPanel();
        console = panel.getConsole();
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
        panel.addControl("Command-line arguments", commandLineArgs);
        buildModel.addActionListener(new BuildModelAction());
        panel.addControl(buildModel);
        runBuild.addActionListener(new RunBuildAction());
        panel.addControl(runBuild);
        runAction.addActionListener(new RunBuildActionAction());
        panel.addControl(runAction);
        frame.setSize(1000, 800);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void onStartOperation() {
        buildModel.setEnabled(false);
        runBuild.setEnabled(false);
        runAction.setEnabled(false);
        console.clearOutput();
    }

    private void onFinishOperation(long timeMillis) {
        console.getOutput().flush();
        console.getError().flush();
        panel.onProgress("Finished (" + timeMillis/1000 + " seconds)");
        buildModel.setEnabled(true);
        runBuild.setEnabled(true);
        runAction.setEnabled(true);
    }

    private abstract class BuildAction implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            onStartOperation();
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
                        console.getError().println("FAILURE:");
                        t.printStackTrace(console.getError());
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

        protected abstract void run(ProjectConnection connection);

        protected void setup(LongRunningOperation operation) {
            operation.addProgressListener(new ProgressListener() {
                public void statusChanged(ProgressEvent event) {
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
        protected void run(ProjectConnection connection) {
            BuildLauncher launcher = connection.newBuild();
            setup(launcher);
            launcher.run();
        }
    }

    private class RunBuildActionAction extends BuildAction {
        @Override
        protected void run(ProjectConnection connection) {
            BuildActionExecuter<String> executer = connection.action(new org.gradle.tooling.BuildAction<String>() {
                @Override
                public String execute(BuildController controller) {
                    return "result";
                }
            });
            setup(executer);
            String result = executer.run();
            console.getOutput().format("result: %s%n", result);
        }
    }

    private class BuildModelAction extends BuildAction {
        @Override
        protected void run(ProjectConnection connection) {
            ModelBuilder<EclipseProject> model = connection.model(EclipseProject.class);
            setup(model);
            EclipseProject project = model.get();
            show(project);
        }

        private void show(EclipseProject project) {
            console.getOutput().format("%s (%s)%n", project.getName(), project);
            for (Task task : project.getGradleProject().getTasks()) {
                console.getOutput().format("    %s (%s)%n", task.getName(), task);
            }
            for (EclipseProject childProject : project.getChildren()) {
                show(childProject);
            }
        }
    }
}

class PathControl extends JPanel {
    private final JTextField path;

    PathControl() {
        path = new JTextField();
        path.setColumns(80);
        add(path);
        JButton select = new JButton("...");
        select.addActionListener(new SelectFileAction());
        add(select);
    }

    public File getFile() {
        return path.getText().isEmpty() ? null : new File(path.getText());
    }

    public void setFile(File file) {
        path.setText(file.getAbsolutePath());
    }

    private class SelectFileAction implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (!path.getText().isEmpty()) {
                File file = new File(path.getText());
                fileChooser.setCurrentDirectory(file.getParentFile());
                fileChooser.setSelectedFile(file);
            }

            int returnVal = fileChooser.showOpenDialog(PathControl.this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                path.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }
}

class MainPanel extends JPanel {
    private final JPanel controls;
    private final JLabel progress;
    private final ConsolePanel console;

    MainPanel() {
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setLayout(new BorderLayout());
        controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        add(controls, BorderLayout.NORTH);

        console = new ConsolePanel();
        add(new JScrollPane(console), BorderLayout.CENTER);

        progress = new JLabel();
        add(progress, BorderLayout.SOUTH);
    }

    void addControl(AbstractButton button) {
        controls.add(button);
    }

    void addControl(String title, JComponent comp) {
        controls.add(new JLabel(title));
        controls.add(comp);
    }

    public ConsolePanel getConsole() {
        return console;
    }

    public void onProgress(final String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            progress.setText(text);
        }
        else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progress.setText(text);
                }
            });
        }
    }
}

class ConsolePanel extends JPanel {
    private final Style stdout;
    private final Style stderr;
    private boolean hasOutput;
    private final JTextPane output;
    private final PrintStream outputStream;
    private final PrintStream errorStream;
    private final BlockingQueue<TextEvent> events = new LinkedBlockingQueue<>();

    ConsolePanel() {
        setLayout(new BorderLayout());

        output = new JTextPane();
        output.setEditable(false);
        output.setEnabled(false);
        output.setText("output goes here..");
        stdout = output.addStyle("stdout", null);
        stderr = output.addStyle("stderr", null);
        StyleConstants.setForeground(stderr, Color.RED);
        add(output, BorderLayout.CENTER);

        outputStream = new PrintStream(new OutputWriter(true), true);
        errorStream = new PrintStream(new OutputWriter(false), true);
    }

    public PrintStream getOutput() {
        return outputStream;
    }

    public PrintStream getError() {
        return errorStream;
    }

    public void clearOutput() {
        output.setText("");
        hasOutput = false;
    }

    private class TextEvent {
        final String text;
        final boolean stdout;

        private TextEvent(String text, boolean stdout) {
            this.text = text;
            this.stdout = stdout;
        }
    }

    private class OutputWriter extends OutputStream {
        private final boolean stdout;

        public OutputWriter(boolean stdout) {
            this.stdout = stdout;
        }

        @Override
        public void write(int i) throws IOException {
            byte[] buffer = new byte[1];
            buffer[0] = (byte) i;
            write(buffer, 0, 1);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            String text = new String(bytes, offset, length);
            events.add(new TextEvent(text, stdout));
            if (SwingUtilities.isEventDispatchThread()) {
                doWriteOutput();
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        doWriteOutput();
                    }
                });
            }
        }

        private void doWriteOutput() {
            while (true) {
                TextEvent event = events.poll();
                if (event == null) {
                    return;
                }
                if (!hasOutput) {
                    output.setText("");
                    output.setEnabled(true);
                    hasOutput = true;
                }
                Document document = output.getDocument();
                try {
                    document.insertString(document.getLength(), event.text, event.stdout ? ConsolePanel.this.stdout : stderr);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}