# Design → Code handoff — Air War 2142, Round 1

> Reviewed against `screens/1–5.png` (live captures, June 2026).
> Three tiers: **BUGS** (broken vs. spec — fix now) → **CORRECTIONS** (Design updated
> `design-tokens.json` + HTML files; port these exact values to the Kotlin scenes) →
> **DEFERRED** (need a design decision first; not code-actionable yet).
>
> Apply corrections by updating `design-tokens.json` first (it's the contract), then the
> owning `.kt` file. Run the game to verify before closing each item.

---

## BUGS — broken vs. spec

### B1 · HUD: FIGHTERS label renders with no life indicators
**Screenshot:** `screens/2-hud.png` — shows `FIGHTERS:` with nothing after it.  
**Spec §5.2:** `"FIGHTERS: ✈ ✈ ✈"` 31px white at `(16, ch−55)` — one icon per life.  
The label draws but the per-life icon calls are missing or the lives array is empty at scene-init.

---

## CORRECTIONS — Design updated; port to Kotlin

Design-side files already reflect these changes (`design-tokens.json`, `screens/settings.html`,
`screens/controller-prefs.html`). The running game does not yet — delta is in the `.kt` files.

### C1 · Settings title string: `"FLIGHT CONTROLS"` → `"SETTINGS"`
**File:** `SettingsScene.kt`  
**Note:** Screenshot confirms the game *already* renders "SETTINGS" — the HTML files were wrong
and have been corrected to match. No token change needed; string literal only.

### C2 · DEBUG OVERLAY toggle: OFF color — red → neutral grey
**File:** `SettingsScene.kt`  
**Token updated:** `panel.settingsControls.debugToggle.offColor` → `#999999`  

| | Old | New |
|---|---|---|
| Fill | `RGBA(255,51,51,60)` | `RGBA(128,128,128,60)` = `color.alpha.controlNeutralOff` |
| Text | `#FF3333` | `#999999` |

Rationale: red signals an error or warning state. DEBUG OVERLAY OFF is the normal default —
nothing is wrong. SFX OFF intentionally stays red (sound disabled is worth noticing).  
Screenshot confirms the game *already* renders grey — HTML was wrong, now corrected.

### C3 · Settings exit button: opaque colored fills → ghost fill (both states)
**File:** `SettingsScene.kt`  
**Token updated:** `panel.settingsControls.exitButton.fill` → `RGBA(255,255,255,20)` for both states  

| | Old | New |
|---|---|---|
| From game | `solidRect` opaque `#00FF88`, label "RESUME GAME" | `solidRect RGBA(255,255,255,20)`, label "RESUME GAME" |
| From menu | `solidRect` opaque `#E53935`, label "SAVE & EXIT" | `solidRect RGBA(255,255,255,20)`, label "SAVE & EXIT" |

Label stays 41px white in both cases. Screenshot confirms game already renders ghost fill.

### C4 · Controller binding rows: transparent → ghost fill
**File:** `ControllerPrefsScene.kt`  
**Token:** reuse `panel.settingsControls.steeringRow.inactiveFill = RGBA(255,255,255,20)`  
Each idle FIRE / ROLL / PAUSE row needs a `solidRect(800, 80, RGBA(255,255,255,20))` at x=100,
matching the inactive steering rows in `SettingsScene`. Screenshot confirms currently transparent.

### C5 · Controller dead zone label string
**File:** `ControllerPrefsScene.kt`  
**Old:** `"DEADZONE: 0.15"`  
**New:** `"STICK DEAD ZONE: 0.15"`  
36px white Chakra Petch at `(100, 760)`. No token change — string only.

### C6 · Controller slider y-position: 810 → 840
**File:** `ControllerPrefsScene.kt`  

| Element | Old y | New y |
|---|---|---|
| `[−]` button `80×60` at x=100 | 810 | **840** |
| `[+]` button `80×60` at x=820 | 810 | **840** |
| Track `620×20` at x=190 | 830 | **860** |

---

## DEFERRED — needs design decision first

Not code-actionable until Design completes a refine pass on each screen.
Listed in suggested priority order.

| # | Screen | Issue |
|---|---|---|
| D1 | **HUD** | Player plane sprite reads tiny vs. island discs — island scale or plane scale needs tuning. Also a gameplay proportion issue. |
| D2 | **HUD** | "PAUSE" centered at top draws the eye away from live action. Consider icon or corner placement. |
| D3 | **HUD** | LOOPS label at x=868 has very thin right margin — risks system chrome overlap on real devices. |
| D4 | **Menu** | Island discs dominate ~60% of canvas on the static menu (they scroll in-game so it's transient there). Composition fix needed. |
| D5 | **Menu** | Button fill `RGBA(0,0,0,85)` has almost no contrast on the gradient — no visual affordance independent of the gold caret. |
| D6 | **Menu** | Dead lower third (~700 logical px below buttons). Player plane sprite not present on title screen — obvious placement. |
| D7 | **Menu** | "AIR WAR" at 56px is 3× smaller than "2142" at 184px — reads as a label, not a co-equal title line. |
| D8 | **Paused** | 190px gap between "MISSION PAUSED" (y=580) and first option (y=770) — items feel disconnected from heading. |
| D9 | **Paused** | HUD elements (SCORE, LOOPS, FIGHTERS, FUEL) bleed through 0.7-alpha overlay — distracting during menu navigation. |
| D10 | **Paused** | No focus indicator on pause options (gold caret or equivalent). |
| D11 | **Paused** | Label "SETTINGS & INPUTS" opens the Settings screen — consider "FLIGHT CONTROLS" to match destination title. |
| D12 | **Settings** | CONTROLLER row looks identical to mode-selector rows (TILT / TOUCH) but is a nav destination. No chevron or arrow affordance. |
| D13 | **Settings** | ~210px dead space between CONTROLLER row (y=1090) and exit button (y=1300). |
| D14 | **Controller** | ~400px dead space between deadzone slider (ends ~y=900) and BACK button (y=1300). |
| D15 | **Controller** | "INVERT Y: OFF" grey-on-dark-panel contrast is low — semantically correct but barely readable. |
| D16 | **Cross-cutting** | The 1942/2142 theme is absent from every screen. Wallpoet is doing all the thematic work alone. No riveted forms, stencil markings, or era-tension visible. Separate refine pass needed. |
| D17 | **Cross-cutting** | Toggle OFF convention: SFX OFF = red, INVERT Y OFF = grey. Recommend unifying: OFF = grey everywhere (unremarkable), ON = green (active). Reserve red for destructive actions only. |

---

*Design files updated this session: `design-tokens.json`, `screens/settings.html`, `screens/controller-prefs.html`.*  
*Supersedes: `design-feedback-round1.md` and `design-to-code-round1.md` (both preserved for history).*

---

## Code responses to Design (Round 2)

### Re: B2 — Menu island z-order
Not a code bug. Draw order is correct — islands render beneath all UI elements. This is a
composition/scale issue (Design deferred item D4). Closed as bug; Design to address in refine pass.

### Re: C4 — Controller binding rows ghost fill
On hold pending Code's call. Design applied the fix in `screens/controller-prefs.html`; porting
to `ControllerPrefsScene.kt` is straightforward (`solidRect(800, 80, RGBA(255,255,255,20))` per
idle row). Holding until Code decides whether to take it now or bundle with the refine pass.
