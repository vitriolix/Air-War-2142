# Spike: Compose-Multiplatform + KorGE interop

**Branch:** `spike/compose-korge-interop` · 2026-06

**Goal:** test the "Compose-First (game as a component)" idea — a real Compose-MP UI shell
that embeds the KorGE engine — on the current stack (KorGE 6.0.0 / Kotlin 2.0.21).

**Setup:** added Compose-MP `1.7.3` + compose-compiler `2.0.21` alongside the KorGE plugin on
`:composeApp` (1.7.x is the line that pairs with Kotlin 2.0.21; ≥1.8 needs Kotlin ≥2.1).
Files: `gradle/libs.versions.toml`, `composeApp/build.gradle.kts`,
`androidMain/.../ComposeHostActivity.kt`, `wasmJsMain/.../WasmComposeProbe.kt`.

## Compile matrix (measured)

| Target | Result | Note |
|---|---|---|
| **Android** | ✅ builds | Compose shell + `AndroidView { KorgeAndroidView(ctx) }` compiles. Needed `compileSdk` 33→34. |
| **Wasm** | ✅ builds | Compose-MP UI compiles for `wasmJs` alongside KorGE. |
| **JVM** | ❌ fails | `IncompatibleComposeRuntimeVersionException` — the compose-compiler plugin is **module-wide**; `jvmMain` has no compose-runtime. |
| **JS (Kotlin/JS)** | ❌ same class of failure | + Compose-MP shared UI historically didn't target Kotlin/JS (a JS *fallback* exists in CMP ≥1.10, but that's about browser reach, not sharing a canvas with KorGE). |

## Key findings

1. **Plugins coexist** at configuration time (KorGE pulls AGP 8.2.2 — well inside CMP 1.7.x's range).
2. **Android embedding is real**: `KorgeAndroidView` drops into a Compose `AndroidView`. Full-screen
   *handoff* (menu and game never co-resident), not compositing.
3. **You cannot bolt both plugins onto the shared module**: the compose-compiler runs on *every*
   compilation, breaking the pure-KorGE JVM/JS/Wasm targets (no runtime on classpath). The clean shape
   is a **module split** — `:game` (KorGE, no Compose) + `:androidApp` (Compose-MP, depends on `:game`).
4. **KMP shares code, not rendering surfaces.** Co-existence with KorGE is per-platform: Android ✅
   (proven), iOS ⚠️ likely (UIKitView; KorGE-iOS not wired here), desktop ❌ (KorGE = GLFW window, no
   Compose `SwingPanel` bridge), web ⚠️ handoff-only + two runtimes in the bundle.

## Bottom line

The only clean "real Compose menus hosting KorGE" path is **Android (+ likely iOS)**; web/desktop must
keep KorGE-native menus. Modern Compose-MP (1.11) would need a KorGE/Kotlin upgrade that is currently
blocked — see `spike/korge7-bump`. This branch is a working reference for the Android-first path; it
intentionally leaves JVM/JS broken (the point of the spike). Not mergeable as-is.
