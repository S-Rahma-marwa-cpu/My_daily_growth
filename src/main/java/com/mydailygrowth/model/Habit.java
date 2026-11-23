package com.mydailygrowth.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Set;

/** Habit plus its recurrence rule. Weekdays use ISO numbering (Monday=1 ... Sunday=7). */
public record Habit(int id, String name, String description, String category, LocalDate createdDate,
                    String scheduleType, Set<DayOfWeek> scheduledDays, int weeklyTarget) {
    public Habit {
        scheduledDays = scheduledDays == null ? Set.of() : Set.copyOf(scheduledDays);
        scheduleType = scheduleType == null ? "DAILY" : scheduleType;
        weeklyTarget = Math.max(1, Math.min(7, weeklyTarget));
    }

    public boolean isScheduledOn(LocalDate date) {
        if (date.isBefore(createdDate)) return false;
        return switch (scheduleType) {
            case "SELECTED_DAYS" -> scheduledDays.contains(date.getDayOfWeek());
            case "WEEKLY_TARGET" -> true; // The weekly quota is assessed over the whole week.
            default -> true;
        };
    }

    public String scheduleDescription() {
        return switch (scheduleType) {
            case "SELECTED_DAYS" -> scheduledDays.stream().sorted()
                    .map(d -> d.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH))
                    .reduce((a, b) -> a + ", " + b).orElse("No days selected");
            case "WEEKLY_TARGET" -> weeklyTarget + " times / week";
            default -> "Every day";
        };
    }
}
