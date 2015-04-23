package net.rubygrapefruit.gradle.gui.visualizations;

public interface TreeVisitor<T> {
    void node(T node);

    void startChildren();

    void endChildren();
}
