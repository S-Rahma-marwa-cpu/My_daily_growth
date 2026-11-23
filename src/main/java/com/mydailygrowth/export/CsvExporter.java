package com.mydailygrowth.export;

import com.mydailygrowth.data.Database;
import com.mydailygrowth.model.Habit;
import com.mydailygrowth.model.LearningEntry;
import com.mydailygrowth.model.MoodEntry;
import com.mydailygrowth.model.Prayer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Exports a selected date range as an Excel-friendly UTF-8 CSV file. */
public final class CsvExporter {
	private CsvExporter() {
	}

	public static void export(Database db, Path path, LocalDate from, LocalDate to) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			writer.write('\uFEFF');
			writer.write("MY DAILY GROWTH REPORT\n");
			writer.write("Period," + csv(from.toString()) + "," + csv(to.toString()) + "\n\n");

			writer.write("HABIT CHECK-INS\nHabit,Category,Schedule,Date,Completed\n");
			List<Habit> habits = db.getHabits();
			if (!to.isBefore(from)) {
				for (Habit habit : habits) {
					for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
						writer.write(
								csv(habit.name()) + "," + csv(habit.category()) + "," + csv(habit.scheduleDescription())
										+ "," + date + "," + db.isCompleted(habit.id(), date) + "\n");
					}
				}
			}

			writer.write("\nPRAYER CHECK-INS\nPrayer,Date,Completed\n");
			if (!to.isBefore(from)) {
				for (Prayer prayer : Prayer.values()) {
					for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
						writer.write(
								csv(prayer.label()) + "," + date + "," + db.isPrayerCompleted(prayer, date) + "\n");
					}
				}
			}

			writer.write("\nMOOD JOURNAL\nDate,Mood,Note\n");
			for (MoodEntry mood : db.getMoods(from, to)) {
				writer.write(mood.date() + "," + csv(mood.mood()) + "," + csv(mood.note()) + "\n");
			}

			writer.write("\nLEARNING JOURNAL\nDate,Title,Details\n");
			for (LearningEntry learning : db.getLearnings(from, to)) {
				writer.write(learning.date() + "," + csv(learning.title()) + "," + csv(learning.details()) + "\n");
			}
		}
	}

	private static String csv(String value) {
		String safe = value == null ? "" : value;
		return "\"" + safe.replace("\"", "\"\"") + "\"";
	}
}
