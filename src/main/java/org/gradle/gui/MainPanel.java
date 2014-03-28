package org.gradle.gui;

import javax.swing.*;
import java.awt.*;

public class MainPanel extends JPanel {
    private final JPanel settings;
    private final JPanel toolbar;
    private final JLabel progress;
    private final ConsolePanel console;
    private final ConsolePanel log;
    private final JTabbedPane tabs;

    public MainPanel() {
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setLayout(new BorderLayout());

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        add(toolbar, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);

        console = new ConsolePanel();
        tabs.addTab("Console", new JScrollPane(console));

        log = new ConsolePanel();
        tabs.addTab("Log", new JScrollPane(log));

        settings = new JPanel();
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        tabs.addTab("Settings", settings);

        progress = new JLabel();
        add(progress, BorderLayout.SOUTH);
    }

    public void addTab(String title, JComponent component) {
        tabs.addTab(title, component);
    }

    public void addToolbarControl(AbstractButton button) {
        toolbar.add(button);
    }

    public void addToolbarControl(String title, JComponent component) {
        toolbar.add(component);
    }

    public void addControl(AbstractButton button) {
        settings.add(button);
    }

    public void addControl(String title, JComponent comp) {
        settings.add(new JLabel(title));
        settings.add(comp);
    }

    public ConsolePanel getConsole() {
        return console;
    }

    public ConsolePanel getLog() {
        return log;
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
