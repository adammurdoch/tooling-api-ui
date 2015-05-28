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
            tree.value("JDK", project.getJdkName());
            tree.value("Java version", project.getLanguageLevel().getLevel());
            tree.collection("Modules", project.getModules(), module -> {
                tree.struct("Module", module.getName(), () -> {
                    tree.collection("Dependencies", module.getDependencies(), dependency -> {
                        if (dependency instanceof IdeaModuleDependency) {
                            IdeaModuleDependency moduleDependency = (IdeaModuleDependency) dependency;
                            tree.struct(String.format("Module %s", moduleDependency.getDependencyModule().getName()), moduleDependency, dep -> {
                                tree.value("Exported", dep.getExported());
                                tree.value("Scope", dep.getScope());
                            });
                        } else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
                            IdeaSingleEntryLibraryDependency libraryDependency = (IdeaSingleEntryLibraryDependency) dependency;
                            String coords = String.format("Library %s:%s:%s", libraryDependency.getGradleModuleVersion().getGroup(),
                                    libraryDependency.getGradleModuleVersion().getName(), libraryDependency.getGradleModuleVersion().getVersion());
                            tree.struct(coords, libraryDependency, dep -> {
                                tree.value("Exported", dep.getExported());
                                tree.value("Scope", dep.getScope());
                            });
                        } else {
                            tree.value(dependency.toString());
                        }
                    });
                });
            });
        });
    }
}
