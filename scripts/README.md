# Project task commands

The canonical commands are **Gradle tasks** (group `game`): run `./gradlew <task>`.
A thin **npm shim** mirrors them (`npm run <alias>` just calls `./gradlew <task>`), and the
shell-heavy ones are implemented by the `scripts/*.sh` files that the Gradle tasks wrap.

```
./gradlew tasks --group game     # list everything
```

> Gradle runs via `./gradlew`, pinned to JDK 21 in `gradle.properties` (KorGE 6 needs JDK 21+),
> so you don't need to set `JAVA_HOME`.

## Launch / play
| Gradle | npm shim | What |
|---|---|---|
| `./gradlew playJvm` | `npm run play:jvm` | Desktop app (`runJvm`). |
| `./gradlew playWeb` | `npm run play:web` | Build + serve web (JS), open browser. |
| `./gradlew playWebHeadless` | `npm run play:web:headless` | Serve web (JS), no browser (prints URL). |
| `./gradlew playWasm` | `npm run play:wasm` | Build + serve web (Wasm), open browser. |
| `./gradlew playWasmHeadless` | `npm run play:wasm:headless` | Serve web (Wasm), no browser (prints URL). |
| `./gradlew playAndroid` | `npm run play:android` | Install + launch on emulator/device. |
| `./gradlew playNative` | `npm run play:native` | (No KN desktop target → points you to `playJvm`.) |

## Tests
| Gradle | npm shim |
|---|---|
| `./gradlew testJvm` | `npm run test:jvm` |
| `./gradlew testWeb` | `npm run test:web` |
| `./gradlew testWasm` | `npm run test:wasm` |
| `./gradlew testAndroid` | `npm run test:android` |
| `./gradlew testAll` | `npm run test:all` |

## Housekeeping
| Gradle | npm shim | |
|---|---|---|
| `./gradlew webConsole` | `npm run web:console` | Boot the web build and stream its **browser** console + errors to the terminal (the web app is client-side; its logs live in the browser). For Wasm/headed: `scripts/web-console.sh wasm --headed`. Ctrl-C stops it. |
| `./gradlew tidyGit` | `npm run git:tidy` | Clean working tree + branch/push status. |
| `scripts/prune-branches.sh` | `npm run branches:prune` | Delete local branches whose upstream is gone **and** merged (ancestor of base or a merged PR — covers squash-merges). For **unmerged** branches it prompts keep/delete/log — so run it **directly** (or via the npm shim) for that prompt. `./gradlew pruneBranches` also works but **can't prompt** (the Gradle daemon has no TTY) → it cleans merged branches and keeps unmerged. `--dry-run` to preview. |
| `./gradlew killServers` | `npm run kill:servers` | Stop JS/Wasm dev servers + `runJvm`. |
| `./gradlew createPr` | `npm run pr:create` | Push branch + open a GitHub PR. For custom flags use the script directly: `scripts/create-pr.sh --draft`. |

## Git hooks
Run `./gradlew installGitHooks` once per clone — it sets `core.hooksPath=scripts/hooks`, enabling
the committed **`pre-commit`** hook. When a commit includes `TASKS.md`, the hook regenerates each
doc's "Tasks" block (`syncDocTasks`) and stages the result, so the generated blocks never drift.
Backstop: `tidyGit` and `releaseCheckGit` run `checkDocTasks`, which **fails** if any block is stale.

## Release prep (each step standalone; `release` runs them in order)
| Gradle | npm shim | |
|---|---|---|
| `./gradlew releaseCheckGit` | `npm run release:check-git` | Working tree is clean. |
| `./gradlew releaseTest` | `npm run release:test` | All tests. |
| `./gradlew releaseBuild` | `npm run release:build` | JVM jar + web prod bundle + Android release APK. |
| `./gradlew releaseVersion` | `npm run release:version` | Print app version. |
| `./gradlew releaseBranch` | `npm run release:branch` | Create/switch `release/<version>`. |
| `./gradlew releaseTag` | `npm run release:tag` | Tag `v<version>`. |
| `./gradlew release` | `npm run release:all` | **check-git → test → build**, ordered (`dependsOn` + `mustRunAfter`). |

## Layout
- `build.gradle.kts` (root) — defines the `game`-group tasks. Build/test/run tasks `dependsOn`
  the real `:composeApp` tasks; shell tasks `Exec` the scripts below.
- `package.json` — thin npm shim → `./gradlew`.
- `scripts/*.sh` — implementation for the shell-heavy tasks (`bash`-based; macOS/Linux).

## Notes
- **git/PR/tag need a git repo.** This project isn't initialized yet — `git init` + add a remote first.
- **Android** needs a running emulator/device (`adb devices`); release APK is unsigned without a signing config.
- `playWeb` starts the web dev server (a nested Gradle `--continuous` process) in the background and leaves it running.
