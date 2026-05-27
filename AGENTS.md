# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android app in `:app`, using Kotlin, Jetpack Compose, Hilt, Retrofit, Room, and KSP. Source code lives under `app/src/main/java/com/example/anhnn_layr/`.

- `presentation/`: Compose screens, components, theme, and `RembgViewModel`.
- `domain/`: models, repository interfaces, and use cases.
- `data/`: Retrofit API, repository implementations, Room database, DAO, and file storage.
- `di/`: Hilt modules for network, database, and repositories.
- `utils/`: bitmap processing, composition, brush, blur, feathering, and MediaStore helpers.
- `app/src/test/`: JVM unit tests. `app/src/androidTest/`: instrumented Android and Compose tests.
- `docs/`: project notes and Anhnn ecosystem rules.

## Build, Test, and Development Commands

- `./gradlew :app:compileDebugKotlin`: fast Kotlin compile/type-check.
- `./gradlew :app:assembleDebug`: build a debug APK.
- `./gradlew :app:installDebug`: install on a connected device or emulator.
- `./gradlew test`: run JVM unit tests under `src/test`.
- `./gradlew :app:testDebugUnitTest --tests "*ClassName.methodName"`: run one JVM test.
- `./gradlew connectedDebugAndroidTest`: run instrumented tests; requires a connected device/emulator.

The rembg backend base URL is currently hardcoded in `di/NetworkModule.kt`; use a LAN IP for real-device testing because device `localhost` is not the development machine.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and idiomatic Compose naming: `PascalCase` for composables and classes, `camelCase` for functions and state fields. Keep composables stateless where practical and hoist callbacks to the ViewModel. UI copy should remain Vietnamese. Heavy bitmap work belongs on `Dispatchers.Default`; network and MediaStore work belongs on `Dispatchers.IO`.

Follow the existing Clean Architecture boundaries. Do not mutate bitmap pipeline state in place; replace state with `copy(...)`.

## Testing Guidelines

Prefer fake repositories over Mockito/MockK for data-layer dependencies. Use coroutine test dispatchers such as `StandardTestDispatcher` when testing ViewModels and use cases. Compose UI tests should use `createComposeRule()`. Name tests by behavior, for example `should_show_error_when_static_service_fails()`. Mock calls to `static.api.hihoay.com` or rembg-compatible services so tests run offline.

## Commit & Pull Request Guidelines

Git history uses Conventional Commit-style messages, for example `feat(drafts): save and resume editor sessions` and `fix(editor): force recompose on every brush stroke segment`. Keep commits scoped and imperative.

Pull requests should include a short problem/solution summary, testing performed, linked issues when applicable, and screenshots or screen recordings for UI changes. Note any backend URL or device/emulator assumptions.

## Agent-Specific Instructions

Read `CLAUDE.md` before substantial changes. It documents the editor bitmap pipeline, export behavior, architecture expectations, and project-specific constraints that should be preserved.
