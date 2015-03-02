package net.rubygrapefruit.gradle.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class PathControl extends JPanel {
    private final JTextField path;

    public PathControl() {
        path = new JTextField();
        path.setColumns(30);
        add(path);
        JButton select = new JButton("...");
        select.addActionListener(new PathControl.SelectFileAction());
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
