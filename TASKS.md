# Tasks & Roadmap

The durable task board — the canonical replacement for the ephemeral in-session task
list. Edit `[ ]` → `[x]` as things land. Details for the decisions live in the design
docs and project memory; this is the actionable index.

> Legend: `[x]` done · `[ ]` open · `[~]` shelved (decided to defer) · `[›]` in flight

## Roadmap — incremental ECS/plugin migration

The engine is being re-architected so plugins are the core structure (moddable). Built
as a sequence of small, perf-gated PRs, game playable at every merge. See
[`docs/0001`](docs/0001-ecs-kernel-and-core-migration.md).

- [x] **PR-A** — ECS kernel + migrate particles (PR #2)
- [ ] **PR-B** — plugin host + turn existing systems into *registered* systems
- [ ] **PR-C…N** — migrate one subsystem per PR: background → bullets → enemies/spawn → player + input-mapping (lift input layer **intact**) → collision → scoring → power-ups → HUD
- [ ] **EMP** — first dogfooded plugin (compiled first-party; no interpreter needed). Mock spec written; gameplay forks still open (see below)
- [ ] Scripting VM / interpreter for 3rd-party plugins (separate, later track)

## Architecture decisions (settled — see `plugin-architecture` memory)

- [x] #1 Plugin model = **B**: data + Kotlin-flavored sandboxed VM (3rd-party) + real-Kotlin compiled-in (first-party)
- [x] #2 **Everything is a plugin**; engine = thin kernel; base game = plugins, peer to mods
- [x] #3 **Incremental** migration; lift input layer intact; no backward-compat burden
- [x] #4 Kernel↔plugin boundary; ECS; standard-lib blessed components
- [x] #5 API contract: API-level + semver; in-DSL manifest; namespacing; `wrap`/`replace` (collision = hard error)
- [x] #6 Distribution = bundled-only near-term (drop folder/zip into `plugins/`); Settings feature-flags off by default
- [x] #7 Sandbox guards device/app, not gameplay; trust via curation
- [x] #8 Sprites = pre-baked namespaced atlases (PNG + `sprites.txt`)
- [x] #13 DSL: strict Kotlin subset, dynamic runtime + static-check via real-Kotlin compile

## Open decisions (shelved — revisit when the slice comes up)

- [~] #9 Maps/paths format — lean: DSL content + standard path primitives in `core.*`
- [~] #10 Shader format — lean: KorGE fragment DSL (not raw GLSL)
- [~] #11 Sound/music format — lean: namespaced audio files, event-hooked
- [~] #12 EMP as first dogfooded plugin — also still-unanswered EMP **gameplay** forks: economy (repurpose SCREEN_BOMB vs new), stun behavior (freeze vs stop-fire), distortion scope

## Constraints & workstreams

- [ ] #14 **Perf gate** — every migration step must not regress vs measured baseline (web median 3.9ms / p95 7.3ms). Keep render batching/pooling; allocation-free hot paths; typed component storage. Check Low Power Mode first.
- [ ] #15 **ECS dev-tooling** — entity inspector, system trace/profiler, deterministic replay, per-system toggles; **VM error reporting = hard requirement**
- [ ] #16 Interpreter tech stack (reference) — antlr-kotlin (Wasm-proven parse) + hand-rolled tree-walker; luak/TENUM refs
- [~] #17 Plugin "store"/registry — deferred (much later); iOS App Store 2.5.2 means curated bundled-only there
- [ ] #18 **iOS target** — add `targetIos()` + `iosMain/Platform.kt` (CANVAS_HEIGHT). Not wired yet; needs a Mac with Xcode

## In flight

- [›] **PR #2** — ECS kernel + particle migration (awaiting merge)
- [›] **PR #3** — `pr-open` bash helper → **to be CLOSED** (superseded by native `prOpen`)
- [›] **PR #4** — render-docs + README → being reworked to **native Gradle tasks in `buildSrc`** (see `tooling-gradle-native-wip` memory for the exact resume checklist)
