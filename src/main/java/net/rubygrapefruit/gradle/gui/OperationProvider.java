package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.LongRunningOperation;
import org.gradle.tooling.ProjectConnection;

public interface OperationProvider<T extends LongRunningOperation> {
    T create(ProjectConnection projectConnection);
}
