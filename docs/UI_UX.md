# UI / UX Specification

## 1. Visual direction

Minimal, utilitarian, fast. Only light and dark modes. No theme store.

Brand colors/icons remain configuration resources so a future name change is cheap.

## 2. Onboarding

### Screen 1 — Welcome

Message: your own stickers/GIFs/media, available from one Android keyboard.

CTA: `Set up keyboard`

### Screen 2 — Privacy

Concise statements:

- typing is not saved,
- media index stays on device,
- Android controls what files the app can access,
- no claim of private-app access.

CTA: `Continue`

### Screen 3 — Enable keyboard

Show status indicator and button that opens Android input-method settings.

After returning, re-check enabled state automatically.

### Screen 4 — Select keyboard

Button invokes input method picker. Re-check default/selected state when possible; do not block forever if Android does not expose exact state cleanly.

### Screen 5 — Connect media

Choices:

- `Scan shared media` (only if permission strategy is appropriate),
- `Choose folders`,
- `Import files`,
- explanatory note for chat-app private folders.

### Screen 6 — Indexing

Show progress as discovered/indexed counts when available. User may continue using app while background indexing completes.

### Screen 7 — Test

A local text field plus instructions to switch to the new keyboard and test typing/media.

## 3. Main app navigation

Bottom navigation:

- Library
- Collections
- Sources
- Settings

Library filters:

- All
- Stickers
- GIFs
- Images
- Favorites
- Recent

## 4. Keyboard media surface

Top row:

- back to letters
- search box/trigger
- filter tabs: Recent / Stickers / GIFs / Favorites

Grid:

- fast cached thumbnails,
- animation only where performance allows,
- long-press opens quick actions (favorite, collection, details),
- tap sends/attempts send.

## 5. Search UX

When user types a query in media mode, the IME search field is internal to the keyboard. It must not accidentally commit query text to the host application.

Clear/back behavior must be obvious.

## 6. Failure UX

Examples:

- Source revoked: “Access to this folder was removed. Reconnect.”
- Target does not accept images: “This field doesn’t accept images from keyboards.”
- Missing file: remove from immediate results; offer rescan in app.
- Permission denied: explain what feature is unavailable; do not loop dialogs.

## 7. Accessibility

- touch targets meet Android guidance,
- TalkBack labels for non-text keys,
- visible pressed/focus states,
- do not encode state only by color,
- font scaling supported in companion app; keyboard key labels remain usable.

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
