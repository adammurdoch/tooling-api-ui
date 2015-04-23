package net.rubygrapefruit.gradle.gui.visualizations;

import java.util.Collection;
import java.util.function.Consumer;

public interface StructureVisitor {
    void value(Object value);

    void value(String name, Object value);

    void struct(String name, Object value, Runnable renderer);

    <T> void collection(String name, Collection<T> collection, Consumer<T> renderer);
}
