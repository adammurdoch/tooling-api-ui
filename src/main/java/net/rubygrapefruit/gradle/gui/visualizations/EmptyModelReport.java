package net.rubygrapefruit.gradle.gui.visualizations;

public class EmptyModelReport extends Report<String> {
    public EmptyModelReport() {
        super("No-op build action");
    }

    @Override
    protected void render(String model, StructureVisitor tree) {
        tree.value(model);
    }
}
