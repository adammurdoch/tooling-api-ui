package net.rubygrapefruit.gradle.gui;

import net.rubygrapefruit.ansi.AnsiParser;
import net.rubygrapefruit.ansi.TextColor;
import net.rubygrapefruit.ansi.Visitor;
import net.rubygrapefruit.ansi.token.*;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ConsolePanel extends JPanel {
    private final ColorScheme unknownEscape;
    private final ColorScheme ansiRed;
    private final ColorScheme ansiGreen;
    private final ColorScheme ansiYellow;
    private final ColorScheme normal;
    private final boolean handleControlSequences;
    private boolean hasOutput;
    private final JTextPane output;
    private final PrintStream outputStream;
    private final BlockingQueue<Token> events = new LinkedBlockingQueue<>();
    private ColorScheme colorScheme;
    private boolean bold;
    private int cursorPos;

    public ConsolePanel(boolean handleControlSequences) {
        this.handleControlSequences = handleControlSequences;
        setLayout(new BorderLayout());

        output = new JTextPane();
        output.setEditable(false);
        output.setEnabled(false);
        output.setFont(new Font("monospaced", Font.PLAIN, 13));
        output.setText("output goes here..");

        Style normal = output.addStyle("stdout", null);
        this.normal = new BoldColorScheme(normal, output);
        colorScheme = this.normal;

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

        add(output, BorderLayout.CENTER);

        Visitor visitor = token -> onEvent(token);
        AnsiParser parser = new AnsiParser();
        outputStream = new PrintStream(new SyncOutputStream(parser.newParser("utf-8", visitor)), true);
    }

    public PrintStream getOutput() {
        return outputStream;
    }

    public PrintStream getError() {
        return outputStream;
    }

    private void doWriteOutput() {
        while (true) {
            Token event = events.poll();
            if (event == null) {
                return;
            }

            if (!hasOutput) {
                output.setText("");
                output.setEnabled(true);
                hasOutput = true;
            }
            StyledDocument document = output.getStyledDocument();

            if (handleControlSequences) {
                if (event instanceof BoldOn) {
                    bold = true;
                    continue;
                }
                if (event instanceof BoldOff) {
                    bold = false;
                    continue;
                }
                if (event instanceof ForegroundColor) {
                    ForegroundColor color = (ForegroundColor) event;
                    if (color.getColor().isDefault()) {
                        colorScheme = normal;
                        continue;
                    }
                    if (color.getColor() == TextColor.RED) {
                        colorScheme = ansiRed;
                        continue;
                    }
                    if (color.getColor() == TextColor.GREEN) {
                        colorScheme = ansiGreen;
                        continue;
                    }
                    if (color.getColor() == TextColor.YELLOW) {
                        colorScheme = ansiYellow;
                        continue;
                    }
                    continue;
                }
                if (event instanceof BackgroundColor) {
                    BackgroundColor color = (BackgroundColor) event;
                    if (color.getColor().isDefault()) {
                        continue;
                    }
                }
                if (event instanceof CursorBackward) {
                    // TODO - handle moving back over end-of-line chars
                    CursorBackward cursorBack = (CursorBackward) event;
                    Element para = document.getParagraphElement(cursorPos);
                    cursorPos = Math.max(para.getStartOffset(), cursorPos - cursorBack.getCount());
                    continue;
                }

                if (event instanceof CursorForward) {
                    // TODO - handle moving forward over end-of-line chars
                    CursorForward cursorForward = (CursorForward) event;
                    Element para = document.getParagraphElement(cursorPos);
                    cursorPos += cursorForward.getCount();
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
                    for (int i = 0; i < cursorUp.getCount() && para.getStartOffset() > 0; i++) {
                        para = document.getParagraphElement(para.getStartOffset() - 1);
                    }
                    cursorPos = Math.min(para.getStartOffset() + pos, para.getEndOffset());
                    continue;
                }
                if (event instanceof CursorDown) {
                    CursorDown cursorDown = (CursorDown) event;
                    Element para = document.getParagraphElement(cursorPos);
                    int pos = cursorPos - para.getStartOffset();
                    for (int i = 0; i < cursorDown.getCount(); i++) {
                        para = document.getParagraphElement(para.getEndOffset() + 1);
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
                if (event instanceof EraseInLine) {
                    try {
                        Element para = document.getParagraphElement(cursorPos);
                        document.remove(para.getStartOffset(), para.getElementCount() - 1);
                        cursorPos = para.getStartOffset();
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }
            }

            String text;
            ColorScheme currentScheme = colorScheme;
            if (event instanceof Text) {
                text = ((Text) event).getText();
            } else if (event instanceof NewLine) {
                text = "\n";
            } else if (event instanceof CarriageReturn) {
                text = "\r";
            } else {
                StringBuilder builder = new StringBuilder();
                event.appendDiagnostic(builder);
                text = builder.toString();
                currentScheme = unknownEscape;
            }

            Style style = bold ? currentScheme.getBold() : currentScheme.getNormal();
            try {
                Element para = document.getParagraphElement(cursorPos);
                int remove = Math.min(para.getEndOffset() - cursorPos - 1, text.length());
                if (remove > 0) {
                    document.remove(cursorPos, remove);
                    // TODO - need to deal with multiple lines in the text
                }
                document.insertString(cursorPos, text, style);
            } catch (BadLocationException e) {
                throw new RuntimeException(e);
            }
            cursorPos += text.length();
        }
    }

    private void onEvent(Token event) {
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

    private class SyncOutputStream extends OutputStream {
        private final OutputStream delegate;

        public SyncOutputStream(OutputStream delegate) {
            this.delegate = delegate;
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
            synchronized (delegate) {
                delegate.write(bytes, offset, length);
            }
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        @Override
        public void close() throws IOException {
        }
    }
}
