# Design drop history — Code → Claude Design

An append-only log of every **drop**: a bundle of files we push from this repo into the
[Claude Design](https://claude.ai/design) project. Each `designExport` / `DesignSync` push that
seeds or updates the project gets one entry here, newest at the bottom, so we have a durable record
of *what* we sent, *when*, and *why* — independent of the Design project's own history. Each drop's
exact bundle is also **snapshotted (committed) under `design/drops/drop-NN-<slug>/`** — a byte-for-byte
copy of the pushed files (project-path layout), so the contents are recoverable, not just the manifest.

This is the Code→Design half of the round-trip's change-note convention (the Design→Code half lands
in `incoming/` and is reviewed via `./gradlew designImport`). When you send a drop, add an entry
**before or as part of** the push so the log never lags the project.

**Per-entry fields**

- **When / Direction** — date and that it's Code → Design.
- **Project** — name + `projectId` (the claude.ai/design target).
- **Trigger** — why this drop went out (seed, new-screen request, token change, re-sync after a code change…).
- **Contents** — the files pushed, by their **project path** (drops mirror the `design/` tree without the `design/` prefix).
- **Provenance** — the `DesignSync` `planId` (or `designExport` invocation) for audit.
- **Archive** — path to the committed byte-for-byte snapshot of the pushed bundle (`design/drops/drop-NN-<slug>/`).
- **Notes** — what we expect back, what changed vs the previous drop, caveats.

---

## Drop 1 — 2026-06-16 — Initial seed bundle

- **Direction:** Code → Design (seed — first push to a fresh project)
- **Project:** `Air War 2142` · `projectId` `dec21c04-883c-4ee2-a195-b9a457866064`
- **Trigger:** Stand up the design-system project and seed it with the full spec bundle so Claude
  Design has everything needed to (1) reproduce the current UI as a component library, then (2)
  refine it. Reproduce-then-refine brief per `PROMPT.md` §1.
- **Contents (11 files, project paths):**
  - `PROMPT.md` — the brief (theme, six-screen specs §5, KorGE constraints §6, deliverable format §7)
  - `README.md` — the round-trip workflow doc
  - `design-tokens.json` — source-of-truth contract (colors, type scale, canvas dims, sprite table)
  - `spec.html` — current-state baseline (real screenshots + sprite gallery)
  - `refinement-mockup.html` — aspirational refine-toward target (labeled NOT-current)
  - `screens/1-menu.png`, `screens/2-hud.png`, `screens/3-paused.png`, `screens/4-settings.png`
  - `assets/sprites.png`, `assets/sprites.txt` — the real 2048×2048 atlas + slice table
- **Provenance:** `DesignSync` `planId` `plan_dec21c04883c4ee2_85dacf6f6620` (`write_files`, 11 written).
  Project tree mirrors `design/` (the `design/` prefix dropped) so the relative links inside
  `spec.html` / `PROMPT.md` resolve in the project.
- **Archive:** [`design/drops/drop-01-seed/`](drops/drop-01-seed/) — byte-for-byte snapshot of the
  pushed bundle, reconstructed from the push-time commit `45276af` (pre-fonts `design-tokens.json`,
  pre-DROPS `README.md`) + the sprite atlas. This is the bundle as Design first received it.
- **Notes:** No screens generated yet at drop time — this is inputs only. Expected back: a reproduce
  pass (one self-contained HTML per screen under `screens/`, `@dsCard` markers, CSS-var tokens),
  pulled into `incoming/` for `designImport`. Game Over / Victory screenshots are not in the bundle
  (not yet captured); their specs are text-only in `PROMPT.md` §5.4–5.5.
