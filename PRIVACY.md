# Privacy Policy — AttendSmartly

**Last Updated**: 2026

At **AttendSmartly**, accessible from our open-source repository and application builds, we respect your privacy. This Privacy Policy outlines what information is collected and how it is managed.

---

## 1. Offline-First & Local Storage

- **Local Database**: All your attendance logs, subjects, schedules, unit rules, and target thresholds are stored **100% locally on your device** inside an encrypted Room Database (`attendsmartly_database.db`).
- **No Remote Telemetry**: AttendSmartly does **NOT** collect, track, or transmit any user usage data, analytics, or personal identifiers to external servers or third parties.

---

## 2. Smart Timetable OCR & API Keys

- **Optional Feature**: If you use the Smart Timetable OCR Scanner to import your schedule from an image, the image is processed via the Google Gemini API.
- **Personal API Key**: You can provide your own personal Google Gemini API key. This key is stored securely in local Android DataStore Preferences and is only sent directly to Google's official API endpoints (`generativelanguage.googleapis.com`) to fulfill your OCR request.
- **No Third-Party Proxies**: No intermediate servers receive your API key or timetable images.

---

## 3. Data Export & Backups

- **User Control**: You have full ownership of your data. You can export JSON backups or CSV reports at any time via Android's native Storage Access Framework (SAF).
- **No Cloud Synchronization**: Backups are stored strictly in the location specified by you on your local storage or connected drive.

---

## 4. Permissions

AttendSmartly requests minimal Android permissions:
- `POST_NOTIFICATIONS`: To send timely class reminders.
- `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: To schedule accurate alarms before upcoming classes.
- `RECEIVE_BOOT_COMPLETED`: To reschedule class reminder alarms when your phone restarts.

---

## 5. Contact & Questions

If you have any questions regarding this Privacy Policy, you can open an issue on GitHub:
- **Developer**: Animesh Gupta ([@agupta07505](https://github.com/agupta07505))
