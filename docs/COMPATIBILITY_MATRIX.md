# Rich content compatibility matrix

Observed evidence as of 2026-10-02. A compatible editor must advertise image MIME types. A successful testbed transfer does not prove that a third-party app accepts the same content. Do not advertise universal compatibility.

| App / field | App version | Device / Android | Date | Format | Outcome | Evidence level |
|---|---|---|---|---|---|---|
| ReaxKey receiver | Development build | TECNO KL5 / Android 14 (API 34) | 2026-10-02 | PNG, JPEG, GIF, WebP | Separate receiver read granted URIs; WebP from the real sticker library read 18,014 bytes | Agent-observed on device; animation fidelity not assessed |
| ReaxKey receiver with no rich-content MIME | Development build | Same | 2026-10-02 | Image | Correctly rejected; explanation and Share fallback appeared | Agent-observed; chooser destination delivery not assessed |
| Telegram | Unknown | Owner's phone; model/API not independently recorded for this app test | Reported 2026-10-02 | Owner did not specify | Working rich content | Owner-reported physical test; no fresh agent retest after latest build |
| Snapchat | Unknown | Owner's phone; model/API not independently recorded for this app test | Reported 2026-10-02 | Owner did not specify | Working rich content | Owner-reported physical test; no fresh agent retest after latest build |
| WhatsApp | Unknown | — | — | — | NOT TESTED for sending | Sticker-folder indexing is a separate observation |
| Instagram, Discord, Messages and other apps | Unknown | — | — | — | NOT TESTED | No compatibility claim |

The owner selected Android's WhatsApp Stickers folder on TECNO KL5 / Android 14. ReaxKey indexed 10,675 stickers from 10,834 WebP files observed in that directory, and showed them in both the companion app and keyboard. An explicit Refresh recovered a system-stopped scan. Automatic MediaStore discovery did not find that directory on this phone despite image permission; a user-selected SAF folder supplied access. This is source-ingestion evidence, not a WhatsApp sending result.

The Telegram and Snapchat reports are regression-critical. Record exact app versions, formats and outcomes at the next physical release check. Do not infer them from the owner report.
