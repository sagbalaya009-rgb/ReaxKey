# Rich Content Compatibility Matrix

Observed on 2026-10-02. Do not infer third-party compatibility from the test receiver.

| App / field | Version | Device | Text | PNG/JPEG | GIF | WebP | Fallback | Evidence |
|---|---|---|---|---|---|---|---|---|
| ReaxKey receiver: PNG/JPEG | dev | TECNO KL5, Android 14 / API 34 | Passed (hi) | Passed, 320 / 816 bytes read | N/A | N/A | N/A | Temporary URI grant read by separate app |
| ReaxKey receiver: GIF | dev | Same device | Not separately tested | N/A | Passed, 42 bytes read | N/A | N/A | Synthetic GIF, animation fidelity not covered |
| ReaxKey receiver: WebP | dev | Same device | Not separately tested | N/A | N/A | Passed, 586 bytes read | N/A | Static WebP fixture |
| ReaxKey receiver: no rich content | dev | Same device | Not separately tested | Correctly rejected | N/A | N/A | Explanation and explicit Share button observed | Chooser destination delivery unverified |
| WhatsApp chat composer | Unverified | Unverified | Unverified | Unverified | Unverified | Unverified | Unverified | No messages sent |
| Telegram / Instagram / Snapchat / Discord / Messages | Unverified | Unverified | Unverified | Unverified | Unverified | Unverified | Unverified | No blanket claims |

## Source discovery (new flow)

TECNO KL5, Android 14: before media permission, the app found four existing app-owned QA imports.
It displayed Phone media with a permission choice and did not claim WhatsApp or Telegram was found.
Android's runtime permission dialog appeared only after Check shared images was tapped.
User selection is pending at the time of this note. App-associated shared media, guided tree
positioning, persisted access across restart and post-discovery IME delivery remain UNVERIFIED.
