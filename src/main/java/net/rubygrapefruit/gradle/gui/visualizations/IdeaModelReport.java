package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

public class IdeaModelReport extends Report<IdeaProject> {
    public IdeaModelReport() {
        super("IDEA model");
    }

    @Override
    protected void render(IdeaProject project, StructureVisitor tree) {
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
