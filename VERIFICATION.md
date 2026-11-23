# V5 verification notes

- Java sources compile with `javac --release 17 -Xlint:all` (no compiler output/warnings).
- Maven build was not run because Maven is not installed in the build environment.
- The new schedule calculation was exercised with a small Java 17 test harness for daily, selected-weekday, and weekly-target rules.
- SQLite migration is additive: `ensureColumn` adds `schedule_type`, `scheduled_days`, and `weekly_target` only when absent. Existing rows receive DAILY / all weekdays / 7 defaults, and completion rows are untouched.
- Full JDBC runtime integration against the SQLite driver could not be executed in this environment; run the app locally with Maven/Internet access to resolve the SQLite JDBC dependency.
- Existing legacy completions on newly-unscheduled weekdays remain in the database and CSV history; they are ignored for schedule-based adherence.
- The existing streak display still uses its legacy consecutive-calendar-day calculation; schedule-aware streaks (especially weekly-target streaks) are a remaining limitation.
