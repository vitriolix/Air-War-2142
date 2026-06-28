package com.vitriolix.airwar2142.scenes

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.input.onClickSuspend
import korlibs.korge.view.*
import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.render.Fonts

// Cross-platform text vertical centering: use font size instead of text.height (which differs between JVM/web).
// Cap-height ratio ~0.72 means the ascender peaks ~72% of the em-square; centering uses em-square center.
private fun centerTextVertically(containerY: Double, containerHeight: Double, fontSize: Double): Double =
    containerY + containerHeight / 2.0 - fontSize * 0.36

class ControllerPrefsScene(
    private val engine: GameEngine,
    private val nav: SceneContainer,
    private val fromGame: Boolean = false,
    private val fireBinding: String = "A",
    private val rollBinding: String = "RB",
    private val pauseBinding: String = "Start",
    private val invertY: Boolean = false,
    private val deadzone: Float = 0.15f,
    private val listeningRow: Int = -1,
    private val initialFocus: Int = 0
) : Scene(), KeyboardNavigable, EscapeHandler {

    override var focusController: FocusController? = null
    private var cancelListenAction: (suspend () -> Unit)? = null

    override suspend fun onEscape(): Boolean {
        if (listeningRow >= 0) {
            cancelListenAction?.invoke()
        } else {
            nav.changeTo { SettingsScene(engine, nav, fromGame, 7) }
        }
        return true
    }

    override suspend fun SContainer.sceneMain() {
        Fonts.load()
        solidRect(1000.0, CANVAS_HEIGHT.toDouble(), Colors["#1E272C"])

        text("CONTROLLER", 72.0, Colors["#00E5FF"], font = Fonts.title).position(100.0, 80.0)
        text("GAMEPAD BINDINGS", 36.0, RGBA(255, 255, 255, 178), font = Fonts.content).position(100.0, 200.0)

        val fc = FocusController()
        val caretX = 48.0

        fun rebuild(focus: Int, listenRow: Int = -1): suspend () -> Unit = {
            nav.changeTo {
                ControllerPrefsScene(engine, nav, fromGame, fireBinding, rollBinding, pauseBinding, invertY, deadzone, listenRow, focus)
            }
        }

        // Binding rows — focus 0, 1, 2
        listOf(
            Triple("FIRE",  fireBinding,  0),
            Triple("ROLL",  rollBinding,  1),
            Triple("PAUSE", pauseBinding, 2)
        ).forEach { (label, chip, idx) ->
            val cy = 290.0 + idx * 110.0
            val listening = listeningRow == idx
            if (listening) solidRect(800.0, 80.0, RGBA(0, 229, 255, 40)).position(100.0, cy)
            val rowLabel = text(label, 36.0, if (listening) Colors["#00E5FF"] else Colors.WHITE, font = Fonts.content)
            rowLabel.x = 116.0
            rowLabel.y = centerTextVertically(cy, 80.0, 36.0)
            // Chip: right-anchored, right edge 32px in from row right (x=868)
            val chipW = if (listening) 340.0 else 120.0
            val chipX = 868.0 - chipW
            solidRect(chipW, 56.0, RGBA(255, 255, 255, 20)).position(chipX, cy + 12.0)
            val chipLabel = text(if (listening) "PRESS A BUTTON…" else chip, 36.0, Colors["#00E5FF"], font = Fonts.content)
            chipLabel.x = chipX + (chipW - chipLabel.width) / 2.0
            chipLabel.y = centerTextVertically(cy + 12.0, 56.0, 36.0)
            val listenAct = rebuild(idx, idx)
            solidRect(800.0, 80.0, RGBA(0, 0, 0, 1)).position(100.0, cy)
                .onClickSuspend(views.coroutineContext) { listenAct() }
            fc.add(caretX, cy + 18.0, listenAct)
        }

        cancelListenAction = rebuild(listeningRow.coerceAtLeast(0))

        // Listening hint — centered, shown only while rebinding
        if (listeningRow >= 0) {
            val hint = text("PRESS ESC TO CANCEL", 36.0, RGBA(255, 255, 255, 128), font = Fonts.content)
            hint.x = ((1000.0 - hint.width) / 2.0).coerceAtLeast(100.0)
            hint.y = 1210.0
        }

        // Invert Y toggle — focus 3
        val invColor = if (invertY) Colors["#00FF88"] else Colors["#999999"]
        val invFill  = if (invertY) RGBA(0, 255, 136, 60) else RGBA(128, 128, 128, 60)
        val invAct: suspend () -> Unit = {
            nav.changeTo {
                ControllerPrefsScene(engine, nav, fromGame, fireBinding, rollBinding, pauseBinding, !invertY, deadzone, -1, 3)
            }
        }
        solidRect(280.0, 70.0, invFill).position(100.0, 640.0)
        val invText = text(if (invertY) "INVERT Y: ON" else "INVERT Y: OFF", 36.0, invColor, font = Fonts.content)
        invText.x = 116.0
        invText.y = centerTextVertically(640.0, 70.0, 36.0)
        solidRect(280.0, 70.0, RGBA(0, 0, 0, 1)).position(100.0, 640.0)
            .onClickSuspend(views.coroutineContext) { invAct() }
        fc.add(caretX, 640.0 + (70.0 - 44.0) / 2.0, invAct)

        // Stick Dead Zone label + [−][track][+] slider — focus 4 (−) and 5 (+)
        val dzFrac = (deadzone * 100 + 0.5f).toInt()
        text("STICK DEAD ZONE: 0.${dzFrac.toString().padStart(2, '0')}", 36.0, Colors.WHITE, font = Fonts.content)
            .position(100.0, 760.0)

        val decAct: suspend () -> Unit = {
            val d = (deadzone - 0.05f).coerceAtLeast(0.00f)
            nav.changeTo { ControllerPrefsScene(engine, nav, fromGame, fireBinding, rollBinding, pauseBinding, invertY, d, -1, 4) }
        }
        val incAct: suspend () -> Unit = {
            val d = (deadzone + 0.05f).coerceAtMost(0.50f)
            nav.changeTo { ControllerPrefsScene(engine, nav, fromGame, fireBinding, rollBinding, pauseBinding, invertY, d, -1, 4) }
        }

        solidRect(80.0, 60.0, Colors["#FF9900"]).position(100.0, 840.0)
        val decText = text("  -  ", 36.0, Colors.WHITE, font = Fonts.content)
        decText.x = 100.0
        decText.y = centerTextVertically(840.0, 60.0, 36.0)
        solidRect(80.0, 60.0, RGBA(0, 0, 0, 1)).position(100.0, 840.0)
            .onClickSuspend(views.coroutineContext) { decAct() }
        fc.add(caretX, 840.0 + (60.0 - 44.0) / 2.0, decAct, leftAct = decAct, rightAct = incAct)

        solidRect(620.0, 20.0, RGBA(255, 255, 255, 20)).position(190.0, 860.0)
        val fillW = deadzone.toDouble() / 0.50 * 620.0
        if (fillW > 0.0) solidRect(fillW, 20.0, Colors["#00E5FF"]).position(190.0, 860.0)

        solidRect(80.0, 60.0, Colors["#FF9900"]).position(820.0, 840.0)
        val incText = text("  +  ", 36.0, Colors.WHITE, font = Fonts.content)
        incText.x = 820.0
        incText.y = centerTextVertically(840.0, 60.0, 36.0)
        solidRect(80.0, 60.0, RGBA(0, 0, 0, 1)).position(820.0, 840.0)
            .onClickSuspend(views.coroutineContext) { incAct() }

        // BACK button — focus 5
        val backY = CANVAS_HEIGHT - 200.0
        val backAct: suspend () -> Unit = { nav.changeTo { SettingsScene(engine, nav, fromGame, 7) } }
        solidRect(380.0, 80.0, RGBA(255, 255, 255, 20)).position(100.0, backY)
        val backText = text("BACK", 41.0, Colors.WHITE, font = Fonts.content)
        backText.x = 130.0
        backText.y = centerTextVertically(backY, 80.0, 41.0)
        solidRect(380.0, 80.0, RGBA(0, 0, 0, 1)).position(100.0, backY)
            .onClickSuspend(views.coroutineContext) { backAct() }
        fc.add(caretX, backY + (80.0 - 44.0) / 2.0, backAct)

        fc.caret = solidRect(18.0, 44.0, Colors["#FFCC00"])
        fc.start(initialFocus)
        focusController = fc
    }
}
