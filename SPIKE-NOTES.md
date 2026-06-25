# Spike: transparent Compose HUD composited over live KorGE gameplay

**Branch:** `spike/compose-overlay` (off `spike/compose-korge-interop`) · 2026-06

**Question:** can we run "Compose everywhere" — a transparent Compose HUD/overlay drawn
*over live KorGE gameplay* (compositing, both rendering every frame), not the menu↔game
handoff — and what does the second render layer cost per frame?

## What's wired (Android)

- `GameHost.kt` — `buildGameConfig(engine, frameMs)`: a `KorgeConfig` that boots straight
  into `GameScene` with a **shared** `GameEngine`, plus an updater that writes an EMA of the
  KorGE frame delta to `frameMs` (the number the perf question is about).
- `ComposeHostActivity.kt` — a `Box` with the KorGE game (`KorgeAndroidView.loadModule(config)`)
  on the bottom and a **transparent Compose HUD on top** (`HudOverlay`) reading the same
  engine's StateFlows (`player.collectAsState()`) + `frameMs`. Single KMP process, one engine.
- `AndroidManifest.xml` — launcher pointed at `ComposeHostActivity` (revert for normal play).

## Status — what's verified here vs. needs a device

✅ **Compiles** (`compileDebugKotlinAndroid`) and **packages** (`assembleDebug` → a debug APK;
manifest merges with `ComposeHostActivity` as launcher, no duplicate classes). The architecture
is sound: shared engine, Compose reads live StateFlows, KorGE boots the real game.

❓ **Not verifiable without an emulator/device** (none installed here — `adb` shows no devices):
1. Does the transparent Compose HUD actually **composite over the GL SurfaceView**?
   `KorgeAndroidView` uses a `KorgwSurfaceView` (GLSurfaceView-style). By default its surface
   sits *below* the view window, so Compose drawn after it in the `Box` should overlay (this is
   how video controls overlay a `SurfaceView`) — but z-order/transparency on SurfaceView is the
   classic gotcha and must be eyeballed on device. If it punches a black hole, switch KorGE to a
   TextureView-backed surface (heavier) or `setZOrderMediaOverlay`.
2. The **frame cost**: does the second render layer + per-frame HUD recomposition hold the
   budget? Read the gold `KorGE … ms` value with the overlay present vs. removed.

## How to run + measure (needs a device/emulator)

```
# point an emulator/device at adb, then:
./gradlew playAndroid          # installs + launches ComposeHostActivity
# tap START → game runs under the transparent HUD; read the "KorGE … ms" readout.
# A/B: comment out `HudOverlay(engine, frameMs)` in ComposeHostActivity → rebuild → compare ms.
```

## Read-through (no run needed)

This is the most ambitious config: compositing (hard) not handoff (easy), Compose on the
per-frame hot path (what the perf gate guards), and KorGE-7-gated for *modern* Compose. Android
is the only target where it's structurally clean (shared process + `AndroidView` overlay); web
would need two stacked canvases with the engine un-shareable across the JS/Wasm boundary, desktop
has no overlay path. The on-device frame-time number is the go/no-go for "Compose HUD over
gameplay." See `spike/compose-korge-interop` (handoff) and `spike/korge-ui-widgets` (no-Compose).
