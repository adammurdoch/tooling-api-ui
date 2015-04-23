package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;
import org.gradle.tooling.model.idea.*;

import javax.swing.*;

public class IdeaModelReport implements Visualization<IdeaProject> {
    private final JTreeBackedStructureVisitor tree = new JTreeBackedStructureVisitor("IDEA model");

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
        tree.struct("Project", project.getName(), () -> {
            tree.collection("Modules", project.getModules(), module -> {
                tree.struct("Module", module.getName(), () -> {
                    tree.collection("Dependencies", module.getDependencies(), dependency -> {
                        if (dependency instanceof IdeaModuleDependency) {
                            IdeaModuleDependency moduleDependency = (IdeaModuleDependency) dependency;
                            tree.value(String.format("Module %s (%s)", moduleDependency.getDependencyModule().getName(), moduleDependency.getScope().getScope()));
                        } else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
                            IdeaSingleEntryLibraryDependency libraryDependency = (IdeaSingleEntryLibraryDependency) dependency;
                            tree.value(String.format("Library %s (%s)", libraryDependency.getGradleModuleVersion(), libraryDependency.getScope().getScope()));
                        } else {
                            tree.value(dependency.toString());
                        }
                    });
                });
            });
        });
    }
}
