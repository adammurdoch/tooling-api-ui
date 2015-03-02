package net.rubygrapefruit.gradle.gui.actions;

import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;

import java.io.Serializable;

public class MultiModel implements Serializable {
    public GradleProject gradleProject;
    public EclipseProject eclipseProject;
    public IdeaProject ideaProject;
}
