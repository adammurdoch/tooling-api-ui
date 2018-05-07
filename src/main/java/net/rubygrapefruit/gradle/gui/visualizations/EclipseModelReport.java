package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.GradleModuleVersion;
import org.gradle.tooling.model.eclipse.EclipseProject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EclipseModelReport extends Report<EclipseProject> {
    public EclipseModelReport() {
        super("Eclipse model");
    }

    @Override
    protected void render(EclipseProject root, StructureVisitor tree) {
        List<EclipseProject> projects = new ArrayList<>();
        collect(root, projects);

        for (EclipseProject project : projects) {
            tree.struct("Project", project.getName(), () -> {
                if (project.getJavaSourceSettings() != null) {
                    tree.value("Java source version",
                            project.getJavaSourceSettings().getSourceLanguageLevel());
                    tree.value("Java target version",
                            project.getJavaSourceSettings().getTargetBytecodeVersion());
                    tree.value("Build JDK version",
                            project.getJavaSourceSettings().getJdk().getJavaVersion());
                    tree.value("Build JDK home",
                            project.getJavaSourceSettings().getJdk().getJavaHome());
                }
                tree.collection("Source directories", project.getSourceDirectories(), srcDir -> {
                    tree.struct("Path " + srcDir.getPath(), srcDir, entry -> {
                        tree.value("Directory", srcDir.getDirectory());
                    });
                });
                tree.collection("Linked resources", project.getLinkedResources(), linkedResource -> {
                    tree.struct("Path " + linkedResource.getName(), linkedResource, entry -> {
                        tree.value("Location", linkedResource.getLocation());
                        tree.value("Location URI", linkedResource.getLocationUri());
                        tree.value("Link type", linkedResource.getType());
                    });
                });
                tree.collection("Project dependencies", project.getProjectDependencies(), dependency -> {
                    tree.struct("Project " + dependency.getPath(), dependency, eclipseDependency -> {
                        tree.value("Exported", eclipseDependency.isExported());
                    });
                });
                tree.collection("Classpath", project.getClasspath(), entry -> {
                    GradleModuleVersion id = entry.getGradleModuleVersion();
                    String displayName;
                    if (id != null) {
                        displayName = String.format("Library %s:%s:%s", id.getGroup(),
                                id.getName(), id.getVersion());
                    } else {
                        displayName = "Library " + entry.getFile().getName();
                    }
                    tree.struct(displayName, entry, externalDependency -> {
                        tree.value("Exported", externalDependency.isExported());
                        tree.value("File", externalDependency.getFile());
                    });
                });
                tree.collection("Natures", project.getProjectNatures(), nature -> {
                    tree.value(nature.getId());
                });
                tree.collection("Builders", project.getBuildCommands(), buildCommand -> {
                    tree.value(buildCommand.getName());
                });
                tree.collection("Child projects", project.getChildren(), child -> {
                    tree.value("Project " + child.getName());
                });
            });
        }
    }

    private void collect(EclipseProject project, Collection<EclipseProject> projects) {
        projects.add(project);
        for (EclipseProject child : project.getChildren()) {
            collect(child, projects);
        }
    }
}
