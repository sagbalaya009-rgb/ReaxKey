# ReaxKey for iPhone — source project

This folder contains a native iOS companion app and custom keyboard extension. It is **not an installable iPhone file yet**. The source can be opened in Xcode after generating the project with XcodeGen. The GitHub iOS workflow builds an unsigned simulator version and uploads an editable project ZIP; a simulator build cannot be installed on a physical iPhone.

## Current behavior

- The companion app imports user-selected PNG, GIF and WebP stickers from Files into a local App Group container. It validates format, size and dimensions, deduplicates by SHA-256, and offers search, favorites, recents, labels, collections, delete and the iOS share sheet.
- The custom keyboard offers ordinary text, shift/caps, backspace, symbols, language selector, system keyboard switching, and a sticker browser. Text remains usable without Full Access.
- With the user's **Allow Full Access** choice, tapping a sticker copies its image to the clipboard. The user then pastes in the destination if that app supports image paste. No keystrokes are logged or sent to a server.
- iOS provides no Android-style `commitContent` API to third-party keyboards. Direct Telegram/Snapchat sticker insertion is **not claimed**. The app's share sheet is another user-initiated route. Actual destination behavior needs real-iPhone testing.
- No account, analytics, advertising, backend or network feature is present.

## Build on a Mac

1. Install a supported current Xcode and XcodeGen.
2. From `ios/`, run `xcodegen generate`, then open `ReaxKey.xcodeproj`.
3. Build the `ReaxKey` scheme in iPhone Simulator. GitHub Actions also runs an unsigned simulator build on each iOS change.
4. For a **physical iPhone**, join the Apple Developer Program, choose and register final bundle IDs, register App Group `group.com.reaxkey.ios`, set the same signing team and provisioning on both targets, and build/install through Xcode or TestFlight. Never commit certificates, private keys or passwords.
5. Before distribution, test Files imports (including actual WebP/GIF), App Group persistence, keyboard enablement, text/secure fields, copy/paste, share sheets, target apps, themes, accessibility, memory at large library sizes and app restart on an iPhone. Complete App Store privacy and listing information from the actual signed binary.

The current development bundle IDs are `com.reaxkey.ios` and `com.reaxkey.ios.keyboard`; these may need a final owner-controlled identifier before first App Store registration. The Android APK cannot be installed on iOS.

## Why there is no IPA yet

This Windows workstation does not have Xcode, and the owner does not yet have a Mac, Apple Developer account or iPhone. A real-device `.ipa` requires Apple signing/provisioning and must be validated on an iPhone before it can be offered as a test download. The source project and unsigned simulator build are honest intermediate artifacts, not substitutes for a signed IPA.
