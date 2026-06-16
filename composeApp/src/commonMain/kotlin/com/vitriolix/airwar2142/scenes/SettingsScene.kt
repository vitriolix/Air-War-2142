package com.vitriolix.airwar2142.scenes

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.input.onClickSuspend
import korlibs.korge.view.*
import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.logic.ControlMode
import com.vitriolix.airwar2142.logic.GameEngine

class SettingsScene(
    private val engine: GameEngine,
    private val nav: SceneContainer,
    private val fromGame: Boolean = false,
    private val initialFocus: Int = 0
) : Scene(), KeyboardNavigable, EscapeHandler {

    override var focusController: FocusController? = null

    // Set in sceneMain; ESC / Android back closes Settings exactly like the exit button.
    private var exitAction: (suspend () -> Unit)? = null
    override suspend fun onEscape(): Boolean {
        exitAction?.invoke()
        return true
    }

    override suspend fun SContainer.sceneMain() {
        solidRect(1000.0, 1500.0, Colors["#1E272C"])

        // Title — 28sp → 72vp
        text("FLIGHT CONTROLS", 72.0, Colors["#00E5FF"]).position(100.0, 80.0)

        // Section label — 14sp → 36vp
        text("STEERING MODE", 36.0, RGBA(255, 255, 255, 178)).position(100.0, 200.0)

        val fc = FocusController()
        val caretX = 48.0
        // The scene rebuilds on every change; re-open focused on the same control.
        fun rebuild(focus: Int): suspend () -> Unit =
            { nav.changeTo { SettingsScene(engine, nav, fromGame, focus) } }

        // Control mode options — focus indices 0..2
        listOf(
            ControlMode.KEYBOARD to "KEYBOARD  (WASD / Space / R)",
            ControlMode.TILT     to "TILT  (Accelerometer — stub)",
            ControlMode.TOUCH    to "TOUCH  (Drag / Double-tap to roll)"
        ).forEachIndexed { i, (mode, label) ->
            val cy = 290.0 + i * 110.0
            val selected = engine.controlMode.value == mode
            val bgColor = if (selected) RGBA(0, 229, 255, 40) else RGBA(255, 255, 255, 20)
            solidRect(800.0, 80.0, bgColor).position(100.0, cy)
            text(label, 36.0, if (selected) Colors["#00E5FF"] else Colors.WHITE).position(116.0, cy + 18.0)
            val act: suspend () -> Unit = { engine.setControlMode(mode); rebuild(i)() }
            solidRect(800.0, 80.0, RGBA(0, 0, 0, 1)).position(100.0, cy)
                .onClickSuspend(views.coroutineContext) { act() }
            fc.add(caretX, cy + 18.0, act)
        }

        // Sensitivity — 14sp → 36vp
        val s = engine.sensitivity; val sensStr = "${s.toInt()}.${((s % 1f) * 10).toInt()}"
        text("SENSITIVITY: ${sensStr}x", 36.0, Colors.WHITE).position(100.0, 660.0)

        // minus — focus index 3
        val decAct: suspend () -> Unit = {
            engine.sensitivity = (engine.sensitivity - 0.1f).coerceAtLeast(0.5f); rebuild(3)()
        }
        solidRect(80.0, 60.0, Colors["#FF9900"]).position(100.0, 710.0)
        text("  -  ", 36.0, Colors.WHITE).position(100.0, 718.0)
        solidRect(80.0, 60.0, RGBA(0, 0, 0, 1)).position(100.0, 710.0)
            .onClickSuspend(views.coroutineContext) { decAct() }
        fc.add(caretX, 718.0, decAct)

        // plus — focus index 4
        val incAct: suspend () -> Unit = {
            engine.sensitivity = (engine.sensitivity + 0.1f).coerceAtMost(2.5f); rebuild(4)()
        }
        solidRect(80.0, 60.0, Colors["#FF9900"]).position(210.0, 710.0)
        text("  +  ", 36.0, Colors.WHITE).position(210.0, 718.0)
        solidRect(80.0, 60.0, RGBA(0, 0, 0, 1)).position(210.0, 710.0)
            .onClickSuspend(views.coroutineContext) { incAct() }
        fc.add(168.0, 718.0, incAct)   // caret left of the + button

        // SFX toggle — focus index 5
        val sfxColor = if (engine.sfxEnabled) Colors["#00FF88"] else Colors["#FF3333"]
        val sfxAct: suspend () -> Unit = { engine.sfxEnabled = !engine.sfxEnabled; rebuild(5)() }
        solidRect(280.0, 70.0, sfxColor.withA(60)).position(100.0, 840.0)
        text(if (engine.sfxEnabled) "SFX: ON" else "SFX: OFF", 36.0, sfxColor).position(120.0, 852.0)
        solidRect(280.0, 70.0, RGBA(0, 0, 0, 1)).position(100.0, 840.0)
            .onClickSuspend(views.coroutineContext) { sfxAct() }
        fc.add(caretX, 852.0, sfxAct)

        // Debug overlay toggle — focus index 6
        val dbgColor = if (engine.showDebugOverlay) Colors["#00FF88"] else Colors["#FF3333"]
        val dbgAct: suspend () -> Unit = { engine.showDebugOverlay = !engine.showDebugOverlay; rebuild(6)() }
        solidRect(440.0, 70.0, dbgColor.withA(60)).position(100.0, 960.0)
        text(if (engine.showDebugOverlay) "DEBUG OVERLAY: ON" else "DEBUG OVERLAY: OFF", 36.0, dbgColor).position(120.0, 972.0)
        solidRect(440.0, 70.0, RGBA(0, 0, 0, 1)).position(100.0, 960.0)
            .onClickSuspend(views.coroutineContext) { dbgAct() }
        fc.add(caretX, 972.0, dbgAct)

        // Exit button — focus index 7. "Resume Game" from an active mission, else "Save & Exit".
        val backY = CANVAS_HEIGHT - 200.0
        val exitLabel = if (fromGame) "RESUME GAME" else "SAVE & EXIT"
        val exitColor = if (fromGame) Colors["#00FF88"] else Colors["#E53935"]
        solidRect(380.0, 80.0, exitColor).position(100.0, backY)
        text(exitLabel, 41.0, Colors.WHITE).position(130.0, backY + 12.0)
        val doExit: suspend () -> Unit = if (fromGame) {
            { engine.togglePause(); nav.changeTo { GameScene(engine, nav) } }
        } else {
            { nav.changeTo { MenuScene(engine, nav) } }
        }
        solidRect(380.0, 80.0, RGBA(0, 0, 0, 1)).position(100.0, backY).onClickSuspend(views.coroutineContext) { doExit() }
        fc.add(caretX, backY + 12.0, doExit)
        exitAction = doExit

        // Focus caret created last so it renders above everything.
        // A solidRect bar (not a glyph) so it doesn't depend on font coverage.
        fc.caret = solidRect(18.0, 44.0, Colors["#FFCC00"])
        fc.start(initialFocus)
        focusController = fc
    }
}
