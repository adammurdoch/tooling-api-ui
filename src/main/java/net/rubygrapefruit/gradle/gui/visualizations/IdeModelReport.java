package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.java.JavaSourceAware;

public abstract class IdeModelReport<T> extends Report<T> {
    public IdeModelReport(String title) {
        super(title);
    }

    protected void renderJavaSettings(JavaSourceAware project, StructureVisitor tree) {
        if (project.getJavaSourceSettings() != null) {
            tree.value("Java source version",
                    project.getJavaSourceSettings().getSourceLanguageLevel());
            tree.value("Java target version",
                    project.getJavaSourceSettings().getTargetBytecodeLevel());
            tree.value("JDK version",
                    project.getJavaSourceSettings().getTargetRuntime().getJavaVersion());
            tree.value("JDK home",
                    project.getJavaSourceSettings().getTargetRuntime().getHomeDirectory());
        }
    }
}
