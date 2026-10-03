# Sticker source discovery

ReaxKey discovers stickers only from supported, public sources. On Android 10 and newer it asks
MediaStore for WebP, PNG and GIF rows whose relative path names a recognized WhatsApp or WhatsApp
Business Stickers directory. The source classifier checks complete path segments, including the
WhatsApp package/location, rather than filenames or every phone image. DCIM, Camera, Screenshots,
Downloads and general WhatsApp Images are not sticker sources. MediaStore results are candidates;
indexing still checks decodable bounds and supported formats in a background worker.

Some Android installations do not publish WhatsApp sticker files through MediaStore even when
the files exist. The Stickers tab therefore offers **Select your WhatsApp Stickers folder**.
Android's document picker receives the exact folder observed on the owner's phone as a location
hint, not as a permission grant or a claim that the folder exists on every device. The user must
approve the folder. ReaxKey persists the returned tree URI, validates it through the provider,
then indexes its sticker files. A different folder can be chosen explicitly in Settings. Revoked
grants appear as a source-health problem rather than being silently treated as empty.

The initial discovery and refresh run outside the UI thread. The IME reads the local Room index;
opening the keyboard does not rescan the directory. Imports are content-URI based. Imported bytes
use a SHA-256 ID; stable source URIs deduplicate records. The migration to schema 3 hides broad
legacy image rows, preserving user metadata for valid sticker records.

Physical evidence on TECNO KL5 / Android 14: ADB observed
`/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Stickers` with 10,834
WebP files. The redesigned app showed no automatic candidate despite granted image access.
Picker grant, indexing and IME display of these files are pending physical verification.

Android storage references:
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/training/data-storage/shared/documents-files
- https://developer.android.com/about/versions/14/changes/partial-photo-video-access
