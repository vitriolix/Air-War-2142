# Investigations

A running log of investigation-only sessions: the **literal prompt** that triggered each
one, the **full literal findings**, and any **follow-up prompts + responses**. Investigations
are discovery/strategy only — no code changes in the investigation itself — so this file is
the durable record of "what we learned and why", separate from `TASKS.md` (work board) and
`docs/` (settled design).

Newest entries on top. Each thread/follow-up carries a **Status**:
`Investigating` · `Deciding` · `Planning` · `Deferred` · `Done` · `Invalid`.

---

## 001 — Prefs persistence, debug-menu routing, Motion key, jump-menu input capture

**Date:** 2026-06-27 · **Overall status:** Mixed — see per-thread status.

### Prompt

> don't do anything but investigate:
> - are we saving and restoring user prefs across runs? we should work on this on a new branch when we come up with a plan.
>     - What are our cross platform pref storage options with KorGE, Korlibs or something like Fleks (https://github.com/Quillraven/Fleks)?
>     - re debug settings like Motion submenu, do we persist these plane control settings across games (or in the future persisted to some permanent storage).
>     - has any work already been done on this investigation?
> - on the debug sub menus logic, it seems like you are hard coding which screen is active to route key handling, should we just use a normal "the menu/window has focus" model? don't do work, just talk to me about the strategy here.
> - debug menu, why does it use 'G' instead of 'M' to open the motion submenu?
> - re in game debug jump menu. Is it ok to overtake all keyboard shortcuts as long as the Jump window is up? don't do work, just discover the tradeoffs of both methods

### Findings

#### 1. User prefs across runs — **Status: Planning** (graduated into [the persistence plan](plan-prefs-persistence.md))

**Are we persisting anything? No — nothing is written to disk on any platform.** The only
`writeText` calls are build-time atlas baking and clipboard. Two in-memory tiers today:

- **Engine config** (`sfxEnabled`, `sensitivity`, `showDebugOverlay`, the `kb*` motion
  params) lives on the `GameEngine` singleton created once in `main()` and shared across
  scenes. Survives scene changes *and* `startGame()` within **one app run**, resets on
  restart.
- **Controller prefs** (bindings, invertY, deadzone) are more ephemeral — `ControllerPrefsScene`
  **constructor params** threaded scene→scene by re-instantiation, not on the engine, and
  **not wired into gameplay**. `design-tokens.json`: *"Bindings are scaffolded defaults; real
  persistence lands with gamepad input."*

**Has any work been done? No.** Git history shows only the controller-prefs UI port
(PRs #24/#25). Acknowledged as future in design notes + task-27 follow-ups.

**Cross-platform storage options:**
- **KorGE `Views.storage` (`NativeStorage`)** — natural fit, already on the classpath (no new
  dep). Synchronous K/V store (`IStorageWithKeys`) with typed delegates
  (`var sfx by views.storage.itemBool("sfx", true)`). Backends: **JVM** = `java.util.Properties`
  file (`game.jvm.storage` in `realSettingsFolder`), **web** = localStorage, **Android** = file.
  Auto-saves on `set`.
- **Korlibs lower-level** (`korlibs.io` Vfs/localStorage) — `NativeStorage` already wraps it.
- **Fleks** — ⚠️ NOT storage. It's a multiplatform **ECS** framework, alt to our hand-rolled
  `ecs/`. Irrelevant to prefs (and we built our own ECS per `docs/0001` for perf control).

**Debug/motion settings:** MOTION submenu mutates `engine.kb*` live; persists within a run,
resets on restart. Open design question: dev-tuning knobs (motion, debug overlay) in a separate
"dev" namespace vs the user-pref store. Lean: separate namespace.

#### 2. Debug submenu routing: enum vs "window has focus" — **Status: Deferred** (design direction noted)

Hardcoded: `engine.debugSubMenu` (`NONE/JUMP/MOTION`) single global enum; every handler guards
on it. A hand-rolled modal state machine for two panels. A "focused window owns input" model is
the better long-term target (we already have `FocusController`/`KeyboardNavigable` in `Focus.kt`,
+ the widget-migration direction). Tradeoffs:
- **Enum (current):** simple for 2 panels; routing smeared across handlers; every new panel
  touches the enum; doesn't compose with FocusController / future UIWindows.
- **Focus/window model:** each window encapsulates its keys, declares modality, scales to N
  panels. Catch: KorGE sibling `stage.keys` handlers all fire — no built-in consume between
  them — so it needs an input-router with consume semantics, or real `UIWindow`s + view-level
  propagation. (See follow-up F5 for the Compose-alignment + UIWindow-move analysis.)

#### 3. Why `G` and not `M` for Motion — **Status: Done** (changed to `M`, 2026-06-27)

Was fairly arbitrary (`G` free; avoided `M`=Menu). `M` is unbound at BASE and more mnemonic, so
**switched the Motion-submenu toggle to `M`** (gated to yield to the JUMP picker where `M`=Menu;
`G` is freed and is now only the JUMP→Game Over target). Verified `M` opens/closes the panel.

#### 4. Jump picker: overtake all keys (modal) vs selective gating — **Status: Deferred** (folds into thread 2)

Current = **selective gating** (jump letters via `onSubMenu(JUMP)`; `P`/`C`/`G`/`J` yield; all
else still fires). Note the sim keeps ticking behind the picker (`engine.tick()` gated only on
`state == PLAYING`). Tradeoffs:
- **Modal capture:** simplest mental model, no per-key gating, no missed-conflict bugs, blocks
  accidental fire/roll/pause; but needs an allow-list for escapes or you get stuck, the sim
  still runs unless you also pause it, and KorGE gives no consume-between-handlers so you build
  a global gate / central dispatcher.
- **Selective gating:** non-intrusive, surgical; but you must find+gate every conflict and the
  burden grows with the keymap.
- **Key insight:** the two submenus have **opposite** needs — jump picker wants modal (you're
  leaving; freeze the sim), motion panel wants pass-through (fly while tuning). That asymmetry
  argues for the focus/window model (thread 2) where each window declares `modal` vs `overlay`.

### Follow-ups

#### F1 (2026-06-27) — Does ControllerPrefs need to affect gameplay? — **Status: Open → TASKS**

> "I think it needs to affect actual gameplay or it's useless right?"

Correct — as-is it's pure UI scaffolding. The bindings/invertY/deadzone are scene constructor
params never read by the engine or any input path, so the screen changes nothing. Two real
pieces of work follow: (a) **wire gamepad input into the engine** (read a pad each tick, map via
the bindings, honor invertY + deadzone); (b) **move these settings onto the engine** (not scene
params) so they're queryable + persistable. (a) is a prerequisite for the multi-select display
(F2) and (b) for persistence. Should become a TASKS item.

#### F2 (2026-06-27) — Multi-select input methods + debug MODE line shows active list — **Status: Investigated / outlined; not started**

> "Since we want input controls to be multi-select, the debug menu should show the list of active input methods. e.g.: 'Keyboard, Controller'. If input multi-select is not working yet, outline what changes / risks."

**Not working yet — input is single-select.** `_controlMode: MutableStateFlow<ControlMode>`
(one of `TILT/TOUCH/KEYBOARD`), chosen radio-style in Settings; `updatePlayerMovement()` is a
`when(mode)` so exactly one branch runs per frame; `shouldShoot` keys off the single mode; the
debug MODE line shows `controlMode.value.name`. Gamepad isn't implemented at all.

**Changes for multi-select:**
1. **Model:** replace single `ControlMode` with a set of enabled input methods (e.g.
   `Set<InputMethod>` or per-method flags). Likely separate "input method" (keyboard/gamepad/
   touch/tilt) from the existing enum.
2. **Movement merge:** the `when` becomes additive — each enabled method contributes a
   movement vector (+ shoot/roll) merged into one. Needs an explicit **merge policy**.
3. **Shoot/roll:** OR across methods (any firing → fire).
4. **Debug MODE line:** join active methods, e.g. `"Keyboard, Controller"` — trivial once the
   model is a set.
5. **Settings UI:** radio → multi-toggle.
6. **Persistence:** store the set (depends on the persistence work).

**Risks:**
- **Merge of absolute vs relative inputs is the core risk:** keyboard adds *velocity*
  (relative) while touch/tilt set an *absolute target/vector* — they fight if both live. Likely
  need precedence or "only one positional method at a time", or a defined blend.
- Touch + keyboard simultaneously → jitter/fighting.
- Auto-fire semantics (TILT/TOUCH currently auto-fire) need rethinking under multi-select.
- Determinism/replay (task-19): every live input source per tick must be recorded.
- Perf: negligible.

The **display** is cheap but only meaningful once multiple methods are truly active, which
depends on gamepad wiring (F1). Suggest: define the InputMethod-set model + a simple merge
(keyboard+gamepad both relative → sum velocity; touch/tilt stay exclusive-ish), wire gamepad,
and the debug line falls out.

#### F3 (2026-06-27) — Must persistence land on the gamepad branch? — **Status: Confirmed (No)**

> "persistence should land on it's own branch, there is nothing explicitly requiring it to land on the gamepad branch is there?"

Correct, nothing requires it. The "lands with gamepad input" note was an assumption, not a
constraint. Persistence (a `PrefStore` over `NativeStorage`) is independent infra; gamepad
bindings are just one *consumer*. It can land first with today's prefs (sfx, sensitivity,
control mode, colorway, motion knobs); gamepad bindings plug in later. → its own branch.

#### F4 (2026-06-27) — Split persistence from the Seed work? — **Status: Confirmed (Yes)**

> "could we split that work out from the Seed work? The Seed work is a long haul and this is a short haul."

Yes. The `PrefStore` is generic K/V infra; the Seed/worldgen work (task-25/task-26) merely becomes a
*consumer* (store `seed + genVersion` as a couple of keys). No dependency the other way.
Persistence is a short, self-contained haul; Seed is a long haul. Decouple — persistence plan
stands alone, Seed consumes it later.

#### F5 (2026-06-27) — Compose-alignment + "move panels onto UIWindows" (for thread 2) — **Status: Deferred (analysis recorded)**

> "can we keep our architecture aligned as much as possible with the Compose method of building nav / focus to smooth out a future port to Compose?"
> "'moving panels onto actual UIWindows and using view-level event propagation.' -- what does that move entail?"

**Compose alignment:** Compose's input model = a **focus tree** (`FocusManager`,
`FocusRequester`, `Modifier.focusable()`/`focusGroup()`), directional focus traversal, and
`Modifier.onPreviewKeyEvent`/`onKeyEvent` with **explicit consume** (return `true`). Events flow
down a focus chain; the focused node gets first crack and can consume. Our `FocusController`/
`KeyboardNavigable` already mirrors Compose's move/activate. Aligning now = extend it to a
**focus stack with preview/consume** (mirrors `onPreviewKeyEvent` + `FocusManager`) and route
key events through it instead of the global enum (which has no Compose analogue). Doing this
makes a later Compose port mostly a mechanical re-expression — the mental model and per-window
key handling transfer directly.

**"Move panels onto actual UIWindows" entails:** replace our hand-built
`container { solidRect + uiText/uiSlider }` panels with KorGE **`UIWindow`** (a real windowed
widget: title bar, draggable/closable, focusable, z-ordered), and let key handling ride KorGE's
**view event propagation** (events dispatch through the view tree; the focused/topmost view can
handle + stop propagation) rather than flat `stage.keys` handlers. Concretely: (1) each debug
panel becomes a `UIWindow`; (2) an active/focused-window notion so the top window consumes
input; (3) migrate the `onSubMenu` guards to per-window key handlers; (4) reconcile with the
HeldKey flight input (must stay intact per CLAUDE.md) — flight keys pass through unless a
**modal** window is focused. Cost: moderate; it's the "do it properly with widgets" path the
migration direction already wants. Deferred for now.
