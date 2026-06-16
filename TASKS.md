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

- [›] **Claude Design round-trip** — `design/` handoff workspace + `designExport`/`designImport` Gradle tasks (buildSrc `Design.kt`). Reproduce-then-refine the UI in claude.ai/design. Built: `PROMPT.md` brief; `design-tokens.json` (authoritative, transcribed from `scenes/*.kt`); `spec.html` faithful baseline = **real screenshots** (`./gradlew captureScreens`) + real sprite-atlas gallery; old idealized mockup split out as `refinement-mockup.html` (labeled NOT-current); `renderDocs` surfaces the whole bundle as a browsable shared spec. **Still open:** capture Game Over / Victory screens (need gameplay); resvg SVG-sprite bake pipeline (decided resvg-only; provision + prove-glow held until this lands).

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
