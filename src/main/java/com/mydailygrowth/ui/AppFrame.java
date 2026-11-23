package com.mydailygrowth.ui;

import com.mydailygrowth.data.Database;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@SuppressWarnings("serial")
public final class AppFrame extends JFrame {
    private final DashboardPanel dashboardPanel;
    private final HabitsPanel habitsPanel;
    private final PrayerPanel prayerPanel;
    private final JournalPanel journalPanel;

    public AppFrame(Database db) {
        super("My Daily Growth");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 720));
        setSize(1280, 850);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout(0, 0));

        dashboardPanel = new DashboardPanel(db);
        habitsPanel = new HabitsPanel(db, this::refreshSummaryPanels);
        prayerPanel = new PrayerPanel(db, this::refreshSummaryPanels);
        journalPanel = new JournalPanel(db, this::refreshSummaryPanels);

        add(buildHeader(), BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.setForeground(Theme.MAUVE);
        tabs.setBackground(Theme.BACKGROUND);
        tabs.addTab("  Overview  ", dashboardPanel);
        tabs.addTab("  My Habits  ", habitsPanel);
        tabs.addTab("  Prayer Tracker  ", prayerPanel);
        tabs.addTab("  Mood & Learning  ", journalPanel);
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected == dashboardPanel) dashboardPanel.refresh();
            else if (selected == habitsPanel) habitsPanel.refresh();
            else if (selected == prayerPanel) prayerPanel.refresh();
            else if (selected == journalPanel) journalPanel.refresh();
        });
        add(tabs, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 5));
        header.setBackground(Theme.MAUVE);
        header.setBorder(new EmptyBorder(18, 24, 17, 24));
        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setOpaque(false);
        JLabel title = new JLabel("My Daily Growth  ♡");
        title.setFont(new Font("SansSerif", Font.BOLD, 27));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("A softer space to build habits, care for your days and celebrate progress.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(new Color(248, 231, 235));
        titleBlock.add(title); titleBlock.add(subtitle);
        JLabel decoration = new JLabel("✿");
        decoration.setFont(new Font("Serif", Font.PLAIN, 38));
        decoration.setForeground(new Color(247, 221, 225));
        header.add(titleBlock, BorderLayout.WEST);
        header.add(decoration, BorderLayout.EAST);
        return header;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BEIGE);
        footer.setBorder(new EmptyBorder(7, 18, 7, 18));
        JLabel text = new JLabel("Small steps count. Be kind to yourself while you grow.");
        text.setFont(new Font("SansSerif", Font.ITALIC, 11));
        text.setForeground(Theme.MAUVE);
        footer.add(text, BorderLayout.CENTER);
        return footer;
    }

    private void refreshSummaryPanels() {
        dashboardPanel.refresh();
        prayerPanel.refresh();
    }
}
