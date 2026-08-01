# Contributing to AttendSmartly

Thank you for your interest in contributing to **AttendSmartly**! We welcome contributions from developers of all skill levels.

---

## How Can I Contribute?

### 1. Reporting Bugs
- Check existing GitHub Issues before submitting a new report.
- Include clear steps to reproduce, expected behavior, actual behavior, Android OS version, and device model.

### 2. Feature Requests
- Open an Issue with the label `enhancement`.
- Describe the feature clearly and explain why it would benefit students using AttendSmartly.

### 3. Pull Requests (PRs)
1. **Fork the Repository**: Create your feature branch from `main`.
2. **Coding Standards**:
   - Follow standard Kotlin coding style conventions.
   - Use Jetpack Compose and Material 3 design guidelines.
   - Ensure clean architecture boundaries (UI layer, ViewModel, Domain, Repository, Data sources).
3. **Commit Messages**: Write clear, descriptive commit messages.
4. **License & Notice**:
   - All source code in AttendSmartly is licensed under the **GNU General Public License v3.0**.
   - Keep the copyright notice header intact in all `.kt` files:
     ```kotlin
     /*
      * AttendSmartly (2026)
      * © Animesh Gupta — github.com/agupta07505
      * Licensed under the GNU GPL v3 License
      * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
      */
     ```
5. **Testing**: Run unit tests using `./gradlew test` before submitting your PR.

---

## Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/agupta07505/AttendSmartly.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle and run the app on an Android Emulator or device (Android 7.0 / API 24+).

---

## Code of Conduct

Please note that this project is released with a [Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.
