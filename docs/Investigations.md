# Investigations

A running log of investigation-only sessions: the **literal prompt** that triggered each
one, the **full literal findings**, and any **follow-up prompts + responses**. Investigations
are discovery/strategy only — no code changes in the investigation itself — so this file is
the durable record of "what we learned and why", separate from `TASKS.md` (work board) and
`docs/` (settled design).

Newest entries on top. Each thread/follow-up carries a **Status**:
`Investigating` · `Deciding` · `Planning` · `Deferred` · `Done` · `Invalid`.

---

## 004 — Interactive Gradle tasks + Sketchfab automated download (retrospective)

**Date:** 2026-07-01 to 2026-07-02 · **Overall status:** `Done` (both landed as code — see PRs #46–#48 — but logged here retroactively since the amount of real investigation involved was worth preserving verbatim, not just as a compacted memory summary; see the info-storage-flow follow-up task in `TASKS.md`).

### Prompt (paraphrased across the session — not literal, since this wasn't flagged as investigation-only up front)

> Can a Gradle task ever be made truly interactive (read real terminal input)? And separately: can we automate Sketchfab model downloads instead of manual click-through, and if so, how (API key vs OAuth, what grant type, what does the token lifecycle look like)?

### Findings — Gradle daemon architecture

**Verdict: task execution always happens inside a forked, terminal-detached "Gradle build daemon" JVM — confirmed via `--info`, not documented prominently anywhere obvious.** `--no-daemon` does not mean "run in-process" — it only controls whether that daemon is *kept alive for reuse* afterward. A fresh daemon is still forked to actually execute the task, with Gradle-internal `--add-opens`/`--add-exports` JVM flags a bootstrap `java` invocation could never organically match (visible in `--info` output as `Starting process 'Gradle build daemon'...`). That daemon has no OS-level controlling terminal, so `/dev/tty`-probing inside a task's `Exec`'d subprocess can never detect a real terminal — tried matching `JAVA_HOME` and `org.gradle.jvmargs` to the invoking JVM to dodge the fork entirely; still forked both times.

**What does work:** Gradle's client↔daemon protocol can forward the launching terminal's real keystrokes if a task explicitly wires `standardInput = System.`in`` on its `ExecSpec` — confirmed end-to-end against a real terminal. Paired with `--console=plain` (stops the rich-UI redraws from interleaving with/hiding a subprocess's own prompt output) and `--no-daemon` (avoids leaving a stale persistent daemon around). Not auto-detectable from inside a daemon-executed task (no reliable signal), so it has to be an explicit opt-in flag (`--force-prompt`) set only by a dedicated wrapper script — never the task's default behavior, to keep the plain invocation CI-safe.

Two real bugs surfaced getting this to actually work, both general bash/shell gotchas beyond this specific feature:
1. A helper function invoked via `x="$(fn)"` (command substitution) only exposes **stdout** as the "return value" — any status/prompt text the function prints to stdout gets silently absorbed into that captured value instead of ever reaching the screen. Fixed by routing all status/prompt output to stderr.
2. Command substitution also forks a **subshell** — any variable a function sets internally (not just what it prints) is invisible to the caller once that subshell exits. Under this repo's `set -u`, reading one of those vars back in the caller crashes with "unbound variable." Fixed by having such functions set global variables directly and calling them as plain statements instead of via `$(...)`.

### Findings — Sketchfab Download API / OAuth2

**Verdict: no static personal API key works for the Download API specifically — confirmed empirically (`{"detail":"Invalid API token"}` when trying the account Settings→Password API token).** The Download API (an extension of Sketchfab's Data API) requires a Bearer OAuth2 access token. Of Sketchfab's three grant types — Authorization Code, Implicit, Resource Owner Password Credentials — chose Authorization Code: one-time browser consent click, the app's password never touches the script (Sketchfab's own docs flag Password Credentials as "less secure"). Implicit was ruled out because it has no refresh token (would need re-auth on every single token expiry, bad for unattended automation).

Sketchfab removed their self-service OAuth-app-registration dashboard at some point — registering an app is now a support-request process with exactly 4 fields (Application name, Grant type, Redirect URI, Username), documented at `sketchfab.com/developers/oauth#registering-your-app`. Verified the real request/response shapes empirically against the live API (not just docs, which were sometimes JS-rendered/unreadable via fetch): the token-exchange endpoint returns `invalid_grant` (not `invalid_client`) when the `client_id`/`client_secret` are valid but the authorization `code` isn't — a useful diagnostic that doesn't require completing the full browser flow. The Download API itself only ever returns glTF (as a ZIP: `scene.gltf` + `scene.bin` + textures) or USDZ — never `.glb` or original source formats (FBX/OBJ/Blend/Maya), confirmed both via docs and by inspecting a real downloaded archive's contents.

Access tokens last 1 month; the Authorization Code grant's refresh token renews them silently (verified: a second run reused the cached refresh token with zero browser interaction). If a refresh token itself goes dead (revoked/expired), the fix is to clear it and re-run the browser consent once — not documented explicitly by Sketchfab, inferred from OAuth2 conventions and confirmed working in practice.

### Why this is logged here despite not being flagged investigation-only up front

Per `feedback-log-investigations` memory, this file is meant for investigation-only (no-code-change) sessions — this one had extensive real code changes too. Logging it anyway because the debugging/research volume was substantial enough that losing the verbatim detail to memory summarization felt like a real cost — see the open follow-up task in `TASKS.md` to revisit where this boundary should actually sit.

---

## 003 — Claude Desktop ↔ Blender MCP Integration (Intel Mac / Blender 4.5)

**Date:** 2026-06-29 · **Overall status:** `Done` (full stack validated).

### Prompt

> Investigate: establish a working MCP connection between Claude Desktop and Blender so Claude can read scene state, execute `bpy` code, and capture viewport screenshots. Intel Mac environment, Blender 4.5.11 LTS.

### Findings

**Verdict: Full integration working. Intel Mac is permanently locked to Blender 4.x LTS (Blender 5.x dropped Intel support), so the official Blender Lab MCP addon (requires 5.1+) is not viable. A community addon stack works reliably.**

#### Correct Stack (Intel Mac)

| Component | What it is | Source |
|-----------|-----------|--------|
| Blender 4.5.x LTS | The 3D app | blender.org |
| Community MCP addon | TCP server inside Blender on port 9876 | github.com/ahujasid/blender-mcp (`addon.py`) |
| `blender-mcp` pip package | stdio↔TCP bridge that Claude Desktop spawns | PyPI via `uvx` |
| Claude Desktop | MCP client | Anthropic |

#### Installation & Configuration

1. **Community addon:** Download `addon.py` from `https://github.com/ahujasid/blender-mcp/blob/main/addon.py` (Raw link). In Blender: **Edit → Preferences → Add-ons → Install from Disk** → enable. Version 1.2+ auto-starts the TCP server on `localhost:9876` when enabled.

2. **Claude Desktop config:** File location: `~/Library/Application Support/Claude/claude_desktop_config.json`
   ```json
   {
     "mcpServers": {
       "blender": {
         "command": "uvx",
         "args": ["blender-mcp"]
       }
     },
     ...rest of config
   }
   ```
   Use `uvx` instead of bare `blender-mcp` — Claude Desktop spawns processes with its own limited environment PATH and does not inherit the shell's user-local paths (e.g., `~/.local/bin`). Bare command names silently fail at runtime. If `uvx` is not found, use the full absolute path (run `which uvx` to find it).

3. **Launch order:** Blender must start before Claude Desktop. (1) Launch Blender (addon auto-starts server), (2) verify with `lsof -i :9876`, (3) launch Claude Desktop. If Claude Desktop launches first, it throws `"MCP blender: Server disconnected"` — this is a timing error, not a config error. Quit Claude Desktop, ensure Blender is running, relaunch.

#### Core Failure Mode: Port 9876 Conflict

Only one process can hold `localhost:9876`. Sources of conflict observed in practice:
- Running `blender-mcp` manually in a terminal while Claude Desktop is active (Claude Desktop spawns its own instance, both fight for the port)
- Having both the community addon entry *and* the official Claude Desktop Extension (`Blender:` uppercase) active simultaneously
- A stale `blender-mcp` process from a previous session

**Fixes:** One entry in `mcpServers`, no more. Remove the official Blender extension from Claude Desktop Extensions panel if installed. Never run `blender-mcp` from the terminal — Claude Desktop owns that process. Diagnostic: `lsof -i :9876` to see what's holding the port; kill orphaned processes.

#### MCP Tool Negotiation

MCP tools are negotiated **at conversation start**, not mid-conversation. Config changes require fully quitting Claude Desktop (Cmd+Q) and relaunching, not just closing the window. If a connectivity issue is fixed mid-conversation, tools will not appear — start a fresh conversation. Stuck tool-call states (spinning, toast errors) block the entire thread; the only fix is a new conversation.

#### Screenshot / Viewport Image (4.5 LTS Limitation)

This is a **known gap** with no clean solution on 4.5.

| Tool | Works? | Notes |
|------|--------|-------|
| `blender:get_viewport_screenshot` | Partially | Image visible to Claude in context window but does NOT appear in the chat UI. Useless for joint review. |
| `Blender:render_viewport_to_path` | No | Only on the official 5.1+ addon. Not available on 4.5. |
| `bpy.ops.render.opengl(write_still=True)` | Yes | Saves to local disk at `scene.render.filepath`. Claude cannot see the file directly, but it lands on your machine at the specified path. |

**Workaround for saving to disk:**
```python
import bpy
bpy.context.scene.render.filepath = "/tmp/viewport_shot.png"
bpy.ops.render.opengl(write_still=True)
```

Joint visual review currently requires an external step (e.g., Claude Design with exported SVG schematics) rather than live viewport capture.

#### Summary of Fixes Applied

1. Diagnosed missing `mcpServers` block in config entirely → added it
2. Identified dual-server conflict (lowercase `blender:` pip install + uppercase `Blender:` Claude Extension) → removed Extension, kept single community addon entry
3. Switched from bare `blender-mcp` command to `uvx blender-mcp` → resolved PATH issue
4. Replaced manual terminal bridge runs with Claude Desktop–managed process → eliminated port conflicts
5. Established launch order: Blender first, then Claude Desktop

#### Open Items

- No clean live screenshot pipeline on Intel/4.5. Options: (a) accept the `bpy.ops.render.opengl` workaround, (b) set up a file watcher on `/tmp/` and display externally, (c) wait for a community addon update that adds a screenshot endpoint.
- `addon (1).py` duplicate file artifact in Blender preferences — low priority cleanup.

---

## 002 — Menu screens: pixel-perfect alignment vs Design spec

**Date:** 2026-06-27 · **Overall status:** `Done` (limitation identified).

### Prompt

> investigate: are you able to get the menu screens we made to look pixel perfect to the spec from Design or are there limitations?

### Findings

**Verdict: Pixel-perfect layout is NOT achievable cross-platform due to `text.height` platform divergence.** This is a known KorGE limitation already tracked in `TASKS.md` task-18.

#### Root Cause

**Text measurement differs between JVM and web backends.** KorGE's `Text.height` property returns different values on the JVM TTF renderer vs the JS/Canvas renderer, causing vertical centering math to produce different Y positions on each platform:

```kotlin
// ControllerPrefsScene.kt, line 65 & 72:
rowLabel.y = cy + (80.0 - rowLabel.height) / 2.0       // uses text.height
chipLabel.y = (cy + 12.0) + (56.0 - chipLabel.height) / 2.0  // uses text.height
```

On JVM, this positions text correctly centered in the row. On web (JS/Canvas), `text.height` is measured differently (font metrics resolution differs), so the result is off-center.

#### Current Implementation Status

All three menu screens are **functionally complete and mostly aligned** visually:

1. **Menu (1-menu.png)** — ✅ Layouts correct: "AIR WAR" cyan title, "2142" gold, buttons, seed editor
2. **Settings (4-settings.png)** — ✅ Layouts correct: title, 3 steering rows with active/inactive, toggles, exit button
3. **Controller Prefs (5-controller-prefs.png)** — ✅ Layouts correct: title, binding chips right-anchored, toggles, slider, back button

**Observed visual alignment:**
- Caret positioning is *correct on the JVM build*, *off on the web build* (captured via Playwright)
- Text rows appear vertically centered on both, but by different mechanisms (JVM's `text.height` = actual metric; web's = approximate)
- Button backgrounds and spacing match the spec

#### Technical Limitations

**Three approaches to pixel-perfect alignment, each with tradeoffs:**

1. **Backend-agnostic cap-height fraction** (recommended per TASKS)
   - Replace `(height - text.height) / 2.0` with `y + height * 0.3` (fixed fraction as a constant)
   - Pro: deterministic across platforms, no `text.height` dependency
   - Con: requires empirical tuning per font size + font family; may drift if design fonts change
   - Effort: low (changes scattered across three scenes; ~15 lines)

2. **Use KorGE's `textBounds` API if available** (aspirational)
   - If KorGE 6 exposes a `textBounds: Rectangle` that's consistent across backends, use it instead of `text.height`
   - Pro: proper bounding-box semantics
   - Con: would need to verify API exists and is reliable
   - Effort: low IF the API exists; high if it doesn't or needs workarounds

3. **Accept cross-platform visual drift** (current state)
   - Keep the code as-is, document that JVM = canonical, web ≈ canonical
   - Pro: no code churn, spec.html captures JVM baseline (the "correct" one)
   - Con: web users see text slightly off-center on some rows
   - Effort: zero (status quo)

#### Design Spec Compliance

**The current code matches the design spec *in intent* but not in pixel-exact cross-platform reproduction.** The Design bundle (`spec.html`) captures screenshots from the JVM build (which are pixel-perfect), so the spec IS being met on JVM. The web divergence is a KorGE runtime limitation, not a code bug.

#### Why It Matters / Doesn't Matter

- **Matters for:** web UI polish, cross-platform consistency, future port to Compose (which may have similar metrics issues)
- **Doesn't matter for:** gameplay, player perception (the misalignment is ~2-4 logical pixels in text Y), function (buttons and toggles work)
- **Historical note:** the issue existed before this investigation — TASKS task-18 is open and was acknowledged during the controller-prefs UI work (PR #25)

#### Recommendation

**Defer detailed pixel-perfect fixes** until task-18 is resolved (likely as part of a broader typography/metrics review). For now:
- Acknowledge the cross-platform divergence in code comments (`// text.height platform divergence: JVM = exact, web ≈ empirical`)
- If web user feedback surfaces poor alignment perception, revisit with approach #1 (cap-height fraction)
- Track this as a side effect of the known task-18 limitation, not a separate bug

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
