# Design Feedback — Air War 2142, Round 1

> Reviewed against the live screenshots (screens/1–5.png) and the north star from PROMPT.md §1:
> **"1942 if it had advanced 200 years without ever abandoning its roots."**
> Feedback is organized screen-by-screen then cross-cutting. Priority order for refine pass at the end.

---

## Screen 1 — Main Menu

**Islands are eating the composition.**
Two overlapping discs cover roughly 60% of the canvas and one partially occludes the "START CAMPAIGN"
button. As static elements they dominate; the title and buttons become secondary. In-game the islands
scroll so the overlap is transient — on the menu they just sit there and crowd everything.

**The button bars have almost no visual presence.**
`RGBA(0,0,0,85)` on the ocean gradient is extremely subtle. There's no frame, no edge, no affordance
that says "tap me." The gold focus caret is doing all the interactive work — a first-time player
has nothing that reads as a button.

**Vertical composition has a dead lower third.**
Title is top-left (y:200–450). Buttons mid-screen (y:640–760). Then ~700 logical px of near-empty
ocean below. The hint text at the very bottom doesn't anchor that space. Options: push title up,
move buttons lower, or place a plane silhouette in the empty area.

**"AIR WAR" at 56px is too small to read as line 1 of the title.**
It's 3× smaller than "2142" — it reads as a label for the number, not a co-equal title line.
Either increase it or find a different typographic relationship between the two lines.

---

## Screen 2 — In-game HUD

**Scale is broken.**
The player plane is ~50px tall at the bottom; the islands are hundreds of pixels across. The plane
reads as a mosquito next to continent-scale atolls. Likely a gameplay-tuning issue too, but visually
the scene reads wrong.

**FIGHTERS: is empty.**
No life indicators appear — just a dangling label. Captured here as a baseline bug; not a design fix.

**"PAUSE" centered at the top is awkward.**
It's a tap target, not information. It draws the eye unnecessarily while the player is watching
live action. A small icon or corner placement would be less intrusive.

**LOOPS label is too close to the right edge.**
At x:868 on a 1000-wide canvas, the right margin is very thin — and on a real device the right
edge is often eaten by system chrome (notch, rounded corner, safe area).

---

## Screen 3 — Paused

**190px gap between "MISSION PAUSED" and the first option.**
The heading and menu items feel like they're on different screens. Bringing the items up tightens
the composition significantly without any other change.

**The HUD is still visible through the overlay.**
SCORE / LOOPS / FIGHTERS / FUEL all show through the 0.7-alpha dim. They add noise while the
player is navigating the pause menu. The HUD should be hidden during pause, or the overlay alpha
should suppress it more completely.

**No focus state is shown.**
The three options have color differentiation (green / white / faint white) but no selected-row
indicator. The menu screen uses the gold caret for this; the pause screen has nothing equivalent.

**"SETTINGS & INPUTS" is the wrong label in this context.**
On the pause screen, that option opens the Flight Controls screen — calling it "SETTINGS & INPUTS"
matches the main menu button but not the destination screen's title. Consider "FLIGHT CONTROLS"
for consistency.

---

## Screen 4 — Flight Controls (Settings)

**DEBUG OVERLAY: OFF being RED is a false alarm.**
Red = danger/error in every UI convention. The toggle is in its normal OFF state — nothing is
wrong. Red should mean "this is ON and that's unusual," not "this is OFF." The SFX toggle
green-ON / red-OFF is more defensible; the debug toggle in its default OFF state genuinely
should not be red. Neutral grey for OFF (with a note flagged back to code) is the right call here.

**The CONTROLLER row is visually identical to TILT and TOUCH.**
It uses the same inactive row treatment, but it's a navigation destination, not a mode selector.
There's no chevron, arrow, or any affordance that says "this takes you somewhere." Users familiar
with standard settings conventions will not know it's a nav item.

**210px of empty space between CONTROLLER row (y:1090) and RESUME GAME (y:1300).**
The exit button floats in a lot of dead space. Either move it up, or treat the gap as intentional
breathing room before a destructive-ish action (which is valid — just name it).

---

## Screen 5 — Controller

**~400px of dead space below the deadzone slider.**
The screen is half empty. BACK is marooned at the bottom of a large void. This is the starkest
vertical rhythm problem in the whole UI — the controls end at roughly y:900 but the screen
continues to y:1500.

**Binding rows have no fill.**
FIRE / ROLL / PAUSE rows appear transparent, while the steering rows in Flight Controls have the
`RGBA(255,255,255,20)` fill. They look lighter and less defined — a small inconsistency between
two sibling screens that share the same row component.

**"INVERT Y: OFF" in grey-on-grey is barely readable.**
The neutral grey treatment is semantically correct (OFF = unremarkable), but the contrast is too
low. The label text needs a touch more brightness without implying danger.

**Chip sizing is arbitrary relative to content.**
A single character ("A") floats in a 120px-wide box; "Start" nearly fills the same box. The chip
size feels chosen for "RB" and doesn't adapt to shorter or longer labels. Worth noting for the
listening state where "PRESS A BUTTON…" must fit the same chip (currently implemented as a
340px-wide variant — make sure that's what ships).

---

## Cross-cutting

### 1. The 1942/2142 theme is completely invisible in the UI
Every screen reads as a generic clean sci-fi mobile UI. Neon on dark navy could be any shooter.
The Wallpoet font has some of the right DNA but it's doing all the thematic work alone.
There is no visual evidence of:
- Riveted/stamped-steel construction
- Field modifications, mismatched panels, layered wear
- WWII-era visual language (stencil markings, roundels, panel lines)
- The tension between retro-handmade and futuristic-precision

This is the entire refine brief and nothing has addressed it yet in any screen.

### 2. No plane on the menu
The hero object of the game — the player's plane — never appears on the title screen. It's the
most available asset (real sprite art exists) and the one element that would immediately say
"this is that kind of game." The lower third of the menu is currently empty; a plane could live there.

### 3. Two conflicting toggle conventions
- Flight Controls: DEBUG OVERLAY OFF = red
- Controller: INVERT Y OFF = neutral grey

Pick one semantic and apply it everywhere. Recommended: **OFF = neutral grey** (nothing wrong),
**ON = green** (active/enabled). Reserve red for destructive or warning states only.

### 4. All screens are center-empty / vertically under-used
The 1000×1500 canvas is tall. Most screens use the top 800–1000px and leave the rest dead.
This isn't inherently wrong (mobile safe areas, thumb-reach zones) but it's a pattern to
address systematically in the refine pass.

---

## Suggested refine priority order

| Priority | Screen | Key problem |
|----------|--------|-------------|
| 1 | **HUD** | Scale, missing life indicators, PAUSE label |
| 2 | **Main Menu** | Island composition, button presence, dead lower third |
| 3 | **Paused** | Gap/spacing, HUD bleed-through, missing focus state |
| 4 | **Flight Controls** | Debug toggle color, CONTROLLER nav affordance |
| 5 | **Controller** | Dead space, row fill consistency, chip sizing |

The theme note (cross-cutting #1) applies to all screens but is a separate pass — agree on the
specific visual direction first (a small mood-board or type treatment), then apply it screen by screen.
