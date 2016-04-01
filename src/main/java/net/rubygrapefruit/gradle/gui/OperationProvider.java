package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.LongRunningOperation;

public interface OperationProvider<T extends LongRunningOperation, CONNECTION> {
    T create(CONNECTION projectConnection);
}
