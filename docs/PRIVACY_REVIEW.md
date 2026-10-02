# Privacy review scope

## Implementation controls

- No typed-text persistence, analytics, account, advertising or network client.
- InputConnection is used only for typing, editor actions and explicit deletion.
- Password/no-personalized-learning fields skip capitalization queries and media-use recording.
- Internal media search has a separate query buffer and does not call the host input connection.
- Media is read through content providers, MediaStore permission or persisted SAF grants.
- App-owned deletion validates the provider authority and canonical imports directory.
- FileProvider exposes only imports and outgoing cache, is non-exported and grants individual URIs.
- Android 12+ cloud and device-transfer rules exclude private application data.
- Diagnostics contain version/API/manufacturer, counts and aggregate source health only.

## Validation to record

Inspect the final merged APK manifest, dependency graph and source before release.
Run import/deletion and receiver tests. Verify secure fields and grant lifetimes on
the physical device. Do not treat this implementation review as completed device QA.
