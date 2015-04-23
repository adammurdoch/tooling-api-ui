package net.rubygrapefruit.gradle.gui.visualizations;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.util.Collection;
import java.util.function.Consumer;

public class JTreeBackedStructureVisitor implements StructureVisitor {
    private final JTreeBackedVisitor tree;

    public JTreeBackedStructureVisitor(String title) {
        this.tree = new JTreeBackedVisitor(title);
    }

    public JComponent getTree() {
        return tree.getTree();
    }

    public void reset() {
        tree.reset();
    }

    @Override
    public void value(Object value) {
        tree.node(value);
    }

    @Override
    public void value(String name, Object value) {
        tree.node(String.format("%s: %s", name, value));
    }

    @Override
    public void struct(String name, Object value, Runnable renderer) {
        value(name, value);
        tree.startChildren();
        renderer.run();
        tree.endChildren();
    }

    @Override
    public <T> void collection(String name, Collection<T> collection, Consumer<T> renderer) {
        if (collection.isEmpty()) {
            tree.node(String.format("%s - empty", name));
            return;
        }

        tree.node(name);
        tree.startChildren();
        for (T t : collection) {
            renderer.accept(t);
        }
        tree.endChildren();
    }
}
