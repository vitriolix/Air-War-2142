# Design history

A record of the game's UI/design state **per release**. Air War 2142's UI is built through round
trips with [Claude Design](https://claude.ai/design) — Code→Design pushes *and* Design→Code imports,
often several per release — so we don't log every individual change. Instead, at **each release** we
snapshot the *current* consolidated UI/design state and add an entry here, newest at the bottom.

Each entry links a committed, **byte-for-byte snapshot** of the `design/` bundle under
`design/history/<id>/` (project-path layout), so the exact UI state of any release is recoverable —
independent of the Claude Design project's own history.

> Cadence is **per release**, not per Design exchange. A single release may fold in many round trips
> (a seed push, a reproduce pass, font adoption, a new screen…); they all roll up into that release's
> one entry + snapshot. Day-to-day Code↔Design transfers live in git history and `incoming/` /
> `designImport`, not here.

**Per-entry fields**

- **Release** — version / tag + date.
- **UI changes** — what changed in the UI since the previous entry, and the Design round trips that fed in (either direction).
- **Snapshot** — path to the committed bundle snapshot under `design/history/<id>/`.
- **Notes** — caveats, follow-ups.

---

## Seed (baseline) — 2026-06-16

The initial UI/design state, **before any release** — the bundle Claude Design first received when the
`Air War 2142` project (`projectId dec21c04-883c-4ee2-a195-b9a457866064`) was stood up. Kept as the
baseline the release history builds on.

- **UI state:** the original spec bundle — `PROMPT.md` (brief), `design-tokens.json` (**pre-fonts**
  contract), `spec.html` + `screens/1-4` (current-state baseline), `refinement-mockup.html`
  (refine-toward target), `assets/sprites.{png,txt}` (atlas). Reproduce-then-refine per `PROMPT.md` §1.
- **Snapshot:** [`design/history/seed/`](history/seed/) — byte-for-byte, reconstructed from the seed
  commit `45276af` (pre-fonts `design-tokens.json`, pre-history `README.md`) + the sprite atlas.
  Seeded via `DesignSync` `planId plan_dec21c04883c4ee2_85dacf6f6620` (11 files).
- **Notes:** inputs only — no screens generated yet at seed time. Everything Design has produced since
  (the reproduce pass; the Wallpoet + Chakra Petch font adoption imported in #20; any new screens)
  rolls up into the **first release** entry, whenever that's cut.
