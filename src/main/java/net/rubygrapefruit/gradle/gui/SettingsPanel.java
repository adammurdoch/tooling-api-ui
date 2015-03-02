package net.rubygrapefruit.gradle.gui;

import javax.swing.*;

public class SettingsPanel extends JPanel {
    private final GroupLayout layout;
    private final GroupLayout.ParallelGroup titles;
    private final GroupLayout.ParallelGroup components;
    private final GroupLayout.SequentialGroup vertical;

    public SettingsPanel() {
        layout = new GroupLayout(this);
        setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        titles = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
        components = layout.createParallelGroup(GroupLayout.Alignment.LEADING, false);
        GroupLayout.SequentialGroup horizontal = layout.createSequentialGroup();
        horizontal.addGroup(titles);
        horizontal.addGroup(components);
        layout.setHorizontalGroup(horizontal);
        vertical = layout.createSequentialGroup();
        layout.setVerticalGroup(vertical);
    }

    public void addControl(AbstractButton button) {
        components.addComponent(button);
        vertical.addComponent(button);
    }

    public void addControl(String title, JComponent comp) {
        JLabel label = new JLabel(title);
        titles.addComponent(label);
        components.addComponent(comp);
        GroupLayout.ParallelGroup row = layout.createParallelGroup(GroupLayout.Alignment.BASELINE, false);
        row.addComponent(label);
        row.addComponent(comp);
        vertical.addGroup(row);
    }
}
