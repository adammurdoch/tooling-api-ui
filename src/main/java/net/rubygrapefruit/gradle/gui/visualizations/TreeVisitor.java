package net.rubygrapefruit.gradle.gui.visualizations;

public interface TreeVisitor {
    void node(Object node);

    void startChildren();

    void endChildren();
}
