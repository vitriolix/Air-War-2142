# Design request — Seed display + copy (Pause) & seed entry (Main Menu)

> Point Claude Design at this file (in the `Air War 2142` project). This is a **mixed** task:
> it **adds new elements to two existing screens** (it is *not* a full reproduce pass, and *not* a
> brand-new screen). Match the current look of each screen exactly and only design the new bits +
> their states. Read `PROMPT.md` first — theme (§1), coordinate system (§3), tokens (§4), the two
> screens being modified (§5.1 Main Menu, §5.3 Paused overlay), KorGE mapping (§6), and deliverable
> format (§7). All of that still applies.

## 0. Why (context)

The game is becoming **deterministic from a seed** (see `TASKS.md` #21): a given seed always produces
the same level layout. So players need to (a) **see** the seed of the run they're playing and **copy**
it to share/replay, and (b) **enter** a seed (typed or pasted) when starting a campaign — including
fun/curated seeds. This request designs those two affordances.

## 1. What to design

1. **Pause overlay (§5.3): a seed line that can be copied.** Show the current run's seed, with a
   tap/click/Enter affordance that copies it to the clipboard, and a brief "COPIED" confirmation state.
2. **Main Menu (§5.1): a SEED row + entry.** Show the seed the next campaign will use (random by
   default), let the player **type or paste** a custom seed, and **randomize** it. START CAMPAIGN then
   launches with that seed.

Both are **flow/UX changes**, not just visuals — call them out (see §2), per the round-trip convention.

## 2. Flow / navigation changes (NEW edges + state)

- **Pause:** no new screen — one new focusable control ("COPY SEED") added to the existing focus order,
  plus a transient **COPIED** state on it. No navigation edge.
- **Main Menu:** the seed row adds a small **entry sub-state** (the field becomes editable: caret, accepts
  typed digits + **paste**, Enter confirms / Esc cancels). This is an in-place state on the Menu, not a
  new screen. The **focus order** gains the seed controls (see below). Flag this as a flow change.

## 3. Layout (logical px — 1000 wide, 1500 tall desktop / 2200 Android; origin top-left)

Reuse each screen's existing background, fonts (**Wallpoet** title / **Chakra Petch** content), and tokens.

### 3a. Pause overlay additions (on top of §5.3 — full-canvas black @ alpha 0.7)
The existing block is at y = 580 (title) / 770 / 840 / 910. Add the seed affordance **below** it so it
doesn't disturb the current layout:

- **Seed line** — Chakra Petch **36px**, at (80, 1010):
  - label **"SEED:"** in `RGBA(255,255,255,160)`, value (e.g. **"4815162342"**) in `#00E5FF`
    immediately after it (left-anchored, manual inset — no centering).
- **COPY SEED control** — a `320×72` rect at (80, 1080), fill `RGBA(255,255,255,20)`, label
  **"[ C ]  COPY SEED"** Chakra 36px white, inset (+20, +16). Focusable.
  - **COPIED state** (transient, ~1.2s after activate): same rect fills `RGBA(0,255,136,40)`, label
    becomes **"COPIED ✓"** in `#00FF88`. Show this as a second card/variant.
- Add **COPY SEED** to the pause focus order (after the existing Resume / Settings / Abandon items);
  gold focus caret per the existing pattern (the §5.3 overlay is keyboard-navigable; reuse that caret).

### 3b. Main Menu additions (on top of §5.1)
Existing buttons: START CAMPAIGN (80, 640), SETTINGS & INPUTS (80, 760). Insert a **seed row between the
title and the buttons** (or directly under the buttons if it reads cleaner — note your choice with a
`<!-- refine -->`). Proposed: a seed row at y ≈ 880, below both buttons, so the buttons stay put.

- **SEED field** — a `560×80` rect at (80, 880), fill `RGBA(0,0,0,85)` (match the buttons):
  - label **"SEED"** Chakra 28px `RGBA(255,255,255,160)` at (+24, +8);
  - value Chakra **40px** `#FFCC00` at (+24, +34) — the gold hero color, since the seed is the shareable
    artifact. Default shows a **random** seed (e.g. "4815162342").
- **PASTE button** — `150×80` rect at (660, 880), fill `RGBA(255,255,255,20)`, label **"PASTE"** Chakra
  36px white. (Clipboard read may be unavailable on some platforms — design a **disabled/greyed** variant
  too: fill `RGBA(255,255,255,20)`, label `#999999`.)
- **RANDOMIZE button** — `150×80` rect at (820, 880), fill `RGBA(255,255,255,20)`, label **"🎲"** or
  **"NEW"** Chakra 36px white (pick one; the dice glyph may lack font coverage — prefer **"NEW"** and
  flag with `<!-- refine -->`).
- **Editing sub-state** (field focused for input): the SEED field fills `RGBA(0,229,255,40)`, shows a
  **blinking caret** after the value, and a hint below at (80, 980) Chakra 31px `RGBA(255,255,255,128)`:
  **"Type a seed • Cmd/Ctrl+V to paste • Enter to confirm • Esc to cancel"**. Show this as a variant.
- **Focus order** becomes: START CAMPAIGN → SETTINGS & INPUTS → SEED field → PASTE → RANDOMIZE (gold
  20×52 caret per §5.1, caretX = control.x − 52).

## 4. Tokens

Reuse `design-tokens.json` verbatim (button fill `RGBA(0,0,0,85)`, ghost fill `RGBA(255,255,255,20)`,
`#00E5FF`/`#FFCC00`/`#00FF88`/`#999999`, the gold caret, Chakra/Wallpoet). Anything genuinely new must be
**proposed as a new token** (flagged `<!-- refine: new token … -->`), e.g.: `seed.field` (size/fill/text),
`seed.copiedFlash` (the `#00FF88` confirm), the **COPIED** timeout duration, and the seed **value format**
(see §6). Don't invent colors/sizes.

## 5. KorGE constraints (reaffirm `PROMPT.md` §6)

Flat `solidRect` (no rounded corners / blur / shadow / web fonts / flex), **left-anchored** text with
manual insets to fake centering, `RGBA(r,g,b,a)` colors, all positions/sizes in logical px so they map
straight to `text(...)` / `solidRect(...)`. Editing caret + COPIED flash are **states/variants**, not
animations (declarative; the Code side renders them as state). Text **input** and **clipboard** are
engine work we wire on import — design only the visual states.

## 6. Behavior notes for the Code side (not for Design to build — context)

- **Seed format:** the engine seed is a 64-bit value, but the player-facing seed should be a friendly
  **string** (digits, and later named seeds like `HALLOWEEN`). Display the string form; the registry maps
  string → seed (`TASKS.md` #21). Keep the field width generous (≥ ~16 chars).
- **Copy/paste:** clipboard write (Pause copy) and read (Menu paste) are platform calls we add behind an
  `expect/actual` on import; the **PASTE-disabled** variant covers platforms without clipboard read.
- This unblocks/aligns with #23 (thread a seed into `GameEngine`) — the Menu's confirmed seed is what
  `startGame` will consume.

## 7. Deliverable (`PROMPT.md` §7)

- `screens/seed-pause.html` — the Pause overlay with the new seed line + COPY SEED, **both** states
  (default + COPIED). First line `<!-- @dsCard group="Screens" -->`.
- `screens/seed-menu.html` — the Main Menu with the SEED row + PASTE + RANDOMIZE, showing the **editing**
  sub-state and the **PASTE-disabled** variant. First line `<!-- @dsCard group="Screens" -->`.
- One `:root` block of CSS variables per file for every token used (reuse existing names).
- Annotate any deviation from §3 (placement choices, glyph fallbacks) with `<!-- refine: … -->`.

## 8. Notes

- This is another round-trip exercise: **modifying existing screens + adding flow state** (distinct from
  the net-new Controller screen in `controller-prefs.md`).
- The game has **no text-entry or clipboard yet** — both are Code-side work on import; design the target
  states and we'll wire input + `expect/actual` clipboard + the seed→registry plumbing.
