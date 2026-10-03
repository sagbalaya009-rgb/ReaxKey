# Permissions, Privacy, and Security

## 1. Privacy posture

A keyboard can theoretically see highly sensitive text. This product deliberately minimizes its authority and data handling.

Default privacy promise:

- typed text is not stored,
- typed text is not uploaded,
- no keystroke analytics,
- no account required,
- no cloud backend required,
- media index stays local,
- no ad SDK,
- no tracker SDK,
- no private-app-storage bypasses.

## 2. Internet permission

Do not declare `android.permission.INTERNET` for the core build unless a later approved feature requires it. This creates a technically enforceable local-first baseline.

If a future network feature is added, isolate it behind an explicit build/configuration decision and update privacy docs.

## 3. Media access

Use the least privilege path that satisfies the user's chosen source:

- system picker for specific media,
- SAF for user-connected folders,
- MediaStore with current Android media permissions only when broad shared-media indexing is clearly requested and policy-compatible.

Do not request `MANAGE_EXTERNAL_STORAGE` as a shortcut. It still does not provide access to all other apps' private/app-specific directories, and it creates significant policy/privacy burden.

## 4. Persisted URI grants

When the user connects a SAF tree, persist read permission where supported. Store the source URI and permission state. Handle revoked grants safely.

## 5. IME enablement

The app cannot silently enable or select itself as the default IME. The onboarding flow opens Android settings/picker and waits for the user to make the choice.

## 6. Sensitive fields

Detect password/sensitive input types and enter privacy mode. Never collect or log surrounding text. Diagnostics must redact user content.

## 7. Logs

Allowed logs:

- lifecycle state names,
- scan counts/durations,
- source health without raw personal paths where avoidable,
- exception types/stack traces,
- build/device/API metadata for diagnostics.

Forbidden logs:

- keystrokes,
- typed text,
- message contents,
- passwords,
- sticker/image raw bytes,
- unredacted external URIs if they expose personal paths and are not needed.

## 8. Diagnostics export

Provide a user-triggered “Export diagnostics” file containing sanitized app/version/device info, recent technical errors, source-health status, and database counts — never typed text or media content.

## Automatic reaction discovery contract — 2026-10-02 (supersedes manual-source-first UX)

The primary add/onboarding action is Find my reactions, never an immediate folder picker.
SourceDiscoveryEngine uses pluggable recognizers over supported metadata: authorized MediaStore
rows, persisted document-tree grants, and owned imports. Named-app attribution requires path-segment
or provider evidence, not installed-app checks or filenames. Results distinguish AVAILABLE,
AVAILABLE_REQUIRES_USER_GRANT, PARTIALLY_AVAILABLE, NOT_FOUND, PRIVATE_OR_INACCESSIBLE,
PERMISSION_REQUIRED and ERROR, with explicit confidence and observed counts.

Discovery describes candidates; shared MediaAccess ingestion scans them through WorkManager;
Room flows update app/IME search without restart. Shared-media buckets are mutually exclusive.
No raw filesystem crawling, production shell/ADB, protected app directories, contacts, chat or
notification access. Only public Android APIs. Existing access is checked before permission requests.
Partial grants remain partial. Manual folder selection is an advanced fallback; any initial picker
URI comes from actual provider evidence or a previous grant, never a guessed guaranteed app path.
The user must approve Android's directory grant. Picker positioning is a hint, never a promise.

Sources UI separates Found on your phone and Added by you. Add reactions orders automatic discovery,
individual import, personal-folder connection, then share help. Empty/lost states use human wording.
Shared imports preview once and accept bulk media together. App visuals use #0B0D10 dark background,
blue/violet accents and 20dp cards. Automated recognizer tests and physical discovery/grant lifecycle
checks are mandatory; record observed results without claiming universal app compatibility.

See docs/SOURCE_DISCOVERY.md for implementation and verification details.
