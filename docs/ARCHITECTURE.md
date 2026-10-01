# Architecture

## 1. System overview

```text
                         Android OS
                             │
              ┌──────────────┴──────────────┐
              │                             │
        Companion App                 InputMethodService
              │                             │
      onboarding/library              typing + media UI
              │                             │
              └──────────────┬──────────────┘
                             │
                   domain/repository layer
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
      Room DB          Media access         Search index
         │          MediaStore / SAF          lexical
         │                   │                   │
         └───────────────────┼───────────────────┘
                             │
                    content URI sender
                             │
                current target application
```

## 2. Module structure

```text
root/
├── app/                    # Android application / manifest / DI composition
├── ime/                    # IME Android library
├── core/
│   ├── model/              # Domain models/contracts
│   ├── data/               # Room and repositories
│   ├── media/              # Scanner/import/URI/content-provider utilities
│   ├── search/             # Search engine + semantic provider seam
│   └── ui/                 # Shared resources/components
├── feature/
│   ├── onboarding/
│   ├── library/
│   └── settings/
├── testbed/                # Debug-only rich-content receiver app
├── docs/
└── work/
```

The installed product is one application. `:ime` is an Android library packaged by `:app`; its `InputMethodService` is declared in the application manifest. `:testbed` is a separate dev-only application module and is never bundled into production.

## 3. Process model

The companion activity and IME normally run under the same application UID and can use the same Room database/repositories. Do not assume both are simultaneously alive. Everything required by the IME must survive process recreation.

Do not keep media index state only in memory.

## 4. Dependency direction

```text
feature/* -> core/model + core/data + core/ui
ime       -> core/model + core/data + core/media + core/search
core/data -> core/model
core/media-> core/model
core/search->core/model
app       -> all modules for wiring
```

Core modules never depend on feature modules.

## 5. Data flow: ingestion

```text
Source grant / MediaStore query / Share import
                │
                ▼
           MediaScanner
                │
        validation + metadata
                │
                ▼
         MediaRepository
                │
                ▼
              Room
                │
                ▼
     Search index projections
```

## 6. Data flow: keyboard query

```text
Media tab opens
      │
      ▼
ViewModel/controller
      │
      ▼
SearchRepository Flow
      │
      ▼
Room indexed query
      │
      ▼
Paged/bounded results
      │
      ▼
Thumbnail grid
```

Never decode full-size images just to show a grid.

## 7. Data flow: send media

```text
user taps result
      │
      ▼
TargetCapabilities(EditorInfo MIME types)
      │
      ├── supported -> obtain content URI -> temporary read grant -> commitContent
      │
      └── unsupported -> explicit fallback UI (Share / explain limitation)
```

## 8. Dependency injection

Use a lightweight DI solution if it materially simplifies lifecycle/testing. Hilt is acceptable, but the agent should not add it purely for fashion. If used, verify `InputMethodService` integration carefully. Manual composition is acceptable for a project of this size if tests remain clean.

## 9. Future semantic provider seam

Define an interface similar to:

```kotlin
interface SemanticMetadataProvider {
    suspend fun analyze(item: MediaItem): SemanticMetadata
}
```

Default implementation: disabled/no-op.

Future on-device vision implementation can produce tags/embeddings. No current feature may depend on this provider returning data.
