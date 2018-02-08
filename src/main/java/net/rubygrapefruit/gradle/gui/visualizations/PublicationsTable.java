package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.gradle.GradlePublication;
import org.gradle.tooling.model.gradle.ProjectPublications;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PublicationsTable implements Visualization<ProjectPublications> {
    private final JTable table;

    public PublicationsTable() {
        table = new JTable();
    }

    @Override
    public String getDisplayName() {
        return "Publications";
    }

    @Override
    public JComponent getMainComponent() {
        return table;
    }

    @Override
    public void update(ProjectPublications model) {
        DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"Project", "Group:module:version"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (GradlePublication publication : model.getPublications()) {
            tableModel.addRow(new Object[]{
                    publication.getProjectIdentifier().getProjectPath(),
                    publication.getId().getGroup() + ":" + publication.getId().getName() + ":" + publication.getId().getVersion()
            });
        }
        table.setModel(tableModel);
    }
}
