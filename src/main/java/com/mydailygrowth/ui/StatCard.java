package com.mydailygrowth.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@SuppressWarnings("serial")
public final class StatCard extends JPanel {
    private final JLabel value = new JLabel("—");
    private final JLabel note = new JLabel(" ");

    public StatCard(String title, String accent) {
        setLayout(new BorderLayout(5, 7));
        setBackground(Theme.SURFACE);
        setBorder(Theme.cardBorder());
        JLabel heading = new JLabel(title.toUpperCase());
        heading.setFont(new Font("SansSerif", Font.BOLD, 10));
        heading.setForeground(Theme.MUTED);
        value.setFont(new Font("SansSerif", Font.BOLD, 25));
        value.setForeground(Theme.MAUVE);
        note.setFont(new Font("SansSerif", Font.PLAIN, 11));
        note.setForeground(Theme.MUTED);
        add(heading, BorderLayout.NORTH);
        add(value, BorderLayout.CENTER);
        add(note, BorderLayout.SOUTH);
    }

    public void setValue(String text, String description) {
        value.setText(text);
        note.setText(description == null ? " " : description);
    }
}
