package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ProjectConnection;

import java.util.List;

public interface ToolingOperationContext {

    <T extends LongRunningOperation> T create(OperationProvider<T, ProjectConnection> provider);

    List<String> getCommandLineArgs();
}
