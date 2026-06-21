package com.vitriolix.airwar2142.scenes

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.paint.LinearGradientPaint
import korlibs.image.vector.ShapeBuilder
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.input.onClickSuspend
import korlibs.korge.view.*
import korlibs.math.geom.Point
import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.logic.BackgroundIsland
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.logic.GameState
import com.vitriolix.airwar2142.logic.SeedField
import com.vitriolix.airwar2142.platform.Clipboard
import com.vitriolix.airwar2142.render.Fonts

class MenuScene(
    private val engine: GameEngine,
    private val nav: SceneContainer
) : Scene(), KeyboardNavigable, EscapeHandler, TextInputTarget {

    override var focusController: FocusController? = null

    // Seed-editor state (Design round 3). The seed value itself lives in the mock SeedField store.
    private var expanded = false
    private var refreshSeed: (() -> Unit)? = null
    private var collapseFn: (() -> Unit)? = null

    // Text entry + Esc are routed from the central key dispatch (main.kt); they act only while the
    // seed editor is expanded so normal menu input is unaffected.
    override fun onChar(c: Char): Boolean {
        if (!expanded) return false
        SeedField.append(c); refreshSeed?.invoke(); return true
    }
    override fun onBackspace(): Boolean {
        if (!expanded) return false
        SeedField.backspace(); refreshSeed?.invoke(); return true
    }
    override suspend fun onEscape(): Boolean {
        if (!expanded) return false
        collapseFn?.invoke(); return true
    }

    override suspend fun SContainer.sceneMain() {
        engine.returnToMenu()
        Fonts.load()

        graphics { }.also { it.updateShape { drawMenuOcean() } }
        val islandGraphics = engine.islands.map { isl ->
            graphics { }.also { g -> g.updateShape { drawMenuIslandCentered(isl) } }
        }

        text("AIR WAR", 56.0, Colors["#00FFFF"], font = Fonts.title).position(90.0, 200.0)
        text("2142",  184.0, Colors["#FFCC00"], font = Fonts.title).position(80.0, 260.0)

        val fc = FocusController()

        // START / SETTINGS: views always present; focus is (re)registered by rebuildFocus().
        val startAction: suspend () -> Unit = {
            engine.startGame()
            nav.changeTo { GameScene(engine, nav) }
        }
        val settingsAction: suspend () -> Unit = {
            nav.changeTo { SettingsScene(engine, nav) }
        }
        menuButtonView("START CAMPAIGN",    80.0, 640.0, startAction)
        menuButtonView("SETTINGS & INPUTS", 80.0, 760.0, settingsAction)

        // ── Seed row: collapsed menu item ⇄ expanded editor (in place at y=880) ──
        val seedRowY = 880.0
        // Collapsed: a normal menu item — "SEED" left, current value (gold) right.
        val collapsed = container {
            solidRect(840.0, 80.0, RGBA(0, 0, 0, 85)).position(80.0, seedRowY)
            text("SEED", 46.0, Colors.WHITE, font = Fonts.content).position(108.0, seedRowY + 14.0)
        }
        val collapsedValue = text("", 40.0, Colors["#FFCC00"], font = Fonts.content)
            .also { collapsed.addChild(it) }.position(0.0, seedRowY + 18.0)

        // Expanded: editor — field (label + value + blinking caret) and the COPY/PASTE/ROLL row.
        val editor = container { visible = false }
        editor.solidRect(1000.0, 250.0, RGBA(0, 0, 0, 140)).position(0.0, seedRowY - 10.0)   // scrim behind editor
        editor.solidRect(840.0, 80.0, RGBA(0, 229, 255, 40)).position(80.0, seedRowY)   // editingFill
        editor.text("SEED", 26.0, RGBA(255, 255, 255, 160), font = Fonts.content).position(104.0, seedRowY + 6.0)
        val fieldValue = editor.text("", 36.0, Colors["#FFCC00"], font = Fonts.content).position(104.0, seedRowY + 38.0)
        val textCaret = editor.solidRect(4.0, 36.0, Colors.WHITE).position(104.0, seedRowY + 38.0)

        // Action row (150×72 ghost rects) at y=980. activate() handlers do the work.
        val copyBtnFill = editor.solidRect(150.0, 72.0, RGBA(255, 255, 255, 20)).position(80.0, 980.0)
        val copyBtnLabel = editor.text("COPY", 34.0, Colors.WHITE, font = Fonts.content)
        copyBtnLabel.position(80.0 + (150.0 - copyBtnLabel.width) / 2.0, 980.0 + 19.0)
        editor.solidRect(150.0, 72.0, RGBA(255, 255, 255, 20)).position(246.0, 980.0)
        val pasteBtnLabel = actionLabel(editor, "PASTE", 246.0)
        editor.solidRect(150.0, 72.0, RGBA(255, 255, 255, 20)).position(412.0, 980.0)
        val rollBtnLabel = actionLabel(editor, "ROLL", 412.0)
        editor.text("Type or paste a seed  •  Saved automatically  •  Esc to close",
            28.0, RGBA(255, 255, 255, 128), font = Fonts.content).position(60.0, 1080.0)

        text("WASD to move  •  Space to fire  •  R to loop", 31.0, RGBA(255, 255, 255, 128), font = Fonts.content)
            .position(60.0, CANVAS_HEIGHT - 60.0)

        // Focus caret created last so it renders above everything.
        fc.caret = solidRect(20.0, 52.0, Colors["#FFCC00"])

        // COPIED flash (~1.2s). Set when COPY activates; counted down in the updater.
        var copyFlashFrames = 0

        fun refresh() {
            collapsedValue.text = SeedField.value
            collapsedValue.x = 892.0 - collapsedValue.width            // right-anchored inside the row
            fieldValue.text = SeedField.value
            textCaret.x = 104.0 + fieldValue.width + 2.0               // caret trails the value
        }
        refreshSeed = ::refresh

        // var lambdas so rebuildFocus and expand/collapse can reference each other (set below).
        var expandSeed: () -> Unit = {}
        var collapseSeed: () -> Unit = {}

        // Focus order differs by mode: collapsed = [START, SETTINGS, SEED]; expanded editor =
        // [field, COPY, PASTE, ROLL]. START/SETTINGS stay visible/clickable while expanded.
        fun rebuildFocus(focusIndex: Int) {
            fc.reset()
            if (!expanded) {
                fc.add(caretX = 28.0, caretY = 654.0, activate = startAction)
                fc.add(caretX = 28.0, caretY = 774.0, activate = settingsAction)
                fc.add(caretX = 28.0, caretY = seedRowY + 14.0, activate = { expandSeed() })
            } else {
                fc.add(caretX = 28.0, caretY = seedRowY + 14.0, activate = { collapseSeed() })
                fc.add(caretX = 28.0, caretY = 990.0, activate = {
                    Clipboard.copy(SeedField.value); copyFlashFrames = 72
                })
                fc.add(caretX = 194.0, caretY = 990.0, activate = {
                    Clipboard.paste()?.let { SeedField.set(it); refresh() }
                })
                fc.add(caretX = 360.0, caretY = 990.0, activate = { SeedField.roll(); refresh() })
            }
            fc.start(focusIndex)
        }

        // expand/collapse swap the row in place + rebuild focus.
        expandSeed = {
            expanded = true
            collapsed.visible = false
            editor.visible = true
            rebuildFocus(0)
            refresh()
        }
        collapseSeed = {
            expanded = false
            editor.visible = false
            collapsed.visible = true
            rebuildFocus(2)   // land back on the SEED item
            refresh()
        }
        collapseFn = collapseSeed   // onEscape closes the editor

        rebuildFocus(0)
        refresh()
        focusController = fc

        var blinkFrame = 0
        addUpdater {
            if (engine.gameState.value == GameState.MENU) engine.tick()
            engine.islands.forEachIndexed { i, isl ->
                islandGraphics[i].position(isl.x.toDouble(), isl.y.toDouble())
            }
            // Blink the text-entry caret while editing (local counter — tick() is a no-op in menu).
            blinkFrame++
            textCaret.visible = expanded && (blinkFrame / 20) % 2 == 0
            // COPIED flash on the COPY button.
            if (copyFlashFrames > 0) {
                copyFlashFrames--
                copyBtnFill.color = RGBA(0, 255, 136, 40)
                copyBtnLabel.text = "COPIED"; copyBtnLabel.color = Colors["#00FF88"]
            } else {
                copyBtnFill.color = RGBA(255, 255, 255, 20)
                copyBtnLabel.text = "COPY"; copyBtnLabel.color = Colors.WHITE
            }
            copyBtnLabel.x = 80.0 + (150.0 - copyBtnLabel.width) / 2.0
            pasteBtnLabel.x = 246.0 + (150.0 - pasteBtnLabel.width) / 2.0
            rollBtnLabel.x = 412.0 + (150.0 - rollBtnLabel.width) / 2.0
        }
    }

    private fun SContainer.menuButtonView(label: String, x: Double, y: Double, action: suspend () -> Unit) {
        solidRect(840.0, 80.0, RGBA(0, 0, 0, 85)).position(x, y)
        text(label, 46.0, Colors.WHITE, font = Fonts.content).position(x + 24.0, y + 14.0)
        solidRect(840.0, 80.0, RGBA(0, 0, 0, 1)).position(x, y).onClickSuspend(views.coroutineContext) { action() }
    }

    private fun actionLabel(parent: Container, label: String, rectX: Double): Text {
        val t = parent.text(label, 34.0, Colors.WHITE, font = Fonts.content)
        t.position(rectX + (150.0 - t.width) / 2.0, 980.0 + 19.0)
        return t
    }
}

// circle(x, y, r) helper for ShapeBuilder in MenuScene
private fun ShapeBuilder.circle(x: Double, y: Double, r: Double) = circle(Point(x, y), r)

// Ocean gradient — drawn once.
private fun ShapeBuilder.drawMenuOcean() {
    val h = CANVAS_HEIGHT.toDouble()
    fill(LinearGradientPaint(0, 0, 0, h)
        .addColorStop(0.0, Colors["#0F2027"])
        .addColorStop(0.5, Colors["#203A43"])
        .addColorStop(1.0, Colors["#2C5364"])
    ) {
        rect(0.0, 0.0, 1000.0, h)   // playWidth
    }
}

// One island, drawn once centered at (0,0); positioned per-frame as it scrolls.
private fun ShapeBuilder.drawMenuIslandCentered(isl: BackgroundIsland) {
    val ir = isl.radius.toDouble()
    fill(Colors["#E2D4A8"]) { circle(0.0, 0.0, ir) }
    fill(Colors["#2E8B57"]) { circle(0.0, 0.0, ir * 0.85) }
    fill(Colors["#1E5B37"]) {
        circle(-ir * 0.2, -ir * 0.1, ir * 0.4)
        circle( ir * 0.3,  ir * 0.2, ir * 0.3)
    }
}
