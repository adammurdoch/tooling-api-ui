package org.gradle.sample;

import org.gradle.gui.UIContext;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.PrintStream;
import java.io.Serializable;

class RunBuildActionAction implements UI.ToolingOperation<Void> {
    private static class MultiModel implements Serializable {
        GradleProject gradleProject;
        EclipseProject eclipseProject;
        IdeaProject ideaProject;
    }

    private static class ToolingBuildAction implements org.gradle.tooling.BuildAction<MultiModel> {
        @Override
        public MultiModel execute(BuildController controller) {
            MultiModel result = new MultiModel();
            result.gradleProject = controller.getModel(GradleProject.class);
            result.eclipseProject = controller.getModel(EclipseProject.class);
            result.ideaProject = controller.getModel(IdeaProject.class);
            return result;
        }
    }

    @Override
    public String getDisplayName(UIContext uiContext) {
        return "client action";
    }

    @Override
    public Void run(ProjectConnection connection, UIContext uiContext) {
        BuildActionExecuter<MultiModel> executer = connection.action(new ToolingBuildAction());
        uiContext.setup(executer);
        MultiModel result = executer.run();

        PrintStream stdOut = uiContext.getConsoleStdOut();

        GradleProject gradleProject = result.gradleProject;
        stdOut.println("== GRADLE ==");
        stdOut.format("path: %s%n", gradleProject.getPath());
        stdOut.format("name: %s%n", gradleProject.getName());
        stdOut.format("build script: %s%n", gradleProject.getBuildScript().getSourceFile());

        EclipseProject eclipseProject = result.eclipseProject;
        stdOut.println();
        stdOut.println("== ECLIPSE ==");
        stdOut.format("name: %s%n", eclipseProject.getName());
        stdOut.format("project dir: %s%n", eclipseProject.getProjectDirectory());

        IdeaProject ideaProject = result.ideaProject;
        stdOut.println();
        stdOut.println("== IDEA ==");
        stdOut.format("name: %s%n", ideaProject.getName());
        stdOut.format("jdk: %s%n", ideaProject.getJdkName());
        stdOut.format("Java language: %s%n", ideaProject.getLanguageLevel().getLevel());
        stdOut.format("output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getOutputDir());
        stdOut.format("test output dir: %s%n", ideaProject.getModules().getAt(0).getCompilerOutput().getTestOutputDir());
        return null;
    }
}
