package net.rubygrapefruit.gradle.gui;

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
    private int insertIndex;

    public MainPanel() {
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        setLayout(new BorderLayout());

        toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        add(toolbar, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);

        settings = new SettingsPanel();
        tabs.addTab("Settings", settings);

        console = new ConsolePanel(true);
        tabs.addTab("Console", new JScrollPane(console));

        log = new ConsolePanel(false);
        tabs.addTab("Log", new JScrollPane(log));

        insertIndex = 3;

        progress = new JLabel();
        add(progress, BorderLayout.SOUTH);
    }

    public Tab addTab(String title, JComponent component) {
        tabs.insertTab(title, null, component, title, insertIndex);
        tabTitles.put(title, insertIndex);
        return new Tab(insertIndex++);
    }

    public void addToolbarControl(JComponent component) {
        toolbar.add(component);
    }

    public void addToolbarControl(String title, JComponent component) {
        toolbar.add(component);
    }

    public void showConsole() {
        tabs.setSelectedIndex(1);
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

    /**
     * Can be invoked from any thread.
     */
    public void onProgress(final String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            progress.setText(text);
        }
        else {
            SwingUtilities.invokeLater(() -> progress.setText(text));
        }
    }

    public class Tab {
        private final int i;

        Tab(int i) {
            this.i = i;
        }

        public void show() {
            tabs.setSelectedIndex(i);
        }
    }
}
