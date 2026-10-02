# Product Specification

## 1. Problem

People accumulate reaction media in different places: saved images, downloaded GIFs, exported stickers, chat-app media folders, screenshots, and imported sticker packs. They often cannot use the same item conveniently in every app because the media is fragmented and individual apps expose different sticker libraries.

The product solves retrieval and reuse: one Android keyboard provides access to the user's authorized media library while the user is typing elsewhere.

## 2. Product principles

- **User-owned media first.** The value is access to the user's existing collection.
- **Local-first.** Search/index/storage works offline.
- **Transparent permissions.** The app tells the user what each access grant enables.
- **No fake universality.** If Android or the target app blocks an operation, communicate it accurately.
- **Fast keyboard surface.** Opening media must feel immediate because the user is mid-conversation.
- **Simple visuals.** Light/dark mode, clean grid, no decorative theme system.
- **Typing remains reliable.** Media features must never make normal typing brittle.
- **Paid once, owned utility.** The commercial model is a one-time Google Play purchase, not a subscription funnel.

## 3. Primary user journeys

### First launch

1. Welcome and one-sentence explanation.
2. Privacy promise and technical limitation summary.
3. Enable keyboard — open Android input-method settings.
4. Select keyboard — invoke Android's input-method picker.
5. Connect media — allow supported media access and/or choose folders with SAF.
6. Initial indexing progress.
7. Test field inside the companion app.
8. “Ready” state with instructions for switching keyboards later.

### Everyday typing

1. User taps a text field in another app.
2. IME opens in text mode.
3. User types normally or selects Emoji / Stickers / GIFs / Media.
4. Media panel queries local index.
5. User searches/browses recents/favorites/sources/collections.
6. Target editor capability is checked.
7. On selection, rich content is committed if supported.
8. If unsupported, UI gives a clear fallback action such as explicit Share.

### Library management

Users can:

- browse all indexed media,
- filter by type/source/folder,
- connect or disconnect a folder/source,
- import single or multiple files,
- receive shared images/GIFs from other apps,
- favorite/unfavorite,
- create collections,
- rename local aliases/tags,
- remove an item from the local index without deleting the original file,
- optionally delete an app-imported copy with confirmation,
- re-index a source,
- view missing/revoked sources.

## 4. Content types

Initial full product support should include, where Android decoding and target-app acceptance allow:

- static WebP,
- animated WebP where supported,
- GIF,
- PNG,
- JPEG.

Video is not a core keyboard-send format. Video files may be discoverable for future expansion but should not clutter sticker/GIF surfaces unless deliberately enabled.

## 5. Sticker semantics

Android does not provide a universal system-level “sticker” file type. Internally, use product classification:

- `STICKER` — image media treated by the app as a sticker based on source/import/user classification and dimensions/format heuristics.
- `GIF` — animated GIF.
- `IMAGE` — other still media.
- `ANIMATED_IMAGE` — e.g., animated WebP.

Do not infer that a WebP file necessarily came from WhatsApp.

## 6. No vision model yet

Search before vision uses:

- filename/display name,
- folder/source name,
- manual tags,
- user collections,
- MIME/type,
- import metadata,
- recency/frequency/favorites.

Future vision can add semantic tags and embeddings through a provider interface without changing media tables or UI contracts.

## 7. Commercial product behavior

- ReaxKey is planned as a **paid Google Play app purchased once**.
- No subscription.
- No ads.
- No required ReaxKey account.
- No artificial "Pro" unlock inside the already-paid app.
- The base product must not require Google Play Billing Library because purchase occurs at app-acquisition level.
- Do not add a network-backed entitlement server merely to prove ownership unless the owner explicitly changes the design.
- Development/debug APKs are for owner testing; they are not the commercial distribution channel.
- Pricing is deliberately not hard-coded in the Android client and will be chosen in Play Console.

## 8. Non-goals

- Cloud sticker marketplace.
- Social network.
- Account system.
- Ads.
- Keylogging or typed-text analytics.
- Circumventing private app storage.
- Pretending every chat app can accept keyboard images.
- Full Gboard-equivalent predictive AI.

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
