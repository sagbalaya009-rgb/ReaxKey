# Rich Content Insertion

## 1. Android mechanism

For images/GIFs, the keyboard uses Android's rich-content input protocol rather than pretending an image is text.

The target editor advertises acceptable MIME types in `EditorInfo`. The IME reads them, and only attempts direct insertion when the selected media MIME type is compatible.

Use AndroidX `EditorInfoCompat` / `InputConnectionCompat` where appropriate for compatibility.

## 2. Send algorithm

```text
selected MediaItem
      │
      ▼
resolve readable content:// URI
      │
      ▼
read target MIME types from current EditorInfo
      │
      ├─ no compatible type -> unsupported flow
      │
      └─ compatible
           │
           ▼
create InputContentInfoCompat
           │
           ▼
commitContent with temporary read permission grant
           │
           ▼
check boolean acceptance / handle failure
```

## 3. URI strategy

- MediaStore items may already have suitable content URIs.
- SAF documents use content URIs.
- App-owned imported files use a non-exported provider/FileProvider-style content URI with grantable temporary permissions.
- Never use `file://` URIs.

## 4. Temporary permissions

The receiving app needs permission to read the selected content URI. Use the platform-supported temporary grant mechanism associated with rich-content commit.

The provider should not be broadly exported. Grant only the selected URI for the required operation.

## 5. Target capability model

Create a runtime model:

```text
TargetCapabilities
- packageName (only if safely available)
- acceptedMimeTypes
- supportsGif
- supportsWebp
- supportsPngJpeg
- directCommitAvailable
```

Do not maintain a giant hard-coded per-app promise table as the primary source of truth. Runtime MIME capability wins.

## 6. Unsupported fallback

When direct commit is unavailable:

- disable or badge unsupported items if known before tap,
- on tap, explain: “This field does not accept images from keyboards.”
- provide explicit `Share` action when feasible, launching Android Sharesheet with the selected media.

Never silently use Accessibility Service, clipboard scraping, automated UI tapping, or app-specific reverse-engineered private APIs.

## 7. Compatibility matrix

Manual validation should record actual behavior for major apps/fields, e.g.:

- WhatsApp chat composer
- Telegram chat composer
- Instagram DM
- Snapchat chat
- Discord message composer
- Google Messages
- Chrome text field
- Notes app

Each result is `PASS_DIRECT`, `PASS_FALLBACK`, `UNSUPPORTED`, or `NOT_TESTED`, with Android/app version and date.
