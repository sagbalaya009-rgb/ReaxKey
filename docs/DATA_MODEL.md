# Data Model

Use Room. Exact table/column names may vary, but semantics should remain stable.

## `media_source`

- `id: Long/String`
- `type: enum`
- `display_name: String`
- `root_uri: String?`
- `permission_flags: Int`
- `enabled: Boolean`
- `health: enum`
- `last_scan_at: Instant?`
- `last_successful_scan_at: Instant?`
- `created_at: Instant`

## `media_item`

- `id`
- `source_id`
- `content_uri`
- `display_name`
- `mime_type`
- `kind: STICKER | GIF | IMAGE | ANIMATED_IMAGE`
- `width: Int?`
- `height: Int?`
- `size_bytes: Long?`
- `last_modified: Long?`
- `duration_ms: Long?`
- `is_animated: Boolean`
- `is_available: Boolean`
- `user_label: String?`
- `content_hash: String?` (lazy/optional)
- `created_at`
- `indexed_at`
- `updated_at`

Unique identity should prevent repeated scans from duplicating the same item.

## `tag`

- `id`
- `name_normalized`
- `display_name`
- `origin: USER | FILENAME | FUTURE_VISION`

## `media_tag`

- `media_id`
- `tag_id`
- optional confidence for future semantic tags

## `collection`

- `id`
- `name`
- `sort_order`
- `created_at`

## `collection_item`

- `collection_id`
- `media_id`
- `added_at`
- unique(collection_id, media_id)

## `media_usage`

Prefer aggregated usage columns unless event history is needed:

- `media_id`
- `use_count`
- `last_used_at`

Do not store the message, recipient, or typed context in which media was used.

## `favorite`

May be a boolean on `media_item` or normalized table. Simplicity favors `is_favorite` + `favorited_at` on the item unless future multi-profile support appears.

## `app_setting`

Use DataStore rather than Room for small preferences:

- theme: SYSTEM/LIGHT/DARK
- active language packs
- haptics preference
- media-grid density
- default media tab
- privacy/diagnostic settings

## Migrations

Every schema version bump requires:

- migration code,
- migration test,
- downgrade not assumed,
- documented reason in `PROJECT_STATE.md` or ADR.
