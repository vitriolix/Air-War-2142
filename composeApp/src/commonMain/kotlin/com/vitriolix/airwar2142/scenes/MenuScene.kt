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

class MenuScene(
    private val engine: GameEngine,
    private val nav: SceneContainer
) : Scene(), KeyboardNavigable {

    override var focusController: FocusController? = null

    override suspend fun SContainer.sceneMain() {
        engine.returnToMenu()

        // Background, view-based (was a per-frame updateShape of the whole scene):
        //   ocean gradient drawn once; each island is its own Graphics drawn once and
        //   just repositioned as it scrolls. Avoids re-tessellating every frame.
        graphics { }.also { it.updateShape { drawMenuOcean() } }
        val islandGraphics = engine.islands.map { isl ->
            graphics { }.also { g -> g.updateShape { drawMenuIslandCentered(isl) } }
        }

        text("AIR WAR", 56.0, Colors["#00FFFF"]).position(90.0, 200.0)
        text("2142",  184.0, Colors["#FFCC00"]).position(80.0, 260.0)

        val fc = FocusController()

        // Buttons: solidRect (clickable) + text layer on top, also registered for focus nav
        menuButton(fc, "START CAMPAIGN",    80.0, 640.0) {
            engine.startGame()
            nav.changeTo { GameScene(engine, nav) }
        }
        menuButton(fc, "SETTINGS & INPUTS", 80.0, 760.0) {
            nav.changeTo { SettingsScene(engine, nav) }
        }

        text("WASD to move  •  Space to fire  •  R to loop", 31.0, RGBA(255, 255, 255, 128))
            .position(60.0, CANVAS_HEIGHT - 60.0)

        // Focus caret created last so it renders above the buttons.
        // A solidRect bar (not a glyph) so it doesn't depend on font coverage.
        fc.caret = solidRect(20.0, 52.0, Colors["#FFCC00"])
        fc.start(0)
        focusController = fc

        addUpdater {
            if (engine.gameState.value == GameState.MENU) engine.tick()
            // Only reposition the island views as they scroll — no per-frame tessellation.
            engine.islands.forEachIndexed { i, isl ->
                islandGraphics[i].position(isl.x.toDouble(), isl.y.toDouble())
            }
        }
    }

    private fun SContainer.menuButton(fc: FocusController, label: String, x: Double, y: Double, action: suspend () -> Unit) {
        solidRect(840.0, 80.0, RGBA(0, 0, 0, 85)).position(x, y)
        text(label, 46.0, Colors.WHITE).position(x + 24.0, y + 14.0)
        // Near-invisible full-size hit area on top — catches clicks on both text and grey padding
        solidRect(840.0, 80.0, RGBA(0, 0, 0, 1)).position(x, y).onClickSuspend(views.coroutineContext) { action() }
        // Caret sits just left of the button, vertically aligned with the label.
        fc.add(caretX = x - 52.0, caretY = y + 14.0, activate = action)
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
