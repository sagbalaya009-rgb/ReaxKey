# Release procedure

ReaxKey is a paid-once Play application. Do not publish without the owner's explicit instruction.

## Reproducible verification

Install JDK 17, Android SDK platform 37.0 and build-tools 36.0.0. Set ANDROID_HOME
or create an untracked local.properties with sdk.dir. Run from a clean checkout:

```text
./gradlew test lint assembleDebug :app:bundleRelease
```

On Windows use gradlew.bat. Debug product APK is under app/build/outputs/apk/debug/.
The separate receiver is under testbed/build/outputs/apk/debug/. Never distribute
the testbed with the product. Automated reports are under each module's build/reports/.

## External signing

Use a dedicated upload key stored outside this repository and backed up securely.
Configure REAXKEY_KEYSTORE, REAXKEY_KEY_ALIAS, REAXKEY_STORE_PASSWORD and
REAXKEY_KEY_PASSWORD in the local build environment or protected CI secrets.
Never put credentials in command-line arguments, source files or chat.
Without these variables, the bundle is unsigned and is not upload-ready.

Version overrides use Gradle properties VERSION_CODE and VERSION_NAME. Increment
VERSION_CODE for each Play upload. Confirm com.reaxkey.keyboard is the final identity.

## Required owner/platform gates

- Physical IME, source grant/revocation, reboot/process recreation and update testing.
- Measured compatibility matrix for actual target app versions.
- Upload key, Play App Signing, developer account verification and test tracks.
- Privacy policy, Data Safety, current target API and media-permission declarations.
- Paid listing price and regions; no free placeholder listing.
- Internal/closed Play acquisition and update tests before production.

Broad shared-media access is optional and explicitly requested from Sources. Review
Google Play's current photo/video-permission policy before submission; if the app's
eligibility is not accepted, retain SAF/document/share import as the supported path.
No INTERNET, all-files, accessibility or billing permission is part of the product.

This checklist is not evidence that any unrecorded release gate has passed.
