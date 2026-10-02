# Project State

## Current checkpoint — sticker-only redesign

Branch: `build/full-reaxkey`. Draft PR: https://github.com/sagbalaya009-rgb/ReaxKey/pull/1

The app now opens on Stickers and has exactly two primary tabs: Stickers and Settings. The
keyboard has a conventional staggered QWERTY surface, symbol pages and an in-IME Media panel.
Shared-media discovery is restricted to recognized WhatsApp sticker directories. Generic photos,
DCIM, screenshots and Downloads are excluded from the sticker index and keyboard grid. A Room
migration hides legacy photo rows without deleting favorites or labels. User-selected sticker
files and SAF folders remain supported. Folder selection persists Android's read grant.

## Executed verification

- `test lint assembleDebug :app:assembleDebugAndroidTest` passed locally after the redesign.
- After the picker hint, `lint assembleDebug :app:assembleDebugAndroidTest :app:bundleRelease`
  passed in an isolated workspace cache. The full unit suite was not rerun against that last
  one-method change: the sandbox denied Robolectric's local cache and the installed Gradle home.
- The redesigned debug APK installed and launched on TECNO KL5 / Android 14. The app rendered
  Stickers and Settings and an empty sticker state. The phone's exact WhatsApp Stickers directory
  exists and contains 10,834 WebP files, observed through ADB. That shell observation does not
  prove the app can read the directory.
- Android reports `READ_MEDIA_IMAGES` allowed for ReaxKey, but the redesigned app did not display
  a WhatsApp source candidate. Android may omit that directory from MediaStore. The SAF picker
  fallback has an initial-location hint for the observed directory; the owner must still select
  the folder in Android's picker. This latest hint has not yet been exercised on device.
- Existing prior device evidence for text and rich content is recorded in
  `docs/COMPATIBILITY_MATRIX.md`; it does not verify the latest keyboard redesign or any
  third-party messaging app.

## Remaining gates

Select the WhatsApp Stickers folder in Android's picker, verify grant persistence, index count,
sticker grid and Media panel on the phone, and verify normal photos remain absent. Retest typing,
symbols, secure fields, light/dark themes and actual rich-content compatibility in target apps.
The latest APK needs another device install. Commit and CI verification are pending. Do not
describe the app as ready for general sharing or supported in
WhatsApp/Snapchat until the corresponding real-world tests pass.

Parent project `sources/` files remain read-only. Debug APK path:
`app/build/outputs/apk/debug/app-debug.apk`.
