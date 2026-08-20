# Changelog — AttendSmartly

All notable changes to **AttendSmartly** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-08-20

### Added
- 🔀 **Class Rescheduling & Extra Class Scheduler**:
  - Added **Add Extra Class** quick action from the Home screen speed dial to schedule one-off or compensatory classes on any day with custom unit counts and times without altering recurring weekly timetables.
  - Added **Reschedule Class** support to move any scheduled class to a new date/time with reason notes, incoming/outgoing reschedule indicator badges, and one-tap **Revert** capability.
  - Context-aware speed dial on the Home screen displaying quick actions for Extra Classes, Rescheduling, Adding Subjects, and Adding Timetable Entries with smooth back navigation.
- 🗂️ **Modular Sub-Window Settings Architecture**:
  - Reorganized Settings into a high-level overview menu with 6 dedicated sub-windows:
    - **Attendance Rules & Goals**: Target percentage slider with instant preset chips (`70%`, `75%`, `80%`, `85%`, `90%`), reminder lead time chips (`5m`, `10m`, `15m`, `30m`), and native Material `DatePickerDialog` for semester calendar dates.
    - **Notifications & Alerts**: Master class reminders switch, Alert sound chime toggle, and Alert vibration feedback toggle with polished icon containers.
    - **Appearance & Theme**: Responsive 3-option theme cards (System, Light, Dark) and Dynamic Material 3 wallpaper-based colors toggle.
    - **AI & Timetable Scanner**: Gemini API key configuration with show/hide toggle and direct link to Google AI Studio.
    - **Data Management & Backup**: JSON backup export & restore, CSV attendance reports, and full data reset with safety confirmations.
    - **About & Updates**: App version `v1.2` badge (Build code), release notes summary, GitHub updates check, license details (GNU GPL v3), and developer info.
  - Smooth animated slide transitions and back button handling between sub-windows.

### Changed
- 🎨 **UI & Component Polish**:
  - Standardized unmarked "Present" button color to neutral `surfaceVariant`, eliminating visual confusion with marked states.
  - Unified icon styling with rounded tinted surface containers for Alert Sound, Alert Vibration, and Dynamic Colors.
  - Removed sample demo data option from Settings in favor of a clean, dedicated data reset workflow.
  - Updated AutoMirrored vector icons across the app.

### Fixed
- 🐛 **Subject Visibility on Specific Dates**:
  - Fixed session resolution where subject sessions were omitted on specific dates due to timetable entry matching; now links sessions by `timetableEntryId` with fallback to `(subjectId, startTime)`.
  - Added `getSessionForSubjectDateAndTime` in `AttendanceDao` to prevent collisions when a subject has multiple classes on the same date (e.g. morning lecture and afternoon lab).
- 💾 **JSON Backup Date Roundtrip Fidelity**:
  - Fixed backup restore issue where blank timetable entry start dates were previously overwritten with `todayIso()`, preserving past and future timetable history across export and restore cycles.
  - Handled `NULL` and empty string date bounds gracefully in Room SQL queries.

---

## [1.1.0] - 2026-08-06

### Added
- 🔔 **Notification Preferences & Sound/Vibration Controls**:
  - Added user preference toggles in Settings screen for enabling/disabling notifications, sound alerts, and vibration feedback.
- 🔑 **Gemini API & Enhanced Onboarding Flow**:
  - Reorganized onboarding setup screen with API key visibility toggle, direct link to Google AI Studio, and multiple setup path options (Upload Timetable OCR, Load Demo Data, or Manual Setup).
- 🛡️ **Android 13+ Notification Permission Prompt**:
  - Integrated runtime `POST_NOTIFICATIONS` permission checks and user prompt dialogs in `MainActivity` and `SettingsScreen`.

### Changed
- 🧮 **Precision Math & Edge-Case Handling in Bunk Calculator**:
  - Updated `AttendanceCalculator` logic with epsilon floating-point tolerance, precise 100% target percentage handling, corrected safe bunks and required units formulas, and improved progress card UI feedback when safe bunks equal zero.
- 🔔 **Interactive Class Reminders**:
  - Extended notification alarms to pass detailed class metadata (room, teacher, duration, units, minutes before), automatically handle session/unit creation on missing entries, and support one-tap mark-present and mark-absent notification actions with safe large icon fallbacks and refined 24dp vector drawables.
- ⚙️ **CI/CD Workflow & Build Setup**:
  - Updated GitHub Actions dependencies (checkout, setup-java, upload-artifact) to stable v4 releases and pinned Kotlin/KSP versions.

---

## [1.0.0] - 2026-08-01

### Added
- 🚀 **Initial Release of AttendSmartly**:
  - **Weekly Timetable Builder**: Full scheduling support for lectures, labs, and tutorials.
  - **Custom Attendance Unit Rules**: Support for multi-hour sessions (e.g. 2-hour lab = 1 unit).
  - **Bunk & Recovery Calculator**: Safe bunk limit calculation and recovery class calculation to reach target attendance percentage.
  - **Class Reminders**: Background reminders via AlarmManager and WorkManager.
  - **Smart Timetable OCR**: Scan timetable photos via Google Gemini API.
  - **Analytics & History**: Interactive progress cards, donut charts, and editable historical logs.
  - **Data Backup & Export**: Import/export JSON timetable backups and CSV attendance reports.
  - **Material Design 3**: Dynamic theming with light and dark mode support.
  - **GPL-3.0 License**: Added copyright notice and licensing compliance across all Kotlin files.
