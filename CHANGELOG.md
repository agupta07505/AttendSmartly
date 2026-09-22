# Changelog — AttendSmartly

All notable changes to **AttendSmartly** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0.0] - 2026-09-22

### Added
- 📍 **Classroom GPS Location & Automated Presence Detection**:
  - **Classroom Location Tagging**: Added GPS coordinate picker directly into the **Add/Edit Subject** dialog with one-tap device location detection (`LocationHelper.getCurrentLocation`), customizable detection radius (`25m`, `50m`, `100m`, `150m`), and coordinate display.
  - **Timetable Slot Overrides**: Added classroom GPS location configuration to **Add/Edit Timetable Entry** dialog, allowing individual timetable slots (e.g. lab rooms or tutorial halls) to inherit the subject's default location or override with specific coordinates and radius.
  - **5-Minute Dwell Loitering Engine**: Integrated Google Play Services Geofencing with `GEOFENCE_TRANSITION_DWELL` and a 5-minute dwell threshold (`setLoiteringDelay(5 * 60 * 1000)`). If a student is present within the classroom radius during scheduled class hours for 5 minutes, their attendance is automatically marked as **Present** with all session units logged.
  - **Foreground & Background Fallback Checker**: Added `LocationAttendanceManager` running foreground checks on the Home dashboard and periodic checks in `ReminderWorker` to ensure presence detection works whether the app is active, backgrounded, or closed.
  - **Location Auto-Attendance Settings**: Added a dedicated **"Location Auto-Attendance"** card in Settings with master enable/disable switch, live runtime permission status indicator, dwell duration indicator, and default radius selection.
  - **Visual Badges & Notifications**: Added visual `📍 [radius]m` and `AUTO-MARKED` status badges on class cards, and created a dedicated notification channel (`AttendSmartly_auto_attendance`) notifying the student when attendance is automatically marked via location.
- 📅 **Semester-Based Attendance Tracking**:
  - Added Semester Start and End date configuration in Settings with native Material `DatePickerDialog` integration.
  - Enforced semester boundaries across the app: the Home screen date selector bounds to the semester range, shows an **Outside Active Semester** banner on non-semester dates, and pauses reminder notifications outside active dates.
- 🎨 **Dynamic Custom Color Palettes**:
  - Added advanced Material 3 seed-based color palette generator (`generateColorSchemeFromSeed`) in `Theme.kt`.
  - Added Custom HEX color input and preset swatches in Settings, dynamically computing harmonious primary, secondary, tertiary, container, and outline tones for both Light and Dark modes.

### Changed
- 🗄️ **Room Database Schema v2**:
  - Bumped `AppDatabase` schema from version `1` to `2`.
  - Added `latitude: Double?`, `longitude: Double?`, and `locationRadiusMeters: Int` to `subjects` and `timetable_entries`.
  - Added `autoMarked: Boolean` to `attendance_sessions`.
  - Enabled `fallbackToDestructiveMigrationOnDowngrade` to ensure clean recovery during testing or app downgrades.
- 🧭 **Location Services Architecture**:
  - Upgraded location pipeline with `GoogleApiAvailability` detection to automatically fall back to native Android `LocationManager` on devices without Google Play Services (e.g., Huawei, custom ROMs, de-Googled devices).
  - Configured location accuracy requests to use `Priority.PRIORITY_BALANCED_POWER_ACCURACY` when only `ACCESS_COARSE_LOCATION` (Approximate Location) is granted, preventing Android 12+ `SecurityException`s.

### Fixed
- 🛡️ **Zero-Crash Startup & Migration Self-Healing (Fixed "App Crashes on Launch Unless Cache/Data is Cleared")**:
  - **Idempotent Room Migration**: Replaced naive `ALTER TABLE` statements in `MIGRATION_1_2` with `addColumnIfNotExists` inspecting SQLite `PRAGMA table_info`. This eliminates `SQLiteException: duplicate column name` on pre-existing columns and safely adds legacy columns (`isRescheduled`, `originalDate`, `originalTime`, `rescheduledToDate`, `rescheduledToTime`, `rescheduledReason`) if upgrading from early v1.0 databases.
  - **DataStore File Corruption Auto-Recovery**: Attached `ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })` to `preferencesDataStore` and hardened `userPreferencesFlow.catch` to catch all `Throwable`s, preventing corrupted preference files from crashing app startup.
  - **WorkManager & Application Startup Protection**: Guarded `Application.onCreate` and `ReminderWorker.schedulePeriodicReminderCheck` against WorkManager initialization failures and internal SQLite database corruption.
  - **Compose LazyColumn Duplicate Key Collision**: Switched schedule items in `HomeScreen.kt` (as well as `SubjectsScreen` and `AnalyticsScreen`) to `itemsIndexed` with unique index-suffixed composite keys (`${entry.id}_${session?.id}_${startTime}_$index`), completely preventing `IllegalArgumentException: Key was already used` in Jetpack Compose.
  - **Date Selector Scroll Bounds**: Guarded `dateRowState.animateScrollToItem` with index bounds validation and exception catching to prevent animation crashes during recomposition.
  - **Geofence Broadcast Receiver Crash Guard**: Protected `GeofencingEvent.fromIntent` against runtime reflection errors on devices without Google Play Services.
- 🐛 **Extra Class Visibility on Home Screen**:
  - Fixed issue where extra classes created without recurring timetable slots (`session.timetableEntryId == null`) were omitted from the Home dashboard schedule list.

---

## [2.0.0] - 2026-08-20

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
    - **About & Updates**: App version `v2.0` badge (Build code), release notes summary, GitHub updates check, license details (GNU GPL v3), and developer info.
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
