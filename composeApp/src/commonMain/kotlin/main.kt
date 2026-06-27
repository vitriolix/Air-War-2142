import com.vitriolix.airwar2142.CANVAS_HEIGHT
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.Korge
import korlibs.korge.input.keys
import korlibs.korge.scene.SceneContainer
import korlibs.korge.view.container
import korlibs.korge.view.position
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import com.vitriolix.airwar2142.audio.NoOpSoundPlayer
import com.vitriolix.airwar2142.input.NoOpSensorInput
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.logic.GameState
import com.vitriolix.airwar2142.render.Fonts
import com.vitriolix.airwar2142.scenes.ControllerPrefsScene
import com.vitriolix.airwar2142.scenes.EscapeHandler
import com.vitriolix.airwar2142.scenes.GameScene
import com.vitriolix.airwar2142.scenes.KeyboardNavigable
import com.vitriolix.airwar2142.scenes.MenuScene
import com.vitriolix.airwar2142.scenes.SettingsScene
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
    // without playing to it. The jump keys (M/H/U/S/K/O/V) are chosen to not collide with
    // any in-game/menu bind, and only act while this picker is open.
    Fonts.load()
    val jumpOverlay = container {
        visible = false
        solidRect(620.0, 660.0, RGBA(0, 0, 0, 225)).position(60.0, 200.0)
        listOf(
            "JUMP TO SCREEN", "",
            "[ M ]  Menu",
            "[ H ]  HUD / in-game",
            "[ U ]  Paused",
            "[ S ]  Settings",
            "[ K ]  Controller",
            "[ O ]  Game Over",
            "[ V ]  Victory",
            "",
            "[ J ]  close"
        ).forEachIndexed { i, s ->
            text(s, if (i == 0) 44.0 else 34.0, Colors["#00E5FF"], font = Fonts.content)
                .position(100.0, 232.0 + i * 52.0)
        }
    }
    fun setJump(v: Boolean) { jumpOverlay.visible = v && engine.showDebugOverlay }
    // True only when a screen-jump key should act: debug on AND the picker open.
    fun jumpArmed() = engine.showDebugOverlay && jumpOverlay.visible

    // Current scene's focus controller, if it supports keyboard/gamepad navigation.
    fun navFocus() = (sc.currentScene as? KeyboardNavigable)?.focusController

    // All discrete key handling lives on the stage — the stage's KeysComponent is the only
    // one guaranteed to receive events regardless of which scene is active.
    stage.keys {
        // ── Game scene controls ────────────────────────────────────────────────
        down(Key.R) {
            if (engine.gameState.value == GameState.PLAYING) engine.triggerRoll()
        }
        // ESC / Android back → let the current scene decide: GameScene opens settings,
        // SettingsScene closes itself (toggle). Routed so a gamepad B button can reuse it.
        down(Key.ESCAPE) { (sc.currentScene as? EscapeHandler)?.onEscape() }
        down(Key.BACK)   { (sc.currentScene as? EscapeHandler)?.onEscape() }
        down(Key.P) { engine.togglePause() }
        // ── Debug jump-to-screen picker ─────────────────────────────────────────
        // J toggles the picker (only while the debug overlay is up). Each screen key
        // acts only while the picker is open (jumpArmed()), so it can't fire in normal
        // play. Forced states (Paused/Game Over/Victory) use debugForceState after
        // startGame() sets up a live world behind the overlay.
        down(Key.J) { if (engine.showDebugOverlay) setJump(!jumpOverlay.visible) }
        down(Key.M) { if (jumpArmed()) { setJump(false); engine.returnToMenu(); sc.changeTo { MenuScene(engine, sc) } } }
        down(Key.H) { if (jumpArmed()) { setJump(false); engine.startGame(); sc.changeTo { GameScene(engine, sc) } } }
        down(Key.U) { if (jumpArmed()) { setJump(false); engine.startGame(); engine.debugForceState(GameState.PAUSED); sc.changeTo { GameScene(engine, sc) } } }
        down(Key.S) { if (jumpArmed()) { setJump(false); sc.changeTo { SettingsScene(engine, sc) } } }
        down(Key.K) { if (jumpArmed()) { setJump(false); sc.changeTo { ControllerPrefsScene(engine, sc) } } }
        down(Key.O) { if (jumpArmed()) { setJump(false); engine.startGame(); engine.debugForceState(GameState.GAME_OVER); sc.changeTo { GameScene(engine, sc) } } }
        down(Key.V) { if (jumpArmed()) { setJump(false); engine.startGame(); engine.debugForceState(GameState.LEVEL_COMPLETE); sc.changeTo { GameScene(engine, sc) } } }
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
        down(Key.C) {
            if (engine.gameState.value == GameState.PAUSED) (sc.currentScene as? GameScene)?.copySeedFromPause()
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
                engine.showDebugOverlay = !engine.showDebugOverlay
                if (!engine.showDebugOverlay) setJump(false)  // hide the jump picker with the overlay
            }
        }
    }

    sc.changeTo { MenuScene(engine, sc) }
}
