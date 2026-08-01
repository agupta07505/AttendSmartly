## Description

<!-- Briefly describe what this PR changes and why. -->

## Related Issue

<!-- Use "Closes #123" or "Fixes #123" when applicable. -->

## Type Of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update
- [ ] UI/UX improvement
- [ ] Refactor
- [ ] Test addition or update
- [ ] Build/CI release change

## App Area

- [ ] Timetable Scheduler & Unit Rules
- [ ] Safe Bunk & Recovery Calculator
- [ ] Attendance History & Session Logs
- [ ] Smart Timetable OCR Scanner
- [ ] Class Reminders (AlarmManager / WorkManager)
- [ ] Data Export / JSON Backups
- [ ] Jetpack Compose UI & Material 3 Theming
- [ ] Room Database & DataStore Preferences
- [ ] CI/CD or Gradle Configuration
- [ ] Documentation

## Screenshots Or Recordings

<!-- Required for visible UI changes. Add before/after screenshots when possible. -->

| Before | After |
|--------|-------|
|        |       |

## Testing

<!-- List commands, devices, emulators, and manual checks performed. -->

- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] Manual device/emulator testing
- [ ] Unit calculation verification (Safe Bunk / Recovery)
- [ ] WorkManager / Alarm notification tested, if affected

## Checklist

- [ ] My code follows the existing project style and architecture.
- [ ] I kept the GPL v3 copyright header notice intact in all modified `.kt` files:
  ```kotlin
  /*
   * AttendSmartly (2026)
   * © Animesh Gupta — github.com/agupta07505
   * Licensed under the GNU GPL v3 License
   * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
   */
  ```
- [ ] I added or updated unit tests where useful.
- [ ] I updated docs when behavior or features changed.
- [ ] I did not commit secrets, API keys, keystores, or generated APKs.
