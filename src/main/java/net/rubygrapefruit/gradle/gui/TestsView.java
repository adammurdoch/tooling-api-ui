package net.rubygrapefruit.gradle.gui;

import org.gradle.tooling.TestLauncher;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.test.TestOperationDescriptor;
import org.gradle.tooling.events.test.TestStartEvent;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

public class TestsView extends JPanel implements ProgressListener {
    private final JTable table;
    private final DefaultTableModel tableModel;

    public TestsView(ToolingOperationExecuter executer) {
        setLayout(new BorderLayout());
        JTextField testNames = new JTextField();
        testNames.setToolTipText("Comma-separated test class or test method names");
        testNames.addActionListener(e -> {
            final String[] tests = (testNames.getText().split(",?\\s+"));
            executer.start(new ToolingOperation<Object>() {
                @Override
                public String getDisplayName(ToolingOperationContext uiContext) {
                    return "run tests " + Arrays.asList(tests);
                }

                @Override
                public Object run(ToolingOperationContext uiContext) {
                    TestLauncher testLauncher = uiContext.create(projectConnection -> projectConnection.newTestLauncher());
                    for (String test : tests) {
                        if (test.matches("[^\\.]+\\.[^\\.]+")) {
                            String[] parts = test.split("\\.");
                            testLauncher.withJvmTestMethods(parts[0], parts[1]);
                        } else {
                            testLauncher.withJvmTestClasses(test);
                        }
                    }
                    testLauncher.run();
                    return null;
                }
            });
        });
        add(testNames, BorderLayout.NORTH);
        table = new JTable();
        tableModel = new DefaultTableModel(new Object[]{"Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(tableModel);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    final TestOperationDescriptor descriptor = (TestOperationDescriptor) tableModel.getValueAt(row, 0);
                    executer.start(new ToolingOperation<Object>() {
                        @Override
                        public String getDisplayName(ToolingOperationContext uiContext) {
                            return "run test " + descriptor;
                        }

                        @Override
                        public Object run(ToolingOperationContext uiContext) {
                            TestLauncher testLauncher = uiContext.create(projectConnection -> projectConnection.newTestLauncher());
                            testLauncher.withTests(descriptor);
                            testLauncher.run();
                            return null;
                        }
                    });
                }
            }
        });
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override
    public void statusChanged(ProgressEvent event) {
        if (event instanceof TestStartEvent) {
            TestStartEvent startEvent = (TestStartEvent) event;
            tableModel.addRow(new Object[]{startEvent.getDescriptor()});
        }
    }

    public void reset() {
        tableModel.setNumRows(0);
    }
}
