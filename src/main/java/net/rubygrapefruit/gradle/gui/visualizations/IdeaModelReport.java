package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.idea.*;

import javax.swing.*;

public class IdeaModelReport implements Visualization<IdeaProject> {
    private final JTreeBackedVisitor<String> tree = new JTreeBackedVisitor<>("IDEA model");

    @Override
    public String getDisplayName() {
        return "IDEA model";
    }

    @Override
    public JComponent getMainComponent() {
        return tree.getTree();
    }

    @Override
    public void update(IdeaProject project) {
        tree.reset();
        tree.node(String.format("Project %s", project.getName()));
        tree.startChildren();
        tree.node("Modules");
        tree.startChildren();
        for (IdeaModule module : project.getModules()) {
            tree.node(String.format("Module %s", module.getName()));
            tree.startChildren();
            tree.node("Dependencies");
            tree.startChildren();
            for (IdeaDependency dependency : module.getDependencies()) {
                if (dependency instanceof IdeaModuleDependency) {
                    IdeaModuleDependency moduleDependency = (IdeaModuleDependency) dependency;
                    tree.node(String.format("Module %s (%s)", moduleDependency.getDependencyModule().getName(), moduleDependency.getScope().getScope()));
                } else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
                    IdeaSingleEntryLibraryDependency libraryDependency = (IdeaSingleEntryLibraryDependency) dependency;
                    tree.node(String.format("Library %s (%s)", libraryDependency.getGradleModuleVersion(), libraryDependency.getScope().getScope()));
                } else {
                    tree.node(dependency.toString());
                }
            }
            tree.endChildren();
            tree.endChildren();
        }
        tree.endChildren();
        tree.endChildren();
    }
}
