# Contributing to AttendSmartly

Thank you for considering contributing to **AttendSmartly - Attendance Tracker**! We welcome contributions from developers, designers, and students of all experience levels.

---

## 📋 Table of Contents

- [Code of Conduct](#-code-of-conduct)
- [How to Contribute](#-how-to-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Features](#suggesting-features)
  - [Submitting Pull Requests](#submitting-pull-requests)
- [Development Setup](#-development-setup)
- [Architecture Guidelines](#-architecture-guidelines)
- [Copyright & Licensing Notice](#-copyright--licensing-notice)

---

## 📜 Code of Conduct

This project and everyone participating in it is governed by the [AttendSmartly Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

---

## 🤝 How to Contribute

### Reporting Bugs

Before creating a bug report, please check existing [GitHub Issues](https://github.com/agupta07505/AttendSmartly/issues) to see if the issue has already been reported.

When creating a bug report using our [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md), please include:
- A clear, descriptive title.
- Exact steps to reproduce the behavior.
- Expected behavior vs actual behavior.
- Android device model and Android OS version.
- Relevant screenshots or logcat snippets.

### Suggesting Features

We welcome feature ideas! Submit a request using our [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md). Provide:
- A clear summary of the feature.
- The student use-case or problem it solves.
- Any mockups or design concepts.

### Submitting Pull Requests

1. **Fork & Branch**: Fork the repository and create a new branch from `dev` (e.g. `feature/widget-support` or `fix/reminder-alarm`).
2. **Keep Changes Focused**: Make small, logical commits focused on a single feature or bug fix.
3. **Run Unit Tests**: Verify your changes locally before opening a PR:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. **Follow Kotlin Conventions**: Adhere to official Kotlin coding style guidelines and Jetpack Compose best practices.
5. **PR Description**: Fill out our [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md) completely.

---

## 🛠 Development Setup

1. Clone your fork:
   ```bash
   git clone https://github.com/agupta07505/AttendSmartly.git
   cd AttendSmartly
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Ensure JDK 17 and Android SDK 34+ are configured in Android Studio.
4. Sync Gradle dependencies.
5. Run the app on an Android Emulator or connected physical device (API 24+).

---

## 🏗 Architecture Guidelines

AttendSmartly strictly follows **Clean Architecture** with **MVVM**:

```text
[ UI Layer: Jetpack Compose Screens / Components ]
                    │
                    ▼
[ ViewModel Layer: StateFlow / UI State Management ]
                    │
                    ▼
[ Domain Layer: Calculators / Models / Use Cases ]
                    │
                    ▼
[ Data Layer: AttendSmartlyRepository ]
       │                         │
       ▼                         ▼
[ Room DAO / DB ]     [ DataStore Preferences ]
```

- **UI Components**: Must be stateless Compose functions where possible.
- **ViewModels**: Expose immutable `StateFlow` streams to the UI.
- **Domain Calculators**: `AttendanceCalculator` logic must remain pure and fully testable without Android framework dependencies.

---

## ⚖️ Copyright & Licensing Notice

All source code in AttendSmartly is licensed under the **GNU General Public License v3.0**.

Every `.kt` file in the project must preserve the following copyright header notice:

```kotlin
/*
 * AttendSmartly (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
```

Thank you for helping make **AttendSmartly** better for college students everywhere!
