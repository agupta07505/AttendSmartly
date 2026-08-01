# Release Signing & Keystore Setup — AttendSmartly

This guide explains how to generate `AttendSmartly.jks`, export `AttendSmartly_base64.txt`, and configure GitHub Actions secrets for signed release builds.

---

## 🔒 1. Security First

Never commit keystore files or passwords to GitHub. The following patterns are automatically ignored in `.gitignore`:
- `*.jks`
- `*.keystore`
- `*_base64.txt`

---

## 🔑 2. How to Generate `AttendSmartly.jks`

### Method A: Via Android Studio (Recommended)
1. Open **Android Studio**.
2. Go to **Build** -> **Generate Signed Bundle / APK...**
3. Select **APK** and click **Next**.
4. Under **Key store path**, click **Create new...**
5. Set:
   - **Key store path**: Browse to your project root `a:\AttendSmartly\AttendSmartly.jks`
   - **Password**: Choose a secure password (e.g. `MySecurePassword123`)
   - **Alias**: `upload` (or `attendsmartly`)
   - **Key Password**: Same as keystore password (or choose one)
   - **Validity**: `25` years
   - **Certificate**: Fill in your First and Last Name (`Animesh Gupta`).
6. Click **OK** and finish the wizard.

### Method B: Via Terminal (`keytool`)
Run this command from your terminal:

```bash
keytool -genkeypair -v -keystore AttendSmartly.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```
Follow the prompts to set your password and certificate details.

---

## 📄 3. How to Generate `AttendSmartly_base64.txt`

Run the included helper PowerShell script from the project root:

```powershell
.\scripts\export-keystore-base64.ps1 -KeystorePath .\AttendSmartly.jks
```

This generates `AttendSmartly_base64.txt` containing a single line of Base64 text encoding your keystore.

---

## ⚙️ 4. Add GitHub Secrets

1. Open your repository on GitHub.
2. Go to **Settings** -> **Secrets and variables** -> **Actions**.
3. Click **New repository secret** and add:

| Secret Name | Value |
| ----------- | ----- |
| `ANDROID_KEYSTORE_BASE64` | Copy the entire single line from `AttendSmartly_base64.txt` |
| `ANDROID_KEYSTORE_PASSWORD` | Password created during keystore generation |
| `KEY_ALIAS` | `upload` (or alias chosen) |
| `KEY_PASSWORD` | Key password |

---

## 🚀 5. Triggering a Signed Release Build

- **Automated Tag Release**: Push a Git tag starting with `v` (e.g., `git tag v1.0.0 && git push origin v1.0.0`).
- **Manual Build**: Go to **Actions** -> **Build Android APK** -> **Run workflow**.
