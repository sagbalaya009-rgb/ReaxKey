# Play Data Safety draft — owner review required

Snapshot: 2026-10-02, current source and merged release manifest. This is a draft for the exact final bundle, not a submitted declaration. Recheck after signing and before Play submission.

## Recommended answers for current binary

| Play question | Draft answer | Basis / qualification |
|---|---|---|
| Does the app collect user data? | No, for the current binary | No Internet permission or ReaxKey backend; no analytics, ads or crash SDK. Typed text is passed to the active editor, not transmitted to ReaxKey. On-device sticker metadata and preferences remain on device. Verify the final dependency graph and manifest. |
| Does the app share user data? | No automatic developer sharing | The user may explicitly send a sticker to a chosen app with a temporary URI grant or export diagnostics to a chosen location. Follow Play's exact current definition for user-initiated transfers when completing the form. |
| Data encrypted in transit? | Not applicable to ReaxKey collection | The app has no declared Internet access or ReaxKey network transmission. Destination apps have independent behavior. |
| Data deletion request? | Local deletion through item removal, source disconnect, clearing storage or uninstall | No ReaxKey account/server record exists. Note that source disconnect hides indexed rows but does not erase original media or a destination app's copy. |
| Independent security review / family or child declarations | Do not claim certification; owner selects actual audience | No external certification has been performed. |

App privacy claims are limited to the shipped app. Google Play services, Android, and destination messaging apps are separate parties. The owner must confirm any SDK or manifest change, Play's current form wording, and the published privacy-policy URL before submission.

## Manifest and permission audit

| Permission | Reason and request point | Decline effect / narrower path |
|---|---|---|
| `READ_EXTERNAL_STORAGE` (max SDK 32) | Optional shared-image discovery, requested only from the source flow on older devices. | Normal typing and document-picker/import paths continue; SAF is narrower. |
| `READ_MEDIA_IMAGES` (API 33+) | Optional discovery of shared sticker-like media; requested by the source flow, not onboarding. | Normal typing and SAF/import/share continue. Google Play photo/video permission eligibility remains an open policy gate. |
| `READ_MEDIA_VISUAL_USER_SELECTED` (API 34+) | Lets Android grant only user-selected images when image access is requested. | Same as above; selected access provides narrower scope. |
| `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE` | Merged from AndroidX WorkManager for scheduled/local scan work; not directly requested as a runtime prompt. Network-state permission does not grant Internet access. | No user-facing permission dialog; confirm necessity when WorkManager is upgraded. |
| App-defined `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | AndroidX internal signature permission for non-exported dynamic receivers. | No user-facing grant. |

No `INTERNET`, contacts, location, microphone, camera, notification, accessibility or all-files permission appears in the audited release manifest. The exported launcher, image-share import activity and IME are intentional; the FileProvider is non-exported and grants URI access only as needed. Backup and device transfer are disabled. The final merged manifest must be inspected again after the final build.

The current dependencies include AndroidX, Room, WorkManager, Compose, Coil and test-only JUnit/Robolectric/AndroidX Test. There is no observed advertising/analytics SDK or network client in production source. The app does not have Internet permission even if a transitive library contains optional network code.
