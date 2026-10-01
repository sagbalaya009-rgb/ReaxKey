# Media Discovery, Import, and Indexing

## 1. Core truth

The app cannot silently crawl every installed app's private storage. The media engine only indexes content it can access through Android-supported paths.

## 2. Supported source types

### A. Shared media / MediaStore

When the user grants appropriate media access, query shared media collections using content URIs. Do not convert everything to raw filesystem paths.

### B. User-connected folders (SAF)

Use `ACTION_OPEN_DOCUMENT_TREE` for directories the system allows the user to select. Persist granted URI permissions when possible. Store the tree URI as a source record.

The scanner recursively enumerates supported media from connected trees with cancellation and bounded concurrency.

### C. Explicit file selection

Use the system document/photo picker for selecting individual media where appropriate.

### D. Share/import receiver

Register an explicit import/share Activity for `ACTION_SEND` and `ACTION_SEND_MULTIPLE` for supported image MIME types. This lets users share/export media into the product from apps that expose a share action.

### E. App-owned imports

If persistent access to a source URI is uncertain, the user may choose “Import a copy.” Store that copy inside app-specific persistent storage. Such imported media remains available to the keyboard but is removed when the app is uninstalled unless the user exports it.

## 3. Source abstraction

```text
MediaSource
- id
- type: MEDIASTORE | SAF_TREE | IMPORTED | SHARE_IMPORT
- displayName
- rootUri (nullable)
- persistedPermissionFlags
- enabled
- lastScanAt
- lastSuccessfulScanAt
- health: OK | REVOKED | MISSING | ERROR
```

## 4. Indexing pipeline

```text
source enumeration
   -> MIME/extension validation
   -> lightweight metadata read
   -> stable identity resolution
   -> classification
   -> thumbnail key creation
   -> upsert Room row
   -> mark scan generation
   -> remove/mark stale rows from that source
```

## 5. Stable identity

For external URI-backed items, prefer a combination of source + normalized URI/document ID. Record size and last-modified when available for change detection.

Do not hash every file during every scan. SHA-256 may be computed for imported copies or lazily for deduplication when necessary.

## 6. Classification without vision

Use MIME type, extension, animation metadata, dimensions, source, and user-assigned classification.

Examples:

- `image/gif` -> GIF
- animated WebP -> ANIMATED_IMAGE
- WebP/PNG with transparent/small reaction-like dimensions can be suggested as sticker-like but never treated as certain source attribution
- user can manually change classification if needed

## 7. Incremental refresh

- MediaStore can be re-queried using modification metadata where available.
- SAF trees are rescanned through WorkManager at reasonable intervals only when useful; avoid battery abuse.
- App start may trigger a lightweight freshness check, not a full blocking scan.
- Provide manual “Rescan now.”

## 8. Deletion semantics

“Remove from library” means remove index membership. It must **not** delete an external original by default.

Deleting an app-owned imported copy requires explicit confirmation and then removes both file and index record.

## 9. Revoked access

If a source permission is revoked:

- preserve metadata as unavailable only if useful,
- mark source health `REVOKED`,
- hide unavailable media from default keyboard results,
- give the user a reconnect action,
- never crash on stale URIs.
