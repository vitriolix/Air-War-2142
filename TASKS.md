# Tasks & Roadmap

The durable task board — the canonical replacement for the ephemeral in-session task
list. Edit `[ ]` → `[x]` as things land. Details for the decisions live in the design
docs and project memory; this is the actionable index.

> Legend: `[x]` done · `[ ]` open · `[~]` shelved (decided to defer) · `[›]` doing
>
> **Per-doc task lists are generated from this file.** Tag an item with a trailing
> `<!-- docs:NNNN -->` (one or more doc numbers, comma-separated) and run
> `./gradlew syncDocTasks` to push it into that doc's "Tasks" block. Edit tasks **here**, never
> inside a doc's generated block.

## Doing

- [›] **PR #24 — Controller Preferences round-trip (in flight).** Phase 1 done: brief (`design/requests/controller-prefs.md`) authored and pushed to the `Air War 2142` claude.ai/design project. Waiting on Phase 2 (Design generates `screens/controller-prefs.html`). Phases 3–5: pull back via DesignSync → new tokens in `design-tokens.json` → `ControllerPrefsScene.kt` → Settings↔Controller navigation wiring → headed verification with real gamepad input.
- [ ] **resvg SVG-sprite bake pipeline** — bake sprite SVGs → atlas PNG via resvg (decided **resvg-only**). Provisioning the toolchain and the prove-glow step are **held until this lands**.
- [ ] **Capture Game Over / Victory screens** — `spec.html`'s baseline is missing these two; `captureScreens` only drives Menu/HUD/Paused/Settings. Specs exist (`PROMPT.md` §5.4 Game Over / §5.5 Victory) but no real screenshots, so they round-trip as text-only. Needs driving the live game into each end state (die → "PLANE DESTROYED"; clear a level → "VICTORY!") and capturing headed + 1:1 (per the SDF/SwiftShader rule). Then add `5-gameover.png` / `6-victory.png` to `design/screens/` and embed them in `spec.html`.
- [ ] **Code↔Design motion/animation round-trip** — sibling to the flow/UX-change gap (PR #19): the round-trip carries **static UI only**, so motion can't be defined or transported on either side today. Gaps: `design-tokens.json` has no timing/easing/duration section; the `PROMPT.md` §7 deliverable + §6 KorGE-mapping are static-primitive-only (no tween/timeline row); `spec.html` is screenshots (structurally can't show motion); `designImport` has no notion of porting an animation. Code-side there are **no** KorGE tweens/`animator`/`Easing` — all motion is the fixed-timestep engine `tick()` + per-frame `addUpdater` (deliberate: determinism/replay #19, perf gate). To close it: (1) a **motion section in `design-tokens.json`** (named durations + easing curves + per-interaction transitions); (2) `PROMPT.md` motion brief + a **CSS→KorGE mapping** (CSS easing → korlibs `Easing`; `@keyframes`/`transition` → `view.tween` / `animator{}`); (3) a **motion-capable spec artifact** — live HTML demo or short GIF/video, since screenshots can't; (4) guardrail: declarative animation for **UI chrome only** (menu/overlay transitions, button + focus-caret feedback); **gameplay motion stays in the fixed-timestep sim** (determinism + perf hard rules). **Design goal:** animations should, where possible, be defined in the **same modular fashion as other assets/actors** — namespaced, data-defined plugin assets aligned with the plugin/ECS architecture (cf. #8 sprites = pre-baked namespaced atlases, #10 shaders = KorGE fragment DSL), so a mod/plugin can ship animations the way it ships sprites. Likely warrants its own **animation/motion format** decision alongside the #9/#10/#11 content-format cluster; see the `plugin-architecture` memory + `docs/0003`.
- [ ] **Code↔Design flow/UX-change round-trip** — the `design/` handoff covers *per-screen visuals + tokens* in both directions (`designExport`/push, `designImport`), but **not UX-flow changes**: when either side changes navigation/flow (insert a difficulty-select screen, add a Restart to Paused, reorder a transition), there's no artifact to record it against and no signal to the other side. Two gaps to close: (1) a **flow-map artifact** in `design/` (e.g. `flow.md` — screens as nodes, transitions + triggers as edges) held to the same source-of-truth discipline as `design-tokens.json`, cross-linked from `PROMPT.md`/`README.md` and surfaced by `renderDocs`; (2) a lightweight **change-note convention** so a flow change on either side is flagged on the next sync (a `CHANGELOG`/`flow:`-style annotation in the bundle), since the round-trip is batch export/import with no live agent↔agent Q&A. Scope is *flow/navigation*, distinct from the visual per-screen specs already in §5.
- [x] **"Text-rendering artifact" was a software-GL rendering artifact in the capture tooling, NOT a font/game/KorGE bug — resolved 2026-06-16.** Thin vertical slivers ("FLIGHTi CONTiROLiS", "STiEERiING") appeared ONLY in `scripts/capture-screens.js` output, never in the real game. **Isolated root cause:** rendering KorGE's SDF text under **SwiftShader (software GL)** — i.e. headless Chromium — not the font, the glyphs, or the screen scale. KorGE's SDF anti-aliasing (`SDFShaders.opAARev` = `clamp(d/fwidth(d)+0.5)`) derives the AA width from screen-space derivatives (`fwidth`); SwiftShader computes those differently from hardware GL, and on ~1-texel-thin stems the threshold clips → slivers. (The default font is *Sani Trixie Sans* via `DefaultTtfFontAsBitmap = DefaultTtfFont.lazyBitmapSDF`, but the font is incidental — any SDF text shows it under software GL.) **Variable isolation:** real GPU @ integer 1:1 = clean; SwiftShader @ integer 1:1 = slivered; SwiftShader @ old 1.2× = slivered. So the non-integer resample (old 600×900@DSF2 capture) was a **red herring**, not a cause — software GL alone does it. *Cause was mis-called three times before this isolation (font glyphs → "SDF path in-game" → "software GL + resample"); the game was always fine.* **Not a KorGE defect:** the AA shader is the standard textbook technique and renders perfectly on real GPUs — the artifact is SwiftShader's `fwidth` precision (environment limitation). At most a low-priority upstream *robustness enhancement* (derivative-free AA width from a texel-size uniform); not worth filing. **No font swap / no SDF change needed.** **Fix landed:** `capture-screens.js` defaults to headed + 1:1 native 1000×1500; `HEADLESS=1 DSF=1 W=1000 H=1500` reproduces the slivers under software GL (that path is for non-visual smoke checks only); `design/screens/*.png` re-captured clean. (Both via the companion design-round-trip PR.) **Note:** the earlier "Verified: reproduces in-game" claim was wrong — judged from headless screenshots, not the live headed app.

## Roadmap — incremental ECS/plugin migration

The engine is being re-architected so plugins are the core structure (moddable). Built
as a sequence of small, perf-gated PRs, game playable at every merge. See
[`docs/0001`](docs/0001-ecs-kernel-and-core-migration.md).

- [x] **PR-A** — ECS kernel + migrate particles (PR #2) <!-- docs:0001 -->
- [ ] **PR-B** — plugin host + turn existing systems into *registered* systems <!-- docs:0001,0002 -->
- [ ] **PR-C…N** — migrate one subsystem per PR: background → bullets → enemies/spawn → player + input-mapping (lift input layer **intact**) → collision → scoring → power-ups → HUD <!-- docs:0001 -->
- [ ] **EMP** — first dogfooded plugin (compiled first-party; no interpreter needed). Mock spec written; gameplay forks still open (see below) <!-- docs:0002,0003 -->
- [ ] Scripting VM / interpreter for 3rd-party plugins (separate, later track) <!-- docs:0002 -->

## Architecture decisions (settled — see `plugin-architecture` memory)

- [x] #1 Plugin model = **B**: data + Kotlin-flavored sandboxed VM (3rd-party) + real-Kotlin compiled-in (first-party) <!-- docs:0002 -->
- [x] #2 **Everything is a plugin**; engine = thin kernel; base game = plugins, peer to mods <!-- docs:0002 -->
- [x] #3 **Incremental** migration; lift input layer intact; no backward-compat burden <!-- docs:0001,0002 -->
- [x] #4 Kernel↔plugin boundary; ECS; standard-lib blessed components <!-- docs:0001,0002 -->
- [x] #5 API contract: API-level + semver; in-DSL manifest; namespacing; `wrap`/`replace` (collision = hard error) <!-- docs:0002 -->
- [x] #6 Distribution = bundled-only near-term (drop folder/zip into `plugins/`); Settings feature-flags off by default <!-- docs:0002 -->
- [x] #7 Sandbox guards device/app, not gameplay; trust via curation <!-- docs:0002 -->
- [x] #8 Sprites = pre-baked namespaced atlases (PNG + `sprites.txt`) <!-- docs:0003 -->
- [x] #13 DSL: strict Kotlin subset, dynamic runtime + static-check via real-Kotlin compile <!-- docs:0002 -->

## Open decisions (shelved — revisit when the slice comes up)

- [~] #9 Maps/paths format — lean: DSL content + standard path primitives in `core.*` <!-- docs:0002 -->
- [~] #10 Shader format — lean: KorGE fragment DSL (not raw GLSL) <!-- docs:0003 -->
- [~] #11 Sound/music format — lean: namespaced audio files, event-hooked <!-- docs:0002 -->
- [~] #12 EMP as first dogfooded plugin — also still-unanswered EMP **gameplay** forks: economy (repurpose SCREEN_BOMB vs new), stun behavior (freeze vs stop-fire), distortion scope <!-- docs:0002,0003 -->

## Constraints & workstreams

- ⚙️ **Perf gate** *(always-on constraint — enforced as a hard rule in `CLAUDE.md`, not a checkbox)* — no migration step may regress base-game frame time vs the measured baseline (web median 3.9ms / p95 7.3ms). A/B each step; keep render batching/pooling; allocation-free hot paths; typed component storage; check Low Power Mode before measuring.
- [ ] #15 **ECS dev-tooling** — entity inspector, system trace/profiler, deterministic replay, per-system toggles; **VM error reporting = hard requirement** <!-- docs:0001,0002 -->
- [ ] #19 **Deterministic command-log + replay** (elaborates #15's "deterministic replay"; needs its own `docs/` design doc before code — next free number; `0002`–`0004` are taken). Goal: log every player command + (optionally) plugin/system actions so a bug session is byte-reproducible. **Analysis done:** sim is already near-deterministic — seeded `Random(42)` (all RNG routed through it) + fixed-timestep `tick()` (ignores wall-clock `dt`; ECS gets `world.update(1f)`). So a session = **seed + ordered command stream pinned to tick**; replay regenerates all system/particle output, so logging plugin actions is only a *verification* trace, not needed to reproduce. The "commands" = `GameEngine`'s mutation surface: `startGame`/`proceedToNextLevel`/`togglePause`/`returnToMenu`/`setControlMode`, per-frame `updateKeyboardInputs`/`updateTouchTarget` + the tilt read inside `tick()`, `triggerRoll`, and `tick()` itself — funneled in from `GameScene.kt` (updater/touch/keys) + `main.kt` (stage keys). **3 gaps to close:** (1) tilt is read live from the sensor inside the tick → must record per-tick as data; (2) event-driven cmds (roll/pause/touch) need a tick stamp to preserve ordering; (3) cross-platform float math (`sin`/`sqrt`) isn't bit-identical JVM/JS/Wasm → honest guarantee is *same-platform* replay. **Open forks (parked — ask before building):** (a) refactor depth: full sealed `GameCommand` + `engine.dispatch()` *[leaning yes — matches the "command pattern" framing + future plugin/VM kernel boundary; must preserve macOS HeldKey/touch hacks]* vs minimal tap recorder; (b) log scope: commands + cheap per-tick state checksum *[leaning — checksum localizes divergence, perf-safe]* vs commands-only vs full per-system trace *[avoid — perf gate]*; (c) replay form: headless harness/test *[leaning]* vs in-app ghost replay vs recorder+format-only. **Perf gate:** recorder must be off by default + delta-encode (log on change), near-zero cost when disabled; A/B it. Log sink is multiplatform (expect/actual): JVM→file, web→console/download, Android→logcat+files. <!-- docs:0001,0004 -->
- [ ] #16 Interpreter tech stack (reference) — antlr-kotlin (Wasm-proven parse) + hand-rolled tree-walker; luak/TENUM refs <!-- docs:0002 -->
- [~] #17 Plugin "store"/registry — deferred (much later); iOS App Store 2.5.2 means curated bundled-only there <!-- docs:0002 -->
- [ ] #18 **iOS target** — add `targetIos()` + `iosMain/Platform.kt` (CANVAS_HEIGHT). Not wired yet; needs a Mac with Xcode <!-- docs:0004 -->
- [x] #20 **`pruneBranches` interactive prompt — verified + Gradle-TTY limitation found.** The keep/delete/log prompt works when the script is run **directly** (`scripts/prune-branches.sh` → chose `d`, force-deleted an unmerged branch ✓). But **`./gradlew pruneBranches` can never prompt** — the Gradle daemon has no controlling terminal, so `/dev/tty` is unavailable even from a real shell; via Gradle it cleans merged branches and keeps unmerged ones. Fix landed: actionable no-TTY message, README/scripts docs note the split, and the `branches:prune` npm shim now calls the script directly (interactive). Merged-delete, unmerged-keep, and current-branch auto-switch were already verified.

## Done

- [x] **PR #2** — ECS kernel + particle migration → **merged** (first ECS slice on `main`) <!-- docs:0001 -->
- [x] **PR #3** — `pr-open` bash helper → closed, superseded by native `openPr` in #4
- [x] **PR #4** — native Gradle tooling (`buildSrc`: `openPr` + `renderDocs`) → **merged** <!-- docs:0004 -->
- [x] **PR #5** — project docs (README, TASKS, CLAUDE) → **merged**
- [x] **PR #14** — rename project to Air War 2142 (`com.vitriolix.airwar2142`); title screen "RETRO CLONE" → "AIR WAR" → **merged**
- [x] **PR #15** — Claude Design round-trip: `design/` workspace + `designExport`/`designImport` tooling + docs surfacing → **merged** (follow-up scope still tracked in **Doing**)
- [x] **PR #16** — Board: "text-rendering artifact" was a software-GL capture-tooling bug, not a font/KorGE bug → **merged**
- [x] **PR #17** — Docs publishing + CI: Pages `/docs/` guides site, JSON/YAML rendering, doc-sync PR gate → **merged** (guides live at `https://vitriolix.github.io/Air-War-2142/docs/`)
- [x] **PR #18** — Dokka: space-free `moduleName` (`Air-War-2142`) for link-safe doc paths → **merged** (`main.kt` package move backed out: KorGE hardcodes `MainKt` as JVM entry point)
- [x] **PR #19** — Board: track Code↔Design flow/UX-change round-trip gap → **merged**
- [x] **PR #20** — Import shipped fonts from Claude Design: Wallpoet (title) + Chakra Petch (content) as TTF; `render/Fonts.kt`; applied across all three scenes → **merged**
- [x] **PR #21** — design: add Code→Design drop history log (superseded by broader DESIGN_HISTORY in PR #23) → **merged**
- [x] **PR #22** — Board: track Code↔Design motion/animation round-trip gap → **merged**
- [x] **PR #23** — design: per-release Design history + bundle snapshots (`DESIGN_HISTORY.md` + `design/design-history/seed/`) → **merged**
- [x] **Claude Design round-trip** — `design/` handoff workspace + `designExport`/`designImport` tooling shipped (PR #15); the spec bundle is now seeded into the `Air War 2142` claude.ai/design project. Remaining follow-ups extracted as their own **Doing** tasks (resvg bake pipeline; Game Over / Victory capture).
