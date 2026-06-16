# Air War 2142

A top-down arcade shoot-'em-up in the style of classic 1942, written in **Kotlin Multiplatform** on the **KorGE** game engine. Runs on desktop (JVM), Android, and the web (JS + WebAssembly); iOS is planned.

> **Status:** playable. Being incrementally re-architected toward a plugin/ECS engine so the whole game is moddable — see [Documentation](#documentation).

This README is the project's landing page and docs index. Render it (and the design docs) to a browsable HTML index with `./gradlew renderDocs`.

## Tech stack

| Area | Choice |
|---|---|
| Language | **Kotlin 2.0.21**, Multiplatform |
| Engine | **KorGE 6.0.0** (`com.soywiz.korge`) + korlibs (korim / korma / korio) |
| Targets | JVM (desktop), Android, JS (browser), **WebAssembly** (wasmJs); iOS planned |
| Build | Gradle (Kotlin DSL), **JDK 21** (required by KorGE 6) |
| Rendering | Build-time vector→**sprite-atlas** bake; batched `Image` draws; ECS-driven (in progress) |
| Tooling | `scripts/` bash helpers exposed as Gradle tasks (+ thin npm shims); **Playwright** for web verify/perf; **pandoc** for docs; **GitHub CLI** (`gh`) for PRs |

## Quick start

Prereqs: **JDK 21**. Some tasks also want Node, `gh`, and `pandoc`.

```bash
./gradlew playJvm          # desktop window
./gradlew playWeb          # serve + open the JS build in a browser
./gradlew playWasm         # serve + open the WebAssembly build
./gradlew playAndroid      # install + launch on a device/emulator
```

Every task has an `npm run …` shim (e.g. `npm run play:web`). The Gradle tasks are canonical.

## Documentation

Design docs live in [`docs/`](docs/) — numbered, ADR-style (what's changing, why, how it's verified). Build a browsable HTML index of this README + every doc:

```bash
./gradlew renderDocs       # or: npm run docs   (renders to build/docs/, opens this README)
```

| Doc | About |
|---|---|
| [TASKS.md](TASKS.md) | Roadmap & task board — current status, what's next, and in-flight PRs |
| [0001 — ECS Kernel & Core Migration](docs/0001-ecs-kernel-and-core-migration.md) | Incremental migration to an ECS/plugin engine; first slice = kernel + particle migration |
| [0002 — Plugin & ECS Architecture](docs/0002-plugin-and-ecs-architecture.md) | Reference: the plugin/kernel model, ECS objects/components/events, the contract, and what's built vs. designed |
| [0003 — Rendering & Sprite Atlas](docs/0003-rendering-and-sprite-atlas.md) | How the game draws: build-time vector→atlas bake (shipped, the perf win) + the EMP ripple-shader Phase 2 spec |
| [0004 — KorGE Port & Platform Notes](docs/0004-korge-port-and-platform-notes.md) | Build setup (JDK 21), KorGE API gotchas, the cross-platform input hacks, and the web perf investigation |
| [scripts/README.md](scripts/README.md) | Task-command reference |
| [CLAUDE.md](CLAUDE.md) | Agent working notes — hard rules, perf gate, and durable-context pointers |

The full plugin/ECS architecture (plugins-as-data with a Kotlin-flavored VM, base-game-as-plugin, ECS, incremental migration) is mapped in [0002](docs/0002-plugin-and-ecs-architecture.md) — including what's built today vs. designed — and fleshed out across the design docs as each slice lands.

**API reference (Dokka).** Browse the Kotlin API docs online — published to GitHub Pages on every push to `main` by [`.github/workflows/api-docs.yml`](.github/workflows/api-docs.yml):

➡️ **<https://vitriolix.github.io/Air-War-2142/>**

Or render them locally to HTML:

```bash
./gradlew renderApiDocs    # or: npm run docs:api   (renders to build/api/)
```

…then open `build/api/index.html` in a browser. *(Local output lives under the gitignored `build/`, so run the task first.)*

## Tasks

Canonical commands are Gradle tasks in the **`game`** group; the `npm run <x>` scripts just call `./gradlew`. List them all: `./gradlew tasks --group game`.

**Play** — `playJvm` · `playWeb` / `playWebHeadless` · `playWasm` / `playWasmHeadless` · `playAndroid` · `playNative`

**Test** — `testJvm` · `testWeb` · `testWasm` · `testAndroid` · `testAll`

**Dev & git**

| Task | Does |
|---|---|
| `renderDocs` | Render Markdown docs to HTML and open this README as the index |
| `syncDocTasks` | Regenerate each doc's Tasks block from `TASKS.md` (single source); `--check` fails if stale |
| `checkDocTasks` | Backstop: fail if any doc's Tasks block is stale (run by `tidyGit`/`releaseCheckGit`) |
| `installGitHooks` | Point git at `scripts/hooks` — installs the pre-commit hook that auto-syncs doc Tasks blocks |
| `renderApiDocs` | Render the Kotlin API reference (Dokka HTML) to `build/api/index.html` |
| `webConsole` | Boot the web build and stream its browser console + errors to the terminal |
| `killServers` | Stop the JS/Wasm dev servers and `runJvm` |
| `tidyGit` | Verify a clean working tree + push state |
| `pruneBranches` | Delete local branches whose upstream is gone **and** merged (keeps unmerged). For the interactive keep/delete prompt on unmerged branches, run `scripts/prune-branches.sh` directly — Gradle's daemon has no TTY, so the task can't prompt |
| `createPr` | Push the current branch and open a GitHub PR |
| `openPr` | Open a PR's page in the browser (current branch, or a number) |
| `bakeAtlas` | Re-bake the sprite atlas from vector art (`:composeApp:bakeAtlas`) |

**Release** — `releaseCheckGit` · `releaseTest` · `releaseBuild` · `releaseVersion` · `releaseBranch` · `releaseTag` · `release` (ordered chain).

## Build notes

- **JDK 21 is required.** KorGE 6 needs JVM ≥ 21, and the plugin can't yet parse Java 26. The path is pinned in `gradle.properties` (`org.gradle.java.home`) — adjust per machine, or point `JAVA_HOME` at a 21.
- **Targets** are declared in `composeApp/build.gradle.kts` `korge { … }` (`targetJvm/targetAndroid/targetJs/targetWasmJs`). iOS (`targetIos()`) is planned, not yet wired.
- **Logical canvas height** (`CANVAS_HEIGHT`) is per-platform: 1500 on JVM/JS/Wasm, **2200 on Android** (taller phone aspect).
- **The sprite atlas is committed.** `sprites.png` / `sprites.txt` under `composeApp/src/commonMain/resources` are generated by `./gradlew :composeApp:bakeAtlas` from the vector art in `render/Shapes.kt`; re-run **and commit** when the art changes.
- **First JS build** may fail on `:kotlinStoreYarnLock` ("Lock file was changed") — run `./gradlew kotlinUpgradeYarnLock` once.

## Hacks & workarounds

Hard-won fixes worth understanding *before* you "tidy them up":

- **macOS keyboard (AWT):** every key press arrives as a DOWN immediately followed by an UP in the *same* frame, so naive per-frame sampling always reads "released" and the plane never moves. `HeldKey` (in `GameScene`) defers a release ~80 ms; a following DOWN cancels it. Don't revert to per-frame polling.
- **`~` / grave key** maps differently per backend: `Key.BACKQUOTE` (JVM), `Key.GRAVE` (Android), `Key.UNKNOWN` + keyCode 192 (JS). The debug-overlay toggle matches all of them.
- **Android canvas height:** the engine's `playHeight` must equal `CANVAS_HEIGHT` (2200 on Android, *not* a hardcoded 1500), or islands/clouds pop ~700 px above the real bottom and the plane can't reach the lower screen.
- **KorGE 6 `onClick` is no longer `suspend`** → use `onClickSuspend(views.coroutineContext) { … }` for handlers that navigate scenes.
- **`@JvmInline` is JVM-only.** Using it in `commonMain` breaks the JS/Wasm compile (`Unresolved reference 'JvmInline'`). Use a plain `data class` for cross-platform value-like types (e.g. the ECS `Entity`).
- **`ShapeBuilder.ellipse(center, radius)`** takes its 2nd arg as a **radius** (`Size`), not a diameter. The art's `ellipse(cx,cy,rx,ry)` helper passes `Size(rx*2, ry*2)`, so those ellipses render at 2× radius — baked into the current look; keep it for parity.
- **Perf testing — check Low Power Mode first** (`pmset -g | grep lowpowermode`, want `0`): LPM throttles the browser to ~30 Hz and invalidates web numbers. **Headless Chromium also throttles rAF (~15 fps)** on its own, so use **headed** Playwright for representative load. Per-frame JS time measured *inside* the rAF callback is valid under throttling; wall-clock fps is not.
- **Two browsers during web tests:** `jsBrowserDevelopmentRun` auto-opens the dev URL in your *default* browser once at startup, while Playwright drives its own bundled **"Chrome for Testing"** — different windows. With `--disable-backgrounding-occluded-windows` / `--disable-renderer-backgrounding`, the Playwright window keeps running full-speed even when covered or unfocused (just don't *minimize* it).
