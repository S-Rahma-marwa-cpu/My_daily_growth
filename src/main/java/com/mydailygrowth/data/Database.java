package com.mydailygrowth.data;

import com.mydailygrowth.model.Habit;
import com.mydailygrowth.model.LearningEntry;
import com.mydailygrowth.model.MoodEntry;
import com.mydailygrowth.model.Prayer;

import java.sql.*;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;

/**
 * SQLite repository. Existing V2/V3 tables are upgraded without deleting user
 * records.
 */
public final class Database implements AutoCloseable {
	private final Connection connection;

	public record MoodSummary(int days, int taskCompletions) {
	}

	public Database() throws SQLException {
		connection = DriverManager.getConnection("jdbc:sqlite:habit_tracker.db");
		try (Statement st = connection.createStatement()) {
			st.execute("PRAGMA foreign_keys = ON");
			st.execute("PRAGMA busy_timeout = 5000");
			st.execute("""
					CREATE TABLE IF NOT EXISTS habits (
					    id INTEGER PRIMARY KEY AUTOINCREMENT,
					    name TEXT NOT NULL UNIQUE,
					    description TEXT NOT NULL DEFAULT '',
					    category TEXT NOT NULL DEFAULT 'Personal',
					    created_date TEXT NOT NULL DEFAULT '1900-01-01'
					)""");
			ensureColumn("habits", "category", "TEXT NOT NULL DEFAULT 'Personal'");
			ensureColumn("habits", "description", "TEXT NOT NULL DEFAULT ''");
			ensureColumn("habits", "created_date", "TEXT NOT NULL DEFAULT '1900-01-01'");
			ensureColumn("habits", "schedule_type", "TEXT NOT NULL DEFAULT 'DAILY'");
			ensureColumn("habits", "scheduled_days", "TEXT NOT NULL DEFAULT '1,2,3,4,5,6,7'");
			ensureColumn("habits", "weekly_target", "INTEGER NOT NULL DEFAULT 7");
			st.execute("""
					CREATE TABLE IF NOT EXISTS completions (
					    habit_id INTEGER NOT NULL,
					    completion_date TEXT NOT NULL,
					    PRIMARY KEY (habit_id, completion_date),
					    FOREIGN KEY (habit_id) REFERENCES habits(id) ON DELETE CASCADE
					)""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS moods (
					    mood_date TEXT PRIMARY KEY,
					    mood_value TEXT NOT NULL,
					    note TEXT NOT NULL DEFAULT ''
					)""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS learnings (
					    id INTEGER PRIMARY KEY AUTOINCREMENT,
					    learning_date TEXT NOT NULL,
					    title TEXT NOT NULL,
					    details TEXT NOT NULL DEFAULT ''
					)""");
			st.execute("""
					CREATE TABLE IF NOT EXISTS prayer_completions (
					    prayer_name TEXT NOT NULL,
					    completion_date TEXT NOT NULL,
					    PRIMARY KEY (prayer_name, completion_date)
					)""");
			st.execute("CREATE INDEX IF NOT EXISTS idx_completions_date ON completions(completion_date)");
			st.execute("CREATE INDEX IF NOT EXISTS idx_learnings_date ON learnings(learning_date)");
		}
	}

	private void ensureColumn(String table, String column, String definition) throws SQLException {
		boolean found = false;
		try (Statement st = connection.createStatement();
				ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
			while (rs.next())
				if (column.equalsIgnoreCase(rs.getString("name")))
					found = true;
		}
		if (!found) {
			try (Statement st = connection.createStatement()) {
				st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
			}
		}
	}

	public List<Habit> getHabits() throws SQLException {
		List<Habit> result = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(
				"SELECT id, name, description, category, created_date, schedule_type, scheduled_days, weekly_target FROM habits ORDER BY category, name");
				ResultSet rs = ps.executeQuery()) {
			while (rs.next())
				result.add(new Habit(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
						LocalDate.parse(rs.getString(5)), rs.getString(6), parseDays(rs.getString(7)), rs.getInt(8)));
		}
		return result;
	}

	public void addHabit(String name, String description, String category) throws SQLException {
		addHabit(name, description, category, "DAILY", Set.of(DayOfWeek.values()), 7);
	}

	public void addHabit(String name, String description, String category, String scheduleType, Set<DayOfWeek> days,
			int weeklyTarget) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(
				"INSERT INTO habits(name, description, category, created_date, schedule_type, scheduled_days, weekly_target) VALUES(?, ?, ?, ?, ?, ?, ?)")) {
			ps.setString(1, requireText(name, "Habit name"));
			ps.setString(2, clean(description));
			ps.setString(3, requireText(category, "Category"));
			ps.setString(4, LocalDate.now().toString());
			bindSchedule(ps, 5, scheduleType, days, weeklyTarget);
			ps.executeUpdate();
		}
	}

	public void updateHabit(int id, String name, String description, String category) throws SQLException {
		updateHabit(id, name, description, category, "DAILY", Set.of(DayOfWeek.values()), 7);
	}

	public void updateHabit(int id, String name, String description, String category, String scheduleType,
			Set<DayOfWeek> days, int weeklyTarget) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(
				"UPDATE habits SET name=?, description=?, category=?, schedule_type=?, scheduled_days=?, weekly_target=? WHERE id=?")) {
			ps.setString(1, requireText(name, "Habit name"));
			ps.setString(2, clean(description));
			ps.setString(3, requireText(category, "Category"));
			bindSchedule(ps, 4, scheduleType, days, weeklyTarget);
			ps.setInt(7, id);
			ps.executeUpdate();
		}
	}

	private void bindSchedule(PreparedStatement ps, int start, String type, Set<DayOfWeek> days, int target)
			throws SQLException {
		String safeType = Set.of("DAILY", "SELECTED_DAYS", "WEEKLY_TARGET").contains(type) ? type : "DAILY";
		Set<DayOfWeek> safeDays = days == null ? Set.of() : days;
		if (safeType.equals("SELECTED_DAYS") && safeDays.isEmpty())
			throw new IllegalArgumentException("Choose at least one weekday.");
		ps.setString(start, safeType);
		ps.setString(start + 1, safeDays.stream().sorted().map(d -> Integer.toString(d.getValue()))
				.reduce((a, b) -> a + "," + b).orElse("1,2,3,4,5,6,7"));
		ps.setInt(start + 2, Math.max(1, Math.min(7, target)));
	}

	private Set<DayOfWeek> parseDays(String encoded) {
		Set<DayOfWeek> days = new LinkedHashSet<>();
		if (encoded != null)
			for (String part : encoded.split(","))
				try {
					days.add(DayOfWeek.of(Integer.parseInt(part)));
				} catch (RuntimeException ignored) {
				}
		return days.isEmpty() ? Set.of(DayOfWeek.values()) : days;
	}

	public void deleteHabit(int id) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("DELETE FROM habits WHERE id=?")) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	public boolean isCompleted(int habitId, LocalDate date) throws SQLException {
		try (PreparedStatement ps = connection
				.prepareStatement("SELECT 1 FROM completions WHERE habit_id=? AND completion_date=?")) {
			ps.setInt(1, habitId);
			ps.setString(2, date.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	public void setCompleted(int habitId, LocalDate date, boolean completed) throws SQLException {
		String sql = completed ? "INSERT OR IGNORE INTO completions(habit_id, completion_date) VALUES(?, ?)"
				: "DELETE FROM completions WHERE habit_id=? AND completion_date=?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, habitId);
			ps.setString(2, date.toString());
			ps.executeUpdate();
		}
	}

	public int countHabitsInCategory(String category) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM habits WHERE category=?")) {
			ps.setString(1, category);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	public int countCompletions(LocalDate from, LocalDate to) throws SQLException {
		return scalarCount("SELECT COUNT(*) FROM completions WHERE completion_date BETWEEN ? AND ?", from, to);
	}

	public int countCompletionsForCategory(String category, LocalDate from, LocalDate to) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT COUNT(*) FROM completions c JOIN habits h ON h.id=c.habit_id
				WHERE h.category=? AND c.completion_date BETWEEN ? AND ?
				""")) {
			ps.setString(1, category);
			ps.setString(2, from.toString());
			ps.setString(3, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	private int scalarCount(String sql, LocalDate from, LocalDate to) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	public Map<LocalDate, Integer> dailyHabitCompletions(LocalDate from, LocalDate to) throws SQLException {
		Map<LocalDate, Integer> result = new LinkedHashMap<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT completion_date, COUNT(*) FROM completions
				WHERE completion_date BETWEEN ? AND ? GROUP BY completion_date ORDER BY completion_date
				""")) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.put(LocalDate.parse(rs.getString(1)), rs.getInt(2));
			}
		}
		return result;
	}

	public boolean isPrayerCompleted(Prayer prayer, LocalDate date) throws SQLException {
		try (PreparedStatement ps = connection
				.prepareStatement("SELECT 1 FROM prayer_completions WHERE prayer_name=? AND completion_date=?")) {
			ps.setString(1, prayer.name());
			ps.setString(2, date.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		}
	}

	public void setPrayerCompleted(Prayer prayer, LocalDate date, boolean completed) throws SQLException {
		String sql = completed ? "INSERT OR IGNORE INTO prayer_completions(prayer_name, completion_date) VALUES(?, ?)"
				: "DELETE FROM prayer_completions WHERE prayer_name=? AND completion_date=?";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, prayer.name());
			ps.setString(2, date.toString());
			ps.executeUpdate();
		}
	}

	public int countPrayerCompletions(LocalDate from, LocalDate to) throws SQLException {
		return scalarCount("SELECT COUNT(*) FROM prayer_completions WHERE completion_date BETWEEN ? AND ?", from, to);
	}

	public int countPrayerCompletions(Prayer prayer, LocalDate from, LocalDate to) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT COUNT(*) FROM prayer_completions
				WHERE prayer_name=? AND completion_date BETWEEN ? AND ?
				""")) {
			ps.setString(1, prayer.name());
			ps.setString(2, from.toString());
			ps.setString(3, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	public Map<LocalDate, Integer> dailyPrayerCompletions(LocalDate from, LocalDate to) throws SQLException {
		Map<LocalDate, Integer> result = new LinkedHashMap<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT completion_date, COUNT(*) FROM prayer_completions
				WHERE completion_date BETWEEN ? AND ? GROUP BY completion_date ORDER BY completion_date
				""")) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.put(LocalDate.parse(rs.getString(1)), rs.getInt(2));
			}
		}
		return result;
	}

	public int currentHabitStreak(int habitId, LocalDate today) throws SQLException {
		Habit habit = getHabits().stream().filter(h -> h.id() == habitId).findFirst().orElse(null);
		if (habit == null)
			return 0;
		if (habit.scheduleType().equals("WEEKLY_TARGET")) {
			int streak = 0;
			LocalDate week = today.with(java.time.DayOfWeek.MONDAY);
			if (weeklyHabitCompletions(habitId, week, week.plusDays(6)) < habit.weeklyTarget())
				week = week.minusWeeks(1);
			while (!week.isBefore(habit.createdDate().with(java.time.DayOfWeek.MONDAY))) {
				if (weeklyHabitCompletions(habitId, week, week.plusDays(6)) < habit.weeklyTarget())
					break;
				streak++;
				week = week.minusWeeks(1);
			}
			return streak;
		}
		LocalDate cursor = today;
		if (habit.isScheduledOn(cursor) && !isCompleted(habitId, cursor))
			cursor = cursor.minusDays(1);
		int streak = 0;
		while (!cursor.isBefore(habit.createdDate())) {
			if (habit.isScheduledOn(cursor)) {
				if (!isCompleted(habitId, cursor))
					break;
				streak++;
			}
			cursor = cursor.minusDays(1);
		}
		return streak;
	}

	private int weeklyHabitCompletions(int habitId, LocalDate from, LocalDate to) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(
				"SELECT COUNT(*) FROM completions WHERE habit_id=? AND completion_date BETWEEN ? AND ?")) {
			ps.setInt(1, habitId);
			ps.setString(2, from.toString());
			ps.setString(3, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		}
	}

	public int currentPrayerStreak(Prayer prayer, LocalDate today) throws SQLException {
		List<LocalDate> dates = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT completion_date FROM prayer_completions
				WHERE prayer_name=? AND completion_date<=? ORDER BY completion_date DESC
				""")) {
			ps.setString(1, prayer.name());
			ps.setString(2, today.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					dates.add(LocalDate.parse(rs.getString(1)));
			}
		}
		return consecutiveStreak(dates, today);
	}

	private List<LocalDate> completionDates(String sql, int id, LocalDate today) throws SQLException {
		List<LocalDate> dates = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, id);
			ps.setString(2, today.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					dates.add(LocalDate.parse(rs.getString(1)));
			}
		}
		return dates;
	}

	private int consecutiveStreak(List<LocalDate> dates, LocalDate today) {
		if (dates.isEmpty())
			return 0;
		LocalDate expected = dates.get(0).equals(today) ? today : today.minusDays(1);
		if (!dates.get(0).equals(expected))
			return 0;
		int streak = 0;
		for (LocalDate date : dates) {
			if (!date.equals(expected))
				break;
			streak++;
			expected = expected.minusDays(1);
		}
		return streak;
	}

	public void saveMood(LocalDate date, String mood, String note) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("""
				INSERT INTO moods(mood_date, mood_value, note) VALUES(?, ?, ?)
				ON CONFLICT(mood_date) DO UPDATE SET mood_value=excluded.mood_value, note=excluded.note
				""")) {
			ps.setString(1, date.toString());
			ps.setString(2, requireText(mood, "Mood"));
			ps.setString(3, clean(note));
			ps.executeUpdate();
		}
	}

	public MoodEntry getMood(LocalDate date) throws SQLException {
		try (PreparedStatement ps = connection
				.prepareStatement("SELECT mood_value, note FROM moods WHERE mood_date=?")) {
			ps.setString(1, date.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? new MoodEntry(date, rs.getString(1), rs.getString(2)) : null;
			}
		}
	}

	public List<MoodEntry> getMoods(LocalDate from, LocalDate to) throws SQLException {
		List<MoodEntry> result = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT mood_date, mood_value, note FROM moods
				WHERE mood_date BETWEEN ? AND ? ORDER BY mood_date DESC
				""")) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.add(new MoodEntry(LocalDate.parse(rs.getString(1)), rs.getString(2), rs.getString(3)));
			}
		}
		return result;
	}

	public Map<String, MoodSummary> moodSummary(LocalDate from, LocalDate to) throws SQLException {
		Map<String, MoodSummary> result = new LinkedHashMap<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT m.mood_value, COUNT(*) AS mood_days,
				       SUM(COALESCE(h.done, 0) + COALESCE(p.done, 0)) AS task_count
				FROM moods m
				LEFT JOIN (SELECT completion_date, COUNT(*) AS done FROM completions GROUP BY completion_date) h
				    ON h.completion_date=m.mood_date
				LEFT JOIN (SELECT completion_date, COUNT(*) AS done FROM prayer_completions GROUP BY completion_date) p
				    ON p.completion_date=m.mood_date
				WHERE m.mood_date BETWEEN ? AND ?
				GROUP BY m.mood_value ORDER BY mood_days DESC
				""")) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.put(rs.getString(1), new MoodSummary(rs.getInt(2), rs.getInt(3)));
			}
		}
		return result;
	}

	public void addLearning(LocalDate date, String title, String details) throws SQLException {
		try (PreparedStatement ps = connection
				.prepareStatement("INSERT INTO learnings(learning_date, title, details) VALUES(?, ?, ?)")) {
			ps.setString(1, date.toString());
			ps.setString(2, requireText(title, "Learning title"));
			ps.setString(3, clean(details));
			ps.executeUpdate();
		}
	}

	public void updateLearning(int id, LocalDate date, String title, String details) throws SQLException {
		try (PreparedStatement ps = connection
				.prepareStatement("UPDATE learnings SET learning_date=?, title=?, details=? WHERE id=?")) {
			ps.setString(1, date.toString());
			ps.setString(2, requireText(title, "Learning title"));
			ps.setString(3, clean(details));
			ps.setInt(4, id);
			ps.executeUpdate();
		}
	}

	public void deleteLearning(int id) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement("DELETE FROM learnings WHERE id=?")) {
			ps.setInt(1, id);
			ps.executeUpdate();
		}
	}

	public List<LearningEntry> getLearnings(LocalDate from, LocalDate to) throws SQLException {
		List<LearningEntry> result = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement("""
				SELECT id, learning_date, title, details FROM learnings
				WHERE learning_date BETWEEN ? AND ? ORDER BY learning_date DESC, id DESC
				""")) {
			ps.setString(1, from.toString());
			ps.setString(2, to.toString());
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					result.add(new LearningEntry(rs.getInt(1), LocalDate.parse(rs.getString(2)), rs.getString(3),
							rs.getString(4)));
			}
		}
		return result;
	}

	public List<LearningEntry> getAllLearnings() throws SQLException {
		return getLearnings(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31));
	}

	public List<MoodEntry> getAllMoods() throws SQLException {
		return getMoods(LocalDate.of(1900, 1, 1), LocalDate.of(9999, 12, 31));
	}

	private String requireText(String value, String field) {
		if (value == null || value.trim().isEmpty())
			throw new IllegalArgumentException(field + " is required.");
		return value.trim();
	}

	private String clean(String value) {
		return value == null ? "" : value.trim();
	}

	@Override
	public void close() throws SQLException {
		connection.close();
	}
}
