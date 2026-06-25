# Spike: KorGE UI widgets (`korlibs.korge.ui`) for the menu UI

**Branch:** `spike/korge-ui-widgets` · 2026-06

**Question:** instead of hand-drawing menus from primitives (solidRect + text), can we use
KorGE's own widget toolkit — and can it match our design and be extended cleanly? Answered by
rebuilding the **Settings** screen on `korlibs.korge.ui`. Screenshots in `spike-screens/`.

## What's here

- `scenes/SettingsSceneUi.kt` — Settings rebuilt on real widgets: a skinned 3-button segmented
  control (steering mode), a stepped slider (sensitivity), `UICheckBox`es (SFX/debug), `UIButton`s.
  Skinned to the design via a `panelSkin()` helper + the cascading style system. Keyboard nav uses
  the app's own `FocusController` (gold caret, UP/DOWN/LEFT/RIGHT/ENTER) — see "focus" below.
- `widgets/UISteppedSlider.kt` — a **reusable compound widget**: `UISlider` flanked by `[-]/[+]`.
  Wraps the stock slider WITHOUT reimplementing its logic (the buttons nudge `slider.value`; the
  slider's setter does clamp + step-snap + onChange). Buttons auto-disable + fade at the bounds.
  One-line per screen: `uiSteppedSlider(...) { onChange { … } }`.
- `scenes/MenuScene.kt` — one-line reroute of the menu's Settings button to `SettingsSceneUi`.
- `composeApp/webpack.config.d/devserver.js` — `devServer.open=false` so the JS dev server stops
  auto-launching the default browser (collided with Playwright's Chrome-for-Testing in captures).

## Findings

1. **Widgets are real and skin to the design.** `UIButton`/`UICheckBox`/`UISlider` expose full
   colour hooks (`bgColorOut/Over/Selected`, `textColor`, `radius`, `elevation`,
   `background.borderColor/Size`); checkbox/slider accents ride the global style cascade
   (`uiSelectedColor`). The dark/cyan/gold look took ~10 lines.
2. **`UIComboBox` can NOT be themed** — its Material white/gray is hardcoded in `init` with no
   style hook. Replaced with a skinned segmented control (also a better match for the 3-row design).
3. **Theming system is real but barebones/inconsistent.** Tiny global token set
   (`textFont/Size/Color`, `uiBackgroundColor/uiSelectedColor/uiUnselectedColor`); no spacing/
   typography/semantic-role system; coverage uneven (slider/checkbox honour the cascade, combo
   ignores it). Per-widget you get `UIRenderer<T>` escape hatches, not a cohesive theme.
4. **No real layout system.** Only `uiVerticalStack`/`uiHorizontalStack`/`*Fill`/`uiGridFill` —
   no flexbox/constraints/weights, no Compose-style measure-layout. Mostly manual `.position()`.
5. **Focus model friction.** KorGE's `UIFocusManager` is **Tab-based**; the app uses arrow-key
   `FocusController`. The widget Settings is driven by the app's `FocusController` so nav matches
   the rest of the game (gold caret, UP/DOWN move, LEFT/RIGHT on the slider, ENTER activate).
   Note `UISlider` itself isn't focusable; the compound widget's `[-]/[+]` handle LEFT/RIGHT.
6. **Gotcha:** the bespoke Chakra Petch subset lacks **U+2212 MINUS SIGN** (renders blank) — use
   ASCII `-`. Same class as the `✈`-not-in-font note elsewhere.

## Bottom line

KorGE-UI is "primitives + escape hatches": real, themeable, extensible (a reusable `UISteppedSlider`
is trivial), but the theme/layout/focus stories are thin, so you'd build a small in-house widget
layer once and reuse. No Compose, no module split, no KorGE 7 — works on all four targets today.
This does not compromise a later Compose move (the StateFlow boundary is the bridge), it just isn't
reused by one. See `spike/compose-korge-interop` and `spike/korge7-bump` for the Compose/upgrade paths.
