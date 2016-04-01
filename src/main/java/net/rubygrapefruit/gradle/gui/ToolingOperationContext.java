package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.connection.GradleConnection;

import java.util.List;

public interface ToolingOperationContext {
    boolean isComposite();

    <T extends LongRunningOperation> T create(OperationProvider<T, ProjectConnection> provider);

    <T extends LongRunningOperation> T createComposite(OperationProvider<T, GradleConnection> provider);

    List<String> getCommandLineArgs();
}
