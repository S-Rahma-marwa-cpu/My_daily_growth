package com.mydailygrowth.ui;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.model.Prayer;
import com.mydailygrowth.service.DateRange;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

@SuppressWarnings("serial")
public final class PrayerPanel extends JPanel {
    private final Database db;
    private final Runnable onChanged;
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel());
    private final JComboBox<DateRange.Period> periodBox = new JComboBox<>(DateRange.Period.values());
    private final Map<Prayer, JCheckBox> checks = new EnumMap<>(Prayer.class);
    private final JLabel daySummary = new JLabel(" ");
    private final JLabel periodSummary = new JLabel(" ");
    private final JProgressBar periodProgress = new JProgressBar(0, 100);

    public PrayerPanel(Database db, Runnable onChanged) {
        this.db = db;
        this.onChanged = onChanged;
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(18, 20, 18, 20));
        add(buildTop(), BorderLayout.NORTH);
        add(buildPrayerCards(), BorderLayout.CENTER);
        add(buildSummary(), BorderLayout.SOUTH);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "EEEE, dd MMM yyyy"));
        periodBox.setSelectedItem(DateRange.Period.MONTHLY);
        dateSpinner.addChangeListener(e -> refresh());
        periodBox.addActionListener(e -> refresh());
        refresh();
    }

    private JComponent buildTop() {
    	 JPanel top = new JPanel(new BorderLayout(8, 4));
        top.setOpaque(false);
        JLabel title = new JLabel("A gentle daily prayer check-in");
        title.setFont(Theme.SECTION);
        title.setForeground(Theme.MAUVE);
        JLabel subtitle = new JLabel("Mark each prayer when you have completed it. Your records stay on this computer.");
        subtitle.setFont(Theme.BODY);
        subtitle.setForeground(Theme.MUTED);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 4));
        labels.setOpaque(false);
        labels.add(title); labels.add(subtitle);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);
        Theme.styleInput(dateSpinner); Theme.styleInput(periodBox);
        controls.add(Theme.label("Date")); controls.add(dateSpinner);
        top.add(labels, BorderLayout.WEST);
        top.add(controls, BorderLayout.EAST);
        return top;
    }

    private JComponent buildPrayerCards() {
    	JPanel grid = new JPanel(
    		    new GridLayout(Prayer.values().length, 1, 0, 12)
    		);
    		grid.setPreferredSize(new Dimension(700, 450));
        grid.setOpaque(false);
        for (Prayer prayer : Prayer.values()) {
            JPanel card = new JPanel(new BorderLayout(12, 4));
            card.setBackground(Theme.SURFACE);
            card.setBorder(Theme.cardBorder());
            card.setPreferredSize(new Dimension(700, 75));
            card.setMinimumSize(new Dimension(400, 75));
            JPanel text = new JPanel(new GridLayout(1, 1, 0, 2));
            text.setOpaque(false);
            JLabel name = new JLabel(prayer.label());
            name.setFont(new Font("SansSerif", Font.BOLD, 16));
            name.setForeground(Theme.MAUVE);
            JLabel arabic = new JLabel(prayer.arabic());
            arabic.setFont(new Font("SansSerif", Font.PLAIN, 12));
            arabic.setForeground(Theme.MUTED);
            text.add(name); text.add(arabic);
            JCheckBox check = new JCheckBox("Completed");
           
            check.setFont(Theme.BODY_BOLD);
            check.setOpaque(false);
            check.setForeground(Theme.ROSE_DARK);
            check.addActionListener(e -> savePrayer(prayer, check.isSelected()));
            checks.put(prayer, check);
            card.add(text, BorderLayout.CENTER);
            card.add(check, BorderLayout.EAST);
            grid.add(card);
        }
        return grid;
    }

    private JComponent buildSummary() {
        JPanel card = Theme.card("YOUR PRAYER CONSISTENCY");
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        Theme.styleInput(periodBox);
        controls.add(Theme.label("Summary period"));
        controls.add(periodBox);
        periodProgress.setStringPainted(true);
        periodProgress.setForeground(Theme.ROSE);
        periodProgress.setBackground(Theme.BLUSH);
        periodProgress.setBorderPainted(false);
        JPanel inner = new JPanel(new BorderLayout(8, 8));
        inner.setOpaque(false);
        inner.add(controls, BorderLayout.NORTH);
        inner.add(periodProgress, BorderLayout.CENTER);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3));
        labels.setOpaque(false);
        daySummary.setFont(Theme.BODY_BOLD); daySummary.setForeground(Theme.MAUVE);
        periodSummary.setFont(Theme.BODY); periodSummary.setForeground(Theme.MUTED);
        labels.add(daySummary); labels.add(periodSummary);
        inner.add(labels, BorderLayout.SOUTH);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    public void refresh() {
        try {
            LocalDate selected = selectedDate();
            boolean editable = !selected.isAfter(LocalDate.now());
            int doneToday = 0;
            for (Prayer prayer : Prayer.values()) {
                boolean done = db.isPrayerCompleted(prayer, selected);
                JCheckBox check = checks.get(prayer);
                check.setSelected(done);
                check.setEnabled(editable);
                if (done) doneToday++;
            }
            daySummary.setText(selected + "  ·  " + doneToday
                    + " of " + Prayer.values().length + " prayers recorded");
            DateRange.Period period = (DateRange.Period) periodBox.getSelectedItem();
            DateRange range = DateRange.forPeriod(period == null ? DateRange.Period.MONTHLY : period, selected);
            int days = range.elapsedDays(LocalDate.now());
            int possible = days * Prayer.values().length;
            int completed = days == 0 ? 0 : db.countPrayerCompletions(range.from(), range.effectiveTo(LocalDate.now()));
            int percent = possible == 0 ? 0 : (int) Math.round(completed * 100.0 / possible);
            periodProgress.setValue(percent);
            periodProgress.setString(percent + "%");
            periodSummary.setText(range.label() + "  ·  " + completed + " / " + possible + " prayers logged");
        } catch (Exception ex) { showError(ex); }
    }

    private void savePrayer(Prayer prayer, boolean completed) {
        try {
            db.setPrayerCompleted(prayer, selectedDate(), completed);
            refresh();
            onChanged.run();
        } catch (Exception ex) { showError(ex); }
    }

    private LocalDate selectedDate() {
        return ((Date) dateSpinner.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Prayer tracker", JOptionPane.ERROR_MESSAGE);
    }
}
