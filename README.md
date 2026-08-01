# AttendSmartly - Attendance Tracker

> **Track classes. Plan bunks. Attend smartly.**

**AttendSmartly** is a clean, modern, student-focused Android attendance-tracking application built natively with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and **Room Database**.

It helps college students track lectures, tutorials, and multi-hour practical lab sessions, configure customizable attendance rules, schedule weekly timetables, receive class reminders, and instantly calculate safe bunks or required consecutive classes to maintain their target attendance percentage.

---

## Features

- 📅 **Weekly Timetable Scheduling**: Easily configure recurring daily classes, classroom locations, teachers, and custom attendance unit rules.
- ⏱️ **Multi-Hour Partial Attendance Tracking**: Flexible attendance units (e.g., 2-hour lecture = 2 units, 2-hour lab = 1 unit). Mark individual units as Present, Absent, or Cancelled.
- 🎯 **Target Attendance & Bunk Calculator**:
  - Automatically calculates overall and subject-wise attendance percentages.
  - **Safe Bunk Calculator**: Calculates exactly how many upcoming classes can be safely skipped without dropping below your target percentage.
  - **Recovery Calculator**: Calculates how many consecutive classes must be attended to recover attendance back to your target.
- 🔔 **Class Reminders**: Background reminders via WorkManager and AlarmManager before scheduled classes.
- 📊 **Detailed Analytics & History**: Visual donut charts, status breakdowns, and editable historical logs.
- 📦 **Offline-First Data Backup & Export**: Export/import JSON backups and export CSV reports directly via Android Storage Access Framework (SAF).
- 🎨 **Material Design 3 Theming**: Dynamic M3 color system, Light/Dark mode support, edge-to-edge layout, and responsive UI.

---

## Tech Stack & Architecture

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: Single-Activity, MVVM, Clean Architecture with Repository pattern
- **Database**: Room Database with KSP & Coroutine Flow
- **Preferences**: DataStore Preferences
- **Background Tasks**: WorkManager & AlarmManager
- **Navigation**: Navigation Compose with type-safe parameters
- **Data Export**: Gson (JSON) & OpenCSV (CSV)

---

## Building & Running

1. Open the project in **Android Studio (Ladybug or newer)**.
2. Ensure JDK 17+ and Android SDK 34+ are configured.
3. Sync Gradle and build the project:
   ```bash
   gradle assembleDebug
   ```
4. Run unit tests:
   ```bash
   gradle test
   ```

---

## Author & License

Designed and developed by **Animesh Gupta** ([@agupta07505](https://github.com/agupta07505)).

Free & Open Source.
