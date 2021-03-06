package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.TaskSelector;
import org.gradle.tooling.model.gradle.BuildInvocations;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class TasksTable implements Visualization<BuildInvocations> {
    private final JTable table;

    public TasksTable() {
        table = new JTable();
    }

    @Override
    public String getDisplayName() {
        return "Task model";
    }

    @Override
    public JComponent getMainComponent() {
        return table;
    }

    @Override
    public void update(BuildInvocations model) {
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Name", "Display name", "Description"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (TaskSelector selector : model.getTaskSelectors()) {
            tableModel.addRow(new Object[]{selector.getName(), selector.getDisplayName(), selector.getDescription()});
        }
        table.setModel(tableModel);
    }
}
