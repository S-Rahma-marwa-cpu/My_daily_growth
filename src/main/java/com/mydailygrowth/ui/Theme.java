package com.mydailygrowth.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {
    public static final Color BACKGROUND = new Color(250, 246, 242);
    public static final Color SURFACE = new Color(255, 253, 250);
    public static final Color ROSE = new Color(194, 128, 148);
    public static final Color ROSE_DARK = new Color(139, 91, 111);
    public static final Color BLUSH = new Color(246, 229, 231);
    public static final Color BEIGE = new Color(239, 226, 211);
    public static final Color MAUVE = new Color(112, 87, 105);
    public static final Color TEXT = new Color(78, 65, 72);
    public static final Color MUTED = new Color(143, 126, 132);
    public static final Color BORDER = new Color(235, 220, 216);
    public static final Color SOFT_GREEN = new Color(225, 239, 226);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font TITLE = new Font("SansSerif", Font.BOLD, 24);
    public static final Font SECTION = new Font("SansSerif", Font.BOLD, 16);

    private Theme() { }

    public static void install() {
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.foreground", MAUVE);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.selectionBackground", BLUSH);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", MAUVE);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("ProgressBar.foreground", ROSE);
        UIManager.put("ProgressBar.background", BLUSH);
        UIManager.put("ProgressBar.selectionForeground", ROSE_DARK);
        UIManager.put("ProgressBar.selectionBackground", ROSE_DARK);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("TextArea.background", SURFACE);
        UIManager.put("CheckBox.background", SURFACE);
        UIManager.put("CheckBox.foreground", TEXT);
    }

    public static JPanel card(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 10));
        panel.setBackground(SURFACE);
        panel.setBorder(cardBorder());
        if (title != null && !title.isBlank()) {
            JLabel heading = new JLabel(title);
            heading.setFont(SECTION);
            heading.setForeground(ROSE_DARK);
            heading.setBorder(new EmptyBorder(0, 0, 3, 0));
            panel.add(heading, BorderLayout.NORTH);
        }
        return panel;
    }

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(15, 16, 15, 16));
    }

    public static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_BOLD);
        label.setForeground(TEXT);
        return label;
    }

    public static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFont(BODY_BOLD);
        button.setForeground(Color.PINK);
        button.setBackground(ROSE_DARK);
        button.setForeground(Color.black);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    public static JButton softButton(String text) {
        JButton button = new JButton(text);
        button.setFont(BODY_BOLD);
        button.setForeground(ROSE_DARK);
        button.setBackground(BLUSH);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(9, 14, 9, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    public static void styleInput(JComponent component) {
        component.setFont(BODY);
        component.setForeground(TEXT);
        component.setBackground(SURFACE);
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        scroll.getViewport().setBackground(SURFACE);
        return scroll;
    }
}
