package net.rubygrapefruit.gradle.gui.visualizations;

import javax.swing.*;
import java.io.File;
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
        tree.node(format(value));
    }

    private String format(Object value) {
        if (value == null) {
            return "(null)";
        }
        if (value instanceof File) {
            File file = (File) value;
            if (file.isFile()) {
                return String.format("%s (%s)", file.getName(), file.getPath());
            }
        }
        return value.toString();
    }

    @Override
    public void value(String name, Object value) {
        tree.node(String.format("%s: %s", name, format(value)));
    }

    @Override
    public <T> void struct(String name, T value, Consumer<T> renderer) {
        value(name);
        tree.startChildren();
        renderer.accept(value);
        tree.endChildren();
    }

    @Override
    public void struct(String name, String value, Runnable renderer) {
        value(name, value);
        tree.startChildren();
        renderer.run();
        tree.endChildren();
    }

    @Override
    public void collection(String name, Collection<?> collection) {
        collection(name, collection, t -> value(t));
    }

    @Override
    public <T> void collection(String name, Collection<T> collection, Consumer<T> renderer) {
        if (collection.isEmpty()) {
            tree.node(String.format("%s: (empty)", name));
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
