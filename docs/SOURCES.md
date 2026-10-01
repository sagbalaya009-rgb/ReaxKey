# Android Platform Sources

These sources ground platform-sensitive architectural assumptions. Coding agents should re-check current Android documentation when implementing because APIs/policies evolve.

## Input methods

- Create an input method: https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method
- `InputMethodService`: https://developer.android.com/reference/android/inputmethodservice/InputMethodService

## Rich content from keyboards

- Image keyboard support / Commit Content API: https://developer.android.com/develop/ui/views/touch-and-input/image-keyboard
- `InputConnection`: https://developer.android.com/reference/android/view/inputmethod/InputConnection
- `InputConnectionCompat`: https://developer.android.com/reference/androidx/core/view/inputmethod/InputConnectionCompat
- `EditorInfoCompat`: https://developer.android.com/reference/androidx/core/view/inputmethod/EditorInfoCompat

## Storage and media

- Storage overview: https://developer.android.com/training/data-storage
- Shared media / MediaStore: https://developer.android.com/training/data-storage/shared/media
- Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files
- App-specific files: https://developer.android.com/training/data-storage/app-specific
- Android 11 storage restrictions: https://developer.android.com/about/versions/11/privacy/storage
- All files access: https://developer.android.com/training/data-storage/manage-all-files

## Architecture interpretation used by this project

- Modern Android prevents ordinary apps from reading another app's private/internal data directory, and Android 11+ also restricts other apps' app-specific external directories.
- SAF can grant access to user-selected files/directories that Android allows through the system picker.
- MediaStore is the platform index for shared media.
- Rich content insertion is cooperative: target editors advertise accepted MIME types; keyboards use `commitContent` with a content URI and permission grant.
