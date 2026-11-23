package com.mydailygrowth.ui;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.model.Habit;
import com.mydailygrowth.service.AdherenceCalculator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public final class HabitsPanel extends JPanel {
    private static final String[] CATEGORIES = {"Study","sport" ,"Prayer", "Spiritual", "Health", "Reading", "Personal", "Other"};
    private final Database db;
    private final Runnable onChanged;
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
    private final JComboBox<String> filterBox = new JComboBox<>();
    private final JLabel progressLabel = new JLabel(" ");
    private List<Habit> habits = List.of();
    private final HabitTableModel model = new HabitTableModel();
    private final JTable table = new JTable(model);
    private final TableRowSorter<HabitTableModel> sorter = new TableRowSorter<>(model);

    public HabitsPanel(Database db, Runnable onChanged) {
        
    	this.db = db;
        this.onChanged = onChanged;
        setLayout(new BorderLayout(10, 10));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(16, 18, 18, 18));
        for (Month month : Month.values()) monthBox.addItem(month.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "####"));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        applyPrettyDesign(); 
        add(buildFooter(), BorderLayout.SOUTH);
        monthBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        monthBox.addActionListener(e -> refresh());
        yearSpinner.addChangeListener(e -> refresh());
        filterBox.addActionListener(e -> applyFilter());
        refresh();
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        for (String category : CATEGORIES) filterBox.addItem(category);
        filterBox.insertItemAt("All categories", 0);
        filterBox.setSelectedIndex(0);
        Theme.styleInput(monthBox);
        Theme.styleInput(yearSpinner);
        Theme.styleInput(filterBox);
        JButton add = Theme.button("+ Add habit");
        JButton edit = Theme.softButton("Edit selected");
        JButton delete = Theme.softButton("Delete selected");
        JButton today = Theme.softButton("Current month");
        add.addActionListener(e -> editHabit(null));
        edit.addActionListener(e -> {
            Habit habit = selectedHabit();
            if (habit == null) info("Choose a habit to edit.");
            else editHabit(habit);
        });
        delete.addActionListener(e -> deleteSelected());
        today.addActionListener(e -> {
            monthBox.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
            yearSpinner.setValue(LocalDate.now().getYear());
        });
        bar.add(Theme.label("Month")); bar.add(monthBox);
        bar.add(Theme.label("Year")); bar.add(yearSpinner);
        bar.add(Theme.label("Filter")); bar.add(filterBox);
        bar.add(add); bar.add(edit); bar.add(delete); bar.add(today);
        return bar;
    }

    private JComponent buildTable() {
        table.setRowSorter(sorter);
        table.setRowHeight(34);
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setGridColor(Theme.BORDER);
        table.setSelectionBackground(Theme.BLUSH);
        table.setSelectionForeground(Theme.TEXT);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.MAUVE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                if (!selected) c.setBackground(row % 2 == 0 ? Theme.SURFACE : Theme.BACKGROUND);
                setForeground(Theme.TEXT);
                setHorizontalAlignment(column == 0 ? LEFT : CENTER);
                return c;
            }
        });
        table.setDefaultRenderer(Boolean.class, new javax.swing.table.TableCellRenderer() {
            private final JCheckBox check = new JCheckBox();
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int column) {
                check.setHorizontalAlignment(SwingConstants.CENTER);
                check.setOpaque(true);
                check.setSelected(Boolean.TRUE.equals(value));
                int modelRow = t.convertRowIndexToModel(row);
                LocalDate cellDate = model.getModelColumnDate(t.convertColumnIndexToModel(column));
                boolean scheduled = cellDate != null && habits.get(modelRow).isScheduledOn(cellDate);
                check.setText(scheduled ? "" : "–");
                check.setEnabled(scheduled && model.isDateEditable(cellDate));
                check.setBackground(selected ? Theme.BLUSH : (row % 2 == 0 ? Theme.SURFACE : Theme.BACKGROUND));
                check.setEnabled(model.isDateEditable(model.getModelColumnDate(column)));
                return check;
            }
        });
        JScrollPane scroll = Theme.scroll(table);
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(6, 5));
        footer.setOpaque(false);
        JLabel hint = new JLabel("Tick only the highlighted scheduled days. A dash means the habit is not due that day. Changes save automatically.");
        hint.setFont(Theme.BODY);
        hint.setForeground(Theme.MUTED);
        progressLabel.setFont(Theme.BODY_BOLD);
        progressLabel.setForeground(Theme.ROSE_DARK);
        footer.add(hint, BorderLayout.NORTH);
        footer.add(progressLabel, BorderLayout.SOUTH);
        return footer;
    }

    public void refresh() {
        try {
            habits = db.getHabits();
            model.setMonth(selectedMonth());
            model.fireTableStructureChanged();
            table.setRowSorter(sorter);
            table.setRowHeight(34);
            if (table.getColumnModel().getColumnCount() > 0) {
                table.getColumnModel().getColumn(0).setPreferredWidth(230);
                for (int i = 1; i < table.getColumnModel().getColumnCount(); i++)
                    table.getColumnModel().getColumn(i).setPreferredWidth(56);
            }
            applyFilter();
            YearMonth ym = selectedMonth();
            LocalDate today = LocalDate.now();
            LocalDate to = ym.isAfter(YearMonth.from(today)) ? ym.atDay(1).minusDays(1)
                    : (ym.equals(YearMonth.from(today)) ? today : ym.atEndOfMonth());
            int days = to.isBefore(ym.atDay(1)) ? 0 : (int) (to.toEpochDay() - ym.atDay(1).toEpochDay() + 1);
            int total = days == 0 ? 0 : AdherenceCalculator.completed(db, habits, null, ym.atDay(1), to);
            int possible = AdherenceCalculator.opportunities(habits, null, ym.atDay(1), to);
            int percent = possible == 0 ? 0 : (int) Math.round(total * 100.0 / possible);
            progressLabel.setText(monthProgressLabel(ym) + ": " + percent + "%  ·  " + total + "/" + possible + " check-ins");
        } catch (SQLException ex) { showError(ex); }
    }

    private YearMonth selectedMonth() {
        return YearMonth.of((Integer) yearSpinner.getValue(), monthBox.getSelectedIndex() + 1);
    }

    private void applyFilter() {
        String category = (String) filterBox.getSelectedItem();
        if (category == null || category.equals("All categories")) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote("· " + category), 0));
    }

    private Habit selectedHabit() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return habits.get(table.convertRowIndexToModel(viewRow));
    }

    private void editHabit(Habit existing) {

        JTextField name = new JTextField(
            existing == null ? "" : existing.name(), 24);

        JTextField description = new JTextField(
            existing == null ? "" : existing.description(), 24);

        JComboBox<String> category = new JComboBox<>(CATEGORIES);

        if (existing != null) {
            if (!List.of(CATEGORIES).contains(existing.category())) {
                category.addItem(existing.category());
            }
            category.setSelectedItem(existing.category());
        }

        JComboBox<String> schedule = new JComboBox<>(
            new String[]{
                "Every day",
                "Selected weekdays",
                "Weekly target (e.g. 3 times)"
            });

        if (existing != null) {
            schedule.setSelectedIndex(
                switch (existing.scheduleType()) {
                    case "SELECTED_DAYS" -> 1;
                    case "WEEKLY_TARGET" -> 2;
                    default -> 0;
                }
            );
        }

        JCheckBox[] weekdayChecks = new JCheckBox[7];

        JPanel daysPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT, 8, 5));

        daysPanel.setBackground(Theme.SURFACE);

        String[] labels = {
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        };

        for (int i = 0; i < 7; i++) {
            weekdayChecks[i] = new JCheckBox(labels[i]);
            weekdayChecks[i].setBackground(Theme.SURFACE);
            weekdayChecks[i].setForeground(Theme.TEXT);
            weekdayChecks[i].setFont(Theme.BODY);
            daysPanel.add(weekdayChecks[i]);
        }

        JSpinner weeklyTarget = new JSpinner(
            new SpinnerNumberModel(
                existing == null ? 3 : existing.weeklyTarget(),
                1, 7, 1));

        JPanel targetPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT));

        targetPanel.setBackground(Theme.SURFACE);
        targetPanel.add(new JLabel("Target per week:"));
        targetPanel.add(weeklyTarget);

        // Keep the dialog stable when changing repeat options
        JPanel emptyPanel = new JPanel();
        emptyPanel.setBackground(Theme.SURFACE);

        JPanel scheduleDetails = new JPanel(new CardLayout());
        scheduleDetails.setBackground(Theme.SURFACE);

        scheduleDetails.add(emptyPanel, "EMPTY");
        scheduleDetails.add(daysPanel, "DAYS");
        scheduleDetails.add(targetPanel, "TARGET");

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setBackground(Theme.SURFACE);

        form.add(Theme.label("Habit name"));
        form.add(name);

        form.add(Theme.label("Short description (optional)"));
        form.add(description);

        form.add(Theme.label("Category"));
        form.add(category);

        form.add(Theme.label("Repeat"));
        form.add(schedule);

        form.add(scheduleDetails);

        Runnable updateScheduleDetails = () -> {
            CardLayout layout =
                (CardLayout) scheduleDetails.getLayout();

            switch (schedule.getSelectedIndex()) {
                case 1 -> layout.show(scheduleDetails, "DAYS");
                case 2 -> layout.show(scheduleDetails, "TARGET");
                default -> layout.show(scheduleDetails, "EMPTY");
            }

            scheduleDetails.revalidate();
            scheduleDetails.repaint();
            form.revalidate();
            form.repaint();
        };

        schedule.addActionListener(e ->
            updateScheduleDetails.run());

        updateScheduleDetails.run();

        Theme.styleInput(name);
        Theme.styleInput(description);
        Theme.styleInput(category);
        Theme.styleInput(schedule);
        Theme.styleInput(weeklyTarget);

        int result = JOptionPane.showConfirmDialog(
            this,
            form,
            existing == null
                ? "Create a new habit"
                : "Edit habit",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) return;

        if (name.getText().trim().isEmpty()) {
            info("Please enter a habit name.");
            return;
        }

        try {
            java.util.Set<java.time.DayOfWeek> selectedDays =
                new java.util.LinkedHashSet<>();
            for (int i = 0; i < 7; i++) {
                if (weekdayChecks[i].isSelected()) {
                    selectedDays.add(
                        java.time.DayOfWeek.of(i + 1));
                }
            }

            String type = switch (schedule.getSelectedIndex()) {
                case 1 -> "SELECTED_DAYS";
                case 2 -> "WEEKLY_TARGET";
                default -> "DAILY";
            };

            if (type.equals("SELECTED_DAYS")
                    && selectedDays.isEmpty()) {
                info("Choose at least one weekday.");
                return;
            }

            int target = (Integer) weeklyTarget.getValue();

            if (existing == null) {
                db.addHabit(
                    name.getText().trim(),
                    description.getText().trim(),
                    (String) category.getSelectedItem(),
                    type,
                    selectedDays,
                    target
                );
            } else {
                db.updateHabit(
                    existing.id(),
                    name.getText().trim(),
                    description.getText().trim(),
                    (String) category.getSelectedItem(),
                    type,
                    selectedDays,
                    target
                );
            }

            refresh();
            onChanged.run();

        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        Habit habit = selectedHabit();
        if (habit == null) { info("Choose a habit to delete."); return; }
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete '" + habit.name() + "' and its saved completion history?",
                "Delete habit", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                db.deleteHabit(habit.id());
                refresh();
                onChanged.run();
            } catch (SQLException ex) { showError(ex); }
        }
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "My Daily Growth", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "My Daily Growth", JOptionPane.ERROR_MESSAGE);
    }

    @SuppressWarnings("serial")
    private final class HabitTableModel extends AbstractTableModel {
        private YearMonth month = YearMonth.now();
        void setMonth(YearMonth month) { this.month = month; }
        LocalDate getModelColumnDate(int modelColumn) { return modelColumn <= 0 ? null : month.atDay(modelColumn); }
        boolean isDateEditable(LocalDate date) { return date != null && !date.isAfter(LocalDate.now()); }
        @Override public int getRowCount() { return habits.size(); }
        @Override public int getColumnCount() { return month.lengthOfMonth() + 1; }
        @Override public String getColumnName(int column) {
            if (column == 0) return "MY HABITS";
            LocalDate date = month.atDay(column);
            return column + "  " + date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        }
        @Override public Class<?> getColumnClass(int column) { return column == 0 ? String.class : Boolean.class; }
        @Override public boolean isCellEditable(int row, int column) { return column > 0 && isDateEditable(getModelColumnDate(column)) && habits.get(row).isScheduledOn(getModelColumnDate(column)); }
        @Override public Object getValueAt(int row, int column) {
            Habit habit = habits.get(row);
            if (column == 0) return "" + habit.name() + "  ·  " + habit.category() + "  ·  " + habit.scheduleDescription();
            try { return db.isCompleted(habit.id(), month.atDay(column)); }
            catch (SQLException ex) { return false; }
        }
        @Override public void setValueAt(Object value, int row, int column) {
            if (!isCellEditable(row, column)) return;
            try {
                db.setCompleted(habits.get(row).id(), month.atDay(column), Boolean.TRUE.equals(value));
                fireTableCellUpdated(row, column);
                refreshProgressOnly();
                onChanged.run();
            } catch (SQLException ex) { showError(ex); }
        }
    }

    private String monthProgressLabel(YearMonth month) {
        YearMonth current = YearMonth.from(LocalDate.now());
        if (month.equals(current)) return "Month-to-date habit adherence";
        if (month.isBefore(current)) return "Monthly habit adherence";
        return "Future-month habit adherence";
    }

    private void refreshProgressOnly() {
        try {
            YearMonth ym = selectedMonth();
            LocalDate today = LocalDate.now();
            LocalDate to = ym.isAfter(YearMonth.from(today)) ? ym.atDay(1).minusDays(1)
                    : (ym.equals(YearMonth.from(today)) ? today : ym.atEndOfMonth());
            int days = to.isBefore(ym.atDay(1)) ? 0 : (int) (to.toEpochDay() - ym.atDay(1).toEpochDay() + 1);
            int total = days == 0 ? 0 : AdherenceCalculator.completed(db, habits, null, ym.atDay(1), to);
            int possible = AdherenceCalculator.opportunities(habits, null, ym.atDay(1), to);
            int percent = possible == 0 ? 0 : (int) Math.round(total * 100.0 / possible);
            progressLabel.setText(monthProgressLabel(ym) + ": " + percent + "%  ·  " + total + "/" + possible + " check-ins");
        } catch (SQLException ex) { showError(ex); }
    }
    private void applyPrettyDesign() {
        // الكتيبة الرقيقة المايلة اللي تحبيها
        java.awt.Font prettyFont = new java.awt.Font("Segoe UI Light", java.awt.Font.ITALIC, 13);
        java.awt.Font headerFont = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12);

        table.setFont(prettyFont);
        table.setRowHeight(38);
        table.setForeground(new java.awt.Color(60, 60, 60));
        table.setGridColor(new java.awt.Color(230, 230, 230));
        table.setShowGrid(true);
        table.setSelectionBackground(new java.awt.Color(255, 228, 235));
        table.setSelectionForeground(java.awt.Color.BLACK);

        // هذا هو الحل تاع الأيام اللي ما يبانوش
        javax.swing.table.JTableHeader header = table.getTableHeader();
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                c.setFont(headerFont);
                c.setBackground(new java.awt.Color(55, 55, 75)); // غامق باش يبان
                c.setForeground(java.awt.Color.WHITE); // كتيبة بيضة تبان
                ((javax.swing.JLabel) c).setHorizontalAlignment(CENTER);
                ((javax.swing.JLabel) c).setBorder(
                    javax.swing.BorderFactory.createMatteBorder(0,0,1,1, new java.awt.Color(80,80,90))
                );
                return c;
            }
        });
        header.setPreferredSize(new java.awt.Dimension(header.getPreferredSize().width, 35));
    }
}
