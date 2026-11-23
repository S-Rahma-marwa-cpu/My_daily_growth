package com.mydailygrowth.service;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.model.Habit;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Schedule-aware habit opportunities and achieved check-ins. */
public final class AdherenceCalculator {
    private AdherenceCalculator() { }

    public static int activeDays(Habit habit, LocalDate from, LocalDate to) {
        return opportunitiesForHabit(habit, from, to);
    }

    public static int opportunities(List<Habit> habits, String category, LocalDate from, LocalDate to) {
        int total = 0;
        for (Habit habit : habits) if (category == null || category.equalsIgnoreCase(habit.category()))
            total += opportunitiesForHabit(habit, from, to);
        return total;
    }

    public static int completed(Database db, List<Habit> habits, String category, LocalDate from, LocalDate to)
            throws SQLException {
        if (to.isBefore(from)) return 0;
        int total = 0;
        for (Habit habit : habits) {
            if (category != null && !category.equalsIgnoreCase(habit.category())) continue;
            if (habit.scheduleType().equals("WEEKLY_TARGET")) {
                Map<String, Integer> perWeek = new HashMap<>();
                for (LocalDate d = max(from, habit.createdDate()); !d.isAfter(to); d = d.plusDays(1)) {
                    if (db.isCompleted(habit.id(), d)) {
                        String week = d.get(WeekFields.ISO.weekBasedYear()) + "-" + d.get(WeekFields.ISO.weekOfWeekBasedYear());
                        perWeek.merge(week, 1, Integer::sum);
                    }
                }
                total += perWeek.values().stream().mapToInt(v -> Math.min(v, habit.weeklyTarget())).sum();
            } else {
                for (LocalDate d = max(from, habit.createdDate()); !d.isAfter(to); d = d.plusDays(1))
                    if (habit.isScheduledOn(d) && db.isCompleted(habit.id(), d)) total++;
            }
        }
        return total;
    }

    private static int opportunitiesForHabit(Habit habit, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || to.isBefore(habit.createdDate())) return 0;
        LocalDate start = max(from, habit.createdDate());
        if (habit.scheduleType().equals("WEEKLY_TARGET")) {
            int total = 0;
            for (LocalDate weekStart = start.with(DayOfWeek.MONDAY); !weekStart.isAfter(to); weekStart = weekStart.plusWeeks(1)) {
                LocalDate segmentStart = max(start, weekStart);
                LocalDate segmentEnd = min(to, weekStart.plusDays(6));
                long availableDays = segmentEnd.toEpochDay() - segmentStart.toEpochDay() + 1;
                total += (int) Math.min(habit.weeklyTarget(), Math.max(0, availableDays));
            }
            return total;
        }
        int total = 0;
        for (LocalDate d = start; !d.isAfter(to); d = d.plusDays(1)) if (habit.isScheduledOn(d)) total++;
        return total;
    }
    private static LocalDate max(LocalDate a, LocalDate b) { return a.isAfter(b) ? a : b; }
    private static LocalDate min(LocalDate a, LocalDate b) { return a.isBefore(b) ? a : b; }
}
