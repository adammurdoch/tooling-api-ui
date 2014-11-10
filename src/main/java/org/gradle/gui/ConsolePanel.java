package org.gradle.gui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsolePanel extends JPanel {
    private final Style stdout;
    private final Style stderr;
    private boolean hasOutput;
    private final JTextPane output;
    private final PrintStream outputStream;
    private final PrintStream errorStream;
    private final BlockingQueue<ConsolePanel.TextEvent> events = new LinkedBlockingQueue<>();

    public ConsolePanel(boolean ansiAware) {
        setLayout(new BorderLayout());

        output = new JTextPane();
        output.setEditable(false);
        output.setEnabled(false);
        output.setFont(new Font("monospaced", Font.PLAIN, 13));
        output.setText("output goes here..");
        stdout = output.addStyle("stdout", null);
        stderr = output.addStyle("stderr", null);
        StyleConstants.setForeground(stderr, Color.RED);
        add(output, BorderLayout.CENTER);

        outputStream = new PrintStream(new ConsolePanel.OutputWriter(new RawByteConsumer(stdout)), true);
        errorStream = new PrintStream(new ConsolePanel.OutputWriter(new RawByteConsumer(stderr)), true);
    }

    public PrintStream getOutput() {
        return outputStream;
    }

    public PrintStream getError() {
        return errorStream;
    }

    private void doWriteOutput() {
        while (true) {
            ConsolePanel.TextEvent event = events.poll();
            if (event == null) {
                return;
            }
            if (!hasOutput) {
                output.setText("");
                output.setEnabled(true);
                hasOutput = true;
            }
            Document document = output.getDocument();
            try {
                document.insertString(document.getLength(), event.text, event.style);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void clearOutput() {
        output.setText("");
        hasOutput = false;
    }

    private class TextEvent {
        final String text;
        final Style style;

        private TextEvent(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }

    private class OutputWriter extends OutputStream {
        private final ByteConsumer sink;

        public OutputWriter(ByteConsumer sink) {
            this.sink = sink;
        }

        @Override
        public void write(int i) throws IOException {
            byte[] buffer = new byte[1];
            buffer[0] = (byte) i;
            write(buffer, 0, 1);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            sink.consume(bytes, offset, length);
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    private interface ByteConsumer {
        void consume(byte[] buffer, int offset, int length);
    }

    private class RawByteConsumer implements ByteConsumer {
        private final Style style;

        public RawByteConsumer(Style style) {
            this.style = style;
        }

        @Override
        public void consume(byte[] bytes, int offset, int length) {
            String text = new String(bytes, offset, length);
            events.add(new ConsolePanel.TextEvent(text, style));
            if (SwingUtilities.isEventDispatchThread()) {
                doWriteOutput();
            } else {
                SwingUtilities.invokeLater(ConsolePanel.this::doWriteOutput);
            }
        }
    }
}
