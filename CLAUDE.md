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

Android: `minSdk 24`, `targetSdk 36`, `compileSdk 36`, JVM target 11. Kotlin + Jetpack Compose, Hilt via KSP, Room for drafts, ML Kit Face Mesh for face editing. Build file is Groovy (`app/build.gradle`), versions in `gradle/libs.versions.toml`.

## Backend dependency (rembg server)

The app calls a self-hosted [rembg](https://github.com/danielgatis/rembg) HTTP server. `BASE_URL` is hardcoded in `di/NetworkModule.kt` (currently `http://192.168.1.15:7000/`). When testing on a real device, change this to the dev machine's LAN IP before building — `localhost` will not work. Endpoint contract is in `data/datasource/RembgApi.kt`:
- `POST /api/remove` — multipart, background removal, returns raw PNG bytes.
- `POST /api/upscale` — multipart + query params (Real-ESRGAN style model/outscale/tile), used by the "Làm nét" (sharpen/upscale) flow.

## Architecture

Clean Architecture in a single `:app` module under package `com.example.anhnn_layr` (note: docs/cursor rules describe a `com.anhnn` package — the actual code is `com.example.anhnn_layr`; treat the docs as aspirational).

- **data/** — `RembgApi` (Retrofit); `RembgRepositoryImpl` / `UpscaleRepositoryImpl` read the picked `Uri` via `ContentResolver` and post multipart; `local/` holds the draft store (Room `AnhnnDatabase` + `DraftFileStore` which persists bitmaps/JSON to app files).
- **domain/** — repository interfaces + use cases (`RemoveBackgroundUseCase`, `UpscaleImageUseCase`, `DraftUseCases`). Pure Kotlin, no Android imports beyond `Uri`/`Bitmap`.
- **di/** — Hilt modules: `NetworkModule`, `DatabaseModule`, `RepositoryModule`.
- **presentation/**
  - `viewmodels/RembgViewModel` is the single source of truth for the editor. It owns `state: RembgUiState` (Idle/Loading/Success/Error), `editor: EditorState` (all editor knobs), `drafts`, `recentPhotos`, and a `messages` SharedFlow for toasts. `UpscaleViewModel` drives the separate upscale flow.
  - `screens/RembgScreen` is the top-level router: it layers overlay screens (camera capture → gallery → photo preview, the `LayrEditorScreen` feature chooser, `UpscaleScreen`) over the `RembgUiState` state machine (Idle → `LayrMainScreen`, Success → `EditorScreen`).
  - `screens/layr/` is the LAYR-branded shell: `LayrMainScreen` (bottom nav Home/Studio/Account), `LayrHomeScreen`, `StudioScreen` (drafts), `AccountScreen`, `LayrTheme`/`LayrColors`.
  - `components/tools/` — one panel per editor tab (`BackgroundToolPanel`, `EraseToolPanel`, `FaceToolPanel`, `EffectsToolPanel`, `CropToolPanel`, `TextToolPanel`) plus shared `ToolTabs`/`ToolPanelDefaults`.
  - All composables are stateless — every callback is hoisted to the ViewModel.
- **utils/** — pure bitmap pipeline functions, called from the ViewModel on `Dispatchers.Default`: `BrushEngine`, `Feather`, `BackgroundBlur`, `FaceDetector`/`FaceReshape`/`FaceSmooth`/`FaceMakeup`, `ImageCropper`, `TextSticker`, `BitmapComposer`, `MediaStoreSaver`, `GalleryStore`.

### Entry flows

After an image is picked/captured, `LayrEditorScreen` offers three paths:
1. **Xoá nền** → `vm.remove(uri)` → rembg server → editor with `isBackgroundRemoved = true`.
2. **Làm nét** → `UpscaleScreen` (separate ViewModel, `/api/upscale`).
3. **Chỉnh ảnh** → `vm.edit(uri)` → editor directly, no server call; the whole image is the "subject". `isBackgroundRemoved = false` hides the Background and Erase tabs (they'd be no-ops) and opens the FX tab.

### Editor data flow (important)

`RembgUiState.Success` carries three bitmaps; never mutate them in-place — always replace via `_state.value = now.copy(...)`:

1. `workingBitmap` — subject after erase/restore paths (`buildWorkingBitmap`). Rebuilt only when paths change.
2. `displayBitmap` — `workingBitmap` after face edits + feather. Face warps (skin smooth → eye enlarge → face slim → lip color, in that fixed order — see comments in `runSubjectRecompose`) are baked here, always computed from the *clean* working bitmap so intensity sliders don't accumulate distortion.
3. `effectedBitmap` — currently identical to `displayBitmap`; it is what the preview renders and what export starts from.

Brightness/contrast/saturation are **not** baked into any bitmap — they are applied as a realtime `ColorFilter` in the preview (`colorAdjustMatrixOrNull` in `SubjectEffects.kt`) and passed to `generateFinalBitmap` as `subjectColorMatrix` at export. Keep preview and export matrices identical.

Recompose requests go through a conflated `Channel` (`subjectTrigger`) so fast slider drags coalesce instead of piling up coroutines. The brush erase tool commits a `TouchPath` per stroke segment — intentional for live feedback, do not batch.

### Face editing

ML Kit Face Mesh (468 points) via `utils/FaceDetector.detectFaceLandmarks`. Detection is lazy — runs once when the "Mặt" tab opens; landmarks are cached in the ViewModel in `workingBitmap` coordinates and invalidated on crop/reset. The warp/makeup math (`FaceReshape`, `FaceSmooth`, `FaceMakeup`) deliberately uses plain-float data classes (`EyeAnchor`, `FacePoint`, `FaceAxis`) instead of `PointF` so it's unit-testable on the JVM (`src/test/.../FaceReshapeTest.kt`) — keep new face math in that style.

### Crop

`applyCrop` supports a rotated `CropFrame` and must transform *everything* in image space: processed + original bitmaps, erase/restore paths, redo stack, and text-sticker centers/rotations. If you add new image-space state, transform it there too.

### Drafts

"Lưu nháp" snapshots the source URI, processed bitmap, `EditorState` (as `EditorStateSnapshot`), and paths via Room + `DraftFileStore`. Snapshot fields added later must be nullable with backward-compatible defaults (see `toEditorState` — old drafts lacking flags get legacy behavior).

### Export

Save button in `EditorScreen` does **not** show a format picker. It calls `pickSaveFormat` with the source MIME (captured at pick time, stored in `EditorState.sourceMimeType`) and the current transparency state. Goal: input format = output format, except PNG/WebP is forced when alpha would be lost. Exports go to `Pictures/TayMaySticker`; camera captures go to `Pictures/AnhnnLayr` (`GalleryStore`), which is also what the "recent photos" Home section queries.

## Conventions

- Follow `docs/clauderules.md` for Anhnn ecosystem rules (Material 3, Purple Gradient `#A1A2FF → #4B4EEE`, `AnhnnGradientButton`, etc.). The LAYR shell additionally uses `LayrColors` (teal accents, dark surfaces).
- UI text in Vietnamese; code comments are largely Vietnamese too — match that.
- The app is locked to Dark theme (`AnhnnlayrTheme(darkTheme = true)` in `MainActivity`) and portrait orientation — do not add light-theme or landscape variants. Edge-to-edge is enabled globally; each screen handles its own insets (no global padding in `MainActivity`).
- Heavy bitmap work goes on `Dispatchers.Default`; I/O (MediaStore, network, Room) on `Dispatchers.IO`. The ViewModel is the only place that launches coroutines.

## Docs

`docs/` holds the Anhnn ecosystem reference material — `clauderules.md` is the source of truth for architecture rules; `feat.md` contains feature specs. Other files (`FONTEND.md`, `Perfopmance.md`, `UI_GUIDE.md`, etc.) are supplementary.
