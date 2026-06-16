# 0004 — KorGE Port: Build, Platform & Input Notes

**Status:** Reference / living. Records the port to KorGE, the build setup, the cross-platform
**input hacks** (hard-won — do not revert), and the **performance investigation**. Facts here are
verified against the repo at time of writing; update as the engine evolves.
**Related:** [`0003` — Rendering & Sprite Atlas](0003-rendering-and-sprite-atlas.md) ·
[`0001`](0001-ecs-kernel-and-core-migration.md) / [`0002`](0002-plugin-and-ecs-architecture.md)
(the ECS/plugin re-architecture now underway) · README "Hacks & workarounds" · `scripts/README.md`.

> This is the "why is it built this way" companion to the README. The README lists the workarounds;
> this doc explains the *evidence* behind each one so the next person doesn't re-pay the cost (the
> macOS keyboard fix alone took ~8 failed attempts).

---

## 1. The port & the engine choice

Air War 2142 was ported from Kotlin Multiplatform + Compose Multiplatform to **KorGE**
(Gradle plugin `com.soywiz.korge`), 5.2.0 → now **6.0.0**.

> **Caveat (don't lose this):** KorGE was named by josh as an *example* of the category "Kotlin
> game engine," **not** a directive to use KorGE specifically. The engine choice is **open to
> reconsideration** (korlibs 6.x or alternatives). A prior session over-read "like KorGE" as
> "KorGE specifically" — don't repeat that. The API details below are confirmed for 6.0.0, but
> treat the engine itself as not pinned.

The old Compose UI files were deleted; `GameEngine.kt` carried over unchanged (no Compose deps —
`StateFlow` still works). It's now being incrementally migrated onto an ECS kernel
([`0001`](0001-ecs-kernel-and-core-migration.md)).

**Current source layout:**
- `commonMain/kotlin/main.kt` — `suspend fun main()`, the Korge entry point (`windowWidth/Height/bgcolor`)
- `scenes/` — `MenuScene`, `GameScene`, `SettingsScene` (KorGE `Scene` subclasses), `Focus.kt`
- `logic/GameEngine.kt` — engine (platform-agnostic)
- `ecs/` — the ECS kernel + particle slice
- `render/` — `Shapes.kt`, `SpriteAtlas.kt`, `AtlasSpec.kt` (see [`0003`](0003-rendering-and-sprite-atlas.md))
- `audio/NoOpSoundPlayer.kt`, `input/NoOpSensorInput.kt` — stubs
- `*/Platform.kt` — expect/actual (`CANVAS_HEIGHT`)

---

## 2. Build config

- **KorGE 6.0.0** (plugin id still `com.soywiz.korge`; the `org.korge` rename is 6.1+, unreleased).
  Versions in `gradle/libs.versions.toml`: `korge = "6.0.0"`, `kotlin = "2.0.21"`.
- **Gradle wrapper 8.14.4** (`gradle/wrapper/gradle-wrapper.properties`).
- **JDK 21 is required, and the constraint is subtle.** KorGE 6's plugin rejects JDK < 21
  (*"requires at least JVM runtime version 21"*) **and cannot parse JDK 26** (configuration fails
  with a cryptic *"What went wrong: 26.0.1"*). Use **JDK 21–24; 21 LTS is safest.**
  - It's the **Gradle daemon JVM** that must be 21+, not the compile JVM — **toolchains do NOT fix
    this.** The daemon JDK is pinned in `gradle.properties`:
    `org.gradle.java.home=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
    (installed via `brew install openjdk@21`, keg-only). Adjust per machine, or point `JAVA_HOME`
    at a 21. *(Watch out: brew `openjdk@23` on this machine is mislinked and reports 26 — don't
    trust it.)*
- **Targets** (`composeApp/build.gradle.kts`, `korge { … }`): `targetJvm()`, `targetAndroid()`,
  `targetJs()`, `targetWasmJs()`. iOS (`targetIos()`) is planned, not wired (`TASKS.md` #18).
- **First JS build** may fail on `:kotlinStoreYarnLock` (*"Lock file was changed"*) — run
  `./gradlew kotlinUpgradeYarnLock` once.

---

## 3. KorGE 6.0.0 API gotchas

### 3.1 Migration breaking change from 5.2.0
- **`onClick { }` is now NON-suspend** (`Function1<MouseEvents, Unit>`). Handlers that call
  suspend fns (`nav.changeTo { }`, suspend `action()` lambdas) no longer compile (*"Suspension
  functions can only be called within coroutine body"*). **Fix:** `onClickSuspend(views.coroutineContext) { … }`
  (import `korlibs.korge.input.onClickSuspend`). `Scene` is a `CoroutineScope` and exposes `views`,
  so `views.coroutineContext` is available in `sceneMain` and helpers. Non-suspend handlers (e.g.
  `engine.togglePause()`) can stay `onClick`.

### 3.2 Standing API gotchas
- `Key` enum is `korlibs.event.Key` (**not** `korlibs.korge.input.Key`).
- `onClick` on a `View`: import `korlibs.korge.input.onClick` **explicitly** — a star-import of
  `korlibs.korge.view.*` only gets `QView.onClick` (wrong type). `QView` is **not** a `View` (it's
  a `List<View>` selector) — don't extend it.
- `ShapeBuilder` extends `Context2d` (Canvas2D-style): `fill(paint) { rect/circle/ellipse }`.
  - `circle(x, y, r)` **does not exist** — use `circle(Point(x, y), r)`.
  - `ellipse(cx, cy, rx, ry)` — use `ellipse(Point(cx, cy), Size2D(rx*2, ry*2))` (2nd arg is
    radius; see [`0003` §4](0003-rendering-and-sprite-atlas.md#4-gotchas-baked-into-the-look)).
  - `roundRect` takes **6** args: `roundRect(x, y, w, h, rx, ry)`.
- `Graphics.updateShape { }` — the `graphics { updateShape { } }` form **does not** work
  (`updateShape` not in scope in the init lambda). Assign first: `val g = graphics { }; g.updateShape { … }`.
- `stage.keys { down(Key.X) { } }` in `main.kt` is **required** to activate key dispatch for the
  whole scene graph — removing it breaks **all** keyboard input, even in child scenes.
- `SceneContainer(views = views)` then `addChild(sc)` for an explicit container (the
  `sceneContainer()` extension may not be importable).
- Click targets: text labels only catch clicks on non-transparent pixels — add a near-invisible
  `solidRect(w, h, RGBA(0,0,0,1))` overlay as the hit target.
- `singleTouch { start/move/end }` blocks receive `TouchEvents.Info` as `it` — use **`it.local.x`
  / `it.local.y`**. Bare `x`/`y` silently resolve to the outer `SContainer.x/y = 0.0` (implicit
  receiver scoping), making the player chase (0,0).
- **Font glyphs:** the default KorGE font lacks `▶` (U+25B6) and other symbols (`•`, `✈`, `◄►▲▼●`)
  may not render — a `text("▶")` shows blank. Use a `solidRect` or ASCII for UI markers; **verify
  any glyph actually renders.**

---

## 4. Input: the macOS keyboard hack (THE critical gotcha) 🟡

> **Hard rule (`CLAUDE.md`): do not revert this to per-frame polling.** It cost ~8 failed attempts.

On macOS, **AWT delivers every key press *and every auto-repeat* as a DOWN immediately followed by
an UP ~0–1 ms later.** Both land in the same render frame, so **any** per-frame sampling of key
state — `views.input.keys.pressing(Key)` *or* boolean flags set by `keys{down{f=true};up{f=false}}`
— reads the trailing `UP = false`. The key looks permanently released; the plane barely moves.
This defeats polling and naive event-flags **identically**, which is why swapping input schemes did
nothing.

Proven by runtime test (not source reading): real OS keystrokes via `osascript` logged
`DOWN t=42318 / UP t=42318` (same ms). A standalone harness feeding `updateKeyboardInputs(left=true)
+ tick()` 30× moved the plane smoothly — so the engine was always fine; the bug was purely the
input layer.

**Fix — `HeldKey` deferred-release (`scenes/GameScene.kt`):**

```
 key DOWN ──► held = true, cancel pending release
 key UP   ──► schedule release at now + 80ms   (do NOT clear immediately)
 each frame: update() clears held only once that time passes

 timeline (60fps, dt≈16ms):
   DOWN  UP(+0ms, schedule@+80)   …auto-repeat DOWN cancels it…   genuine release → clears @+80ms
    │     │                                                              │
    ▼     ▼                                                              ▼
   held ─────────────────────────────────────────────────────────────────  (stays held across
                                                                              spurious UPs/repeat)
```

A following DOWN (the paired event *or* auto-repeat) cancels the pending release, so the key stays
held across the spurious UP and across auto-repeat; a genuine release clears in 80 ms (masked by
the engine's velocity friction). **The window must exceed `dt`** — at the old 10 fps (dt≈100 ms)
the 80 ms window expired within one frame and the fix looked broken; it only works because
view-based rendering restored ~60 fps (dt≈16 ms). The time source is a frame-accumulated `clockMs`
(sum of `dt`), **not** `System.currentTimeMillis` (which isn't in `commonMain`).

---

## 5. Input: the grave/`~` key maps differently per backend 🟡

The **same physical key** maps to **different `Key` values** per backend, so the debug-overlay
toggle must match all of them:

| Backend | Signal |
|---|---|
| JVM / AWT | `Key.BACKQUOTE` (`AwtKeyMap`) |
| Android | `Key.GRAVE` (`AndroidKeyMap` — no BACKQUOTE entry) |
| JS | `Key.UNKNOWN` (no `when` case in `DefaultGameWindowJs`); reliable signal is **`keyCode == 192`** (also true on JVM) |

One handler matches all signals (two handlers would double-toggle), in `main.kt`'s `stage.keys {}`:

```kotlin
down { ev ->
    if (ev.key == Key.BACKQUOTE || ev.key == Key.GRAVE || ev.keyCode == 192 ||
        ev.character == '`' || ev.character == '~') {
        engine.showDebugOverlay = !engine.showDebugOverlay
    }
}
```

Verified on the user's real Android keyboard (after adding `Key.GRAVE`, found in `AndroidKeyMap`
bytecode) and on web. **General lesson:** verify per-backend `Key` mappings — don't assume
JVM == Android. *(Synthetic key injection via `osascript`/CGEvent bypasses the OS input layer, so
it can false-positive — it tests the binding, not real key delivery.)*

Other input infrastructure: `Focus.kt` defines `KeyboardNavigable`/`FocusController` (input-agnostic
`move`/`activate`, built so a gamepad can reuse it) and `EscapeHandler` (ESC/Android-back routing).
**KorGE 6 limitation:** the Android **Back** button cannot be consumed from game code (the
generated `KorgwActivity` always calls `super.onBackPressed()` → finish); only a plugin-level fork
could fix it. Pure `KEYCODE_ESCAPE` is fine.

---

## 6. `playHeight` vs `CANVAS_HEIGHT` — the Android-only "pop" bug 🟡

> **Hard rule (`CLAUDE.md`): keep the `CANVAS_HEIGHT` coupling.**

`CANVAS_HEIGHT` (expect/actual in `Platform.kt`) is **1500 on JVM/JS/Wasm but 2200 on Android**
(taller phone aspect). `GameEngine` originally hardcoded `playHeight = 1500f`, so on Android the
canvas rendered 2200 tall while the engine wrapped/clamped at 1500: **islands/clouds popped out of
existence ~700 px above the real bottom**, and the plane couldn't fly below y=1500. Fix:

```kotlin
val playHeight = CANVAS_HEIGHT.toFloat()   // not a hardcoded 1500
```

All wrap/clamp/spawn/cull logic is relative to `playHeight`, so it scales correctly. `playWidth =
1000` matches `windowWidth` on all platforms, so only height was wrong. **This bug is invisible on
JVM** (1500 == 1500) — it only reproduces on Android (confirmed fixed on the emulator).

---

## 7. Performance investigation

### 7.1 The web "30 fps" had TWO stacked causes
josh saw ~30 fps on web; it was two separate things masking each other:
1. **macOS Low Power Mode throttles the browser's `requestAnimationFrame` to 30 Hz** (native apps
   are exempt). Verified: headed-Playwright raw rAF on a *blank page* = 33.9 ms (~30) under
   `lowpowermode 1`; with LPM off + AC = **16.7 ms (60 fps)**.
2. **The game is render/CPU-bound on web** and couldn't sustain 60 even with the browser at 60.

> **Hard rule (`CLAUDE.md`): check Low Power Mode first.** `pmset -g | grep lowpowermode` (want
> `0`), ideally on AC. LPM silently invalidates web perf numbers — josh lost a whole measurement
> round to it. Headless Chromium *also* throttles rAF (~15 fps) and uses software WebGL → always
> use **headed** Playwright; per-frame JS time inside the rAF callback is valid under throttling,
> but wall-clock fps is not.

### 7.2 In-game is RENDER-bound — and the fix
Profiled via temporary `views.gameWindow.renderTime`/`updateTime` logged to the browser console
(captured with Playwright): **update < 1 ms; render ≈ 10–18 ms** (climbs with enemies/particles).
So Wasm can't buy in-game 60 (it's the WebGL render path, not CPU).

The dominant cost was the **player vector re-tessellating every frame** (`updateShape{drawPlayer}`
≈ 14 ms by itself; the P-38 is ~20 fill ops). **Baseline median renderTime 17.5 ms** (right at the
16.6 ms/60 fps edge → vsync-locked to 30) even with 0 particles.

**Fix (`GameScene`):** re-tessellate the player **only when its shape changes** —
`val playerShapeKey = "${escortsActive}|${rollProgress>0}"`, redraw on change vs `lastPlayerShapeKey`;
movement / roll-scale / rotation / invuln-blink stay cheap per-frame transforms. Result:
**renderTime 17.5 ms → ~3 ms** → ample 60 fps headroom. (`MenuScene` was fixed the same way; the
broader sprite-atlas migration in [`0003`](0003-rendering-and-sprite-atlas.md) generalizes this.)

> **Lesson:** per-frame `updateShape` (CPU/GPU re-tessellation of a vector) is very expensive on
> web (~8–14 ms for one moderate shape). Make `Graphics` **event-driven, never per-frame**, unless
> the shape truly changes.

### 7.3 Dead ends, recorded so they're not re-chased
- **Wasm** is a real CPU win (menu 20→29 fps, ~+43 %) but does **not** reach in-game 60 (both JS &
  Wasm vsync-lock ~30 in-game — the limiter is render, not CPU). Kept permanently anyway.
- **`ReadPixels` stalls = red herring.** Wrapping `gl.readPixels` in the browser and counting
  showed **0 readbacks/frame** in menu and gameplay; the startup `GPU stall due to ReadPixels`
  warning is a one-time context-init artifact. In-game 30 fps is genuine render *work*.

---

## 8. Verification surfaces & methodology

The project's discipline is **prove it by running the real app**, not by compiling (`CLAUDE.md`).
What each surface affords:

| Surface | Screenshots | Synthetic input | Notes |
|---|---|---|---|
| **JVM** | focus-free (CoreGraphics window-id capture via `/tmp/winid.swift` → `screencapture -l<id>`) | `osascript` needs the window **frontmost** + user hands-off (host activity hijacks it) | focus-free input only via in-app `gameWindow.dispatchKeyEvent` (tests logic, not OS delivery) |
| **Web (JS)** | headless Playwright, fully parallel-safe (own browser, CDP input) | yes | **preferred default** for UI/nav/scene-logic; but render fidelity (text ghosting) + key mappings still need native confirmation |
| **Android** | `adb exec-out screencap` (focus-free) | `adb shell input …` (focus-free) | user must not poke the emulator simultaneously |

**The debugging method that finally worked** (on the macOS keyboard bug): stop reading source and
guessing. (1) **Isolate** — a standalone harness driving `GameEngine` directly proved the engine
OK → bug is in the input layer. (2) **Observe the real app** — instrument with `println` of input
flags + `player.x`, temp auto-boot into `GameScene`, drive with real `osascript` keystrokes, grep
stdout. (3) Kill **all** prior app instances before each test (a stale window steals `osascript`
focus → false results); there's no `timeout` on this Mac, so run in background and poll the log.

---

## 9. Tooling & the task surface

Canonical commands are **Gradle tasks (group `game`)** in the root `build.gradle.kts`; `package.json`
is a thin `npm run <alias>` → `./gradlew <task>` shim; shell-heavy logic lives in `scripts/*.sh`
(all call the JDK21-pinned `./gradlew`). Two dev-tooling commands (`openPr`, `renderDocs`) are
native `buildSrc` custom task classes; the rest (`play*`, `webConsole`, `tidyGit`, `killServers`,
`createPr`, `release*`) are still `Exec`-wrapped scripts. Full reference: `scripts/README.md`.

- **No web backend** — the web app is 100 % client-side. Runtime logs/errors live in the **browser
  console** (DevTools); the dev server only shows build logs. The `webConsole` task streams the
  browser console + errors to the terminal headlessly.

---

## 10. What still needs work

- **Sound** — `NoOpSoundPlayer` stub; real audio would use the KorGE sound API.
- **Tilt control** — `NoOpSensorInput` returns (0,0); real impl needs a platform accelerometer via
  expect/actual.
- **iOS target** — not wired (`TASKS.md` #18; needs a Mac with Xcode).
- **The ECS/plugin re-architecture** — underway; see [`0001`](0001-ecs-kernel-and-core-migration.md)
  and [`0002`](0002-plugin-and-ecs-architecture.md). The input layer above must be **lifted into
  the kernel intact**, not re-typed.

---

<!-- TASKS:auto START -->
## Tasks (from TASKS.md)

<!-- Generated from TASKS.md by `./gradlew syncDocTasks` — edit tasks there, not here. -->

- [ ] #19 **Deterministic command-log + replay** (elaborates #15's "deterministic replay"; needs its own `docs/` design doc before code — next free number; `0002`–`0004` are taken). Goal: log every player command + (optionally) plugin/system actions so a bug session is byte-reproducible. **Analysis done:** sim is already near-deterministic — seeded `Random(42)` (all RNG routed through it) + fixed-timestep `tick()` (ignores wall-clock `dt`; ECS gets `world.update(1f)`). So a session = **seed + ordered command stream pinned to tick**; replay regenerates all system/particle output, so logging plugin actions is only a *verification* trace, not needed to reproduce. The "commands" = `GameEngine`'s mutation surface: `startGame`/`proceedToNextLevel`/`togglePause`/`returnToMenu`/`setControlMode`, per-frame `updateKeyboardInputs`/`updateTouchTarget` + the tilt read inside `tick()`, `triggerRoll`, and `tick()` itself — funneled in from `GameScene.kt` (updater/touch/keys) + `main.kt` (stage keys). **3 gaps to close:** (1) tilt is read live from the sensor inside the tick → must record per-tick as data; (2) event-driven cmds (roll/pause/touch) need a tick stamp to preserve ordering; (3) cross-platform float math (`sin`/`sqrt`) isn't bit-identical JVM/JS/Wasm → honest guarantee is *same-platform* replay. **Open forks (parked — ask before building):** (a) refactor depth: full sealed `GameCommand` + `engine.dispatch()` *[leaning yes — matches the "command pattern" framing + future plugin/VM kernel boundary; must preserve macOS HeldKey/touch hacks]* vs minimal tap recorder; (b) log scope: commands + cheap per-tick state checksum *[leaning — checksum localizes divergence, perf-safe]* vs commands-only vs full per-system trace *[avoid — perf gate]*; (c) replay form: headless harness/test *[leaning]* vs in-app ghost replay vs recorder+format-only. **Perf gate:** recorder must be off by default + delta-encode (log on change), near-zero cost when disabled; A/B it. Log sink is multiplatform (expect/actual): JVM→file, web→console/download, Android→logcat+files.
- [ ] #18 **iOS target** — add `targetIos()` + `iosMain/Platform.kt` (CANVAS_HEIGHT). Not wired yet; needs a Mac with Xcode
- [x] **PR #4** — native Gradle tooling (`buildSrc`: `openPr` + `renderDocs`) → **merged**

<!-- TASKS:auto END -->
