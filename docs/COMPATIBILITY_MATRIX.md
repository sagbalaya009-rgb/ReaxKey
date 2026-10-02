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

## Earlier broad-image discovery (superseded)

TECNO KL5, Android 14: before media permission, the app found four existing app-owned QA imports.
It displayed Phone media with a permission choice and did not claim WhatsApp or Telegram was found.
Android's runtime permission dialog appeared only after Check shared images was tapped. The owner
subsequently granted image access in Android; the app reported full access and the phone exposed
1,415 WhatsApp-attributed image records plus two in Downloads. These are MIME-filtered accessible
image counts, not verified personal reaction counts. The discovery screen explicitly warns that
some may be ordinary photos. The four app-owned imports were checked by default; WhatsApp and
Downloads were unchecked until explicitly selected. No phone media was bulk-added in this test.
That broad-image flow has been removed. The sticker-only build excludes Downloads, photos and
general image folders; its app screen rendered an empty sticker state on the same phone despite
granted image access. ADB confirmed 10,834 WebP files in the actual WhatsApp Stickers directory,
but SAF folder approval, indexing, IME display and cross-app sticker delivery remain UNVERIFIED.
The latest folder-picker hint is not yet installed on the phone.
