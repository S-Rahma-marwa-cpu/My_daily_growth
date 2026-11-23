package com.mydailygrowth.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public record DateRange(LocalDate from, LocalDate to, String label) {
    public enum Period { DAILY, WEEKLY, MONTHLY }

    public static DateRange forPeriod(Period period, LocalDate anchor) {
        return switch (period) {
            case DAILY -> new DateRange(anchor, anchor, anchor.toString());
            case WEEKLY -> {
                LocalDate start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate end = start.plusDays(6);
                yield new DateRange(start, end, start + " – " + end);
            }
            case MONTHLY -> {
                YearMonth month = YearMonth.from(anchor);
                yield new DateRange(month.atDay(1), month.atEndOfMonth(), month.getMonth() + " " + month.getYear());
            }
        };
    }

    public LocalDate effectiveTo(LocalDate today) {
        return to.isAfter(today) ? today : to;
    }

    public int elapsedDays(LocalDate today) {
        LocalDate end = effectiveTo(today);
        if (end.isBefore(from)) return 0;
        return (int) (end.toEpochDay() - from.toEpochDay() + 1);
    }
}
