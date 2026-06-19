# Design request — Controller Preferences screen (NEW)

> Point Claude Design at this file (in the `Air War 2142` project) to design a **brand-new screen**
> that doesn't exist in the game yet. This is a *design-fresh* task, **not** a reproduce pass — there's
> no current-state baseline to match. Stay inside the constraints below so it ports cleanly to KorGE.
> Read `PROMPT.md` first for the theme (§1), coordinate system (§3), tokens (§4), KorGE mapping (§6),
> and deliverable format (§7) — all of that still applies.

## 1. What to design

A **Controller Preferences** screen for gamepad players: rebindable **gamepad bindings** + an
**Invert Y** toggle + a **Deadzone** stepper. Match the look of the existing Settings screen
(`PROMPT.md` §5.6 "FLIGHT CONTROLS") — flat panels, same tokens, same gold focus caret — so it reads
as a sibling of that screen.

## 2. Where it lives in the flow (NEW navigation edge)

Reached **from the Settings screen**: Settings gains a new **"CONTROLLER"** row/button that opens this
screen; this screen's **BACK** returns to Settings. So the flow gains one edge:
`Settings → Controller → (back) → Settings`. Call this out — it's a flow change, not just a visual one.
(Movement is the gamepad **left stick** and is *not* rebindable; only action buttons are.)

## 3. Layout (logical px — 1000 wide, 1500 tall desktop / 2200 Android; origin top-left)

Solid `#1E272C` background, flat `solidRect` + text (no panels/blur). Fonts per the shipped set
(`design-tokens.json` → `typography.fonts`): **Wallpoet** for the title, **Chakra Petch** for content.

- **Title** — "CONTROLLER", Wallpoet, **72px**, `#00E5FF`, at (100, 80). *(matches FLIGHT CONTROLS)*
- **Section label** — "GAMEPAD BINDINGS", Chakra Petch, **36px**, `RGBA(255,255,255,178)`, at (100, 200).
- **Three binding rows** — `800×80` at x=100, y = 290 / 400 / 510 (110px pitch, like the steering rows):
  - Left: action label, Chakra **36px** white, at (116, cy+18) — **FIRE**, **ROLL**, **PAUSE**.
  - Right: a **binding chip** showing the current button — a `120×56` box at (~760, cy+12), fill
    `RGBA(255,255,255,20)`, label Chakra 36px `#00E5FF` (left-anchored, manual inset). Defaults:
    FIRE = `A`, ROLL = `RB`, PAUSE = `Start`.
  - **Listening state** (while rebinding a row): the row fills `RGBA(0,229,255,40)`, its label goes
    `#00E5FF`, the chip reads **"PRESS…"**, and a hint appears under the rows: "Press a button — Esc to
    cancel" (Chakra 31px, `RGBA(255,255,255,128)`). Show this as a second card/variant.
- **Invert Y toggle** — `280×70` at (100, 640): "INVERT Y: ON" / "INVERT Y: OFF", Chakra 36px;
  on = `#00FF88`, off = `#FF3333`; fill = that color @ alpha 60. *(same pattern as the SFX toggle)*
- **Deadzone stepper** — "DEADZONE: 0.15", Chakra 36px white at (100, 760); two **opaque orange**
  `#FF9900` steppers `80×60` at (100, 810) / (210, 810), white "  -  " / "  +  " labels. Range
  **0.00–0.50, step 0.05**. *(same pattern as the sensitivity stepper)*
- **BACK button** — `380×80` near the bottom (y = canvasHeight − 200): "BACK", fill
  `RGBA(255,255,255,20)`, label white **41px** at (130, +12). Returns to Settings.
- **Gold focus caret** — solid `18×44` `#FFCC00` bar at x ≈ 48, left of the focused control; the screen
  is keyboard/gamepad navigable. Focus order: FIRE, ROLL, PAUSE, INVERT Y, DEADZONE −, DEADZONE +, BACK.

## 4. Tokens

Reuse `design-tokens.json` verbatim (colors, type scale, caret, toggle/stepper styles already there).
Anything genuinely new must be **proposed as new tokens** (so we add them to `design-tokens.json`
first), e.g.: `controller.bindingChip` (size + fill + text color), the binding **defaults**
(FIRE/ROLL/PAUSE), `controller.invertY` default (off), `controller.deadzone` (default 0.15, range
0.00–0.50, step 0.05). Flag them with `<!-- refine: new token … -->`; don't invent colors/sizes.

## 5. KorGE constraints (reaffirm `PROMPT.md` §6)

Flat `solidRect` (no rounded corners), **left-anchored** text (offset manually to fake centering in the
chips/steppers), `RGBA(r,g,b,a)` colors, **no** blur / drop-shadow / web fonts / flex auto-layout. All
positions/sizes in logical px so they map straight to `text(...)` / `solidRect(...)` calls. The
"listening" capture is an interaction *state* — render it as a variant, not animation.

## 6. Deliverable (`PROMPT.md` §7)

- `screens/controller-prefs.html` — self-contained, first line `<!-- @dsCard group="Screens" -->`.
- One `:root` block of CSS variables for every token used (reuse the existing names).
- Show **both states**: default + "listening while rebinding" (two cards, or one with a visible toggle).
- Annotate any deviation from §3 with `<!-- refine: … -->`.

## 7. Notes

- This is the new-screen leg of the round-trip test: a net-new screen + a new flow edge.
- The game has **no gamepad input yet** — we build that on the Code side when importing. Design the
  screen as the target; we'll wire real bindings/persistence + invert-Y + deadzone into the engine.
