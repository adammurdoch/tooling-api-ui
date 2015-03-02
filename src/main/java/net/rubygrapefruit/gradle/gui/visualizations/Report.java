package net.rubygrapefruit.gradle.gui.visualizations;

import net.rubygrapefruit.gradle.gui.Visualization;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public abstract class Report<T> implements Visualization<T> {
    JTextPane text;

    public Report() {
        this.text = new JTextPane();
        text.setFont(new Font("monospaced", Font.PLAIN, 13));
        text.setEditable(false);
    }

    @Override
    public JComponent getMainComponent() {
        return text;
    }

    @Override
    public void update(T model) {
        StringWriter contents = new StringWriter();
        PrintWriter output = new PrintWriter(contents);
        render(model, output);
        output.flush();
        text.setText(contents.toString());
    }

    protected abstract void render(T project, PrintWriter output);
}