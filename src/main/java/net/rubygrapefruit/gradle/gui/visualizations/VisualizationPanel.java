package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.ProgressAwareVisualization;
import net.rubygrapefruit.gradle.gui.ToolingOperation;
import net.rubygrapefruit.gradle.gui.ToolingOperationExecuter;
import net.rubygrapefruit.gradle.gui.Visualization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class VisualizationPanel<T> implements ProgressAwareVisualization<T> {
    private final ToolingOperation<? extends T> operation;
    private final Visualization<? super T> visualization;
    private final ToolingOperationExecuter executer;
    private final JButton button;
    private final JLayeredPane main;
    private final JLabel overlay;
    private final JPanel overlayPanel;
    private final JScrollPane mainWrapper;

    public VisualizationPanel(ToolingOperation<? extends T> operation, Visualization<? super T> visualization,
                              ToolingOperationExecuter executer) {
        this.operation = operation;
        this.visualization = visualization;
        this.executer = executer;
        this.main = new JLayeredPane();

        JComponent mainComponent = visualization.getMainComponent();
        mainWrapper = new JScrollPane(mainComponent);
        main.add(mainWrapper, JLayeredPane.DEFAULT_LAYER);

        overlay = new JLabel();
        overlay.setText(String.format("Click '%s' to load", visualization.getDisplayName()));
        overlay.setFont(overlay.getFont().deriveFont(18f));
        overlayPanel = new JPanel();
        overlayPanel.add(overlay);
        overlayPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        main.add(overlayPanel, JLayeredPane.MODAL_LAYER);
        main.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeOverlay();
            }
        });
        button = new JButton("Refresh");
        button.setEnabled(false);
        button.addActionListener(e -> start());
        main.add(button, JLayeredPane.DEFAULT_LAYER);
    }

    void resizeOverlay() {
        Dimension buttonSize = button.getPreferredSize();
        button.setLocation(main.getWidth() - buttonSize.width, 0);
        button.setSize(buttonSize);
        mainWrapper.setLocation(0, buttonSize.height + 5);
        mainWrapper.setSize(main.getSize().width, main.getSize().height - mainWrapper.getY());
        overlayPanel.setSize(overlayPanel.getPreferredSize());
        overlayPanel.setLocation((main.getWidth() - overlayPanel.getWidth()) / 2,
                (main.getHeight() - overlayPanel.getHeight()) / 4);
    }

    public void start() {
        executer.start(operation, this);
    }

    @Override
    public String getDisplayName() {
        return visualization.getDisplayName();
    }

    @Override
    public void started() {
        visualization.getMainComponent().setEnabled(false);
        overlay.setText("Loading");
        resizeOverlay();
        overlayPanel.setVisible(true);
    }

    @Override
    public void update(T model) {
        try {
            visualization.update(model);
        } catch (RuntimeException e) {
            overlay.setText("Failed");
            throw e;
        }
        overlayPanel.setVisible(false);
        resizeOverlay();
        visualization.getMainComponent().setEnabled(true);
    }

    @Override
    public void failed() {
        overlay.setText("Failed");
    }

    public JButton getRefreshButton() {
        return button;
    }

    public JComponent getMainComponent() {
        return main;
    }
}
