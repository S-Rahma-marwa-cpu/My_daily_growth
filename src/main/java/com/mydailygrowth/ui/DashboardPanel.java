package com.mydailygrowth.ui;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.export.CsvExporter;
import com.mydailygrowth.model.Habit;
import com.mydailygrowth.model.Prayer;
import com.mydailygrowth.service.DateRange;
import com.mydailygrowth.service.AdherenceCalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
public final class DashboardPanel extends JPanel {
    private final Database db;
    private final JComboBox<DateRange.Period> periodBox = new JComboBox<>(DateRange.Period.values());
    private final JSpinner anchorSpinner = new JSpinner(new SpinnerDateModel());
    private final JLabel rangeLabel = new JLabel(" ");
    private final StatCard overallCard = new StatCard("Overall adherence", "rose");
    private final StatCard studyCard = new StatCard("Study consistency", "rose");
    private final StatCard prayerCard = new StatCard("Prayer consistency", "rose");
    private final StatCard streakCard = new StatCard("Best streak", "rose");
    private final BarChartPanel chart = new BarChartPanel();
    private final JPanel categoryRows = new JPanel();
    private final JTextArea moodInsight = new JTextArea();
    private final JLabel totalLabel = new JLabel(" ");

    public DashboardPanel(Database db) {
        this.db = db;
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 18, 18, 18));
        add(buildControls(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        periodBox.setSelectedItem(DateRange.Period.MONTHLY);
        periodBox.addActionListener(e -> refresh());
        anchorSpinner.addChangeListener(e -> refresh());
        refresh();
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        Theme.styleInput(periodBox);
        periodBox.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                String text = value instanceof DateRange.Period p ? switch (p) {
                    case DAILY -> "Daily"; case WEEKLY -> "Weekly"; case MONTHLY -> "Monthly";
                } : String.valueOf(value);
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });
        anchorSpinner.setEditor(new JSpinner.DateEditor(anchorSpinner, "dd MMM yyyy"));
        Theme.styleInput(anchorSpinner);
        left.add(Theme.label("View:"));
        left.add(periodBox);
        left.add(Theme.label("Date:"));
        left.add(anchorSpinner);
        rangeLabel.setFont(Theme.BODY);
        rangeLabel.setForeground(Theme.MUTED);
        panel.add(left, BorderLayout.WEST);
        panel.add(rangeLabel, BorderLayout.CENTER);
        JButton export = Theme.softButton("Export CSV report");
        export.addActionListener(e -> exportReport());
        panel.add(export, BorderLayout.EAST);
        return panel;
    }

    private JComponent buildContent() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);
        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 0));
        stats.setOpaque(false);
        stats.add(overallCard);
        stats.add(studyCard);
        stats.add(prayerCard);
        stats.add(streakCard);

        JPanel chartCard = Theme.card("YOUR DAILY CHECK-INS");
        chartCard.add(chart, BorderLayout.CENTER);
        chartCard.add(totalLabel, BorderLayout.SOUTH);
        totalLabel.setFont(Theme.BODY);
        totalLabel.setForeground(Theme.MUTED);

        categoryRows.setLayout(new BoxLayout(categoryRows, BoxLayout.Y_AXIS));
        categoryRows.setBackground(Theme.SURFACE);
        JPanel categoryCard = Theme.card("CATEGORY BREAKDOWN");
        categoryCard.add(categoryRows, BorderLayout.CENTER);

        moodInsight.setEditable(false);
        moodInsight.setLineWrap(true);
        moodInsight.setWrapStyleWord(true);
        moodInsight.setFont(Theme.BODY);
        moodInsight.setForeground(Theme.TEXT);
        moodInsight.setBackground(Theme.SURFACE);
        moodInsight.setBorder(null);
        JPanel moodCard = Theme.card("MOOD NOTES & PATTERNS");
        moodCard.add(Theme.scroll(moodInsight), BorderLayout.CENTER);

        JPanel lower = new JPanel(new GridLayout(1, 2, 12, 0));
        lower.setOpaque(false);
        lower.add(categoryCard);
        lower.add(moodCard);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(chartCard, BorderLayout.CENTER);
        center.add(lower, BorderLayout.SOUTH);
        content.add(stats, BorderLayout.NORTH);
        content.add(center, BorderLayout.CENTER);
        return content;
    }

    public void refresh() {
        try {
            LocalDate anchor = ((Date) anchorSpinner.getValue()).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            DateRange.Period period = (DateRange.Period) periodBox.getSelectedItem();
            DateRange range = DateRange.forPeriod(period == null ? DateRange.Period.MONTHLY : period, anchor);
            LocalDate today = LocalDate.now();
            LocalDate effectiveTo = range.effectiveTo(today);
            int days = range.elapsedDays(today);
            rangeLabel.setText(range.label());

            List<Habit> habits = db.getHabits();
            int habitDone = days == 0 ? 0 : AdherenceCalculator.completed(db, habits, null, range.from(), effectiveTo);
            int prayerDone = days == 0 ? 0 : db.countPrayerCompletions(range.from(), effectiveTo);
            int habitPossible = AdherenceCalculator.opportunities(habits, null, range.from(), effectiveTo);
            int prayerPossible = Prayer.values().length * days;
            int overallPossible = habitPossible + prayerPossible;
            int overallDone = habitDone + prayerDone;
            int overallPct = percent(overallDone, overallPossible);
            int studyCount = (int) habits.stream().filter(h -> h.category().equalsIgnoreCase("Study")).count();
            int studyPossible = AdherenceCalculator.opportunities(habits, "Study", range.from(), effectiveTo);
            int studyDone = days == 0 ? 0 : AdherenceCalculator.completed(db, habits, "Study", range.from(), effectiveTo);
            int studyPct = percent(studyDone, studyPossible);
            int prayerPct = percent(prayerDone, prayerPossible);
            overallCard.setValue(overallPct + "%", overallDone + " of " + overallPossible + " check-ins");
            studyCard.setValue(studyCount == 0 ? "—" : studyPct + "%",
                    studyCount == 0 ? "Add a habit in Study" : studyDone + " of " + studyPossible + " study check-ins");
            prayerCard.setValue(prayerPct + "%", prayerDone + " of " + prayerPossible + " prayers logged");

            int bestStreak = 0;
            String streakName = "No streak yet";
            String streakUnit = "days";
            for (Habit habit : habits) {
                int streak = db.currentHabitStreak(habit.id(), anchor.isAfter(today) ? today : anchor);
                if (streak > bestStreak) { bestStreak = streak; streakName = habit.name(); streakUnit = habit.scheduleType().equals("WEEKLY_TARGET") ? "weeks" : "scheduled days"; }
            }
            for (Prayer prayer : Prayer.values()) {
                int streak = db.currentPrayerStreak(prayer, anchor.isAfter(today) ? today : anchor);
                if (streak > bestStreak) { bestStreak = streak; streakName = prayer.label(); streakUnit = "days"; }
            }
            streakCard.setValue(bestStreak + " " + streakUnit, bestStreak == 0 ? "A fresh start is always possible" : streakName);

            Map<LocalDate, Integer> daily = new LinkedHashMap<>();
            if (days > 0) {
                daily.putAll(db.dailyHabitCompletions(range.from(), effectiveTo));
                db.dailyPrayerCompletions(range.from(), effectiveTo).forEach((date, count) ->
                        daily.merge(date, count, Integer::sum));
            }
            chart.setData(daily, range.from(), range.to());
            totalLabel.setText("Logged " + overallDone + " check-ins during this " + period.name().toLowerCase() + " period.");
            refreshCategories(habits, range, effectiveTo, days);
            refreshMood(range, effectiveTo, days);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshCategories(List<Habit> habits, DateRange range, LocalDate effectiveTo, int days) throws Exception {
        categoryRows.removeAll();
        String[] categories = {"Study", "Prayer", "Spiritual", "Health", "Reading", "Personal", "Other"};
        for (String category : categories) {
            int habitCount = (int) habits.stream().filter(h -> h.category().equalsIgnoreCase(category)).count();
            int done = days == 0 ? 0 : AdherenceCalculator.completed(db, habits, category, range.from(), effectiveTo);
            int possible = AdherenceCalculator.opportunities(habits, category, range.from(), effectiveTo);
            int pct = percent(done, possible);
            JPanel row = new JPanel(new BorderLayout(8, 4));
            row.setOpaque(false);
            JLabel title = new JLabel(category + (habitCount == 0 ? "  ·  no habits yet" : "  ·  " + pct + "%"));
            title.setFont(Theme.BODY_BOLD);
            title.setForeground(Theme.TEXT);
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(pct);
            bar.setStringPainted(false);
            bar.setForeground(category.equals("Study") ? Theme.ROSE : new Color(180, 151, 151));
            bar.setBackground(Theme.BLUSH);
            bar.setBorderPainted(false);
            row.add(title, BorderLayout.NORTH);
            row.add(bar, BorderLayout.CENTER);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            categoryRows.add(row);
            categoryRows.add(Box.createVerticalStrut(3));
        }
        categoryRows.revalidate();
        categoryRows.repaint();
    }

    private void refreshMood(DateRange range, LocalDate effectiveTo, int days) throws Exception {
        if (days == 0) {
            moodInsight.setText("No dates have elapsed in this period yet. Add a mood entry in Mood & Journal when you're ready.");
            return;
        }
        Map<String, Database.MoodSummary> summary = db.moodSummary(range.from(), effectiveTo);
        if (summary.isEmpty()) {
            moodInsight.setText("No mood entries in this period yet. Your journal is a gentle place to notice how your days feel. This is a personal reflection tool, not a mental-health assessment.");
            return;
        }
        StringBuilder text = new StringBuilder("Your most frequently logged mood is ");
        Map.Entry<String, Database.MoodSummary> top = summary.entrySet().iterator().next();
        text.append(top.getKey()).append(" ( ").append(top.getValue().days()).append(" day(s) ).\n\n");
        for (Map.Entry<String, Database.MoodSummary> entry : summary.entrySet()) {
            double average = entry.getValue().days() == 0 ? 0
                    : entry.getValue().taskCompletions() / (double) entry.getValue().days();
            text.append(entry.getKey()).append(": ").append(entry.getValue().days()).append(" day(s), ")
                    .append(String.format(java.util.Locale.US, "%.1f", average))
                    .append(" completed habit/prayer check-ins per logged day.\n");
        }
        text.append("\nThese are patterns in the entries you recorded, not a diagnosis or proof that mood caused a change in productivity.");
        moodInsight.setText(text.toString());
        moodInsight.setCaretPosition(0);
    }

    private int percent(int value, int total) { return total <= 0 ? 0 : (int) Math.round(value * 100.0 / total); }

    private void exportReport() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("my-daily-growth-report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            LocalDate anchor = ((Date) anchorSpinner.getValue()).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            DateRange range = DateRange.forPeriod((DateRange.Period) periodBox.getSelectedItem(), anchor);
            LocalDate to = range.effectiveTo(LocalDate.now());
            CsvExporter.export(db, chooser.getSelectedFile().toPath(), range.from(), to);
            JOptionPane.showMessageDialog(this, "Your CSV report has been saved.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { showError(ex); }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Something went wrong:\n" + ex.getMessage(), "My Daily Growth", JOptionPane.ERROR_MESSAGE);
    }
}
