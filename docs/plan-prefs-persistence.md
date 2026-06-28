# Plan — Cross-platform user-prefs persistence

**Branch:** `plan/prefs-persistence` · **Status:** plan only (no implementation) · **Date:** 2026-06-27

Derived from [Investigations.md](Investigations.md) #001 (threads 1, F1, F3, F4). This is a **short, self-contained
haul**, intentionally **decoupled from the Seed/worldgen work** (#25/#26) — Seed merely becomes a
consumer of this store later; there is no dependency the other way.

> Plain-language doc (no `docs/NNNN` managed-tasks block) so it doesn't participate in the
> `checkDocTasks`/`SyncDocTasks` sync. Promote to a numbered design doc + TASKS items if/when this
> is approved.

## Goal

Persist and restore user settings across app runs on every platform (JVM, web JS/wasm, Android),
behind a small typed wrapper, with schema versioning and a clean split between **shippable user
prefs** and **dev-only tuning knobs**.

## Current state (from Investigation 001)

- **Nothing is persisted** on any platform. Engine config lives in-memory on the `GameEngine`
  singleton (survives a run, resets on restart). Controller prefs are ephemeral `ControllerPrefsScene`
  constructor params — not on the engine, not wired to gameplay.
- No prior persistence work exists.

## Storage substrate

Use KorGE's built-in **`Views.storage: NativeStorage`** (`korlibs.korge.service.storage`):
- Already on the classpath — **no new dependency**.
- Synchronous key→value store (`IStorageWithKeys`); auto-saves on each `set`.
- Backends per platform: **JVM** = `java.util.Properties` file (`game.jvm.storage` in
  `realSettingsFolder`), **web** = `localStorage`, **Android** = file.
- Ships with `StorageKey<T>` typed delegates (`itemBool/itemInt/itemDouble/itemString`) and an
  `InmemoryStorage` for tests. *(`InmemoryStorage` is KorGE's actual class name — lowercase `m`, sic — not a typo; `InMemoryStorage` won't resolve.)*

Rejected: **Fleks** (it's an ECS framework, not storage); raw `korlibs.io` Vfs/localStorage
(`NativeStorage` already wraps it cleanly).

## Design

### 1. `PrefStore` facade
A thin typed wrapper over `IStorage` so call sites never touch raw string keys:
- Typed accessors with defaults (bool/int/float/double/string/enum).
- Namespace prefixes per category so categories are separable/droppable.
- Backed by `NativeStorage` in the app, `InmemoryStorage` in tests → unit-testable with no real backend.

### 2. Two namespaces
- **`user.*` (ships):** `sfxEnabled`, `sensitivity`, control method(s) (→ a set, after multi-select,
  F2), controller bindings / invertY / deadzone (after F1 wiring), plane colorway, last seed.
- **`dev.*` (dev-only, may be excluded from release):** motion params
  (`kbAccel`/`kbMaxSpeed`/`kbHeldFriction`/`kbStopFriction`), debug-overlay default. Optionally gate
  behind a build flag so dev knobs don't ship.

### 3. Schema versioning
- A `meta.schemaVersion` key. On load: missing/older → run forward-only migrations (tiny, pure);
  unknown/newer → fall back to defaults (never crash). Persistence is **best-effort** and must never
  block startup.

### 4. Wiring
- **Load at init:** `GameEngine` seeds its fields from `PrefStore` at construction (or an explicit
  `loadPrefs()` from `main` before the first scene). `GameEngine` stays the single source of truth;
  `PrefStore` is the durable mirror.
- **Save on change:** persisted-field setters write through. **Throttle continuous controls** — the
  MOTION sliders fire `onChange` per step, so persist on panel-close / debounce, not per drag tick.

### 5. Move ControllerPrefs onto the engine (relates to F1)
- Bindings/invertY/deadzone move from scene constructor params to `GameEngine` fields read by the
  input layer. The **storage** for these can land here even before gamepad input exists (F1); the
  values simply won't affect gameplay until that input is wired.

## Sequencing (each step independently shippable; A/B perf-gate any in-tick change)

1. **`PrefStore` + namespaces + schema-version + tests** (InmemoryStorage). No behavior change.
2. **Wire existing engine config** (`sfxEnabled`, `sensitivity`, control mode) through `PrefStore`:
   load-at-init + save-on-change. Verify across a real restart (JVM **and** web static build).
3. **Move ControllerPrefs settings onto the engine + persist** (inert until gamepad wiring, F1).
4. **Persist dev/motion knobs** under `dev.*`, plus a **"reset to defaults"** in the MOTION panel
   (this also closes the TASKS #27 follow-up). Optional / gated.
5. Later: **Seed** (#25/#26) consumes `user.lastSeed` + `genVersion`. No dependency back onto persistence.

## Risks / open decisions

- **Save throttling** for continuous controls (motion sliders) — save on close/release, not per tick.
- **dev-knob shipping** — separate `dev.*` namespace; decide whether to compile out of release.
- **Web storage availability** — `localStorage` can be disabled/full (private mode); `NativeStorage`
  is best-effort and swallows errors. Treat persistence as best-effort, never gate startup on it.
- **Android/iOS `realSettingsFolder`** — verify the path resolves per platform when those targets land.
- **Determinism** — a persisted seed feeds worldgen; store the `(genVersion, seed)` pair (ties #25/#26).

## Out of scope

- Cloud sync, profiles, multiple save slots.
- Gamepad input **implementation** (separate branch) — this only stores its settings.
- Multi-select input model (F2) — independent; this just persists whatever the model ends up being.

## Verification

- **Unit tests** with `InmemoryStorage`: round-trip, defaults, schema migration.
- **Manual:** set a pref → restart the app (JVM + web **static** dist) → confirm it restores. Use a
  static dist for web captures (dev-server HMR reloads confound screenshots — see the
  `web-hmr-blank-capture` memory).
