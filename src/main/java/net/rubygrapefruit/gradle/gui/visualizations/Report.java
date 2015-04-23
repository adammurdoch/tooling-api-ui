package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;

import javax.swing.*;

public abstract class Report<T> implements Visualization<T> {
    private final JTreeBackedStructureVisitor tree;
    private final String title;

    public Report(String title) {
        this.title = title;
        tree = new JTreeBackedStructureVisitor(title);
    }

    @Override
    public String getDisplayName() {
        return title;
    }

    @Override
    public JComponent getMainComponent() {
        return tree.getTree();
    }

    @Override
    public void update(T model) {
        tree.reset();
        render(model, tree);
    }

    protected abstract void render(T model, StructureVisitor output);
}
