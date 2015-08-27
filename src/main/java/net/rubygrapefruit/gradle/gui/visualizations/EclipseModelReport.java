package net.rubygrapefruit.gradle.gui.visualizations;

import org.gradle.tooling.model.eclipse.EclipseProject;

public class EclipseModelReport extends Report<EclipseProject> {
    public EclipseModelReport() {
        super("Eclipse model");
    }

    @Override
    protected void render(EclipseProject project, StructureVisitor tree) {
        tree.struct("Project", project.getName(), () -> {
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
            tree.collection("Dependencies", project.getProjectDependencies(), dependency -> {
                tree.struct(dependency.getTargetProject().getName(), dependency, eclipseDependency -> {
                    tree.value("Exported", eclipseDependency.isExported());
                });
            });
            tree.collection("Classpath", project.getClasspath(), entry -> {
                String coords = String.format("Library %s:%s:%s", entry.getGradleModuleVersion().getGroup(),
                        entry.getGradleModuleVersion().getName(), entry.getGradleModuleVersion().getVersion());
                tree.struct(coords, entry, externalDependency -> {
                    tree.value("Exported", externalDependency.isExported());
                    tree.value("File", externalDependency.getFile().getName());
                });
            });
            tree.collection("Children", project.getChildren(), child -> render(child, tree));
        });
    }
}
