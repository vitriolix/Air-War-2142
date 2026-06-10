import com.example.clone1942.CANVAS_HEIGHT
import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.input.keys
import korlibs.korge.scene.SceneContainer
import com.example.clone1942.audio.NoOpSoundPlayer
import com.example.clone1942.input.NoOpSensorInput
import com.example.clone1942.logic.GameEngine
import com.example.clone1942.logic.GameState
import com.example.clone1942.scenes.EscapeHandler
import com.example.clone1942.scenes.GameScene
import com.example.clone1942.scenes.KeyboardNavigable
import com.example.clone1942.scenes.MenuScene
import com.example.clone1942.scenes.SettingsScene

suspend fun main() = Korge(
    windowWidth = 1000,
    windowHeight = CANVAS_HEIGHT,
    title = "1942 Retro Clone",
    bgcolor = Colors["#0F2027"]
) {
    // Prevent the platform from acting on ESC/back before the game handles it
    views.gameWindow.exitProcessOnClose = false

    val engine = GameEngine(NoOpSoundPlayer(), NoOpSensorInput())
    val sc = SceneContainer(views = views)
    addChild(sc)

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
        down(Key.UP)   { navFocus()?.move(-1) }
        down(Key.DOWN) { navFocus()?.move(+1) }
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
            }
        }
    }

    sc.changeTo { MenuScene(engine, sc) }
}
