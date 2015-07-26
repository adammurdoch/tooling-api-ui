package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.LongRunningOperation;

import java.util.List;

public interface ToolingOperationContext {
    <T extends LongRunningOperation> T create(OperationProvider<T> provider);

    List<String> getCommandLineArgs();
}
