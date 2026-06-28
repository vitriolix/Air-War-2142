import com.vitriolix.airwar2142.CANVAS_HEIGHT
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.Korge
import korlibs.korge.input.keys
import korlibs.korge.scene.SceneContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import com.vitriolix.airwar2142.audio.NoOpSoundPlayer
import com.vitriolix.airwar2142.input.NoOpSensorInput
import com.vitriolix.airwar2142.logic.DebugSubMenu
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.logic.GameState
import com.vitriolix.airwar2142.render.Fonts
import com.vitriolix.airwar2142.scenes.ControllerPrefsScene
import com.vitriolix.airwar2142.scenes.EscapeHandler
import com.vitriolix.airwar2142.scenes.GameScene
import com.vitriolix.airwar2142.scenes.KeyboardNavigable
import com.vitriolix.airwar2142.scenes.MenuScene
import com.vitriolix.airwar2142.scenes.SettingsScene
import com.vitriolix.airwar2142.scenes.onSubMenu
import com.vitriolix.airwar2142.scenes.TextInputTarget

suspend fun main() = Korge(
    windowWidth = 1000,
    windowHeight = CANVAS_HEIGHT,
    title = "Air War 2142",
    bgcolor = Colors["#0F2027"]
) {
    // Prevent the platform from acting on ESC/back before the game handles it
    views.gameWindow.exitProcessOnClose = false

    val engine = GameEngine(NoOpSoundPlayer(), NoOpSensorInput())
    val sc = SceneContainer(views = views)
    addChild(sc)

    // ── Debug "jump to screen" picker (dev tool) ────────────────────────────────
    // A stage-level overlay (above all scenes) so it works from any scene. Gated on the
    // debug overlay (`~`): press J to toggle this picker, then a per-screen key to jump
    // straight there — including Game Over / Victory / Paused, which normal play can only
    // reach by dying / killing a boss / pausing. Lets tests + captures land on any screen
    // without playing to it. Each jump key only acts while this picker is up (onSubMenu),
    // and conflicting global binds (P=pause, C=copy-seed, G=motion) yield to it while open.
    // Docked bottom-left, sharing the base readout's footprint (x=8, bottom edge ch-46).
    Fonts.load()
    val jumpItems = listOf(
        "JUMP TO SCREEN", "",
        "[ M ]  Menu",
        "[ H ]  HUD / in-game",
        "[ P ]  Paused",
        "[ S ]  Settings",
        "[ C ]  Controller",
        "[ G ]  Game Over",
        "[ V ]  Victory",
        "",
        "[ J ] / [ ESC ]  back"
    )
    val jumpRowH = 40.0; val jumpPadX = 20.0; val jumpPadTop = 16.0
    val jumpPanelW = 396.0
    val jumpPanelH = jumpPadTop * 2 + jumpItems.size * jumpRowH
    val jumpOverlay = container {
        visible = false
        solidRect(jumpPanelW, jumpPanelH, RGBA(0, 0, 0, 225))
        jumpItems.forEachIndexed { i, s ->
            text(s, if (i == 0) 30.0 else 26.0, Colors["#00E5FF"], font = Fonts.content)
                .position(jumpPadX, jumpPadTop + i * jumpRowH)
        }
    }.position(8.0, CANVAS_HEIGHT.toDouble() - 46.0 - jumpPanelH)
    // The jump picker is the JUMP debug sub-menu. Visibility of every debug surface is
    // derived from the single source of truth (engine.debugSubMenu) by per-frame updaters
    // (the stage one below + GameScene's), so transitions only ever set state — they never
    // poke individual overlays (which is what let cross-file menus drift out of sync).
    fun gotoMenu(m: DebugSubMenu) { if (engine.showDebugOverlay) engine.debugSubMenu = m }

    // Current scene's focus controller, if it supports keyboard/gamepad navigation.
    fun navFocus() = (sc.currentScene as? KeyboardNavigable)?.focusController

    // All discrete key handling lives on the stage — the stage's KeysComponent is the only
    // one guaranteed to receive events regardless of which scene is active.
    stage.keys {
        // ── Game scene controls ────────────────────────────────────────────────
        down(Key.R) {
            if (engine.gameState.value == GameState.PLAYING) engine.triggerRoll()
        }
        // ESC / Android back → if a debug sub-menu (Jump/Motion) is open, back OUT to the
        // base debug state first; otherwise let the current scene decide: GameScene opens
        // settings, SettingsScene closes itself (toggle). Routed so a gamepad B can reuse it.
        suspend fun escapeAction() {
            if (engine.showDebugOverlay && engine.debugSubMenu != DebugSubMenu.NONE) gotoMenu(DebugSubMenu.NONE)
            else (sc.currentScene as? EscapeHandler)?.onEscape()
        }
        down(Key.ESCAPE) { escapeAction() }
        down(Key.BACK)   { escapeAction() }
        down(Key.P) { if (engine.debugSubMenu != DebugSubMenu.JUMP) engine.togglePause() }  // yields 'P' to the JUMP picker
        // ── Debug jump-to-screen picker ─────────────────────────────────────────
        // J toggles the picker (only while the debug overlay is up). Each screen key
        // acts only while the picker is open (jumpArmed()), so it can't fire in normal
        // play. Forced states (Paused/Game Over/Victory) use debugForceState after
        // startGame() sets up a live world behind the overlay.
        // J toggles the JUMP picker — but yields `j` to the MOTION panel (row-down) while
        // that menu owns it. Both handlers fire (same stage), so this guard is how the key
        // is effectively "unbound" here when MOTION is active.
        down(Key.J) {
            when (engine.debugSubMenu) {
                DebugSubMenu.MOTION -> {}                          // 'j' belongs to the motion panel
                DebugSubMenu.JUMP   -> gotoMenu(DebugSubMenu.NONE) // close → back to BASE
                DebugSubMenu.NONE   -> gotoMenu(DebugSubMenu.JUMP) // open (no-op if debug off)
            }
        }
        // Screen-jump keys — only live while the JUMP sub-menu is the active debug menu
        // (uniform onSubMenu gating). Each backs out to BASE then jumps; the debug overlay
        // stays on, so the destination scene shows its base readout.
        onSubMenu(engine, DebugSubMenu.JUMP, Key.M) { gotoMenu(DebugSubMenu.NONE); engine.returnToMenu(); sc.changeTo { MenuScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.H) { gotoMenu(DebugSubMenu.NONE); engine.startGame(); sc.changeTo { GameScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.P) { gotoMenu(DebugSubMenu.NONE); engine.startGame(); engine.debugForceState(GameState.PAUSED); sc.changeTo { GameScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.S) { gotoMenu(DebugSubMenu.NONE); sc.changeTo { SettingsScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.C) { gotoMenu(DebugSubMenu.NONE); sc.changeTo { ControllerPrefsScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.G) { gotoMenu(DebugSubMenu.NONE); engine.startGame(); engine.debugForceState(GameState.GAME_OVER); sc.changeTo { GameScene(engine, sc) } }
        onSubMenu(engine, DebugSubMenu.JUMP, Key.V) { gotoMenu(DebugSubMenu.NONE); engine.startGame(); engine.debugForceState(GameState.LEVEL_COMPLETE); sc.changeTo { GameScene(engine, sc) } }
        down(Key.Q) {
            val st = engine.gameState.value
            if (st == GameState.GAME_OVER || st == GameState.PAUSED || st == GameState.LEVEL_COMPLETE) {
                engine.returnToMenu()
                sc.changeTo { MenuScene(engine, sc) }
            }
        }
        down(Key.RETURN) {
            val focus = navFocus()
            if (focus != null) {
                focus.activate()   // activate the focused menu item
            } else when (engine.gameState.value) {
                GameState.GAME_OVER      -> engine.startGame()
                GameState.LEVEL_COMPLETE -> engine.proceedToNextLevel()
                else -> {}
            }
        }
        // ── Menu/UI focus navigation (arrows) ───────────────────────────────────
        // No-ops in GameScene (not KeyboardNavigable), so in-game arrow movement is
        // unaffected. Same move()/activate() entry points a gamepad can drive later.
        down(Key.UP)    { navFocus()?.move(-1) }
        down(Key.DOWN)  { navFocus()?.move(+1) }
        down(Key.LEFT)  { navFocus()?.activateLeft() }
        down(Key.RIGHT) { navFocus()?.activateRight() }
        // vi-style focus nav IN ADDITION to the arrows, for consistency with the debug
        // panels. Gated on !showDebugOverlay so they can't clash with the debug menus'
        // h/j/k/l (which require the overlay) or the J jump-toggle. No-op in GameScene
        // (navFocus() == null), so flight (arrows/WASD) is untouched.
        down(Key.K) { if (!engine.showDebugOverlay) navFocus()?.move(-1) }
        down(Key.J) { if (!engine.showDebugOverlay) navFocus()?.move(+1) }
        down(Key.H) { if (!engine.showDebugOverlay) navFocus()?.activateLeft() }
        down(Key.L) { if (!engine.showDebugOverlay) navFocus()?.activateRight() }
        // ── Text entry (seed field) ─────────────────────────────────────────────
        // Routed stage-level like the rest; the scene gates whether it's capturing.
        // Digits only (seeds are numeric for now); backspace edits. No per-frame polling.
        down(Key.BACKSPACE) { (sc.currentScene as? TextInputTarget)?.onBackspace() }
        down { ev ->
            // Digit chars are unreliable on JS key-down (same reason the grave handler uses keyCode);
            // derive the digit from keyCode: 48–57 top row, 96–105 numpad. character is a fallback.
            val kc = ev.keyCode
            val c: Char? = when {
                kc in 48..57  -> '0' + (kc - 48)
                kc in 96..105 -> '0' + (kc - 96)
                ev.character.isDigit() -> ev.character
                else -> null
            }
            if (c != null) (sc.currentScene as? TextInputTarget)?.onChar(c)
        }
        // [C] copies the seed on the pause overlay (the Menu editor's COPY is a focus item).
        // Yields 'C' to the JUMP picker (where it = Controller) while that menu is open.
        down(Key.C) {
            if (engine.debugSubMenu != DebugSubMenu.JUMP && engine.gameState.value == GameState.PAUSED)
                (sc.currentScene as? GameScene)?.copySeedFromPause()
        }
        // ~ (backtick/tilde key) toggles the debug overlay, console-style.
        // The grave key maps to DIFFERENT Key enums per backend: JVM/AWT → Key.BACKQUOTE,
        // Android → Key.GRAVE. Match both (plus the typed character as a layout-agnostic
        // fallback) in one keydown handler so it can't double-toggle.
        down { ev ->
            // Grave key is reported differently per backend: JVM=Key.BACKQUOTE, Android=Key.GRAVE,
            // JS=Key.UNKNOWN (not mapped). keyCode 192 is the grave key on JS *and* JVM, so it
            // covers the JS gap. All in one handler so it can't double-toggle.
            if (ev.key == Key.BACKQUOTE || ev.key == Key.GRAVE || ev.keyCode == 192 ||
                ev.character == '`' || ev.character == '~') {
                engine.showDebugOverlay = !engine.showDebugOverlay  // setter collapses any sub-menu when off
            }
        }
    }

    // Derive the JUMP picker's visibility from the shared debug state every frame, so it
    // stays in sync no matter which file triggered the transition (e.g. [G] in GameScene).
    stage.addUpdater { jumpOverlay.visible = engine.showDebugOverlay && engine.debugSubMenu == DebugSubMenu.JUMP }

    sc.changeTo { MenuScene(engine, sc) }
}
