# Changelog — AttendSmartly

All notable changes to **AttendSmartly** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
