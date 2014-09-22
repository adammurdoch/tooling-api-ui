package org.gradle.gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainPanel extends JPanel {
    private final SettingsPanel settings;
    private final JPanel toolbar;
    private final JLabel progress;
    private final ConsolePanel console;
    private final ConsolePanel log;
    private final JTabbedPane tabs;
    private final Map<String, Integer> tabTitles = new HashMap<>();
    private int insertIndex = 1;

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

        settings = new SettingsPanel();
        tabs.addTab("Settings", settings);

        progress = new JLabel();
        add(progress, BorderLayout.SOUTH);
    }

    public void addTab(String title, JComponent component) {
        tabs.insertTab(title, null, component, title, insertIndex);
        tabTitles.put(title, insertIndex);
        insertIndex++;
    }

    public void addToolbarControl(AbstractButton button) {
        toolbar.add(button);
    }

    public void addToolbarControl(String title, JComponent component) {
        toolbar.add(component);
    }

    public void showTab(String title) {
        tabs.setSelectedIndex(tabTitles.get(title));
    }

    public void showConsole() {
        tabs.setSelectedIndex(0);
    }

    public SettingsPanel getSettings() {
        return settings;
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
