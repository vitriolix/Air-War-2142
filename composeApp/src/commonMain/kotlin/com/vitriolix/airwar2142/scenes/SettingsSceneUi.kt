package com.vitriolix.airwar2142.scenes

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.input.onClick
import korlibs.korge.input.onClickSuspend
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.style.*
import korlibs.korge.ui.*
import korlibs.korge.view.*
import korlibs.math.geom.Size
import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.logic.ControlMode
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.render.Fonts
import com.vitriolix.airwar2142.widgets.uiSteppedSlider

// ── SPIKE (spike/korge-ui-widgets) ───────────────────────────────────────────
// Settings on KorGE's `korlibs.korge.ui` widgets, skinned to the design, and driven
// by the app's own FocusController (gold caret + UP/DOWN move, LEFT/RIGHT, ENTER) so
// keyboard nav matches the rest of the game — instead of KorGE's Tab-only focus
// manager. UIComboBox can't be themed (Material colours hardcoded), so steering is a
// skinned 3-button segmented control.
private val CYAN = Colors["#00E5FF"]
private val GHOST = RGBA(255, 255, 255, 20)
private val CYAN_FILL = RGBA(0, 229, 255, 40)
private val CYAN_HOVER = RGBA(0, 229, 255, 70)

private fun UIButton.panelSkin(active: Boolean = false, accent: RGBA = CYAN) {
    elevation = false
    radius = 4.0
    textSize = 32.0
    bgColorOut = if (active) CYAN_FILL else GHOST
    bgColorOver = CYAN_HOVER
    bgColorSelected = CYAN_HOVER
    textColor = if (active) accent else Colors.WHITE
    background.borderColor = if (active) accent else RGBA(255, 255, 255, 45)
    background.borderSize = if (active) 2.0 else 1.0
}

class SettingsSceneUi(
    private val engine: GameEngine,
    private val nav: SceneContainer,
    private val fromGame: Boolean = false,
    private val initialFocus: Int = 0,
) : Scene(), KeyboardNavigable, EscapeHandler {

    override var focusController: FocusController? = null
    private var exitAction: (suspend () -> Unit)? = null
    override suspend fun onEscape(): Boolean { exitAction?.invoke(); return true }

    override suspend fun SContainer.sceneMain() {
        Fonts.load()
        val ctx = views.coroutineContext
        solidRect(1000.0, CANVAS_HEIGHT.toDouble(), Colors["#1E272C"])

        styles {
            textFont = Fonts.content
            textColor = Colors.WHITE
            textSize = 34.0
            uiSelectedColor = CYAN
            uiUnselectedColor = RGBA(255, 255, 255, 90)
        }

        text("SETTINGS", 72.0, CYAN, font = Fonts.title).position(100.0, 70.0)

        val fc = FocusController()

        // ── STEERING MODE — skinned segmented control ──
        uiText("STEERING MODE", Size(360.0, 40.0)) { styles { textSize = 30.0; textColor = RGBA(255, 255, 255, 178) } }
            .position(100.0, 190.0)
        val modes = listOf(ControlMode.KEYBOARD, ControlMode.TILT, ControlMode.TOUCH)
        val modeLabels = listOf("KEYBOARD  (WASD / Space / R)", "TILT  (Accelerometer)", "TOUCH  (Drag / double-tap)")
        val modeButtons = ArrayList<UIButton>()
        fun selectMode(sel: Int) = modeButtons.forEachIndexed { i, b -> b.panelSkin(active = i == sel) }
        // Same action for click and keyboard-activate.
        val modeActs = modes.indices.map { i -> suspend { engine.setControlMode(modes[i]); selectMode(i) } }
        modeLabels.forEachIndexed { i, label ->
            val b = uiButton(label = label, size = Size(800.0, 72.0)) { onClick { engine.setControlMode(modes[i]); selectMode(i) } }
            b.position(100.0, 250.0 + i * 84.0)
            modeButtons.add(b)
        }
        selectMode(modes.indexOf(engine.controlMode.value).coerceAtLeast(0))

        // ── SENSITIVITY — reusable stepped slider; LEFT/RIGHT drive it via focus ──
        val sensLabel = uiText("SENSITIVITY: ${fmt(engine.sensitivity)}x", Size(500.0, 40.0)) { styles { textSize = 32.0 } }
        sensLabel.position(100.0, 540.0)
        val sens = uiSteppedSlider(value = engine.sensitivity.toDouble(), min = 0.5, max = 2.5, step = 0.1,
            size = Size(800.0, 56.0), skin = { panelSkin() }) {
            onChange { v -> engine.sensitivity = v.toFloat(); sensLabel.text = "SENSITIVITY: ${fmt(engine.sensitivity)}x" }
        }
        sens.position(100.0, 590.0)

        // ── SFX + DEBUG ──
        val sfxCb = uiCheckBox(Size(360.0, 56.0), checked = engine.sfxEnabled, text = "SFX enabled") {
            onChange { engine.sfxEnabled = it.checked }
        }
        sfxCb.position(100.0, 680.0)
        val dbgCb = uiCheckBox(Size(420.0, 56.0), checked = engine.showDebugOverlay, text = "Debug overlay") {
            onChange { engine.showDebugOverlay = it.checked }
        }
        dbgCb.position(100.0, 760.0)

        // ── CONTROLLER + EXIT ──
        uiButton(label = "CONTROLLER", size = Size(360.0, 72.0)) {
            panelSkin()
            onClickSuspend(ctx) { nav.changeTo { ControllerPrefsScene(engine, nav, fromGame) } }
        }.position(100.0, 860.0)

        val doExit: suspend () -> Unit = if (fromGame) {
            { engine.togglePause(); nav.changeTo { GameScene(engine, nav) } }
        } else {
            { nav.changeTo { MenuScene(engine, nav) } }
        }
        exitAction = doExit
        uiButton(label = if (fromGame) "RESUME GAME" else "SAVE & EXIT", size = Size(380.0, 80.0)) {
            panelSkin(active = true, accent = Colors["#FFCC00"])
            textSize = 38.0
            onClickSuspend(ctx) { doExit() }
        }.position(100.0, CANVAS_HEIGHT - 200.0)

        // ── Focus model — the app's caret-based controller (UP/DOWN/LEFT/RIGHT/ENTER
        //    routed from main.kt). Order: 3 modes, slider, SFX, debug, controller, exit.
        modeActs.forEachIndexed { i, act -> fc.add(caretX = 64.0, caretY = 250.0 + i * 84.0 + 14.0, activate = act) }
        fc.add(caretX = 64.0, caretY = 590.0 + 6.0, activate = {},
            leftAct = { sens.decrement() }, rightAct = { sens.increment() })
        fc.add(caretX = 64.0, caretY = 680.0 + 6.0,
            activate = { engine.sfxEnabled = !engine.sfxEnabled; sfxCb.checked = engine.sfxEnabled })
        fc.add(caretX = 64.0, caretY = 760.0 + 6.0,
            activate = { engine.showDebugOverlay = !engine.showDebugOverlay; dbgCb.checked = engine.showDebugOverlay })
        fc.add(caretX = 64.0, caretY = 860.0 + 14.0,
            activate = { nav.changeTo { ControllerPrefsScene(engine, nav, fromGame) } })
        fc.add(caretX = 64.0, caretY = CANVAS_HEIGHT - 200.0 + 18.0, activate = doExit)

        // Gold caret marker, created last so it draws above the widgets.
        fc.caret = solidRect(18.0, 44.0, Colors["#FFCC00"])
        fc.start(initialFocus)
        focusController = fc
    }

    private fun fmt(s: Float): String = "${(s * 10).toInt() / 10}.${(s * 10).toInt() % 10}"
}
