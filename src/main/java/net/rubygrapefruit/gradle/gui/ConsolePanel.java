package net.rubygrapefruit.gradle.gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsolePanel extends JPanel {
    private final ColorScheme knownEscape;
    private final ColorScheme unknownEscape;
    private final ColorScheme ansiRed;
    private final ColorScheme ansiGreen;
    private final ColorScheme ansiYellow;
    private boolean hasOutput;
    private final JTextPane output;
    private final PrintStream outputStream;
    private final PrintStream errorStream;
    private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
    private ColorScheme colorScheme;
    private boolean bold;
    private int cursorPos;

    public ConsolePanel(boolean ansiAware) {
        setLayout(new BorderLayout());

        output = new JTextPane();
        output.setEditable(false);
        output.setEnabled(false);
        output.setFont(new Font("monospaced", Font.PLAIN, 13));
        output.setText("output goes here..");

        Style stdout = output.addStyle("stdout", null);

        Style red = output.addStyle("ansiRed", null);
        StyleConstants.setForeground(red, Color.RED);
        this.ansiRed = new BoldColorScheme(red, output);

        Style yellow = output.addStyle("ansiYellow", null);
        StyleConstants.setForeground(yellow, new Color(200, 200, 20));
        this.ansiYellow = new BoldColorScheme(yellow, output);

        Style green = output.addStyle("ansiGreen", null);
        StyleConstants.setForeground(green, new Color(20, 200, 20));
        this.ansiGreen = new BoldColorScheme(green, output);

        Style unknownEscape = output.addStyle("unknownEscape", null);
        StyleConstants.setForeground(unknownEscape, Color.WHITE);
        StyleConstants.setBackground(unknownEscape, Color.RED);
        this.unknownEscape = new NoBoldColorScheme(unknownEscape);

        Style knownEscape = output.addStyle("knownEscape", null);
        StyleConstants.setForeground(knownEscape, Color.WHITE);
        StyleConstants.setBackground(knownEscape, new Color(80, 127, 180));
        this.knownEscape = new NoBoldColorScheme(knownEscape);

        add(output, BorderLayout.CENTER);

        ByteConsumer stdoutSink = new AnsiByteConsumer(new BoldColorScheme(stdout, output));
        ByteConsumer stderrSink = new AnsiByteConsumer(ansiRed);

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
            Event event = events.poll();
            if (event == null) {
                return;
            }
            if (event instanceof Bold) {
                bold = true;
                continue;
            }
            if (event instanceof Normal) {
                bold = false;
                continue;
            }
            if (event instanceof ForegroundColor) {
                ForegroundColor color = (ForegroundColor) event;
                colorScheme = color.colorScheme;
                continue;
            }

            if (!hasOutput) {
                output.setText("");
                output.setEnabled(true);
                hasOutput = true;
            }

            StyledDocument document = output.getStyledDocument();

            if (event instanceof CursorBack) {
                // TODO - handle moving back over end-of-line chars
                CursorBack cursorBack = (CursorBack) event;
                Element para = document.getParagraphElement(cursorPos);
                cursorPos = Math.max(para.getStartOffset(), cursorPos - cursorBack.count);
                continue;
            }

            if (event instanceof CursorForward) {
                // TODO - handle moving forward over end-of-line chars
                CursorForward cursorForward = (CursorForward) event;
                Element para = document.getParagraphElement(cursorPos);
                cursorPos += cursorForward.count;
                while (cursorPos >= para.getEndOffset()) {
                    try {
                        document.insertString(para.getEndOffset() - 1, " ", null);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                }
                continue;
            }
            if (event instanceof CursorUp) {
                CursorUp cursorUp = (CursorUp) event;
                Element para = document.getParagraphElement(cursorPos);
                int pos = cursorPos - para.getStartOffset();
                for (int i = 0; i < cursorUp.count && para.getStartOffset() > 0; i++) {
                    para = document.getParagraphElement(para.getStartOffset() - 1);
                }
                cursorPos = Math.min(para.getStartOffset() + pos, para.getEndOffset());
                continue;
            }

            if (event instanceof EraseToEndOfLine) {
                try {
                    if (cursorPos == document.getLength()) {
                        continue;
                    }
                    Element para = document.getParagraphElement(cursorPos);
                    document.remove(cursorPos, Math.min(document.getLength(), para.getEndOffset()) - cursorPos);
                } catch (BadLocationException e) {
                    throw new RuntimeException(e);
                }
                continue;
            }

            TextEvent text = (TextEvent) event;

            ColorScheme currentScheme = colorScheme != null ? colorScheme : text.colorScheme;
            Style style = bold ? currentScheme.getBold() : currentScheme.getNormal();
            try {
                // TODO - need to overwrite existing text on the current line
                document.insertString(cursorPos, text.text, style);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
            cursorPos += text.text.length();
        }
    }

    private void onEvent(Event event) {
        events.add(event);
        if (SwingUtilities.isEventDispatchThread()) {
            doWriteOutput();
        } else {
            SwingUtilities.invokeLater(this::doWriteOutput);
        }
    }

    public void clearOutput() {
        output.setText("");
        cursorPos = 0;
        hasOutput = false;
    }

    private interface ColorScheme {
        public Style getNormal();

        public Style getBold();
    }

    private static class BoldColorScheme implements ColorScheme {
        private final Style normal;
        private final Style bold;

        public BoldColorScheme(Style normal, JTextPane owner) {
            this.normal = normal;
            bold = owner.addStyle(normal.getName() + "Bold", normal);
            StyleConstants.setBold(bold, true);
        }

        public Style getNormal() {
            return normal;
        }

        public Style getBold() {
            return bold;
        }
    }

    private static class NoBoldColorScheme implements ColorScheme {
        private final Style style;

        public NoBoldColorScheme(Style style) {
            this.style = style;
        }

        @Override
        public Style getNormal() {
            return style;
        }

        @Override
        public Style getBold() {
            return style;
        }
    }

    private abstract class Event {
    }

    private class CursorBack extends Event {
        final int count;

        public CursorBack(int count) {
            this.count = count;
        }
    }

    private class CursorUp extends Event {
        final int count;

        public CursorUp(int count) {
            this.count = count;
        }
    }

    private class CursorForward extends Event {
        final int count;

        public CursorForward(int count) {
            this.count = count;
        }
    }

    private class EraseToEndOfLine extends Event {
    }

    private class Bold extends Event {
    }

    private class Normal extends Event {
    }

    private class ForegroundColor extends Event {
        final ColorScheme colorScheme;

        public ForegroundColor(ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
        }
    }

    private class TextEvent extends Event {
        final String text;
        final ColorScheme colorScheme;

        private TextEvent(String text, ColorScheme colorScheme) {
            this.text = text;
            this.colorScheme = colorScheme;
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
            synchronized (sink) {
                sink.consume(new Buffer(bytes, offset, length));
            }
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

    enum State {
        Normal, LeftParen, Param, Code
    }

    private class AnsiByteConsumer implements ByteConsumer {
        private final StringBuilder currentSequence = new StringBuilder();
        private final ColorScheme colorScheme;
        private State state = State.Normal;

        public AnsiByteConsumer(ColorScheme colorScheme) {
            this.colorScheme = colorScheme;
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
                        if ((nextDigit < '0' || nextDigit > '9') && nextDigit != ';') {
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
                        if (!handleEscape(string, next)) {
                            onEvent(new TextEvent("ESC[" + string + next, unknownEscape));
                        }
                        state = State.Normal;
                        break;
                    case Normal:
                        Buffer prefix = buffer.consumeToNext((byte) 27);
                        if (prefix == null) {
                            onEvent(new TextEvent(buffer.consumeString(), colorScheme));
                            return;
                        }
                        onEvent(new TextEvent(prefix.consumeString(), colorScheme));
                        state = State.LeftParen;
                        buffer.consume();
                        currentSequence.setLength(0);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }

        private boolean handleEscape(String param, char code) {
            if (code == 'm' && param.equals("1")) {
                onEvent(new Bold());
                return true;
            } else if (code == 'm' && param.equals("22")) {
                onEvent(new Normal());
                return true;
            } else if (code == 'm' && param.equals("22;1")) {
                onEvent(new Normal());
                onEvent(new Bold());
                return true;
            } else if (code == 'm' && param.equals("31")) {
                onEvent(new ForegroundColor(ansiRed));
                return true;
            } else if (code == 'm' && param.equals("32")) {
                onEvent(new ForegroundColor(ansiGreen));
                return true;
            } else if (code == 'm' && param.equals("33")) {
                onEvent(new ForegroundColor(ansiYellow));
                return true;
            } else if (code == 'm' && param.equals("39")) {
                onEvent(new ForegroundColor(null));
                return true;
            } else if (code == 'D') {
                onEvent(new CursorBack(Integer.parseInt(param)));
                return true;
            } else if (code == 'A') {
                onEvent(new CursorUp(Integer.parseInt(param)));
                return true;
            } else if (code == 'C') {
                onEvent(new CursorForward(Integer.parseInt(param)));
                return true;
            } else if (code == 'K' && param.equals("0")) {
                onEvent(new EraseToEndOfLine());
                return true;
            }
            return false;
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
    }
}
