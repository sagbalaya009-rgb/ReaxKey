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
