# Design → Code handoff — Air War 2142, Round 3 (Seed display + entry)

> Design built the two screens requested in `round3-seed-display.md`. This is an **additive**
> round — new elements + new flow state on two existing screens, no net-new screens.
> Same three tiers as Round 2: **NEW WORK** (build these states in Kotlin) →
> **NEW TOKENS** (Design proposes; add to `design-tokens.json` first, then the `.kt`) →
> **DECISIONS** (deviations from the request Design made this session — confirm or push back).
>
> Apply by adding the proposed tokens to `design-tokens.json` (the contract), then the owning
> `.kt` scene. The HTML is the visual spec; clipboard + text-entry are engine work to wire on import.

**Design files this session:** `screens/seed-pause.html`, `screens/seed-menu.html` (both
`@dsCard group="Screens"`). No existing screen HTML changed — these are new files.

---

## NEW WORK — build in Kotlin

### N1 · Pause overlay: SEED line + COPY SEED control
**File:** `GameScene.kt` (pause overlay, §5.3) · **Spec:** `screens/seed-pause.html`

- **Seed line** — Chakra 36px at **(80, 1010)**: `"SEED:"` in `RGBA(255,255,255,160)`, value
  (`"4815162342"`) in `#00E5FF` immediately after, left-anchored manual inset.
- **COPY SEED control** — `solidRect 320×72` at **(500, 992)** fill `RGBA(255,255,255,20)`,
  label `"[ C ]  COPY SEED"` Chakra 36px white inset (+20,+16). **Focusable** — append to the pause
  focus order after Resume / Settings / Abandon; gold `20×52` caret, caretX = control.x − 52.
- **COPIED state** (transient, ~1.2s after activate): same rect fills `RGBA(0,255,136,40)`, label
  `"COPIED ✓"` in `#00FF88`, then reverts.
- **Clipboard write** is the only engine call here — `expect/actual`, wired on import.

> ⚠️ **Deviation from request §3a** — see **D1**: COPY SEED moved onto the *same line* as the seed
> value (x=500, y=992), not stacked below at (80,1080).

### N2 · Main Menu: SEED menu item → expandable seed editor
**File:** `MenuScene.kt` (§5.1) · **Spec:** `screens/seed-menu.html`

This is a **flow change** (new in-place state on the Menu, no new screen). Three states:

1. **Collapsed (default)** — a normal menu item `solidRect 840×80` at **(80, 880)** fill
   `RGBA(0,0,0,85)` (matches START / SETTINGS): label `"SEED"` Chakra 46px white left-anchored
   (+28), current seed value `"4815162342"` Chakra 40px `#FFCC00` right-anchored (−28). Default
   menu focus stays on START CAMPAIGN.
2. **Expanded (editing)** — activating the item swaps the row **in place** to the editor:
   - **field** `solidRect 840×80` at (80,880) fill `RGBA(0,229,255,40)`, label `"SEED"` 26px
     `RGBA(255,255,255,160)` + value 38px `#FFCC00`, blinking text caret after the value;
   - **action row** at y=980, three `solidRect 150×72` ghost rects `RGBA(255,255,255,20)`,
     labels Chakra 34px white: **COPY** (x=80) · **PASTE** (x=246) · **ROLL** (x=412);
   - **hint** at (60, 1080) Chakra 28px `RGBA(255,255,255,128)`:
     `"Type or paste a seed • Saved automatically • Esc to close"`;
   - gold caret on the field (caretX=28, y=894).
3. **Expanded · COPIED** — activating COPY flashes that button `RGBA(0,255,136,40)` /
   `"COPIED ✓"` `#00FF88` for ~1.2s (same treatment as N1), focus caret on COPY (x=28, y=990).

- **Focus order (expanded):** SEED field → COPY → PASTE → ROLL (gold caret, caretX = control.x − 52).
- **PASTE-disabled** variant (platforms w/o clipboard read): ghost fill kept, label `#999999`.
  (Built earlier; kept as a documented state — `.mbtn.disabled` in the HTML.)
- **Engine work on import:** text entry, clipboard read (PASTE) + write (COPY), seed→registry
  string mapping (`TASKS.md` #21), thread confirmed seed into `startGame` (#23).

> ⚠️ **Deviations from request §3b** — see **D2 / D3 / D4**: (1) collapsed-to-expand menu-item
> model instead of an always-visible field; (2) added a **COPY** button on the Menu; (3) **ROLL**
> label instead of `NEW`/🎲; (4) **immediate save** on edit — no Enter-to-confirm step.

---

## NEW TOKENS — Design proposes; add to `design-tokens.json`

All flagged `<!-- refine: new token … -->` in the HTML. Reuse existing names everywhere else
(`menu-button-fill` `RGBA(0,0,0,85)`, `ghost-fill` `RGBA(255,255,255,20)`, `#00E5FF`/`#FFCC00`/
`#00FF88`/`#999999`, gold caret, Chakra/Wallpoet).

| Token | Value | Used by |
|---|---|---|
| `seed.field.fill` | `RGBA(0,0,0,85)` (reuse `menu-button-fill`) | collapsed item + field |
| `seed.field.editingFill` | `RGBA(0,229,255,40)` | editing sub-state |
| `seed.copiedFlash` | `RGBA(0,255,136,40)` | COPIED confirm (Pause N1 + Menu N2, shared) |
| `seed.copiedTimeout` | **~1.2s** | COPIED revert duration (both screens) |
| `seed.valueFormat` | friendly **string** (digits; later named e.g. `HALLOWEEN`), field ≥ ~16 chars | display form; registry maps string→64-bit seed (§6) |

---

## DECISIONS — Design's deviations this session (confirm or push back)

| # | Where | Request said | Design did | Why |
|---|---|---|---|---|
| **D1** | Pause | COPY SEED stacked below seed value at (80, 1080) | On the **same line**, right of the value at **(500, 992)** | User direction — tighter, one-line reading; frees the lower overlay. |
| **D2** | Menu | Always-visible SEED field between/under buttons | **Collapsed menu item → expands** to editor on activate | User direction — keeps the menu clean; seed editor is opt-in. |
| **D3** | Menu | PASTE + RANDOMIZE only | Added a **COPY** button (COPY · PASTE · ROLL) | User direction — symmetry with Pause; copy the next-run seed from the menu too. |
| **D4** | Menu | Enter confirms / Esc cancels | **Immediate save** on change; **Esc returns to display-only** | User direction — no save button; edits persist live. Hint copy updated to match. |
| **D5** | Menu | `"🎲"` or `"NEW"` | **"ROLL"** | Dice glyph lacks Chakra coverage; "ROLL" reads as randomize. (We tried a monochrome glyph set — looked off in-style, reverted.) |
| **D6** | Menu | hint 31px @ (80, 980) | **28px @ (60, 1080)** | Full hint string clips the 1000px canvas at 31px — same risk in KorGE `text()`. Use these values. |

> **Note on D2/D3/D4:** these change the Menu interaction model from the original request
> (in-place field) to a collapsed-item-expands-to-editor flow with live save. If Code prefers the
> simpler always-visible field, the field/action-row visuals are unchanged — only the entry/exit
> trigger differs. Flag back if you'd rather not build the expand/collapse.

---

## Behavior recap (engine work on import — context, not Design's to build)

- **Seed format:** display the friendly **string**; registry maps string → 64-bit engine seed
  (`TASKS.md` #21). Keep field ≥ ~16 chars.
- **Clipboard:** write (COPY, both screens) + read (PASTE, Menu) behind `expect/actual`; the
  PASTE-disabled variant covers platforms without read.
- **Save model (D4):** the field's current value *is* the next-run seed — no confirm step;
  `startGame` consumes it directly (aligns with #23). Esc only closes the editor (display-only),
  it does **not** revert.
- **States, not animations:** editing caret + COPIED flash are declarative states the Code side
  renders; no tweening required.

---

*Supersedes nothing — additive to Round 2. Request: `round3-seed-display.md`.*
