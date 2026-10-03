# Rich content compatibility matrix

Observed evidence as of 2026-10-03. A compatible editor must advertise image MIME types. A successful testbed transfer does not prove that a third-party app accepts the same content. Do not advertise universal compatibility.

| App / field | App version | Device / Android | Date | Format | Outcome | Evidence level |
|---|---|---|---|---|---|---|
| ReaxKey receiver | Development build | TECNO KL5 / Android 14 (API 34) | 2026-10-02 | PNG, JPEG, GIF, WebP | Separate receiver read granted URIs; WebP from the real sticker library read 18,014 bytes | Agent-observed on device; animation fidelity not assessed |
| ReaxKey receiver with no rich-content MIME | Development build | Same | 2026-10-02 | Image | Correctly rejected; explanation and Share fallback appeared | Agent-observed; chooser destination delivery not assessed |
| ReaxKey receiver after typing upgrade | Development build | TECNO KL5 / Android 14 (API 34) | 2026-10-03 | WebP | Local receiver read a real library sticker through the updated IME (`image/webp`, 4,822 bytes) | Agent-observed on device; third-party delivery not implied |
| Telegram | Unknown | TECNO KL5 / Android 14 (API 34), owner-reported on the connected test phone | 2026-10-03 | Owner did not specify | Working rich content on installed ReaxKey 1.0.0 debug build | Owner-reported physical retest; no agent-sent chat content |
| Snapchat | Unknown | TECNO KL5 / Android 14 (API 34), owner-reported on the connected test phone | 2026-10-03 | Owner did not specify | Working rich content on installed ReaxKey 1.0.0 debug build | Owner-reported physical retest; no agent-sent chat content |
| Telegram after typing upgrade | Unknown | TECNO KL5 / Android 14 (API 34), owner-reported on the connected test phone | 2026-10-03 | Owner did not specify | Sticker worked with updated ReaxKey debug APK | Owner-reported physical retest; no agent-sent chat content |
| Snapchat after typing upgrade | Unknown | TECNO KL5 / Android 14 (API 34), owner-reported on the connected test phone | 2026-10-03 | Owner did not specify | Sticker worked with updated ReaxKey debug APK | Owner-reported physical retest; no agent-sent chat content |
| WhatsApp | Unknown | — | — | — | NOT TESTED for sending | Sticker-folder indexing is a separate observation |
| Instagram, Discord, Messages and other apps | Unknown | — | — | — | NOT TESTED | No compatibility claim |

The owner selected Android's WhatsApp Stickers folder on TECNO KL5 / Android 14. ReaxKey indexed 10,675 stickers from 10,834 WebP files observed in that directory, and showed them in both the companion app and keyboard. An explicit Refresh recovered a system-stopped scan. Automatic MediaStore discovery did not find that directory on this phone despite image permission; a user-selected SAF folder supplied access. This is source-ingestion evidence, not a WhatsApp sending result.

The Telegram and Snapchat results are regression-critical. The owner explicitly retested both after installation of the 1.0.0 debug build and reported success. Exact third-party app versions and sticker formats remain unknown; do not infer them from that report. A signed Play-track build still needs its own acceptance check.

The later typing-upgrade APK was installed on 2026-10-03 and its local receiver
transfer passed. The owner then reported that sticker sending worked in both
Telegram and Snapchat on the newer APK. Exact destination app versions and
sticker formats were not provided, and no Play-track build has been tested.
