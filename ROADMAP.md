# AttendSmartly — Product Roadmap (2026 - 2027)

This document outlines the official development roadmap and future feature specifications for **AttendSmartly - Attendance Tracker**.

---

## 📌 Milestone Overview

| Milestone | Status | Target Release | Key Focus |
| --------- | ------ | -------------- | --------- |
| **v1.0.0** | :white_check_mark: Completed | Q3 2026 | Core Attendance Tracker, Timetable Builder, Bunk Calculator, Smart OCR |
| **v1.1.0** | :white_check_mark: Completed | Q3 2026 | Notification Preferences, Gemini Onboarding & Precision Math |
| **v2.0.0** | :white_check_mark: Completed | Q3 2026 | Class Rescheduling, Extra Classes, Modular Settings & Roundtrip Data Integrity |
| **v2.1.0** | 🟡 In Planning | Q4 2026 | Android Glance Widgets, Interactive Notifications & Calendar Sync |
| **v2.2.0** | ⏳ Scheduled | Q1 2027 | Multi-Semester Archiving, Historical Comparison & Term Reports |
| **v2.3.0** | ⏳ Scheduled | Q2 2027 | Advanced "What-If" Scenario Simulator, Exam Schedules |
| **v3.0.0** | 🔮 Vision | Q3 2027 | Wear OS App Companion, Opt-In Encrypted Cloud Sync (WebDAV/Drive) |

---

## 🎯 Version 2.0.0 — Class Rescheduling, Extra Classes & Modular Settings

### 🔀 Class Rescheduling & Extra Classes
- [x] Floating action speed dial on Home screen with dedicated actions for Extra Classes and Rescheduling.
- [x] One-off extra class scheduler with custom unit count without altering recurring weekly timetables.
- [x] Class rescheduling with reason metadata, notice indicators, and instant one-tap revert.

### 🗂️ Modular Settings Screen
- [x] Reorganized Settings into 6 dedicated sub-windows with smooth animated transitions and back handling.
- [x] Quick preset chips for Target Percentage (`70%`, `75%`, `80%`, `85%`, `90%`) and Reminder Lead Times (`5m`, `10m`, `15m`, `30m`).
- [x] Native Material `DatePickerDialog` integration for Semester Start & End dates.
- [x] Polished Alert Sound, Alert Vibration, and Dynamic Material 3 icon containers.

### 💾 Data Integrity & Backup Fidelity
- [x] Fixed subject session resolution on specific dates with fallback matching.
- [x] `getSessionForSubjectDateAndTime` in `AttendanceDao` for multiple classes of the same subject on the same day.
- [x] Full roundtrip preservation of start/end dates during JSON backup restore.

---

## 🎯 Version 1.1.0 — Notifications, Gemini Onboarding & Precision Math

### 🔔 Notification Settings & Reminders
- [x] Customizable notification toggles for enabling/disabling reminders, sound, and vibration options.
- [x] Android 13+ runtime `POST_NOTIFICATIONS` permission prompt in `MainActivity` and `SettingsScreen`.
- [x] Detailed reminder alarms carrying class room numbers, instructor names, duration, and unit counts with instant notification actions.

### 🔑 Gemini API & Onboarding Enhancements
- [x] Flexible onboarding path choices: Upload Timetable OCR, Load Demo Data, or Manual Setup.
- [x] Gemini API key input card with visibility toggle and direct link to Google AI Studio.

### 🧮 Calculation Precision & Bunk Math Polish
- [x] Epsilon tolerance floating-point math for precision calculation of safe bunks and recovery classes.
- [x] Target threshold (100%) edge-case formula corrections and progress card label handling for 0 safe bunks.

---

## 📲 Version 1.2.0 — Widgets & Quick Actions

### 📲 Home Screen Widgets (Glance API)
- [ ] **Quick Mark Widget**: View your current/next class and log Present, Absent, or Cancelled directly from your Android home screen without launching the full app.
- [ ] **Bunk & Percentage Glance Widget**: Real-time display of overall attendance percentage and safe bunk allowance.

### 🔔 Smart Notification Enhancements
- [ ] Customizable reminder offset times (5, 10, 15, 30, or 60 minutes before class).
- [ ] Snooze reminder capability for rescheduled lectures.

---

## 📅 Version 1.3.0 — Multi-Semester Archiving

### 📁 Term Archiving System
- [ ] Archive completed semester data to keep daily views focused on current term classes.
- [ ] Historical semester comparison in Analytics tab (compare Spring vs Fall attendance performance).
- [ ] Export full multi-semester PDF/CSV academic record summary.

---

## 🚀 Version 1.4.0 — Calendar Sync & Predictive Simulations

### 🔄 Calendar Integration
- [ ] Export class schedule directly to Android System Calendar / Google Calendar via `.ics` format.
- [ ] Import university exam dates and academic holiday calendars automatically.

### 🔮 "What-If" Bunk Simulator
- [ ] Interactive slider to simulate planned upcoming absences (e.g. "What if I take next Friday off?") and see real-time impact on target percentages.
- [ ] Semester completion projection: Calculates projected final attendance percentage at the end of the term.

---

## 🔮 Long-Term Vision (v2.0+)

- [ ] **Wear OS Companion App**: Check your next lecture room number and log attendance directly from your smartwatch.
- [ ] **Opt-In Encrypted Cloud Backup**: End-to-end encrypted backup to Google Drive or custom WebDAV servers for seamless device upgrades.
- [ ] **Desktop Companion (Kotlin Multiplatform)**: Manage timetables on desktop with cross-device sync.

---

## 💡 Suggest a Feature

Have an idea for AttendSmartly? We love community feedback! Submit a feature request using our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).
