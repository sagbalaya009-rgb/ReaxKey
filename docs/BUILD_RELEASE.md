# Build, APK, Phone Testing, and Release

## 1. Local prerequisites

- current stable Android Studio,
- bundled/current supported JDK,
- Android SDK/platform matching project target,
- Git,
- USB cable or wireless debugging if supported.

The coding agent should generate and commit the Gradle wrapper.

## 2. Debug APK

From repository root:

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

Expected APK after the project exists:

`app/build/outputs/apk/debug/app-debug.apk`

## 3. Testing directly from Android Studio

1. On phone, enable Developer options.
2. Enable USB debugging.
3. Connect phone and approve the debugging prompt.
4. Select device in Android Studio.
5. Run the `app` configuration.
6. Android Studio installs and launches the app.
7. Onboarding opens keyboard settings; user enables the IME.
8. User selects the IME from the system picker.
9. Test inside companion test field and real apps.

## 4. Manual APK installation

A debug APK can also be copied/downloaded to the phone. Android may require allowing installation from that source. Debug signing is for development only.

## 5. GitHub Actions design

Once Gradle scaffold exists, add CI that on push/PR:

- checks out repository,
- sets up supported JDK,
- caches Gradle safely,
- runs unit tests,
- runs Android lint/static checks,
- builds `assembleDebug`,
- uploads debug APK as an Actions artifact.

Do not create a permanently signed public release from a committed keystore.

## 6. Release signing

For a release APK/AAB:

- create a dedicated upload/release keystore,
- keep it outside Git,
- back it up securely,
- use Android Studio signing or CI secrets,
- never paste private key material into AI chat or repository docs.

The current default is `versionName=1.0.0`, `versionCode=1`. Each bundle uploaded to Play needs a strictly higher, never reused version code. Set `VERSION_CODE` and `VERSION_NAME` as Gradle properties for later releases; record the uploaded code in the release log. The unsigned `bundleRelease` output is a build verification artifact, not a Play-uploadable bundle. Enrol in Play App Signing and create a dedicated upload key outside this repository with a strong password chosen by the owner. Back up the key and passwords securely. The build reads `REAXKEY_KEYSTORE`, `REAXKEY_STORE_PASSWORD`, `REAXKEY_KEY_ALIAS` and `REAXKEY_KEY_PASSWORD` from the environment. Verify the signed AAB certificate fingerprint before upload and preserve that fingerprint for updates. Never place signing values in commands, CI logs, docs or chat. CI intentionally publishes only an explicitly named unsigned release bundle.

## 7. Versioning

Use monotonically increasing Android `versionCode` and readable `versionName` (e.g. semantic versioning once releases begin).

## 8. Distribution

Development/personal testing: debug or owner-signed APK sideload.

Commercial distribution target: **Google Play paid app, one-time purchase**. The Android client must not contain subscription or ad SDKs. The price is configured in Play Console, not hard-coded in the app.

Before production release:

- verify `com.reaxkey.keyboard` is the final unique application ID,
- use Android App Bundle (`.aab`) for Play release unless current Play requirements state otherwise,
- configure Play App Signing/upload key correctly,
- complete current Google Play policy declarations for keyboard/IME behavior, media permissions, privacy policy and Data Safety,
- verify the current target API requirement at release time,
- run closed/internal testing before production,
- verify purchase/install/update behavior from an actual Play test account.

Do not publish a free production listing as a placeholder if the intent is for ReaxKey to launch as a paid app. Commercial configuration decisions belong in Play Console and `docs/COMMERCIAL_DISTRIBUTION.md`.
