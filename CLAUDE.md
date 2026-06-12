# Air War 2142 — agent notes

## Hard rules (do not violate)

- **Verify by running the app.** Prove a change by building, running, and driving the real
  app and capturing what you observe — not by `compile`/typecheck/test runs. For the web use
  *headed* Playwright (headless throttles rAF), and **confirm macOS Low Power Mode is OFF
  first** (`pmset -g | grep lowpowermode`) or the perf numbers are invalid.
- **Perf gate on the ECS/plugin migration.** No migration step may regress base-game frame
  time vs the measured baseline (web median ~3.9ms / p95 ~7.3ms). Keep the sprite-atlas
  batching + view pooling; keep hot paths allocation-free; use typed component storage
  (not a HashMap per component). A/B each step before moving on.
- **Don't re-derive the input layer.** The macOS keyboard/touch fixes (HeldKey deferred
  release, `singleTouch` *Anywhere, the per-backend grave/`~` key) are hard-won — move them
  **verbatim**, never revert to per-frame key polling. Same for the Android `CANVAS_HEIGHT`
  (2200) coupling. Read the README's "Hacks & workarounds" before touching this code.
- **Commit in atomic steps.** On complex tasks, commit each stable, concrete step as its
  own commit (each one compiling/working) — e.g. "ECS kernel" then "migrate particles",
  not one giant commit. PRs are **squash-merged** on GitHub, so granular branch commits
  never pollute `main`'s history; favor many small commits over one big one.

## Pointers (durable context — the in-session task list is ephemeral)

- **Task board:** read `TASKS.md` (project root) when planning or resuming feature work;
  update it (`[ ]`→`[x]`) as things land. **As soon as a PR's branch is closed — merged
  _or_ superseded — mark its item `[x]` and move it from `Doing` to `Done`** (a closed
  branch means that line of work is resolved).
- **Architecture:** plugin/ECS re-architecture — decisions in the `plugin-architecture`
  memory and `docs/0001`. Read before touching engine/render code.
- **README:** task reference, build notes, and the full workarounds list.
- **Build:** JDK 21 required (KorGE 6). Commands are Gradle tasks in the `game` group
  (`./gradlew tasks --group game`); npm scripts are thin shims.
