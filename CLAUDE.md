# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
./gradlew :app:compileDebugKotlin          # fast type-check / compile
./gradlew :app:assembleDebug               # build debug APK
./gradlew :app:installDebug                # install to a connected device/emulator
./gradlew test                             # JVM unit tests (src/test)
./gradlew :app:testDebugUnitTest --tests "*ClassName.methodName"   # single test
./gradlew connectedDebugAndroidTest        # instrumented Compose tests (needs device)
```

Android: `minSdk 24`, `targetSdk 36`, `compileSdk 36`, JVM target 11. Kotlin + Jetpack Compose with KSP for Hilt.

## Backend dependency (rembg)

The app calls a self-hosted [rembg](https://github.com/danielgatis/rembg) HTTP server. `BASE_URL` is hardcoded in `di/NetworkModule.kt` (currently `http://192.168.0.101:7000/`). When testing on a real device, change this to the dev machine's LAN IP before building — `localhost` will not work. Endpoint contract is in `data/datasource/RembgApi.kt` (`POST /api/remove` multipart, returns raw PNG bytes).

## Architecture

Clean Architecture in a single `:app` module under package `com.example.anhnn_layr` (note: docs/cursor rules describe a `com.anhnn` package — the actual code is `com.example.anhnn_layr`; treat the docs as aspirational).

- **data/** — `RembgApi` (Retrofit), `RembgRepositoryImpl` reads the picked `Uri` via `ContentResolver`, posts multipart, returns `RembgResult(originalBitmap, processedBytes)`.
- **domain/** — `RembgRepository` interface + `RemoveBackgroundUseCase`. Pure Kotlin, no Android imports beyond `Uri`.
- **di/** — Hilt modules: `NetworkModule` (OkHttp/Retrofit/RembgApi singletons), `RepositoryModule` (binds repository impl).
- **presentation/**
  - `viewmodels/RembgViewModel` is the single source of truth. It owns two `StateFlow`s: `state: RembgUiState` (Idle/Loading/Success/Error) and `editor: EditorState` (all editor knobs).
  - `screens/RembgScreen` is the top-level router that switches on `RembgUiState` between `HomeScreen`, loading, error, and `EditorScreen`.
  - All composables are stateless — every callback is hoisted to the ViewModel.
- **utils/** — pure bitmap pipeline functions, called from the ViewModel on `Dispatchers.Default`:
  - `BrushEngine.buildWorkingBitmap` applies erase/restore paths against the processed/original pair.
  - `Feather.applyFeather`, `SubjectEffects.applySubjectEffects` (outline → shadow → color matrix), `BackgroundBlur.blurBackground`.
  - `BitmapComposer.generateFinalBitmap` composes background image + background color + subject for export. Drawing order matters and must mirror the preview.
  - `MediaStoreSaver` writes to `Pictures/TayMaySticker` via MediaStore. `pickSaveFormat(sourceMime, hasTransparency)` chooses PNG/JPEG/WebP — matches the input format, but forces PNG/WebP when the result has alpha so transparency is preserved.

### Editor data flow (important)

`RembgUiState.Success` carries three bitmaps that form a pipeline; never mutate them in-place — always replace via `_state.value = now.copy(...)`:

1. `workingBitmap` — subject after erase/restore paths. Rebuilt when paths change.
2. `displayBitmap` — `workingBitmap` after feather.
3. `effectedBitmap` — `displayBitmap` after outline/shadow/color-matrix. This is what the preview renders and what export starts from.

`recomposeSubject` rebuilds all three; `recomposeEffected` only rebuilds layer 3. Both cancel the previous `effectsJob` so sliders dragging fast don't pile up coroutines. The brush erase tool commits a `TouchPath` per stroke segment (see `commit 0a46741`) — this is intentional for live recomposition feedback, do not batch.

### Export

Save button in `EditorScreen` does **not** show a format picker. It calls `pickSaveFormat` with the source MIME (captured from `contentResolver.getType(uri)` at pick time and stored in `EditorState.sourceMimeType`) and the current transparency state. Goal: input format = output format, except when alpha would be lost.

## Conventions

- Follow `docs/clauderules.md` for Anhnn ecosystem rules (Material 3, Purple Gradient `#A1A2FF → #4B4EEE`, `AnhnnGradientButton`, etc.).
- UI text in Vietnamese.
- `MainActivity` is locked to portrait — do not add landscape layouts.
- Heavy bitmap work goes on `Dispatchers.Default`; I/O (MediaStore, network) on `Dispatchers.IO`. ViewModel is the only place that launches coroutines.

## Docs

`docs/` holds the Anhnn ecosystem reference material — `clauderules.md` is the source of truth for architecture rules; `feat.md` contains the spec for the current subject-effects feature. Other files (`FONTEND.md`, `Perfopmance.md`, `UI_GUIDE.md`, etc.) are supplementary.
