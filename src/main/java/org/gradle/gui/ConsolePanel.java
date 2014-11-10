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
    private final Style knownEscape;
    private final Style unknownEscape;
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
        unknownEscape = output.addStyle("unknownEscape", null);
        StyleConstants.setForeground(unknownEscape, Color.WHITE);
        StyleConstants.setBackground(unknownEscape, Color.RED);
        knownEscape = output.addStyle("knownEscape", null);
        StyleConstants.setForeground(knownEscape, Color.WHITE);
        StyleConstants.setBackground(knownEscape, Color.BLUE);
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
            sink.consume(new Buffer(bytes, offset, length));
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    private interface ByteConsumer {
        void consume(Buffer buffer);
    }

    private class RawByteConsumer implements ByteConsumer {
        private final Style style;

        public RawByteConsumer(Style style) {
            this.style = style;
        }

        @Override
        public void consume(Buffer buffer) {
            String text = buffer.consumeString();
            onEvent(new TextEvent(text, style));
        }
    }

    enum State {
        Normal, LeftParen, Param, Code
    }

    private class AnsiByteConsumer implements ByteConsumer {
        private final ByteConsumer consumer;
        private final StringBuilder currentSequence = new StringBuilder();
        private State state = State.Normal;

        private AnsiByteConsumer(ByteConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void consume(Buffer buffer) {
            while (buffer.hasMore()) {
                switch (state) {
                    case LeftParen:
                        if (buffer.peek() != '[') {
                            onEvent(new TextEvent("ESC", unknownEscape));
                            state = State.Normal;
                        } else {
                            buffer.consume();
                            state = State.Param;
                        }
                        break;
                    case Param:
                        byte nextDigit = buffer.peek();
                        if (nextDigit < '0' || nextDigit > '9') {
                            state = State.Code;
                        } else {
                            currentSequence.append((char) nextDigit);
                            buffer.consume();
                        }
                        break;
                    case Code:
                        char next = (char) buffer.peek();
                        buffer.consume();
                        String string = currentSequence.toString();
                        if (next == 'm' && string.equals("1")) {
                            onEvent(new TextEvent("[BOLD]", knownEscape));
                        } else if (next == 'm' && string.equals("22")) {
                            onEvent(new TextEvent("[NORMAL]", knownEscape));
                        } else if (next == 'D') {
                            onEvent(new TextEvent("[BACK:" + string + "]", knownEscape));
                        } else if (next == 'A') {
                            onEvent(new TextEvent("[UP:" + string + "]", knownEscape));
                        } else if (next == 'C') {
                            onEvent(new TextEvent("[FORWARD:" + string + "]", knownEscape));
                        } else if (next == 'K' && string.equals("0")) {
                            onEvent(new TextEvent("[ERASE-TO-END-LINE]", knownEscape));
                        } else {
                            onEvent(new TextEvent("ESC[" + string + next, unknownEscape));
                        }
                        state = State.Normal;
                        break;
                    case Normal:
                        Buffer prefix = buffer.consumeToNext((byte) 27);
                        if (prefix == null) {
                            consumer.consume(buffer);
                            return;
                        }
                        consumer.consume(prefix);
                        state = State.LeftParen;
                        buffer.consume();
                        currentSequence.setLength(0);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
    }

    private static class Buffer {
        private final byte[] buffer;
        private int offset;
        private int length;

        public Buffer(byte[] buffer, int offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        public String consumeString() {
            return new String(buffer, offset, length);
        }

        public byte peek() {
            return buffer[offset];
        }

        public void consume() {
            offset++;
            length--;
        }

        public boolean hasMore() {
            return length > 0;
        }

        public Buffer consumeToNext(byte value) {
            int maxOffset = offset + length;
            for (int nextValue = offset; nextValue < maxOffset; nextValue++) {
                if (buffer[nextValue] == value) {
                    int count = nextValue - offset;
                    Buffer result = new Buffer(buffer, offset, count);
                    offset = nextValue;
                    length -= count;
                    return result;
                }
            }
            return null;
        }

        public void empty() {
            offset = 0;
            length = 0;
        }

        public void append(byte value) {
            buffer[offset + length] = value;
            length++;
        }
    }
}
