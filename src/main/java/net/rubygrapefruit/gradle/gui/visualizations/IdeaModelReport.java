package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.jarjar.com.google.common.base.Function;
import org.gradle.tooling.model.idea.IdeaJavaLanguageSettings;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSingleEntryLibraryDependency;

public class IdeaModelReport extends Report<IdeaProject> {
    public IdeaModelReport() {
        super("IDEA model");
    }
    private void renderJavaSettings(IdeaJavaLanguageSettings settings, IdeaJavaLanguageSettings inheritFrom, StructureVisitor tree) {
        if (settings == null) {
            return;
        }

        tree.value("Java source version", inherited(settings, inheritFrom, s -> s.getLanguageLevel()));
        tree.value("Java target version", inherited(settings, inheritFrom, s -> s.getTargetBytecodeVersion()));
        tree.value("JDK version",
                inherited(settings, inheritFrom, s -> s.getJdk() == null ? null : s.getJdk().getJavaVersion()));
        tree.value("JDK home",
                inherited(settings, inheritFrom, s -> s.getJdk() == null ? null : s.getJdk().getJavaHome()));
    }

    private Object inherited(IdeaJavaLanguageSettings settings, IdeaJavaLanguageSettings inheritFrom, Function<IdeaJavaLanguageSettings, Object> extractor) {
        Object value = extractor.apply(settings);
        if (value != null) {
            return value;
        }
        return String.format("(inherited - %s)", extractor.apply(inheritFrom));
    }

    private Object inherited(boolean inherited, Object value) {
        if (inherited && value == null) {
            return "(inherited)";
        }
        if (inherited) {
            return value + " (should be null)";
        }
        if (value == null) {
            return "null (should be non-null)";
        }
        return value;
    }

    @Override
    protected void render(IdeaProject project, StructureVisitor tree) {
        tree.struct("Project", project.getName(), () -> {
            tree.value("JDK", project.getJdkName());
            tree.value("Java language level", project.getLanguageLevel().getLevel());
            renderJavaSettings(project.getJavaLanguageSettings(), null, tree);
            tree.collection("Modules", project.getModules(), module -> {
                tree.struct("Module", module.getName(), () -> {
                    renderJavaSettings(module.getJavaLanguageSettings(), project.getJavaLanguageSettings(), tree);
                    tree.collection("Content roots", module.getContentRoots(), contentRoot -> {
                        tree.value("Directory", contentRoot.getRootDirectory());
                        tree.collection("Source directories", contentRoot.getSourceDirectories(), sourceDir -> {
                            tree.value("Directory", sourceDir.getDirectory());
                            tree.value("Generated", sourceDir.isGenerated());
                        });
                        tree.collection("Generated Source directories", contentRoot.getGeneratedSourceDirectories(), sourceDir -> {
                            tree.value("Directory", sourceDir.getDirectory());
                            tree.value("Generated", sourceDir.isGenerated());
                        });
                        tree.collection("Test Source directories", contentRoot.getTestDirectories(), sourceDir -> {
                            tree.value("Directory", sourceDir.getDirectory());
                            tree.value("Generated", sourceDir.isGenerated());
                        });
                        tree.collection("Generated test Source directories", contentRoot.getGeneratedTestDirectories(), sourceDir -> {
                            tree.value("Directory", sourceDir.getDirectory());
                            tree.value("Generated", sourceDir.isGenerated());
                        });
                        tree.collection("Excluded directories", contentRoot.getExcludeDirectories(), dir -> {
                            tree.value("Directory", dir);
                        });
                    });
                    tree.value("Compiler main output", inherited(module.getCompilerOutput().getInheritOutputDirs(), module.getCompilerOutput().getOutputDir()));
                    tree.value("Compiler test output", inherited(module.getCompilerOutput().getInheritOutputDirs(), module.getCompilerOutput().getTestOutputDir()));
                    tree.collection("Dependencies", module.getDependencies(), dependency -> {
                        if (dependency instanceof IdeaModuleDependency) {
                            IdeaModuleDependency moduleDependency = (IdeaModuleDependency) dependency;
                            tree.struct(String.format("Module %s", moduleDependency.getDependencyModule().getName()), moduleDependency, dep -> {
                                tree.value("Exported", dep.getExported());
                                tree.value("Scope", dep.getScope().getScope());
                            });
                        } else if (dependency instanceof IdeaSingleEntryLibraryDependency) {
                            IdeaSingleEntryLibraryDependency libraryDependency = (IdeaSingleEntryLibraryDependency) dependency;
                            String coords = String.format("Library %s:%s:%s", libraryDependency.getGradleModuleVersion().getGroup(),
                                    libraryDependency.getGradleModuleVersion().getName(), libraryDependency.getGradleModuleVersion().getVersion());
                            tree.struct(coords, libraryDependency, dep -> {
                                tree.value("Scope", dep.getScope().getScope());
                                tree.value("Exported", dep.getExported());
                                tree.value("File", dep.getFile());
                                tree.value("Source", dep.getSource());
                                tree.value("Javadoc", dep.getJavadoc());
                            });
                        } else {
                            tree.value("(unknown - " + dependency + ")");
                        }
                    });
                });
            });
        });
    }
}
