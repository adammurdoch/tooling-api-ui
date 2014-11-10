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
    private final Style escape;
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
        Style stdout = output.addStyle("stdout", null);
        Style stderr = output.addStyle("stderr", null);
        StyleConstants.setForeground(stderr, Color.RED);
        escape = output.addStyle("escape", null);
        StyleConstants.setForeground(escape, Color.WHITE);
        StyleConstants.setBackground(escape, Color.RED);
        add(output, BorderLayout.CENTER);

        ByteConsumer stdoutSink = new RawByteConsumer(stdout);
        ByteConsumer stderrSink = new RawByteConsumer(stderr);
        if (ansiAware) {
            stdoutSink = new AnsiByteConsumer(stdoutSink);
            stderrSink = new AnsiByteConsumer(stderrSink);
        }

        outputStream = new PrintStream(new ConsolePanel.OutputWriter(stdoutSink), true);
        errorStream = new PrintStream(new ConsolePanel.OutputWriter(stderrSink), true);
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

    private void onEvent(TextEvent event) {
        events.add(event);
        if (SwingUtilities.isEventDispatchThread()) {
            doWriteOutput();
        } else {
            SwingUtilities.invokeLater(this::doWriteOutput);
        }
    }

    public void clearOutput() {
        output.setText("");
        hasOutput = false;
    }

    private abstract class Event {
    }

    private class TextEvent extends Event {
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
            onEvent(new TextEvent(text, style));
        }
    }

    private class AnsiByteConsumer implements ByteConsumer {
        private final ByteConsumer consumer;
        private final byte[] currentSequence = new byte[256];
        private int currentPos;

        private AnsiByteConsumer(ByteConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void consume(byte[] bytes, int offset, int length) {
            while (length > 0) {
                int maxOffset = offset + length;
                int nextEscape = offset;
                for (; nextEscape < maxOffset && bytes[nextEscape] != 27; nextEscape++) {
                }
                if (nextEscape == maxOffset) {
                    consumer.consume(bytes, offset, length);
                    return;
                }
                int count = nextEscape - offset;
                consumer.consume(bytes, offset, count);
                onEvent(new TextEvent("ESC", escape));
                offset = nextEscape + 1;
                length = length - count - 1;
            }
        }
    }
}
