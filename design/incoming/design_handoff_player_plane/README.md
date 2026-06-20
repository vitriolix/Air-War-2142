# Handoff: Player Plane — sprite, states, damage & death FX

## Overview
This is the spec for the **player's twin‑boom interceptor** (a P‑38‑style fighter) in *Air War 2142*:
its top‑down look, its colorway options, its banking pose, and its full battle‑damage / death
animation lifecycle including exhaust backfire, crash breakup, and water/land impact effects.

The companion file `plane-design.html` is the **living visual reference** — open it in a browser and
use the right‑hand panel to toggle every state described below. It also exports the sprite as PNG
(135×157 base cell, 270×314 @2x) via the buttons at the bottom.

## About the design files
`plane-design.html` is a **design reference built in HTML/SVG** — a prototype that shows the intended
look and behavior, **not production code to copy**. The plane is **procedurally drawn** (SVG paths
generated in JS) and animated on `requestAnimationFrame`. Your job is to **re‑implement this in the
game's existing environment — Kotlin + KorGE (web JS/Wasm, JVM desktop, Android)** — using KorGE's
own primitives (vector `Graphics`, `Sprite`/`Image`, tweens, particle emitters) and the project's
established patterns. Treat the HTML as the authoritative description of *what to build*, not *how to
structure the code*.

Two reasonable implementation strategies — pick what fits the engine best:
- **Bake to sprites**: render each state/variant to texture atlases at build time (the HTML's PNG
  export shows the target framing) and animate transforms/particles at runtime. Best for perf on
  mobile/web.
- **Procedural at runtime**: reproduce the path math in KorGE vector graphics. More flexible for the
  parameterized colorways/geometry but heavier.
Most of this (the airframe + damage decals) wants to be baked; the **death sequence and exhaust FX**
are dynamic and want runtime transforms + particles regardless.

## Fidelity
**High‑fidelity** for shape, proportion, color, and motion timing. Exact hex values, pixel
coordinates, and animation timings are given below and are intended to be matched. The art style is
deliberately **"video‑game realistic," not photoreal** — dimensional cylinder shading, baked AO
contact shadows, panel lines and rivets, cumulative weathering. Keep that middle ground (see the
"cargo‑cult maintenance" north star in the project's `PROMPT.md` §1).

---

## Coordinate system
All coordinates below are in the SVG's internal user units. The plane is drawn around a centerline at
**cx = 140**, nose pointing **up** (−y), tail **down** (+y). The booms sit at `cx ± gap`.
Convert to your engine's units by treating the sprite cell as **135 × 157** (the base PNG export);
the art is authored larger and cropped to that cell on export.

Key fixed Y bands (intact plane): spinner tips ≈ 70, prop discs ≈ 88, wing leading edge **138**,
canopy 132–172, wing trailing edge **184**, exhaust scoops ≈ 226–266, boom tails ≈ 300, tailplane
(horizontal stabilizer) ≈ 300–316, vertical‑stab tips ≈ 328.

## Geometry parameters (panel sliders)
| Param | Default | Range | Meaning |
|---|---|---|---|
| `span` | 186 | 110–240 | half‑wingspan in units (full wingspan = `2*span` ≈ 372px) |
| `gap` | 64 | 40–64 | distance from centerline to each engine boom |
| `nose` | 12 | 0–24 | nose/gondola length forward of the spinners |
| `crashShrink` | 8 | 3–12 | death‑state: wreck shrinks to **1/crashShrink** of size as it falls (see Death) |

Wing planform: root chord spans Y **138 → 184** (chord 46); tips taper to chord ≈ 12. Leading edge
gets a specular highlight strip; trailing edge a thin AO shadow. Optional yellow **wingtip caps**
(toggle `caps`).

Booms: slender nacelles ~28 units wide, cowl ring at Y≈100–116, exhaust **scoops on both sides** of
each boom at Y≈226–266 (flame/backfire emits from the scoop mouth at ≈ `boomX ± 15, y 259`).

Gondola (center pod): blunt nose just ahead of the spinners; **bottom of the pod ends at the wing
trailing edge (Y 184)** — do not let it overhang past the wing. Canopy bubble Y 134–172. Twin nose
gun barrels poke forward of the nose.

Vertical stabilizers: one on each boom at the tail, yellow tip band. Tailplane is the horizontal
surface joining the two booms.

---

## Colorways (selectable)
**Hull tones** (`hull`, default 0): Olive Drab `#5A6038`, Field Green `#47532F`, Gunmetal `#54606A`,
Bare Steel `#8B9AA3`, Desert Tan `#B49A63`, Sea Blue `#3C5670`, Charcoal `#3A3F3A`.
All shading (light/dark/edge/specular/AO) is derived from the base hull hue by HSL lightness shifts
(roughly +30 spec, +16 lite2, +8 light, −9 dark, −17 dark2, −26 line, −34 edge, −46 AO).

**Wingtip accent** (`accent`, default 0 = `#E7B73C` yellow): also `#C0392B`, `#ECEFF1`, `#D98A2B`,
`#2E7D46`, `#1F6F8B`.

**Canopy glass** (`canopy`, default **3 = "Smoke" grey**, inner `#2A323A` / highlight `#8090A0`):
also Field Glass, Cyan, Gold, Amber, Violet. Rendered as a radial gradient with two white reflection
slivers.

Spinner cones are dark with a **yellow tip** (`#E7B73C`); props are a near‑black metal disc
(`#222a30`) drawn as a spinning 3‑blade shape behind two faint motion‑blur ellipses.

---

## Frame variants (`variant`)
- **normal** — single plane, top‑down.
- **bank** — rolled 3/4 view. Implemented as: rotate **+12°** about (140,168), then **scaleX 0.62**
  (foreshorten), about the same pivot. Crucially, the **tall parts shift proud of the body toward the
  high side**: canopy +11px, vertical stab +17px, fin +9px, spinner +5px in X; plus a thin inboard
  **side‑face** sliver is revealed on the pod and stabs. This is what sells "rotated," not "squashed."
- **escorts** — the main plane flanked by two 0.5‑scale wingmen offset laterally (≈ `span*0.62 + 44`).

---

## Battle damage (`damage`: 0 New, 1 Minor, 2 Major, 3 Dead) + variants (`dmgSeed` 0–3)
Damage is **cumulative and layered**, driven by a single integer `dmgSeed` (0–3):
- Marks for level *N* are generated from `rng(seed, level)` and **level‑1 marks persist into level‑2**
  (Major = Minor's marks + a second layer). This yields **4 distinct, repeatable variants per stage**.
- **In‑engine: roll `dmgSeed` once when the plane spawns and keep it for that life.** In the HTML,
  tapping a damage button re‑rolls it so you can preview all four.
- Marks are generated **per airframe part** (wingL/wingR/boomL/boomR/gond) so that in the Dead state
  they **travel with the piece** as it breaks apart — the wreck starts already battle‑damaged.

Mark types: **bullet holes** (dark core + bright torn‑metal lip), **scorch** (soft dark ellipses),
**scratches** (short edge‑color lines). Major adds heavy scorch and a **torn‑off wingtip** on a
seed‑chosen side.

**Always‑on weathering** (even at "New"): exhaust soot streaks down the booms, grimy wash off the wing
trailing edge, oily stains on the pod, chipped‑paint bare‑metal mottle near roots/cowls, scattered
dust speckle, plus a **subtle worn‑metal panel texture** — faint light mottling + light rivet‑seam
rows along booms/pod/wings. Keep it subtle; the plane should look *used*, not destroyed.

---

## Animated FX (all on a per‑frame timeline; `t` in seconds)

### Exhaust = sporadic backfire (damage 0–2, when `flame` on)
**Not a constant flame.** Each of the 4 scoop ports is idle most of the time (a faint dark‑red ember
only). Occasionally it **backfires**: a sharp reddish flame pop + a kicked‑out smoke puff.
- Per port (phase `ph = portIndex*2.3 + 1.1`): `n = sin(2.1t+ph) + sin(3.7t+ph·1.7) + sin(0.9t+ph·0.5)`
  (range ~−3..3), `pop = max(0, n − 1.95) / 1.05` → 0 most of the time, 0..1 on a pop.
- When `pop ≤ 0.02`: draw only a faint ember (`#B5300A`, ~0.2 alpha).
- When popping: flame length `6 + pop*30`, width `2.4 + pop*3.4`, reddish‑orange gradient
  (`#FFE08A → #FF8A1F → #E8430C → transparent`), bright `#FFCC70` core, plus a `#3a3a34` smoke puff
  just past the flame tip. Ports are staggered so pops feel irregular and engine‑like.

### Battle fire & smoke (damage ≥ 2)
Persistent flame at the damaged (right) boom area (Y≈152) flickering at ~12 Hz, with a column of dark
smoke puffs **trailing downstream (downward + drifting)**, growing and fading over ~1.4 s.

### Death sequence (damage 3) — the crash
A looping ~**7.4 s** timeline. Break the airframe into **exactly 4 chunks**:
1. **Left assembly** = left wing + left boom (+ fin, spinner) + left vertical stab + left prop
2. **Right assembly** = right wing + right boom (+ fin, spinner) + right vertical stab + right prop
3. **Center pod** (gondola/canopy)
4. **Tailplane** (horizontal stabilizer)

Timeline:
- **t 0 → 0.34 s:** pieces still in formation; **explosions fire at the break seams** (between pod and
  each boom, pod nose, outboard wings) — bright `exp` radial gradient flashes expanding & fading.
- **t 0.34 → ~3.0 s (fly):** progress `st = (t−0.34)/2.7` clamped 0..1. Each chunk:
  - drifts laterally (**left chunk left, right chunk right**, pod/tail random) by `±|vx|*st*150`,
  - falls under gravity: `y = anchorY + 60*st + 470*st²` (clamped to ground line `gY`),
  - **spins** (`spin*st*~460°`) and **flips** (scaleY oscillates) while airborne,
  - **shrinks** from full size to **`1/crashShrink`** (default 1/8 — i.e. ~8× smaller) as it falls,
  - emits trailing **smoke puffs** and occasional **mid‑air explosions**.
- **Impact (~t 2.5 s):** see surface effects below.
- **t 2.6 s → fade:** burning wreckage sits on the surface (several flickering fires + rising smoke
  column). Everything **fades out over t 5.6 → ~7.2 s**, then the loop restarts.

`gY` (ground/surface line) ≈ bottom of the viewBox − 46.

### Crash surface (`terrain`: `sea` | `land`)
At impact the surface effect branches:
- **Sea** (default): a **water splash** — an expanding white foam **ring** on the surface, a central
  **crown column** of water that rises and falls (`sin` envelope), and **ballistic droplets** arcing
  up and falling back. Cool white‑blue tones (`#eaf6f9`, `#cfe9f0`, `#dff1f6`). Lasts ~1.8 s.
- **Land**: a **dust cloud** — ~24 brown/tan billowing puffs (`#b9a47e`,`#a8946a`,`#c7b487`,`#8f7d54`)
  expanding low and wide from the impact point, **lingering ~3 s** (longer than the splash) and
  fading. The stage background also tints to a dirt gradient on land.

**In‑engine:** pass whether the plane went down over water or land and pick the matching effect.

---

## Props / animation notes
- Propeller discs spin continuously (CSS `@keyframes` in the HTML; use a rotation tween/shader in
  KorGE). They stop on death (chunks carry a static prop).
- Entrance/idle: none required; the sprite is steady‑state. All motion is the FX above.
- The whole design scales to fit its container; in‑game, size to your gameplay scale and keep the FX
  timings in **seconds** (they are framerate‑independent).

## Design tokens (UI chrome around the iterator — not the plane)
The reference panel uses the game UI palette: cyan `#00E5FF`, gold `#FFCC00`, green `#00FF88`,
red `#E53935`, panel `#161e22`, ink `#dfe9ee`; fonts **Wallpoet** (titles) and **Chakra Petch** (body).
These are the established Air War 2142 UI tokens — use the codebase's existing values, don't re‑derive.

## Files
- `plane-design.html` — the interactive reference + sprite exporter (open in a browser).
- `screenshots/` — stills of the key states for quick reference:
  - `01-state.png` New (clean, weathered), `02-state.png` Major damage,
  - `03-state.png` Banking (3/4 roll), `04-state.png` Dead — mid breakup (4 chunks),
  - `05-state.png` Sea impact (water splash), `06-state.png` Land impact (dust cloud).
- See the project's `PROMPT.md` for the overall art‑direction north star ("cargo‑cult maintenance").
