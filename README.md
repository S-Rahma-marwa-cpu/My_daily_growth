# My Daily Growth — Java Swing 4.0

A personal desktop tracker with a soft blush-pink, dusty-rose, beige and mauve theme. Built with Java 17, Swing and SQLite.

## Features

- **Overview dashboard:** daily, weekly and monthly views; overall adherence; study consistency; prayer consistency; best streak; daily completion chart; category breakdown; mood patterns.
- **Flexible habits:** create, edit and delete habits; choose a category; filter the monthly calendar by category; tick completed days in a dynamic month grid (28–31 days and leap years supported).
- **Separate five-prayer tracker:** Fajr, Dhuhr, Asr, Maghrib and Isha, each saved independently for the selected date; daily, weekly and monthly prayer totals.
- **Study tracking:** add as many habits as needed under the Study category (for example, revision, exercises, language practice); study adherence is calculated separately.
- **Mood journal:** one mood entry per date, optional notes, monthly history and a gentle summary of the moods recorded.
- **Learning journal:** add, edit and delete dated learning notes.
- **CSV report:** export habits, prayer check-ins, mood entries and learning notes for the selected dashboard period. UTF-8 with a BOM for spreadsheet compatibility.
- **Local persistence:** SQLite database stored as `habit_tracker.db` in the application's working directory.
- **Fair first-month calculations:** new habits count toward expected check-ins from their creation date. Older V2/V3 habits did not store a creation date, so their migration default is a legacy start date and their existing history is preserved.
- **Data-preserving upgrade:** existing V2/V3 `habits`, `completions`, `moods` and `learnings` records are retained. Missing columns and the prayer table are added without dropping existing tables.

## Project structure

```text
MyDailyGrowth/
├── pom.xml
├── README.md
├── README_AR.md
└── src/main/java/com/mydailygrowth/
    ├── Main.java
    ├── data/
    │   └── Database.java
    ├── export/
    │   └── CsvExporter.java
    ├── model/
    │   ├── Habit.java
    │   ├── LearningEntry.java
    │   ├── MoodEntry.java
    │   └── Prayer.java
    ├── service/
    │   ├── AdherenceCalculator.java
    │   └── DateRange.java
    └── ui/
        ├── AppFrame.java
        ├── BarChartPanel.java
        ├── DashboardPanel.java
        ├── HabitsPanel.java
        ├── JournalPanel.java
        ├── PrayerPanel.java
        ├── StatCard.java
        └── Theme.java
```

Each package has one responsibility: `model` holds data types, `data` handles SQLite, `service` contains date/adherence calculations, `ui` contains Swing screens and styling, and `export` creates CSV reports.

## Requirements

- JDK 17 or newer
- Apache Maven 3.8 or newer
- Internet access on the first Maven run so Maven can download the SQLite JDBC dependency

## Run from a terminal

1. Extract the ZIP.
2. Open a terminal **inside the `MyDailyGrowth` folder** (the folder containing `pom.xml`).
3. Run:

```bash
mvn clean compile
mvn exec:java
```

The first run downloads SQLite JDBC and creates `habit_tracker.db` in that working folder. On Windows, you can also double-click `run-windows.bat`; on macOS/Linux, run `./run-macos-linux.sh` from the extracted folder.

## Open in IntelliJ IDEA

1. Start IntelliJ IDEA and choose **Open**.
2. Select the extracted `MyDailyGrowth` folder (not just `src`).
3. When prompted, trust/open it as a Maven project; wait for Maven import to finish.
4. Open `src/main/java/com/mydailygrowth/Main.java`.
5. Click the green Run icon beside `main`, or use the Maven tool window → `my-daily-growth` → `Plugins` → `exec` → `exec:java`.

## Open in Eclipse

1. Choose **File → Import → Maven → Existing Maven Projects**.
2. Select the extracted `MyDailyGrowth` folder and finish the import.
3. Open `Main.java` under `src/main/java`.
4. Right-click it → **Run As → Java Application**.

## Your data and backups

- Keep `habit_tracker.db` to keep your habits, check-ins, prayer history, moods and learning notes.
- Back it up by closing the application and copying that file to a safe location.
- Do not delete the database file when updating the Java source.
- The application is local-only; it does not upload journal or prayer data to a server.

See [VERIFICATION.md](VERIFICATION.md) for the checks completed while preparing this ZIP and the environment limitation.

## Notes and limitations

- Habits are treated as daily habits. The app does not yet support custom schedules such as weekdays-only or three times per week.
- Prayer check-ins are separate from regular habits and always use the five named prayers.
- Mood insights summarize only the entries you choose to record. They are not medical or psychological assessments and do not prove that mood caused productivity changes.
- There are no push notifications, login/accounts or cloud synchronization in this version.
- The CSV export is intended for spreadsheet software such as Excel or LibreOffice Calc.

## Habit schedules (V5)
Each habit can now use one of three recurrence rules:
- **Every day** (default, including habits upgraded from older databases).
- **Selected weekdays** (choose any combination of Monday through Sunday).
- **Weekly target** (1–7 completions per ISO week, e.g. 3 times/week).

Habit adherence in the habits screen and dashboard uses scheduled opportunities rather than calendar days. Weekly-target habits contribute up to the selected quota per week; extra check-ins are retained in history but do not inflate the adherence percentage. Unscheduled dates are shown as a dash and cannot be checked. The SQLite migration adds `schedule_type`, `scheduled_days`, and `weekly_target` with defaults that preserve legacy habits as daily habits. Existing completion history is not deleted.
