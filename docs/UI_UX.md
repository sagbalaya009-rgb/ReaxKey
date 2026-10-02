# ReaxKey UI and interaction

The companion app has exactly two primary tabs. **Stickers** is the start tab and shows an
Import stickers action, Refresh, recognized WhatsApp sticker candidates, and the local sticker
grid. When the grid is empty it offers a folder picker for WhatsApp Stickers. **Settings** holds
keyboard enable/select actions, theme and haptic controls, sticker sources, privacy and
diagnostics. Setup appears only as useful actions in Settings; no onboarding slideshow remains.

The IME prioritizes a normal staggered QWERTY layout. Media sits in the toolbar and changes
the keyboard area into an in-IME sticker panel. ABC returns to typing. The ?123, more-symbols
and ABC transitions are predictable. Character keys share a tight surface; Shift, Backspace and
the contextual action key are visually distinct. The gap-touch correction targets only nearby
keys and leaves touches inside visible key bounds unchanged. Secure fields hide Media.

The Media panel uses cached, aspect-preserving thumbnails and local index queries. Its empty
state points to the companion app. It never displays generic photo rows. If an editor rejects
rich content, the panel explains the limitation and offers Android's Share action.

Light and dark modes use neutral surfaces and standard controls. Source attribution and access
are described in `SOURCE_DISCOVERY.md`; current physical verification is in `PROJECT_STATE.md`.
