package com.example.clone1942.scenes

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.paint.LinearGradientPaint
import korlibs.image.vector.ShapeBuilder
import korlibs.korge.input.keys
import korlibs.korge.input.onClick
import korlibs.korge.input.onClickSuspend
import korlibs.korge.input.singleTouch
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.view.*
import korlibs.math.geom.Angle
import korlibs.math.geom.Point
import korlibs.math.geom.Size2D
import com.example.clone1942.CANVAS_HEIGHT
import com.example.clone1942.logic.*
import kotlin.math.*

class GameScene(
    private val engine: GameEngine,
    private val nav: SceneContainer
) : Scene(), EscapeHandler {

    // ESC / Android back while playing → open the settings (pause) menu.
    override suspend fun onEscape(): Boolean {
        if (engine.gameState.value == GameState.PLAYING) {
            engine.togglePause()
            nav.changeTo { SettingsScene(engine, nav, fromGame = true) }
            return true
        }
        return false
    }

    override suspend fun SContainer.sceneMain() {
        val ch = CANVAS_HEIGHT.toDouble()

        // ── Z-layer containers (back → front) ─────────────────────────────────
        val bgContainer       = container { }   // ocean + islands + clouds
        val powerupGraphics   = graphics { }    // power-ups (tessellated — rare)
        val enemyContainer    = container { }   // one container per live enemy
        val bulletContainer   = container { }   // bullet pool
        val particleContainer = container { }   // particle pool

        // ── Ocean gradient — Graphics drawn ONCE, never updated ───────────────
        bgContainer.graphics { }.also { g ->
            g.updateShape { drawOcean(engine, ch) }
        }

        // ── Island views — Graphics drawn ONCE per island, centered at (0,0) ──
        val islandGraphics = engine.islands.map { isl ->
            bgContainer.graphics { }.also { g -> g.updateShape { drawIslandCentered(isl) } }
        }

        // ── Cloud views — same approach ───────────────────────────────────────
        val cloudGraphics = engine.clouds.map { cloud ->
            bgContainer.graphics { }.also { g -> g.updateShape { drawCloudCentered(cloud) } }
        }

        // ── Player view (tessellated each frame — animated exhaust) ───────────
        val playerView     = container { }
        val playerGraphics = playerView.graphics { }

        // ── Enemy tracking: enemy.id → (rootContainer, hpFgBar?) ─────────────
        val enemyViews = mutableMapOf<Int, Pair<Container, View?>>()

        // ── Bullet pools — Graphics drawn ONCE per slot ───────────────────────
        val playerBulletPool = Array(30) {
            bulletContainer.graphics { }.also { g ->
                g.updateShape { drawPlayerBulletShape() }
                g.visible = false
            }
        }
        val enemyBulletPool = Array(50) {
            bulletContainer.graphics { }.also { g ->
                g.updateShape { drawEnemyBulletShape() }
                g.visible = false
            }
        }

        // ── Particle pool — white circle at radius 1, tinted via colorMul ────
        val particlePool = Array(120) {
            particleContainer.graphics { }.also { g ->
                g.updateShape { fill(Colors.WHITE) { circle(Point(0.0, 0.0), 1.0) } }
                g.visible = false
            }
        }

        // ── HUD ───────────────────────────────────────────────────────────────
        text("SCORE", 28.0, Colors["#00E5FF"]).position(16.0, 16.0)
        val scoreText = text("0", 51.0, Colors.WHITE).position(16.0, 52.0)
        text("LOOPS", 28.0, Colors["#FF9900"]).position(868.0, 16.0)
        val loopsText = text("3", 51.0, Colors.WHITE).position(868.0, 52.0)
        val pauseLabel = text("PAUSE", 31.0, Colors.WHITE).position(446.0, 16.0)
        pauseLabel.onClick { engine.togglePause() }
        val livesText = text("FIGHTERS: ✈ ✈ ✈", 31.0, Colors.WHITE).position(16.0, ch - 55.0)
        text("FUEL / ENERGY", 26.0, Colors["#00FF88"]).position(620.0, ch - 62.0)
        val fuelText = text("100%", 26.0, Colors.WHITE).position(890.0, ch - 62.0)

        // ── Debug overlay ─────────────────────────────────────────────────────
        val dbgLabels = listOf("FPS", "TICK", "POS", "FUEL", "LIVES", "ENEMIES", "PARTS", "KEYS", "MODE")
        val dbgFontSize = 20.0; val dbgRowH = 24.0
        val dbgY = ch - (dbgLabels.size * dbgRowH + 14.0) - 70.0
        val debugContainer = container {
            solidRect(340.0, dbgLabels.size * dbgRowH + 14.0, RGBA(0, 0, 0, 165))
            dbgLabels.forEachIndexed { i, lbl ->
                text(lbl.padEnd(7), dbgFontSize, Colors["#00E5FF"]).position(6.0, 7.0 + i * dbgRowH)
            }
        }.position(8.0, dbgY)
        val dbgValues = dbgLabels.indices.map { i ->
            text("", dbgFontSize, Colors.WHITE).also { debugContainer.addChild(it) }
                .position(86.0, 7.0 + i * dbgRowH)
        }
        val dbgFpsText = dbgValues[0]
        var fpsAccumMs = 0.0; var fpsFrames = 0; var fpsDisplay = 0; var frameMsDisplay = 0.0

        // HUD value cache — setting Text.text re-triggers layout, so only update on change.
        var lastScore = Int.MIN_VALUE; var lastLoops = Int.MIN_VALUE
        var lastLives = Int.MIN_VALUE; var lastFuelPct = Int.MIN_VALUE
        var lastPaused: Boolean? = null

        // Player vector is expensive to tessellate (~8-12ms on web). Only redraw it when
        // a SHAPE-affecting state changes (escorts on/off, roll start/end); movement, roll
        // scale/rotation and the invuln blink are all cheap transforms done every frame.
        var lastPlayerShapeKey: String? = null

        // ── Overlay ───────────────────────────────────────────────────────────
        val overlayBg   = solidRect(1000.0, ch, RGBA(0, 0, 0, 0)).position(0.0, 0.0)
        val overlayHead = text("", 56.0, Colors.WHITE).position(80.0, 580.0)
        val overlaySub  = text("", 36.0, Colors.WHITE).position(80.0, 670.0)
        val overlayBtn1 = text("", 36.0, Colors["#00FF88"]).position(80.0, 770.0)
        val overlayBtn2 = text("", 36.0, Colors.WHITE).position(80.0, 840.0)
        val overlayBtn3 = text("", 36.0, RGBA(255, 255, 255, 160)).position(80.0, 910.0)
        overlayHead.visible = false; overlaySub.visible = false
        overlayBtn1.visible = false; overlayBtn2.visible = false; overlayBtn3.visible = false

        overlayBtn1.onClick {
            when (engine.gameState.value) {
                GameState.PAUSED         -> engine.togglePause()
                GameState.GAME_OVER      -> engine.startGame()
                GameState.LEVEL_COMPLETE -> engine.proceedToNextLevel()
                else -> {}
            }
        }
        overlayBtn2.onClickSuspend(views.coroutineContext) {
            when (engine.gameState.value) {
                GameState.PAUSED         -> nav.changeTo { SettingsScene(engine, nav, fromGame = true) }
                GameState.GAME_OVER,
                GameState.LEVEL_COMPLETE -> { engine.returnToMenu(); nav.changeTo { MenuScene(engine, nav) } }
                else -> {}
            }
        }
        overlayBtn3.onClickSuspend(views.coroutineContext) {
            if (engine.gameState.value == GameState.PAUSED) {
                engine.returnToMenu(); nav.changeTo { MenuScene(engine, nav) }
            }
        }

        // ── Keyboard state — deferred-release to survive macOS AWT key behaviour ─
        // On macOS every key press (and auto-repeat) is delivered as a DOWN
        // immediately followed by an UP within ~1ms — both land in the same render
        // frame, so any per-frame sampling of the key state reads "released".
        // HeldKey defers the release by a few ms; a following DOWN cancels it, so
        // the key stays "held" across the spurious UP and across auto-repeat.
        // `clockMs` is a frame-accumulated clock shared with the handlers.
        var clockMs = 0.0
        val hLeft = HeldKey { clockMs }; val hRight = HeldKey { clockMs }
        val hUp   = HeldKey { clockMs }; val hDown  = HeldKey { clockMs }
        val hShoot = HeldKey { clockMs }
        views.stage.keys {
            down(Key.LEFT)  { hLeft.down()  };  up(Key.LEFT)  { hLeft.up()  }
            down(Key.A)     { hLeft.down()  };  up(Key.A)     { hLeft.up()  }
            down(Key.RIGHT) { hRight.down() };  up(Key.RIGHT) { hRight.up() }
            down(Key.D)     { hRight.down() };  up(Key.D)     { hRight.up() }
            down(Key.UP)    { hUp.down()    };  up(Key.UP)    { hUp.up()    }
            down(Key.W)     { hUp.down()    };  up(Key.W)     { hUp.up()    }
            down(Key.DOWN)  { hDown.down()  };  up(Key.DOWN)  { hDown.up()  }
            down(Key.S)     { hDown.down()  };  up(Key.S)     { hDown.up()  }
            down(Key.SPACE) { hShoot.down() };  up(Key.SPACE) { hShoot.up() }
        }

        // ── Touch input — use *Anywhere variants to bypass hitTest on SContainer ─
        // start/move/end only fire when hitTest(touchPos) != null, which silently
        // skips taps on Graphics views (ocean, enemies). *Anywhere always fires.
        var tapElapsedMs = 999.0
        singleTouch(supportStartAnywhere = true) {
            startAnywhere {
                if (tapElapsedMs < 300.0) engine.triggerRoll()
                tapElapsedMs = 0.0
                engine.updateTouchTarget(it.local.x.toFloat(), it.local.y.toFloat())
            }
            moveAnywhere  { engine.updateTouchTarget(it.local.x.toFloat(), it.local.y.toFloat()) }
            endAnywhere   { engine.updateTouchTarget(null, null) }
        }

        // ── Main updater ──────────────────────────────────────────────────────
        addUpdater { dt ->
            val state  = engine.gameState.value
            var player = engine.player.value

            val dtMs = dt.inWholeMilliseconds.toDouble()
            clockMs += dtMs
            tapElapsedMs += dtMs

            // Apply any pending deferred releases now that the clock has advanced.
            hLeft.update(); hRight.update(); hUp.update(); hDown.update(); hShoot.update()

            if (state == GameState.PLAYING) {
                engine.updateKeyboardInputs(
                    left  = hLeft.held,
                    right = hRight.held,
                    up    = hUp.held,
                    down  = hDown.held,
                    shoot = hShoot.held
                )
                engine.tick()
                player = engine.player.value   // refresh post-tick so the view has no 1-frame lag

                // ── Background: reposition views, no tessellation ────────────
                engine.islands.forEachIndexed { i, isl ->
                    islandGraphics[i].position(isl.x.toDouble(), isl.y.toDouble())
                }
                engine.clouds.forEachIndexed { i, cloud ->
                    cloudGraphics[i].position(cloud.x.toDouble(), cloud.y.toDouble())
                }

                // ── Power-ups: tessellated (≤3 simple shapes on screen) ───────
                powerupGraphics.updateShape { drawPowerupsCentered(engine) }

                // ── Enemies: create on spawn, reposition each frame ────────────
                val activeIds = engine.enemies.mapTo(HashSet()) { it.id }
                val deadIds   = enemyViews.keys.filter { it !in activeIds }
                deadIds.forEach { id ->
                    enemyViews[id]?.first?.removeFromParent()
                    enemyViews.remove(id)
                }
                engine.enemies.forEach { e ->
                    val ev = enemyViews.getOrPut(e.id) {
                        val hh = e.type.height / 2.0
                        val needsHpBar = e.type == EnemyType.HEAVY_FIGHTER || e.type == EnemyType.BOSS
                        val barW = if (e.type == EnemyType.BOSS) 200.0 else 100.0
                        var hpFg: View? = null
                        val root = enemyContainer.container {
                            val g = graphics { }
                            g.updateShape { drawEnemyCentered(e) }
                            if (needsHpBar) {
                                solidRect(barW, 8.0, RGBA(50, 50, 50, 200))
                                    .position(-barW / 2, -hh - 14.0)
                                hpFg = solidRect(barW, 8.0, Colors["#FF3333"])
                                    .position(-barW / 2, -hh - 14.0)
                            }
                        }
                        root to hpFg
                    }
                    ev.first.position(e.x.toDouble(), e.y.toDouble())
                    ev.second?.let { fg ->
                        fg.scaleX = (e.health.toFloat() / e.type.baseHealth.toFloat()).toDouble()
                    }
                }

                // ── Bullets: match pool slots by type, hide unused ─────────────
                playerBulletPool.forEach { it.visible = false }
                enemyBulletPool.forEach  { it.visible = false }
                var pbi = 0; var ebi = 0
                engine.bullets.forEach { b ->
                    if (b.isPlayerOwned && pbi < playerBulletPool.size) {
                        playerBulletPool[pbi].apply { visible = true; position(b.x.toDouble(), b.y.toDouble()) }
                        pbi++
                    } else if (!b.isPlayerOwned && ebi < enemyBulletPool.size) {
                        enemyBulletPool[ebi].apply { visible = true; position(b.x.toDouble(), b.y.toDouble()) }
                        ebi++
                    }
                }

                // ── Particles: pool — scale + colorMul, no tessellation ────────
                particlePool.forEach { it.visible = false }
                engine.particles.forEachIndexed { i, p ->
                    if (i >= particlePool.size) return@forEachIndexed
                    val lifePct = p.currentLife.toFloat() / p.maxLife.toFloat()
                    val alpha   = (lifePct * 255).toInt().coerceIn(0, 255)
                    val base    = argbToRgba(p.color)
                    val radius  = (p.size * (0.5f + lifePct * 0.5f)).toDouble()
                    particlePool[i].apply {
                        visible = true
                        position(p.x.toDouble(), p.y.toDouble())
                        scaleX = radius; scaleY = radius
                        colorMul = RGBA(base.r, base.g, base.b, alpha)
                    }
                }
            }

            // ── Player ── movement/roll/blink are cheap transforms every frame; the
            // vector is only re-tessellated when its shape actually changes (see above).
            playerView.x = player.x.toDouble()
            playerView.y = player.y.toDouble()
            val rollScale = if (player.rollProgress > 0f)
                0.5 + 0.5 * sin(player.rollProgress * PI.toFloat())
            else 1.0
            playerView.scaleX  = rollScale
            playerView.rotation = Angle.fromDegrees(player.rollProgress * 360.0)
            val blink = player.invulnTicks > 0 && (player.invulnTicks / 4) % 2 == 0
            playerView.visible = !blink
            val playerShapeKey = "${player.escortsActive}|${player.rollProgress > 0f}"
            if (playerShapeKey != lastPlayerShapeKey) {
                playerGraphics.updateShape { drawPlayer(player, engine.debugTickCount) }
                lastPlayerShapeKey = playerShapeKey
            }

            // ── Debug overlay ─────────────────────────────────────────────────
            // FPS accumulator runs always so the counter is correct the instant it's
            // toggled on; the (costly) per-field text updates only run when visible.
            debugContainer.visible = engine.showDebugOverlay
            fpsAccumMs += dt.inWholeMilliseconds.toDouble(); fpsFrames++
            if (fpsAccumMs >= 500.0) {
                fpsDisplay     = (fpsFrames * 1000.0 / fpsAccumMs).toInt()
                frameMsDisplay = fpsAccumMs / fpsFrames
                fpsAccumMs     = 0.0; fpsFrames = 0
            }
            if (engine.showDebugOverlay) {
                val fpsColor = when {
                    fpsDisplay >= 55 -> Colors["#00FF88"]
                    fpsDisplay >= 30 -> Colors["#FFCC00"]
                    else             -> Colors["#FF4444"]
                }
                val keyStr = buildString {
                    if (engine.debugKeyLeft)  append("◄")
                    if (engine.debugKeyRight) append("►")
                    if (engine.debugKeyUp)    append("▲")
                    if (engine.debugKeyDown)  append("▼")
                    if (engine.keyShoot)      append("●")
                    if (isEmpty())            append("·")
                }
                val fMs = frameMsDisplay.let { "${(it * 10).toInt() / 10}.${(it * 10).toInt() % 10}" }
                dbgFpsText.colorMul = fpsColor
                dbgValues[0].text = "$fpsDisplay  (${fMs} ms)"
                dbgValues[1].text = "${engine.debugTickCount}"
                dbgValues[2].text = "${player.x.toInt()}, ${player.y.toInt()}"
                dbgValues[3].text = "${player.fuel.toInt()}%"
                dbgValues[4].text = "${player.lives}   LOOPS ${player.rollsLeft}"
                dbgValues[5].text = "${engine.enemies.size}   BULLETS ${engine.bullets.size}"
                dbgValues[6].text = "${engine.particles.size}"
                dbgValues[7].text = keyStr
                dbgValues[8].text = engine.controlMode.value.name
                dbgValues[7].colorMul = Colors["#FFCC00"]
            }

            // ── HUD ── only reassign (and re-layout) text when the value changes ──
            if (player.score != lastScore) {
                scoreText.text = "${player.score}"; lastScore = player.score
            }
            if (player.rollsLeft != lastLoops) {
                loopsText.text = "${player.rollsLeft}"; lastLoops = player.rollsLeft
            }
            val lives = player.lives.coerceAtLeast(0)
            if (lives != lastLives) {
                livesText.text = "FIGHTERS: ${"✈ ".repeat(lives).trim()}"; lastLives = lives
            }
            val fuelPct = player.fuel.toInt()
            if (fuelPct != lastFuelPct) {
                fuelText.text = "${fuelPct}%"; lastFuelPct = fuelPct
            }
            val paused = state == GameState.PAUSED
            if (paused != lastPaused) {
                pauseLabel.text = if (paused) "RESUME" else "PAUSE"; lastPaused = paused
            }

            // ── Overlay state ─────────────────────────────────────────────────
            when (state) {
                GameState.PAUSED -> {
                    overlayBg.colorMul = Colors.BLACK; overlayBg.alpha = 0.7
                    overlayHead.fontSize = 56.0
                    overlayHead.text = "MISSION PAUSED"; overlaySub.text = ""
                    overlayBtn1.text = "[ P ]  Resume flight"
                    overlayBtn2.text = "SETTINGS & INPUTS"
                    overlayBtn3.text = "[ Q ]  Abandon mission"
                    overlayHead.visible = true; overlaySub.visible = false
                    overlayBtn1.visible = true; overlayBtn2.visible = true; overlayBtn3.visible = true
                }
                GameState.GAME_OVER -> {
                    overlayBg.colorMul = Colors.BLACK; overlayBg.alpha = 0.85
                    overlayHead.fontSize = 66.0
                    overlayHead.text = "PLANE DESTROYED"
                    overlaySub.text  = "Score: ${player.score}   Kills: ${engine.kills}"
                    overlayBtn1.text = "[ ENTER ]  Sortie again"
                    overlayBtn2.text = "[ Q ]  Return to HQ"
                    overlayHead.visible = true; overlaySub.visible = true
                    overlayBtn1.visible = true; overlayBtn2.visible = true; overlayBtn3.visible = false
                }
                GameState.LEVEL_COMPLETE -> {
                    overlayBg.colorMul = Colors.BLACK; overlayBg.alpha = 0.8
                    overlayHead.fontSize = 82.0
                    overlayHead.text = "VICTORY!"
                    overlaySub.text  = "Level ${engine.level} cleared"
                    overlayBtn1.text = "[ ENTER ]  Next mission"
                    overlayBtn2.text = "[ Q ]  Return to HQ"
                    overlayHead.visible = true; overlaySub.visible = true
                    overlayBtn1.visible = true; overlayBtn2.visible = true; overlayBtn3.visible = false
                }
                else -> {
                    overlayBg.alpha = 0.0
                    overlayHead.visible = false; overlaySub.visible = false
                    overlayBtn1.visible = false; overlayBtn2.visible = false; overlayBtn3.visible = false
                }
            }
        }
    }
}

// ── Deferred-release key state ────────────────────────────────────────────────
//
// macOS AWT delivers each key press (and each auto-repeat) as a DOWN immediately
// followed by an UP ~1ms later. Both events are consumed within a single render
// frame, so naive per-frame sampling of the key state always reads "released" and
// the plane never moves. HeldKey ignores an UP for `releaseDelayMs`; a subsequent
// DOWN cancels the pending release. A genuine key release (no following DOWN)
// clears after the short delay, which the engine's velocity friction masks.
private class HeldKey(private val releaseDelayMs: Double = 80.0, private val nowMs: () -> Double) {
    var held = false
        private set
    private var releaseAt = Double.NaN   // NaN = no pending release

    fun down() { held = true; releaseAt = Double.NaN }
    fun up()   { if (held) releaseAt = nowMs() + releaseDelayMs }
    fun update() {
        if (!releaseAt.isNaN() && nowMs() >= releaseAt) {
            held = false
            releaseAt = Double.NaN
        }
    }
}

// ── ShapeBuilder helpers ──────────────────────────────────────────────────────

private fun ShapeBuilder.circle(x: Double, y: Double, r: Double) = circle(Point(x, y), r)

private fun ShapeBuilder.ellipse(cx: Double, cy: Double, rx: Double, ry: Double) {
    ellipse(Point(cx, cy), Size2D(rx * 2.0, ry * 2.0))
}

private fun argbToRgba(argb: Int) = RGBA(
    (argb ushr 16) and 0xFF,
    (argb ushr  8) and 0xFF,
     argb          and 0xFF,
    (argb ushr 24) and 0xFF
)

// ── Background — drawn ONCE on startup ───────────────────────────────────────

private fun ShapeBuilder.drawOcean(engine: GameEngine, canvasH: Double) {
    fill(LinearGradientPaint(0, 0, 0, canvasH)
        .addColorStop(0.0, Colors["#0F2027"])
        .addColorStop(0.5, Colors["#203A43"])
        .addColorStop(1.0, Colors["#2C5364"])
    ) { rect(0.0, 0.0, engine.playWidth.toDouble(), canvasH) }
}

private fun ShapeBuilder.drawIslandCentered(isl: BackgroundIsland) {
    val ir = isl.radius.toDouble()
    fill(Colors["#D4C490"]) { circle(0.0, 0.0, ir) }
    fill(Colors["#4CAF50"]) { circle(0.0, 0.0, ir * 0.78) }
    fill(Colors["#2E7D32"]) { circle(0.0, 0.0, ir * 0.55) }
    fill(Colors["#1B5E20"]) {
        circle(-ir * 0.25, -ir * 0.15, ir * 0.28)
        circle( ir * 0.20,  ir * 0.20, ir * 0.22)
    }
    fill(RGBA(255, 255, 220, 30)) { circle(-ir * 0.15, -ir * 0.15, ir * 0.9) }
}

private fun ShapeBuilder.drawCloudCentered(cloud: BackgroundCloud) {
    val cw = 90.0 * cloud.scaleX; val ch = 50.0 * cloud.scaleY
    fill(RGBA(255, 255, 255, 55)) {
        ellipse(0.0,        0.0,         cw * 0.5,  ch * 0.5)
        ellipse(-cw * 0.4, -ch * 0.1,   cw * 0.35, ch * 0.38)
        ellipse( cw * 0.3,  ch * 0.05,  cw * 0.32, ch * 0.32)
    }
}

// ── Power-ups — still tessellated (≤3 on screen, low overhead) ───────────────

private fun ShapeBuilder.drawPowerupsCentered(engine: GameEngine) {
    engine.powerUps.forEach { pu ->
        val x = pu.x.toDouble(); val y = pu.y.toDouble()
        val w = pu.width.toDouble(); val h = pu.height.toDouble()
        fill(RGBA(0, 20, 40, 200))  { roundRect(x - w/2, y - h/2, w, h, 10.0, 10.0) }
        fill(RGBA(0, 229, 255, 80)) { circle(x, y, w * 0.32) }
        fill(Colors["#00FFFF"])     { circle(x, y, w * 0.14) }
        fill(Colors["#00E5FF"]) {
            roundRect(x - w/2,       y - h/2,       w,   3.0, 1.0, 1.0)
            roundRect(x - w/2,       y + h/2 - 3.0, w,   3.0, 1.0, 1.0)
            roundRect(x - w/2,       y - h/2,        3.0, h,   1.0, 1.0)
            roundRect(x + w/2 - 3.0, y - h/2,        3.0, h,   1.0, 1.0)
        }
    }
}

// ── Enemies — drawn ONCE per spawn, centered at (0,0) ────────────────────────

private fun ShapeBuilder.drawEnemyCentered(e: Enemy) {
    val hw = e.type.width / 2.0; val hh = e.type.height / 2.0
    when (e.type) {

        EnemyType.SCOUT -> {
            fill(Colors["#2E7D32"]) { roundRect(-6.0, -hh * 0.9, 12.0, hh * 1.8, 6.0, 6.0) }
            fill(Colors["#388E3C"]) { ellipse(0.0, hh * 0.1, hw * 0.95, 9.0) }
            fill(Colors["#1B5E20"]) { circle(0.0, -hh * 0.65, 9.0) }
            fill(RGBA(160, 230, 160, 65)) { circle(0.0, -hh * 0.9, 11.0) }
            fill(RGBA(60, 180, 100, 210)) { ellipse(0.0, -hh * 0.2, 4.5, 8.0) }
            fill(Colors["#1B5E20"]) { roundRect(-3.0, hh * 0.6, 6.0, 9.0, 2.0, 2.0) }
        }

        EnemyType.DIVER -> {
            fill(Colors["#1565C0"]) { roundRect(-9.0, -hh * 0.88, 18.0, hh * 1.76, 8.0, 8.0) }
            fill(Colors["#1976D2"]) { ellipse(0.0, hh * 0.08, hw, 12.0) }
            fill(Colors["#0D47A1"]) { circle(0.0, -hh * 0.6, 12.0) }
            fill(RGBA(130, 185, 255, 65)) { circle(0.0, -hh * 0.88, 13.0) }
            fill(Colors["#0D47A1"]) {
                roundRect(-24.0, hh * 0.1, 9.0, 14.0, 4.0, 4.0)
                roundRect( 15.0, hh * 0.1, 9.0, 14.0, 4.0, 4.0)
            }
            fill(RGBA(70, 150, 255, 195)) { ellipse(0.0, -hh * 0.1, 6.0, 11.0) }
            fill(Colors["#0D47A1"]) { roundRect(-4.0, hh * 0.6, 8.0, 10.0, 2.0, 2.0) }
        }

        EnemyType.SQUADRON_RED -> {
            fill(Colors["#B71C1C"]) { roundRect(-5.5, -hh * 0.9, 11.0, hh * 1.8, 5.0, 5.0) }
            fill(Colors["#C62828"]) { ellipse(0.0, hh * 0.05, hw * 0.9, 8.0) }
            fill(Colors["#7F0000"]) { circle(0.0, -hh * 0.62, 8.0) }
            fill(RGBA(255, 150, 150, 65)) { circle(0.0, -hh * 0.88, 10.0) }
            fill(Colors["#7F0000"]) {
                roundRect(-hw * 0.72, -3.0, 8.0, 7.0, 2.0, 2.0)
                roundRect( hw * 0.58, -3.0, 8.0, 7.0, 2.0, 2.0)
            }
            fill(RGBA(255, 70, 70, 205)) { ellipse(0.0, -hh * 0.18, 4.0, 7.5) }
            fill(Colors["#7F0000"]) { roundRect(-3.0, hh * 0.62, 6.0, 8.0, 2.0, 2.0) }
        }

        EnemyType.HEAVY_FIGHTER -> {
            fill(Colors["#37474F"]) { roundRect(-11.0, -hh * 0.88, 22.0, hh * 1.76, 8.0, 8.0) }
            fill(Colors["#455A64"]) { roundRect(-hw, -10.0, hw * 2, 20.0, 6.0, 6.0) }
            fill(Colors["#263238"]) {
                roundRect(-50.0, -hh * 0.55, 16.0, hh * 0.9, 6.0, 6.0)
                roundRect( 34.0, -hh * 0.55, 16.0, hh * 0.9, 6.0, 6.0)
            }
            fill(RGBA(130, 165, 180, 60)) {
                circle(-42.0, -hh * 0.7, 13.0)
                circle( 42.0, -hh * 0.7, 13.0)
            }
            fill(RGBA(80, 95, 105, 120)) {
                roundRect(-48.0, hh * 0.25, 12.0, 10.0, 3.0, 3.0)
                roundRect( 36.0, hh * 0.25, 12.0, 10.0, 3.0, 3.0)
            }
            fill(RGBA(60, 130, 170, 195)) { ellipse(0.0, -hh * 0.38, 7.0, 13.0) }
            fill(Colors["#37474F"]) { roundRect(-8.0, hh * 0.68, 16.0, 9.0, 3.0, 3.0) }
            // HP bar drawn as SolidRect siblings in the enemy container
        }

        EnemyType.BOSS -> {
            fill(Colors["#1A1A1A"]) { roundRect(-40.0, -hh * 0.9, 80.0, hh * 1.8, 10.0, 10.0) }
            fill(Colors["#212121"]) { roundRect(-hw, -12.0, hw * 2, 24.0, 6.0, 6.0) }
            fill(Colors["#111111"]) {
                roundRect(-100.0, -hh * 0.5, 18.0, hh * 0.85, 6.0, 6.0)
                roundRect( -52.0, -hh * 0.5, 16.0, hh * 0.8,  5.0, 5.0)
                roundRect(  36.0, -hh * 0.5, 16.0, hh * 0.8,  5.0, 5.0)
                roundRect(  82.0, -hh * 0.5, 18.0, hh * 0.85, 6.0, 6.0)
            }
            fill(RGBA(80, 100, 110, 50)) {
                circle(-91.0, -hh * 0.72, 15.0)
                circle(-44.0, -hh * 0.72, 14.0)
                circle( 44.0, -hh * 0.72, 14.0)
                circle( 91.0, -hh * 0.72, 15.0)
            }
            fill(Colors["#2A2E32"]) {
                circle(-22.0, -30.0, 9.0)
                circle( 22.0, -30.0, 9.0)
                circle(  0.0,  40.0,  9.0)
            }
            fill(Colors["#111111"]) {
                roundRect(-24.0, -48.0, 4.0, 20.0, 2.0, 2.0)
                roundRect( 20.0, -48.0, 4.0, 20.0, 2.0, 2.0)
                roundRect(  -2.0,  42.0, 4.0, 22.0, 2.0, 2.0)
            }
            fill(RGBA(25, 110, 160, 185)) { roundRect(-20.0, -hh * 0.28, 40.0, 28.0, 6.0, 6.0) }
            fill(RGBA(255, 20, 20, 215))  { ellipse(0.0, hh * 0.62, 15.0, 5.5) }
            fill(RGBA(255, 140, 0, 180))  { ellipse(0.0, hh * 0.62, 7.0, 2.5) }
            // HP bar drawn as SolidRect siblings in the enemy container
        }
    }
}

// ── Bullets — drawn ONCE per pool slot, centered at (0,0) ────────────────────

private fun ShapeBuilder.drawPlayerBulletShape() {
    fill(RGBA(0, 255, 255, 180))   { roundRect(-4.0, -14.0, 8.0, 28.0, 4.0, 4.0) }
    fill(RGBA(180, 255, 255, 220)) { roundRect(-2.0, -12.0, 4.0, 24.0, 2.0, 2.0) }
    fill(Colors.WHITE)             { roundRect(-1.0,  -8.0, 2.0, 16.0, 1.0, 1.0) }
}

private fun ShapeBuilder.drawEnemyBulletShape() {
    fill(RGBA(200, 0, 0, 220))     { circle(0.0, 0.0, 8.0) }
    fill(RGBA(255, 80, 80, 255))   { circle(0.0, 0.0, 5.0) }
    fill(RGBA(255, 200, 200, 255)) { circle(0.0, 0.0, 2.5) }
}

// ── Player — P-38 Lightning, centered at (0,0), facing up (−y = forward) ─────

private fun ShapeBuilder.drawPlayer(p: PlayerState, tick: Long) {
    val hw = p.width / 2.0; val hh = p.height / 2.0
    val bx = 20.0; val bw = 8.0

    if (p.escortsActive && p.rollProgress == 0f) {
        fill(Colors["#546E7A"]) {
            roundRect(-hw - 46.0, -6.0, 26.0, 12.0, 3.0, 3.0)
            roundRect( hw + 20.0, -6.0, 26.0, 12.0, 3.0, 3.0)
        }
        fill(Colors["#607D8B"]) {
            roundRect(-hw - 54.0, -2.0, 42.0, 4.0, 2.0, 2.0)
            roundRect( hw + 12.0, -2.0, 42.0, 4.0, 2.0, 2.0)
        }
    }

    if (p.rollProgress == 0f) {
        val fl = 7.0 + (tick % 6) * 1.5
        fill(RGBA(255, 70, 0, 200))  { circle(-bx, hh + fl * 0.35, fl) }
        fill(RGBA(255, 70, 0, 200))  { circle( bx, hh + fl * 0.35, fl) }
        fill(RGBA(255, 190, 0, 180)) { circle(-bx, hh + fl * 0.15, fl * 0.5) }
        fill(RGBA(255, 190, 0, 180)) { circle( bx, hh + fl * 0.15, fl * 0.5) }
    }

    fill(Colors["#546E7A"]) {
        roundRect(-bx - bw/2, -hh, bw, hh * 2, 4.0, 4.0)
        roundRect( bx - bw/2, -hh, bw, hh * 2, 4.0, 4.0)
    }
    fill(Colors["#607D8B"]) {
        roundRect(-bx - 14.0, hh - 9.0, 28.0, 6.0, 3.0, 3.0)
        roundRect( bx - 14.0, hh - 9.0, 28.0, 6.0, 3.0, 3.0)
    }
    fill(Colors["#90A4AE"]) { roundRect(-hw, -7.0, hw * 2, 14.0, 5.0, 5.0) }
    fill(Colors["#B71C1C"]) {
        roundRect(-hw,       -6.0, 10.0, 12.0, 3.0, 3.0)
        roundRect(hw - 10.0, -6.0, 10.0, 12.0, 3.0, 3.0)
    }
    fill(Colors["#B0BEC5"]) { roundRect(-7.0, -hh * 0.73, 14.0, hh * 0.82, 5.0, 5.0) }
    fill(Colors["#90A4AE"]) { roundRect(-5.0, -hh * 0.42, 10.0, 2.0, 1.0, 1.0) }
    fill(RGBA(0, 210, 240, 220))  { ellipse(0.0, -hh * 0.55, 5.0, 9.0) }
    fill(RGBA(220, 250, 255, 90)) { ellipse(-1.0, -hh * 0.58, 2.0, 4.0) }
    fill(RGBA(170, 215, 235, 75)) {
        circle(-bx, -hh + 7.0, 12.0)
        circle( bx, -hh + 7.0, 12.0)
    }
    fill(Colors["#37474F"]) {
        circle(-bx, -hh + 7.0, 2.5)
        circle( bx, -hh + 7.0, 2.5)
    }
}
