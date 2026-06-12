# 0003 — Rendering & the Sprite Atlas

**Status:** Phase 1 **shipped** (build-time vector→atlas bake, on `main`). Phase 2 (EMP ripple
shader + enemy stun) **spec locked, not built**.
**Related:** [`0002` — Plugin & ECS Architecture](0002-plugin-and-ecs-architecture.md) (the EMP
plugin reframes Phase 2) · [`0004` — KorGE Port & Platform Notes](0004-korge-port-and-platform-notes.md)
(the render-perf history that motivated this) · README "Hacks & workarounds".

> Retrospective + forward spec for how the game draws. The headline: **art is authored as vector
> shapes, baked to a sprite atlas at build time, and drawn as batched `Image`s** — which took web
> frame time from ~10 ms to ~3.9 ms. Phase 2 adds shader effects on top of that sprite base.

---

## 1. Why — per-frame tessellation was the bottleneck

The original renderer drew every shape with `Graphics.updateShape { … }` **every frame**, which
re-tessellates the vector on the CPU. With many shapes that's brutal on web (see
[`0004` §perf](0004-korge-port-and-platform-notes.md#7-performance-investigation)): the player
alone (~20 fill ops) cost ~14 ms/frame to re-tessellate, and enemy-spawn / power-up
re-tessellation produced long tail hitches.

Two complementary fixes were chosen together: **atlas sprites** (bake the vectors once, draw as
textured quads) + **shaders** (for effects that sprites can't do, like the EMP ripple).

---

## 2. Phase 1 — the build-time bake pipeline ✅

The pipeline rasterizes the vector art **once at build time** into a single atlas page, committed
to the repo, and loaded as batched `Image`s at runtime. **No tessellation in the frame loop.**

```
 BUILD TIME (./gradlew :composeApp:bakeAtlas)                 RUNTIME (per frame)
 ───────────────────────────────────────────                 ───────────────────
 render/Shapes.kt            jvmMain/bake/BakeAtlas.kt         render/SpriteAtlas.kt
 (korim ShapeBuilder,   →    rasterize each shape @3×     →    load atlas once,
  NO korge-view dep,         renderWithHotspot(native=false)   build anchored Image
  so it's bakeable           pack → MutableAtlas (1 page)      (anchor=hotspot,
  headless)                  write resources/sprites.png       scale=1/3)
       │                           + sprites.txt                      │
       │                     (name x y w h hotspotX hotspotY)         ▼
       └── shared AtlasSpec (commonMain): slice names,          GameScene: position/scale
           ref sizes, BAKE_SCALE ───────────────────────────►   pooled Images each frame
                                                                 (no updateShape)
```

**Key facts:**
- The shape functions (`drawPlayer`, `drawEnemyCentered`, …) live in `commonMain/.../render/Shapes.kt`
  and depend **only on korim's `ShapeBuilder`** (no korge-view types) — that's what makes them
  rasterizable headless in `jvmMain` with no AWT.
- Bake = `buildShape{}.renderWithHotspot(scale = 3, native = false)` → cropped `Bitmap32` + hotspot
  (center), packed into one `MutableAtlas` page (`GROW_IMAGE`), written as `sprites.png` (2048²,
  ~154 K, 13 slices) + `sprites.txt`.
- `AtlasSpec` (commonMain) is the shared contract: slice names, reference sizes, and `BAKE_SCALE`,
  used by both the baker and the loader.
- **Assets are committed** (`sprites.png` / `sprites.txt` under `composeApp/src/commonMain/resources`).
  This was a deliberate choice (commit-on-demand over bake-in-build): **re-run `bakeAtlas` and
  commit whenever the vector art changes.**
- `SpriteAtlas.kt` loads the atlas once and builds anchored `Image`s (anchor = hotspot/size,
  `scale = 1/3` to undo the 3× bake).

**What stayed vector** (drawn once, not per-frame, so cheap): the **ocean** gradient (drawn once
at scene init) and **particles** (a white circle at radius 1, tinted via `colorMul` + scaled per
frame — GPU-only, no tessellation).

### 2.1 Render strategy by object (current `GameScene`)

| Object | How it's drawn |
|---|---|
| Ocean | `Graphics` drawn **once** at init, never updated |
| Islands / clouds | atlas `Image`, scaled per object vs a reference radius/scale, repositioned per frame |
| Player | 3 baked variants (normal / escorts / roll); visibility-swap on shape change, container transforms per frame |
| Enemies | atlas `Image` created on spawn; HEAVY_FIGHTER/BOSS get a `solidRect` HP bar (`fg.scaleX = hp`) |
| Bullets | two `Image` pools (30 player, 50 enemy); hide-all then show+position active slots |
| Power-ups | atlas `Image` pool, scaled per pickup |
| Particles | 120 `Graphics` pool (white circle r=1), per frame: `scaleX/Y = radius`, `colorMul` (since migrated to ECS — see [`0001`](0001-ecs-kernel-and-core-migration.md)) |

---

## 3. The payoff — measured web A/B

Headed Chromium, Low Power Mode **off** on both runs (mandatory — see
[`0004` §7](0004-korge-port-and-platform-notes.md#7-performance-investigation)), identical 12 s
drive, vectors → sprites:

| Metric (per-frame JS, `/tmp/measure.js`) | Vectors (before) | Sprites (after) | Change |
|---|---|---|---|
| Throughput (uncapped) | 48 fps | **199 fps** | 4.1× |
| Median frame | 10.3 ms | **3.9 ms** | 2.6× |
| p95 | 320 ms | **7.3 ms** | ~44× |
| p99 | 855 ms | **15.1 ms** | ~57× |
| Janky frames (>16.7 ms) | 40.3 % | **0.8 %** | — |

**The tail was the story.** Median improved 2.6×, but p95/p99 improved ~40–60× — the baseline's
per-frame enemy/power-up re-tessellation produced multi-hundred-millisecond hitches that sprites
eliminated entirely. (These **3.9 ms / 7.3 ms** numbers are now the project's perf-gate baseline —
see `CLAUDE.md`.)

> Measurement tool: `/tmp/measure.js` (Playwright) wraps `requestAnimationFrame` to time the
> per-frame JS = draw-call dispatch cost. **Headed is required** — headless Chromium throttles rAF
> to ~15 fps and uses software WebGL.

---

## 4. Gotchas baked into the look

- **`ShapeBuilder.ellipse(center, radius: Size)`** takes its 2nd arg as a **radius**, not a
  diameter. The art's `ellipse(cx, cy, rx, ry)` helper passes `Size(rx*2, ry*2)`, so those
  ellipses render at **2× radius**. This is baked into the current look — **keep it for parity.**
- **Atlas is 2048² but mostly empty** (~16 MB VRAM) — could be cropped to a tighter page.
- **Cloud premult** round-trips cleanly (no darkening) because parity is by construction: the
  baked shapes use the identical shape code the vectors did.

---

## 5. Phase 2 — EMP bomb + ripple shader + enemy stun ⬜ (spec locked, do later)

> **Cross-ref:** [`0002` §7](0002-plugin-and-ecs-architecture.md#7-the-plugin-model--dsl-1-13)
> reframes this as the **EMP plugin** (a `Stun` component + `StunSystem`). The *rendering* spec
> below is the same; the *gameplay forks* (energy economy, trigger, freeze-vs-stop-fire) are still
> open (`TASKS.md` #12).

- **Trigger:** the player fires an EMP bomb → a radial **distortion ripple** (custom
  `ShaderFilter`, `FragmentShader{}` DSL, cross-platform) that **emanates as a growing circle
  centered on the player.**
- **Gameplay:** as the expanding wavefront **passes each enemy**, that enemy enters a **stunned
  state for a few seconds** (per-enemy hit test against the wavefront radius vs. time).
- **Stun visuals:** stunned enemy sprite **fades to greyscale** (cheap color-matrix/shader); at
  the moment the wave hits, the enemy **shakes briefly** (a transient position offset — a hit
  reaction). Both are cheap *now that objects are sprites*.

Engine support still needed: the EMP trigger, wave-radius-vs-time, the per-enemy wavefront hit
test, and the stun timer + greyscale + shake. KorGE shader recon is done and green: `ShaderFilter`
(abstract) + the `FragmentShader{}` DSL, with the built-in `WaveFilter` as a working reference;
filters render-to-texture internally.

---

## 6. Parked follow-ups

- `MenuScene` still has a redundant private `circle` (dedupe vs `render.circle`).
- `drawPowerupsCentered` is now unused (sprites replaced it).
- Crop the atlas page (2048² → tight) to cut VRAM.
- The player exhaust flame no longer flickers (drawn statically as part of the shape-swap fix); a
  cheap transform-animated view could restore it — judged not worth it.

---

<!-- TASKS:auto START -->
## Tasks (from TASKS.md)

<!-- Generated from TASKS.md by `./gradlew syncDocTasks` — edit tasks there, not here. -->

- [ ] **EMP** — first dogfooded plugin (compiled first-party; no interpreter needed). Mock spec written; gameplay forks still open (see below)
- [x] #8 Sprites = pre-baked namespaced atlases (PNG + `sprites.txt`)
- [~] #10 Shader format — lean: KorGE fragment DSL (not raw GLSL)
- [~] #12 EMP as first dogfooded plugin — also still-unanswered EMP **gameplay** forks: economy (repurpose SCREEN_BOMB vs new), stun behavior (freeze vs stop-fire), distortion scope

<!-- TASKS:auto END -->
