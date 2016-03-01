package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

public class IdeaModelReport extends Report<IdeaProject> {
    public IdeaModelReport() {
        super("IDEA model");
    }
    private void renderJavaSettings(IdeaJavaLanguageSettings settings, StructureVisitor tree) {
        if (settings != null) {
            tree.value("Java source version", inherited(settings.getLanguageLevel()));
            tree.value("Java target version", inherited(settings.getTargetBytecodeVersion()));
            if (settings.getJdk() != null) {
                tree.value("JDK version", settings.getJdk().getJavaVersion());
                tree.value("JDK home", settings.getJdk().getJavaHome());
            } else {
                tree.value("JDK version", "(inherited)");
                tree.value("JDK home", "(inherited)");
            }
        }
    }

    private Object inherited(Object value) {
        return value == null ? "(inherited)" : value;
    }

    @Override
    protected void render(IdeaProject project, StructureVisitor tree) {
        tree.struct("Project", project.getName(), () -> {
            tree.value("JDK", project.getJdkName());
            tree.value("Java language level", project.getLanguageLevel().getLevel());
            renderJavaSettings(project.getJavaLanguageSettings(), tree);
            tree.collection("Modules", project.getModules(), module -> {
                tree.struct("Module", module.getName(), () -> {
                    renderJavaSettings(module.getJavaLanguageSettings(), tree);
                    tree.collection("Content roots", module.getContentRoots(), contentRoot -> {
                        tree.value(contentRoot.getRootDirectory());
                    });
                    tree.value("Compiler main output", module.getCompilerOutput().getOutputDir());
                    tree.value("Compiler test output", module.getCompilerOutput().getTestOutputDir());
                    tree.value("Compiler output inherited", module.getCompilerOutput().getInheritOutputDirs());
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
