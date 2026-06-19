# Claude Design handoff — Air War 2142 UI

> **Paste this whole file into a Claude Design (claude.ai/design) design-system project**, and
> attach the files listed under *Inputs*. It tells Claude Design exactly what the game
> looks like today so it can (1) reproduce the current UI as a component library, then (2) help us
> refine it. The round-trip back into the codebase is handled by the `designImport` Gradle task —
> see `design/README.md`.

---

## 1. Your task

You are designing the UI for **Air War 2142**, a vertically-scrolling arcade shoot-'em-up built in
**Kotlin + KorGE**, shipping to **web (JS/Wasm), JVM desktop, and Android**.

**Setting & theme (the visual north star).** Air War 2142 is *not* a straight remake of the 1942
arcade game — it's an **alt-history reimagining**. In this timeline WWII-era air warfare never gave
way to jets-and-missiles doctrine; instead the prop-plane dogfight, the flak, the carrier strike and
the squadron formation just *kept evolving* for two more centuries. The result is a **fusion of eras**
— think steampunk's past/future blend, but anchored to WWII: weathered riveted airframes and
roundel-style insignia alongside energy weapons, sci-fi HUD readouts, and impossible ordnance. The
north star is "1942 if it had advanced 200 years without ever abandoning its roots" — lean into that
tension (hand-built/retro **and** futuristic), not clean modern-military and not pure sci-fi. NOTE:
the *current* UI doesn't express this yet — it's the direction to **refine toward**, never the
reproduce baseline.

Do this in two passes:

1. **Reproduce — pixel-faithful.** Build a component library that recreates the *current* in-game
   UI exactly: same strings, colors, type sizes, and layout as the source-of-truth tokens and the
   screen specs below. This is our visual regression baseline. Don't improve anything yet.
2. **Refine — on request.** Once the baseline matches, we'll iterate screen-by-screen. Every
   refinement must stay **reproducible in KorGE** (see §6) and must reuse the existing tokens
   unless we explicitly add new ones.

**Work one component/screen at a time.** Do not wholesale-replace the library. This mirrors how
`designImport` brings changes back: incrementally, reviewed per file.

## 2. Inputs (attached / in this project)

| File | What it is |
|------|-----------|
| `design-tokens.json` | **Source of truth.** Colors, type scale, canvas dims, sprite table — all transcribed from the live Kotlin source, with the originating file noted per token. |
| `spec.html` | **The current-state baseline** — real screenshots captured from the running game + a gallery of the real sprite art + the token summary. **This is what "reproduce" means.** |
| `screens/*.png` | The raw screenshots embedded by `spec.html` (Menu, HUD, Paused, Settings). Game Over / Victory aren't captured yet — see §5.4–5.5 for their specs. |
| `refinement-mockup.html` | An **aspirational** mockup (glass panels, idealized layout, `✈` emoji). The game does **not** render this — use it only as a *refine-toward* target, never as current truth. |
| `assets/sprites.png` + `assets/sprites.txt` | The real sprite atlas (2048×2048) and its slice table. The planes are **these sprites**, not emoji. `sprites.txt` cols = `name x y atlasW atlasH hotspotX hotspotY`; display size = atlasW/3 × atlasH/3. |

## 3. The coordinate system (read this first)

Everything in the game is laid out in a **logical canvas 1000 px wide**. Height is
**1500 px on desktop/web** and **2200 px on Android** (taller phone aspect). The origin is top-left,
y grows down.

- All positions and type sizes in §5 are in these **logical px** — not CSS px, not "sp".
- The HUD bottom row anchors to the **bottom edge** (`ch - 55`, `ch - 62`), so it must reflow with
  canvas height, not sit at a fixed y. Top row is anchored to the top.
- For mobile mockups, a 1000×2200 logical canvas maps to a portrait phone. `refinement-mockup.html` uses 390×844
  frames — that's fine as a *display* scale, but keep the **relative** geometry from §5 and quote
  measurements in logical px so they translate back to KorGE cleanly.

## 4. Design tokens

Use `design-tokens.json` verbatim and expose them as CSS custom properties so refinements stay consistent.

**Color** — `--cyan #00FFFF` (title/borders), `--hud-cyan #00E5FF` (SCORE label, active state),
`--gold #FFCC00` (the "2142" title, focus caret, score/credit values), `--orange #FF9900` (LOOPS,
secondary), `--green #00FF88` (fuel, victory), `--red #E53935` (game-over/danger), `--white #FFFFFF`,
`--dark #1E272C` (settings/modal bg). The ocean gradient is `#0F2027 → #203A43 → #2C5364` top-to-bottom.
Islands: sand `#E2D4A8`, grass `#2E8B57`, foliage `#1E5B37`.

**Fonts** — the game ships two TTFs (SIL OFL): **Wallpoet** (titles/heads — `Fonts.title`) and
**Chakra Petch 600** (all content — `Fonts.content`). Use these exact faces in mockups so spacing
and weight match the live build.

**Type scale** (logical px; KorGE sizes are logical px, not CSS pt): title-gold **184**, title-cyan / paused-head **56**,
victory-head **82**, gameover-head **66**, score **51**, menu-button **46**, settings-exit **41**,
overlay-sub / settings-item **36**, hud-pause / fighters / menu-hint **31**, score/loops label **28**,
fuel **26**, debug **20**.

## 5. The six screens (authoritative — transcribed from source)

Strings, sizes and positions below come straight from `scenes/*.kt`. Reproduce them exactly.

### 5.1 Main Menu — `MenuScene.kt`
- Ocean gradient fills the whole canvas. Background islands scroll up (sand disc, grass disc at
  0.85×, two small foliage discs).
- Title, two lines, left-aligned: **"AIR WAR"** at 56px `#00FFFF`, pos (90, 200); **"2142"** at
  **184px** `#FFCC00`, pos (80, 260). (The big gold "2142" is the hero element.)
- Two buttons, each a **840×80** rect filled `RGBA(0,0,0,85)` with a label at **46px** white, inset
  (+24, +14): **"START CAMPAIGN"** at (80, 640), **"SETTINGS & INPUTS"** at (80, 760).
- A **gold focus caret** — a solid 20×52 `#FFCC00` bar — sits just left of the focused button
  (caretX = button.x − 52). Keyboard/gamepad navigable.
- Bottom hint at 31px `RGBA(255,255,255,128)`: **"WASD to move  •  Space to fire  •  R to loop"**,
  pos (60, ch − 60).

### 5.2 In-game HUD — `GameScene.kt`
Drawn over the live ocean + sprites. Flat text, no panels currently.
- Top-left: **"SCORE"** 28px `#00E5FF` at (16,16); value (e.g. "0") **51px** white at (16,52).
- Top-right: **"LOOPS"** 28px `#FF9900` at (868,16); value ("3") 51px white at (868,52).
- Top-center: **"PAUSE"** 31px white at (446,16) — clickable; becomes **"RESUME"** while paused.
- Bottom-left: **"FIGHTERS: ✈ ✈ ✈"** 31px white at (16, ch−55) — one ✈ per life.
- Bottom-right: **"FUEL / ENERGY"** 26px `#00FF88` at (620, ch−62); **"100%"** 26px white at (890, ch−62).
- (Debug overlay exists — a 340-wide black box of FPS/TICK/… rows — hidden in normal play; ignore for UI design.)

### 5.3 Paused overlay — `GameScene.kt`
Full-canvas black at **alpha 0.7** over the frozen game. Left-aligned text block:
- **"MISSION PAUSED"** 56px white at (80,580)
- **"[ P ]  Resume flight"** 36px `#00FF88` at (80,770)
- **"SETTINGS & INPUTS"** 36px white at (80,840)
- **"[ Q ]  Abandon mission"** 36px `RGBA(255,255,255,160)` at (80,910)

### 5.4 Game Over — `GameScene.kt`
Full-canvas black at **alpha 0.85**. Left-aligned:
- **"PLANE DESTROYED"** 66px white at (80,580)
- sub: **"Score: 12450   Kills: 17"** 36px white at (80,670)
- **"[ ENTER ]  Sortie again"** 36px `#00FF88` at (80,770)
- **"[ Q ]  Return to HQ"** 36px white at (80,840)

### 5.5 Victory / Level Complete — `GameScene.kt`
Full-canvas black at **alpha 0.8**. Left-aligned:
- **"VICTORY!"** 82px white at (80,580)
- sub: **"Level 3 cleared"** 36px white at (80,670)
- **"[ ENTER ]  Next mission"** 36px `#00FF88` at (80,770)
- **"[ Q ]  Return to HQ"** 36px white at (80,840)

### 5.6 Settings — `SettingsScene.kt`
Solid `#1E272C` background (1000×1500). All flat `solidRect` + text — no panels. Gold focus caret
(18×44 `#FFCC00` bar) sits at x≈48 left of the focused control.
- Title **"SETTINGS"** 72px `#00E5FF` (Wallpoet) at (100,80).
- Section label **"STEERING MODE"** 36px `RGBA(255,255,255,178)` at (100,200).
- Three steering rows (800×80 at x=100, y = 290/400/510), each a rect + 36px label (Chakra Petch):
  - **"KEYBOARD  (WASD / Space / R)"**, **"TILT  (Accelerometer — stub)"**, **"TOUCH  (Drag / Double-tap to roll)"**
  - active row: fill `RGBA(0,229,255,40)`, label `#00E5FF`; inactive: fill `RGBA(255,255,255,20)`, label white.
- **"SENSITIVITY: 1.0x"** 36px white at (100,660). Two **opaque orange `#FF9900`** stepper buttons (80×60)
  at (100,710)/(210,710) with white **"  -  "** / **"  +  "** labels.
- **SFX toggle** (280×70 at (100,840)): label **"SFX: ON"**/**"SFX: OFF"** 36px; on = `#00FF88`, off = `#FF3333`; fill = color at alpha 60.
- **DEBUG OVERLAY toggle** (440×70 at (100,960)): **"DEBUG OVERLAY: ON/OFF"** 36px; on = `#00FF88`, **off = `#999999`** (grey, not red); fill = color at alpha 60.
- **CONTROLLER nav row** (800×80 ghost `RGBA(255,255,255,20)` at (100,1090)): label **"CONTROLLER"** 36px white. Navigates to §5.7.
- **Exit button** (380×80 ghost `RGBA(255,255,255,20)` at (100, ch−200)): label **"RESUME GAME"** (from game) or **"SAVE & EXIT"** (from menu), 41px white. Both states use the same ghost fill — no colored prominence.

### 5.7 Controller Preferences — `ControllerPrefsScene.kt`
Solid `#1E272C` background (1000×ch). Same gold focus caret pattern as §5.6.
- Title **"CONTROLLER"** 72px `#00E5FF` (Wallpoet) at (100,80).
- Section label **"GAMEPAD BINDINGS"** 36px `RGBA(255,255,255,178)` at (100,200).
- Three binding rows (800×80 ghost at x=100, y = 290/400/510), label 36px white at (116, cy+center):
  - **"FIRE"**, **"ROLL"**, **"PAUSE"** — each has a right-anchored **chip** (120×56 ghost `RGBA(255,255,255,20)`, right edge at x=868). Chip label 36px `#00E5FF`, horizontally + vertically centered within the chip.
  - Default chip values: **"A"** / **"RB"** / **"Start"**.
  - Listening state (rebinding in progress): row fill `RGBA(0,229,255,40)`, chip expands to 340px wide, label becomes **"PRESS A BUTTON…"**; centered hint **"PRESS ESC TO CANCEL"** 36px `RGBA(255,255,255,128)` at y=1210.
- **INVERT Y toggle** (280×70 at (100,640)): label **"INVERT Y: ON"**/**"INVERT Y: OFF"** 36px; on = `#00FF88`, off = `#999999`; fill = color at alpha 60.
- **STICK DEAD ZONE** label 36px white at (100,760): **"STICK DEAD ZONE: 0.15"**. Below it:
  - **[−]** button (80×60 opaque `#FF9900`) at (100,840); **[+]** button same at (820,840). White **"  −  "** / **"  +  "** labels, vertically centered.
  - Track (620×20 ghost `RGBA(255,255,255,20)`) at (190,860); cyan fill from left proportional to value.
  - Left/Right arrow keys (and d-pad) adjust value when the slider row is focused.
- **BACK button** (380×80 ghost `RGBA(255,255,255,20)` at (100, ch−200)): label **"BACK"** 41px white, centered.

## 6. KorGE mapping — what's reproducible (constraints)

We render this UI back in KorGE, so refinements must map to its primitives. Keep these in mind:

| Want | KorGE primitive | Notes |
|------|-----------------|-------|
| Rectangle / panel | `solidRect(w, h, RGBA)` | flat fill; **no rounded corners on solidRect** |
| Rounded / custom shape | `graphics { fill(RGBA){ roundRect(...) } }` | costs a tessellation per frame — use sparingly, never in tight loops |
| Text | `text(string, sizePx, color, font)` | Wallpoet or Chakra Petch 600; **left-anchored**; no rich wrapping |
| Gradient | `LinearGradientPaint` in a `graphics{}` | OK for large static fills (ocean BG); avoid per-frame regeneration |
| Color | `RGBA(r,g,b,a)` a=0-255, or `Colors["#RRGGBB"]` | |
| Sprite | atlas `Image` | from `sprites.png`; draw at `displayW/H`, scale linearly |
| Particle burst | `addUpdater` + pooled `solidRect`s | keep total active particles ≤ 200 on web; sustained emitters are more expensive than one-shot bursts — **prefer a short flash over a looping effect** if in doubt |
| Shader / glow | KorGE fragment DSL (`Filter`) | available but costly on Android mid-range; flag any glow/blur request so we can decide whether to fake with a layered alpha rect instead |

**Not available / avoid:** CSS `backdrop-filter` / true gaussian blur (no glass blur — fake with a
flat translucent fill), drop-shadows on text, flexbox auto-layout (we position by absolute
logical coords). If a refinement needs one of these, **flag it explicitly** so we decide whether to
fake it or extend the engine. Center-anchored text needs manual offset — prefer left-anchored.

**Draw-call budget:** each `solidRect` and `text()` = one draw call. UI screens are fine at the
current element counts (~20–30 per screen). Avoid proposing elaborate tiled or per-pixel backgrounds.

**Text vertical centering:** `text.height` (font line-height) varies ±4 px between the JVM and
web/JS backends for the same font and size. Do not spec pixel-precise vertical positions for button
labels — the code centers by measured height at runtime. Spec "vertically centered in the button"
and let the engine resolve it.

## 7. Deliverable format (so `designImport` can consume it)

Produce a **design-system project** with:

- **CSS variables** for all tokens in `design-tokens.json` (one `:root` block, reused everywhere).
- **One self-contained HTML file per component/screen** under a clear tree, e.g.
  `screens/menu.html`, `screens/hud.html`, `components/button.html`, `components/panel.html`,
  `foundations/colors.html`, `foundations/type.html`.
- A **`@dsCard` marker** as the first line of each preview HTML so the Design System pane indexes it:
  `<!-- @dsCard group="Screens" -->` (groups: `Foundations`, `Components`, `Screens`).
- **No external assets** beyond `assets/sprites.png` — inline CSS, reference sprites by their slice
  box from `sprites.txt` (background-position crop) when you show in-world art.
- Annotate any element that deviates from §5 (a refinement) with an HTML comment
  `<!-- refine: … -->` so the diff back into code is obvious.

## 8. House rules

- Strings, colors and sizes in the reproduce pass must match §5 / `design-tokens.json` to the digit. If a
  value looks wrong, note it — don't silently "fix" it.
- Treat every value in `design-tokens.json` as the contract. New tokens get added there first, with a name.
- Keep everything in logical-px terms (or a clearly-stated uniform scale) so it round-trips to KorGE.
- Mobile portrait is the primary framing; desktop is the same layout at 1000×1500.
