package com.mydailygrowth.ui;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.model.LearningEntry;
import com.mydailygrowth.model.MoodEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@SuppressWarnings("serial")
public final class JournalPanel extends JPanel {
    private final Database db;
    private final Runnable onChanged;
    private final JSpinner moodDate = new JSpinner(new SpinnerDateModel());
    private final JComboBox<String> moodBox = new JComboBox<>(new String[]{"Excellent", "Good", "Okay", "Low", "Bad"});
    private final JTextArea moodNote = new JTextArea(4, 24);
    private final JLabel moodSummary = new JLabel(" ");
    private final MoodTableModel moodModel = new MoodTableModel();
    private final JTable moodTable = new JTable(moodModel);

    private final JSpinner learningDate = new JSpinner(new SpinnerDateModel());
    private final JTextField learningTitle = new JTextField();
    private final JTextArea learningDetails = new JTextArea(3, 24);
    private final LearningTableModel learningModel = new LearningTableModel();
    private final JTable learningTable = new JTable(learningModel);

    public JournalPanel(Database db, Runnable onChanged) {
        this.db = db;
        this.onChanged = onChanged;
        setLayout(new GridLayout(1, 2, 14, 0));
        setBackground(Theme.BACKGROUND);
        setBorder(new EmptyBorder(18, 18, 18, 18));
        add(buildMoodPanel());
        add(buildLearningPanel());
        moodDate.setEditor(new JSpinner.DateEditor(moodDate, "dd MMM yyyy"));
        learningDate.setEditor(new JSpinner.DateEditor(learningDate, "dd MMM yyyy"));
        moodDate.addChangeListener(e -> { loadMoodForSelectedDate(); refreshMoodHistory(); });
        refresh();
    }

    private JComponent buildMoodPanel() {
        JPanel panel = Theme.card("MOOD JOURNAL");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 3, 4, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0; form.add(Theme.label("Date"), c);
        c.gridy++; Theme.styleInput(moodDate); form.add(moodDate, c);
        c.gridy++; form.add(Theme.label("How did you feel?"), c);
        c.gridy++; Theme.styleInput(moodBox); form.add(moodBox, c);
        c.gridy++; form.add(Theme.label("A note about your day (optional)"), c);
        c.gridy++; moodNote.setLineWrap(true); moodNote.setWrapStyleWord(true); moodNote.setFont(Theme.BODY);
        form.add(new JScrollPane(moodNote), c);
        c.gridy++; JButton save = Theme.button("Save mood entry");
        save.addActionListener(e -> saveMood()); form.add(save, c);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);

        JPanel history = new JPanel(new BorderLayout(5, 5));
        history.setOpaque(false);
        JLabel historyTitle = Theme.label("This month's mood history");
        history.add(historyTitle, BorderLayout.NORTH);
        styleTable(moodTable);
        moodTable.setRowHeight(27);
        moodTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && moodTable.getSelectedRow() >= 0) {
                int row = moodTable.convertRowIndexToModel(moodTable.getSelectedRow());
                MoodEntry entry = moodModel.getAt(row);
                moodDate.setValue(toDate(entry.date()));
                loadMoodForSelectedDate();
            }
        });
        history.add(Theme.scroll(moodTable), BorderLayout.CENTER);
        content.add(history, BorderLayout.CENTER);
        moodSummary.setFont(Theme.BODY);
        moodSummary.setForeground(Theme.ROSE_DARK);
        JPanel footer = new JPanel(new BorderLayout(4, 4));
        footer.setOpaque(false);
        footer.add(moodSummary, BorderLayout.NORTH);
        JLabel note = new JLabel("Your mood notes are for personal reflection, not diagnosis.");
        note.setFont(new Font("SansSerif", Font.ITALIC, 10)); note.setForeground(Theme.MUTED);
        footer.add(note, BorderLayout.SOUTH);
        content.add(footer, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildLearningPanel() {
        JPanel panel = Theme.card("LEARNING JOURNAL");
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 3, 4, 3);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0; c.gridy = 0; form.add(Theme.label("Date"), c);
        c.gridy++; Theme.styleInput(learningDate); form.add(learningDate, c);
        c.gridy++; form.add(Theme.label("What did you learn?"), c);
        c.gridy++; Theme.styleInput(learningTitle); form.add(learningTitle, c);
        c.gridy++; form.add(Theme.label("Details / useful notes"), c);
        c.gridy++; learningDetails.setLineWrap(true); learningDetails.setWrapStyleWord(true); learningDetails.setFont(Theme.BODY);
        form.add(new JScrollPane(learningDetails), c);
        c.gridy++; JButton add = Theme.button("＋ Add learning");
        add.addActionListener(e -> addLearning()); form.add(add, c);
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);

        styleTable(learningTable);
        learningTable.setRowHeight(32);
        learningTable.getColumnModel().getColumn(0).setPreferredWidth(95);
        learningTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        learningTable.getColumnModel().getColumn(2).setPreferredWidth(190);
        content.add(Theme.scroll(learningTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        actions.setOpaque(false);
        JButton edit = Theme.softButton("Edit selected");
        JButton delete = Theme.softButton("Delete selected");
        edit.addActionListener(e -> editLearning());
        delete.addActionListener(e -> deleteLearning());
        actions.add(edit); actions.add(delete);
        content.add(actions, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void styleTable(JTable table) {
        table.setFont(Theme.BODY);
        table.setForeground(Theme.TEXT);
        table.setBackground(Theme.SURFACE);
        table.setSelectionBackground(Theme.BLUSH);
        table.setSelectionForeground(Theme.TEXT);
        table.setGridColor(Theme.BORDER);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(Theme.BODY_BOLD);
        table.getTableHeader().setBackground(Theme.MAUVE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);
    }

    public void refresh() {
        loadMoodForSelectedDate();
        refreshMoodHistory();
        refreshLearnings();
    }

    private void loadMoodForSelectedDate() {
        try {
            LocalDate date = selectedMoodDate();
            MoodEntry entry = db.getMood(date);
            if (entry == null) {
                moodBox.setSelectedItem("Good");
                moodNote.setText("");
            } else {
                moodBox.setSelectedItem(entry.mood());
                moodNote.setText(entry.note());
            }
            refreshMoodSummary(date);
        } catch (Exception ex) { showError(ex); }
    }

    private void refreshMoodHistory() {
        try {
            LocalDate date = selectedMoodDate();
            YearMonth month = YearMonth.from(date);
            moodModel.setEntries(db.getMoods(month.atDay(1), month.atEndOfMonth()));
        } catch (Exception ex) { showError(ex); }
    }

    private void refreshMoodSummary(LocalDate date) throws Exception {
        YearMonth month = YearMonth.from(date);
        List<MoodEntry> entries = db.getMoods(month.atDay(1), month.atEndOfMonth());
        moodSummary.setText("Logged " + entries.size() + " mood day(s) in " + month.getMonth() + ".");
    }

    private void saveMood() {
        try {
            db.saveMood(selectedMoodDate(), (String) moodBox.getSelectedItem(), moodNote.getText());
            refreshMoodHistory();
            refreshMoodSummary(selectedMoodDate());
            onChanged.run();
            JOptionPane.showMessageDialog(this, "Your mood entry has been saved.", "Mood journal", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) { showError(ex); }
    }

    private void addLearning() {
        try {
            db.addLearning(selectedLearningDate(), learningTitle.getText(), learningDetails.getText());
            learningTitle.setText("");
            learningDetails.setText("");
            refreshLearnings();
            onChanged.run();
        } catch (Exception ex) { showError(ex); }
    }

    private void editLearning() {
        int row = learningTable.getSelectedRow();
        if (row < 0) { info("Choose a learning entry to edit."); return; }
        LearningEntry entry = learningModel.getAt(learningTable.convertRowIndexToModel(row));
        JSpinner date = new JSpinner(new SpinnerDateModel(toDate(entry.date()), null, null, java.util.Calendar.DAY_OF_MONTH));
        date.setEditor(new JSpinner.DateEditor(date, "dd MMM yyyy"));
        JTextField title = new JTextField(entry.title(), 25);
        JTextArea details = new JTextArea(entry.details(), 5, 25);
        details.setLineWrap(true); details.setWrapStyleWord(true);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(Theme.label("Date")); form.add(date);
        form.add(Theme.label("Title")); form.add(title);
        form.add(Theme.label("Details")); form.add(new JScrollPane(details));
        int result = JOptionPane.showConfirmDialog(this, form, "Edit learning entry", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDate newDate = ((Date) date.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                db.updateLearning(entry.id(), newDate, title.getText(), details.getText());
                refreshLearnings();
                onChanged.run();
            } catch (Exception ex) { showError(ex); }
        }
    }

    private void deleteLearning() {
        int row = learningTable.getSelectedRow();
        if (row < 0) { info("Choose a learning entry to delete."); return; }
        LearningEntry entry = learningModel.getAt(learningTable.convertRowIndexToModel(row));
        int answer = JOptionPane.showConfirmDialog(this, "Delete '" + entry.title() + "'?",
                "Delete learning", JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            try {
                db.deleteLearning(entry.id());
                refreshLearnings();
                onChanged.run();
            } catch (Exception ex) { showError(ex); }
        }
    }

    private void refreshLearnings() {
        try { learningModel.setEntries(db.getAllLearnings()); }
        catch (Exception ex) { showError(ex); }
    }

    private LocalDate selectedMoodDate() {
        return ((Date) moodDate.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDate selectedLearningDate() {
        return ((Date) learningDate.getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private void info(String message) {
        JOptionPane.showMessageDialog(this, message, "My Daily Growth", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "My Daily Growth", JOptionPane.ERROR_MESSAGE);
    }

    @SuppressWarnings("serial")
    private static final class MoodTableModel extends AbstractTableModel {
        private List<MoodEntry> entries = List.of();
        void setEntries(List<MoodEntry> entries) { this.entries = List.copyOf(entries); fireTableDataChanged(); }
        MoodEntry getAt(int row) { return entries.get(row); }
        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return 2; }
        @Override public String getColumnName(int column) { return column == 0 ? "DATE" : "MOOD"; }
        @Override public Object getValueAt(int row, int column) {
            MoodEntry entry = entries.get(row);
            return column == 0 ? entry.date() : entry.mood();
        }
    }

    @SuppressWarnings("serial")
    private static final class LearningTableModel extends AbstractTableModel {
        private List<LearningEntry> entries = List.of();
        void setEntries(List<LearningEntry> entries) { this.entries = List.copyOf(entries); fireTableDataChanged(); }
        LearningEntry getAt(int row) { return entries.get(row); }
        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return 3; }
        @Override public String getColumnName(int column) { return switch (column) {
            case 0 -> "DATE"; case 1 -> "WHAT I LEARNED"; default -> "DETAILS";
        }; }
        @Override public Object getValueAt(int row, int column) {
            LearningEntry entry = entries.get(row);
            return switch (column) {
                case 0 -> entry.date(); case 1 -> entry.title(); default -> entry.details();
            };
        }
    }
}
