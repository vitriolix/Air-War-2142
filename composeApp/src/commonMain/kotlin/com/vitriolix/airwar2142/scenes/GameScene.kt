package com.vitriolix.airwar2142.scenes

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.input.keys
import korlibs.korge.input.onClick
import korlibs.korge.input.onClickSuspend
import korlibs.korge.input.singleTouch
import korlibs.korge.scene.Scene
import korlibs.korge.scene.SceneContainer
import korlibs.korge.style.*
import korlibs.korge.ui.UISlider
import korlibs.korge.ui.changed
import korlibs.korge.ui.uiSlider
import korlibs.korge.ui.uiText
import korlibs.korge.view.*
import korlibs.math.geom.Angle
import korlibs.math.geom.Point
import korlibs.math.geom.Size
import korlibs.number.niceStr
import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.ecs.Particle
import com.vitriolix.airwar2142.ecs.Position
import com.vitriolix.airwar2142.logic.*
import com.vitriolix.airwar2142.platform.Clipboard
import com.vitriolix.airwar2142.render.*
import kotlin.math.*

class GameScene(
    private val engine: GameEngine,
    private val nav: SceneContainer
) : Scene(), EscapeHandler {

    // Pause-overlay seed COPY flash (~1.2s @ 60fps). Set by copySeedFromPause(), counted down in
    // the updater. The pause overlay isn't focus-navigable (it's [P]/[Q] key-driven), so COPY uses
    // the [C] hotkey (routed from main.kt) rather than a focus caret.
    private var pauseCopyFlash = 0

    /** Copy the current (mock) seed to the clipboard from the pause overlay + trigger the COPIED flash. */
    fun copySeedFromPause() {
        if (engine.gameState.value != GameState.PAUSED) return
        Clipboard.copy(SeedField.value)
        pauseCopyFlash = 72
    }

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

        // ── Sprite atlas — build-time baked vectors, drawn as batched Images ──
        val atlas = SpriteAtlas.load()
        Fonts.load()

        // ── Z-layer containers (back → front) ─────────────────────────────────
        val bgContainer       = container { }   // ocean + islands + clouds
        val powerupContainer  = container { }   // power-up sprite pool
        val enemyContainer    = container { }   // one container per live enemy
        val bulletContainer   = container { }   // bullet sprite pool
        val particleContainer = container { }   // particle pool

        // ── Ocean gradient — Graphics drawn ONCE, never updated ───────────────
        bgContainer.graphics { }.also { g ->
            g.updateShape { drawOcean(engine, ch) }
        }

        // ── Island sprites — baked at a reference radius, scaled per island ───
        val islandViews = engine.islands.map { isl ->
            atlas.image(bgContainer, AtlasSpec.ISLAND).also { img ->
                val s = (isl.radius.toDouble() / AtlasSpec.ISLAND_REF_RADIUS) * SpriteAtlas.INV_SCALE
                img.scaleX = s; img.scaleY = s
            }
        }

        // ── Cloud sprites — baked at scale 1, scaled per cloud ────────────────
        val cloudViews = engine.clouds.map { cloud ->
            atlas.image(bgContainer, AtlasSpec.CLOUD).also { img ->
                img.scaleX = (cloud.scaleX.toDouble() / AtlasSpec.CLOUD_REF_SCALE) * SpriteAtlas.INV_SCALE
                img.scaleY = (cloud.scaleY.toDouble() / AtlasSpec.CLOUD_REF_SCALE) * SpriteAtlas.INV_SCALE
            }
        }

        // ── Player sprites — 3 baked variants; show one, transform the container ─
        val playerView       = container { }
        val playerImgNormal  = atlas.image(playerView, AtlasSpec.PLAYER_NORMAL)
        val playerImgEscorts = atlas.image(playerView, AtlasSpec.PLAYER_ESCORTS)
        val playerImgRoll    = atlas.image(playerView, AtlasSpec.PLAYER_ROLL)
        playerImgEscorts.visible = false
        playerImgRoll.visible = false

        // ── Enemy tracking: enemy.id → (rootContainer, hpFgBar?) ─────────────
        val enemyViews = mutableMapOf<Int, Pair<Container, View?>>()

        // ── Bullet sprite pools ───────────────────────────────────────────────
        val playerBulletPool = Array(30) {
            atlas.image(bulletContainer, AtlasSpec.BULLET_PLAYER).also { it.visible = false }
        }
        val enemyBulletPool = Array(50) {
            atlas.image(bulletContainer, AtlasSpec.BULLET_ENEMY).also { it.visible = false }
        }

        // ── Power-up sprite pool — baked at a reference size, scaled per pickup ─
        val powerupPool = Array(8) {
            atlas.image(powerupContainer, AtlasSpec.POWERUP).also { it.visible = false }
        }

        // ── Particle pool — white circle at radius 1, tinted via colorMul ────
        val particlePool = Array(120) {
            particleContainer.graphics { }.also { g ->
                g.updateShape { fill(Colors.WHITE) { circle(Point(0.0, 0.0), 1.0) } }
                g.visible = false
            }
        }

        // ── HUD ───────────────────────────────────────────────────────────────
        text("SCORE", 28.0, Colors["#00E5FF"], font = Fonts.content).position(16.0, 16.0)
        val scoreText = text("0", 51.0, Colors.WHITE, font = Fonts.content).position(16.0, 52.0)
        text("LOOPS", 28.0, Colors["#FF9900"], font = Fonts.content).position(868.0, 16.0)
        val loopsText = text("3", 51.0, Colors.WHITE, font = Fonts.content).position(868.0, 52.0)
        val pauseLabel = text("PAUSE", 31.0, Colors.WHITE, font = Fonts.content).position(446.0, 16.0)
        pauseLabel.onClick { engine.togglePause() }
        // ✈ (U+2708) isn't in Chakra Petch's Latin subset; using * as a placeholder.
        // Long-term: replace with small plane-sprite indicators (see TASKS.md).
        // Note: only update label when lives > 0 — game-over state (lives=0) is covered
        // by the overlay, and the web/JS build intermittently reads lives=0 at first tick
        // even after startGame() sets 3 (tracked in TASKS.md for investigation).
        val livesText = text("FIGHTERS: * * *", 31.0, Colors.WHITE, font = Fonts.content).position(16.0, ch - 55.0)
        text("FUEL / ENERGY", 26.0, Colors["#00FF88"], font = Fonts.content).position(620.0, ch - 62.0)
        val fuelText = text("100%", 26.0, Colors.WHITE, font = Fonts.content).position(890.0, ch - 62.0)

        // ── Debug overlay ─────────────────────────────────────────────────────
        val dbgLabels = listOf("FPS", "TICK", "POS", "FUEL", "LIVES", "ENEMIES", "PARTS", "KEYS", "MODE")
        val dbgFontSize = 20.0; val dbgRowH = 24.0
        val dbgY = ch - (dbgLabels.size * dbgRowH + 14.0) - 70.0
        val debugContainer = container {
            solidRect(340.0, dbgLabels.size * dbgRowH + 14.0 + dbgRowH, RGBA(0, 0, 0, 165))
            dbgLabels.forEachIndexed { i, lbl ->
                text(lbl.padEnd(7), dbgFontSize, Colors["#00E5FF"]).position(6.0, 7.0 + i * dbgRowH)
            }
            // Discoverability for the stage-level jump-to-screen picker (see main.kt):
            // available from any scene while this debug overlay is up.
            text("[ J ]  jump to screen     [ M ]  motion", dbgFontSize * 0.85, RGBA(255, 204, 0, 220))
                .position(6.0, 9.0 + dbgLabels.size * dbgRowH)
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

        // The player has 3 baked sprite variants (normal / escorts / roll). Only swap
        // which one is visible when a SHAPE-affecting state changes (escorts on/off,
        // roll start/end); movement, roll squash/rotation and the invuln blink are all
        // cheap transforms applied to the container every frame.
        var lastPlayerShapeKey: String? = null

        // ── Motion tuning panel (KorGE UI widgets) ──────────────────────────────
        // Live-tune the keyboard-motion params while flying. Open with [G] while the debug
        // overlay (`~`) is up — it's the MOTION debug sub-menu. Built from uiSlider/uiText
        // widgets (not hand-rolled) — the project is migrating UI onto the widget toolkit.
        // Keyboard nav (vi-style; only while this menu is active — see the keys block):
        //   j / k = next / prev row,  h / l = decrease / increase the selected slider.
        // Doesn't collide with flying (arrows + WASD) or the JUMP picker — keys are gated
        // on the active debug sub-menu. Mouse drag still works too.
        // Docked bottom-left, sharing the base readout's footprint (same x=8, same bottom
        // edge ch-46) — a sub-menu replaces the base readout in place rather than floating.
        val motionPanel = container {
            solidRect(396.0, 392.0, RGBA(0, 0, 0, 190))
        }.position(8.0, ch - 46.0 - 392.0)
        motionPanel.styles { textColor = Colors.WHITE; textSize = 22.0 }
        // Selected-row highlight (sits behind the labels/sliders; moved on j/k).
        val motionSelHighlight = motionPanel.solidRect(372.0, 58.0, RGBA(0, 229, 255, 45)).position(12.0, 0.0)
        motionPanel.uiText("MOTION  ·  [M] back · j/k row · h/l adjust", Size(372.0, 26.0)) {
            styles { textColor = Colors["#00E5FF"]; textSize = 21.0 }
        }.position(16.0, 12.0)
        motionPanel.visible = false

        val motionSliders = mutableListOf<UISlider>()
        val motionRowYs = mutableListOf<Double>()
        var motionRowY = 56.0
        fun motionRow(name: String, lo: Double, hi: Double, stepV: Double, get: () -> Float, set: (Float) -> Unit) {
            val rowY = motionRowY
            val label = motionPanel.uiText("", Size(364.0, 24.0)).position(16.0, rowY)
            fun refresh() { label.text = "$name   ${get().toDouble().niceStr(2)}" }
            refresh()
            val slider = motionPanel.uiSlider(value = get().toDouble(), min = lo, max = hi, step = stepV, size = Size(360.0, 18.0)) {
                position(18.0, rowY + 30.0)
                showTooltip = false   // value is always shown in the row label; the tooltip renders
                                      // in a separate container that our panel.visible toggle wouldn't hide
                changed { v -> set(v.toFloat()); refresh() }
            }
            motionSliders += slider
            motionRowYs += rowY
            motionRowY += 78.0
        }
        motionRow("ACCEL",         0.5, 8.0,  0.1,  { engine.kbAccel },        { engine.kbAccel = it })
        motionRow("MAX SPEED",     4.0, 24.0, 0.5,  { engine.kbMaxSpeed },     { engine.kbMaxSpeed = it })
        motionRow("HOLD FRICTION", 0.5, 0.95, 0.01, { engine.kbHeldFriction }, { engine.kbHeldFriction = it })
        motionRow("STOP FRICTION", 0.0, 0.95, 0.01, { engine.kbStopFriction }, { engine.kbStopFriction = it })

        var motionSel = 0
        fun selectMotionRow(i: Int) {
            val n = motionRowYs.size
            motionSel = ((i % n) + n) % n   // wrap around top/bottom
            motionSelHighlight.position(12.0, motionRowYs[motionSel] - 6.0)
        }
        selectMotionRow(0)
        // ±one step on the selected slider; UISlider clamps to [min,max] and fires onChange
        // (which refreshes its label), so the spinner value + plane motion update live.
        fun nudgeMotion(dir: Int) { val s = motionSliders[motionSel]; s.value += dir * s.step }

        // ── Overlay ───────────────────────────────────────────────────────────
        val overlayBg   = solidRect(1000.0, ch, RGBA(0, 0, 0, 0)).position(0.0, 0.0)
        val overlayHead = text("", 56.0, Colors.WHITE, font = Fonts.title).position(80.0, 580.0)
        val overlaySub  = text("", 36.0, Colors.WHITE, font = Fonts.content).position(80.0, 670.0)
        val overlayBtn1 = text("", 36.0, Colors["#00FF88"], font = Fonts.content).position(80.0, 770.0)
        val overlayBtn2 = text("", 36.0, Colors.WHITE, font = Fonts.content).position(80.0, 840.0)
        val overlayBtn3 = text("", 36.0, RGBA(255, 255, 255, 160), font = Fonts.content).position(80.0, 910.0)
        overlayHead.visible = false; overlaySub.visible = false
        overlayBtn1.visible = false; overlayBtn2.visible = false; overlayBtn3.visible = false

        // round3: pause-only SEED line + COPY SEED control. [ C ] copies (routed from main.kt).
        val seedLabel = text("SEED:", 36.0, RGBA(255, 255, 255, 160), font = Fonts.content).position(80.0, 1010.0)
        val seedValue = text("", 36.0, Colors["#00E5FF"], font = Fonts.content).position(80.0 + seedLabel.width + 14.0, 1010.0)
        val copySeedRect = solidRect(320.0, 72.0, RGBA(255, 255, 255, 20)).position(500.0, 992.0)
        val copySeedLabel = text("[ C ]  COPY SEED", 36.0, Colors.WHITE, font = Fonts.content).position(500.0, 1010.0)
        copySeedLabel.x = 500.0 + (320.0 - copySeedLabel.width) / 2.0
        seedLabel.visible = false; seedValue.visible = false
        copySeedRect.visible = false; copySeedLabel.visible = false

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
            // [G] enters/leaves the MOTION debug sub-menu (only while the debug overlay is
            // up). j/k select a row, h/l adjust it — all gated on MOTION being active, so
            // they never touch flight (arrows/WASD) or the JUMP picker's keys.
            // [M] = Motion (mnemonic). Yields to the JUMP picker (where M = Menu) while it's up.
            down(Key.M) {
                if (engine.showDebugOverlay && engine.debugSubMenu != DebugSubMenu.JUMP)
                    engine.debugSubMenu = if (engine.debugSubMenu == DebugSubMenu.MOTION) DebugSubMenu.NONE else DebugSubMenu.MOTION
            }
            onSubMenu(engine, DebugSubMenu.MOTION, Key.K) { selectMotionRow(motionSel - 1) }
            onSubMenu(engine, DebugSubMenu.MOTION, Key.J) { selectMotionRow(motionSel + 1) }
            onSubMenu(engine, DebugSubMenu.MOTION, Key.H) { nudgeMotion(-1) }
            onSubMenu(engine, DebugSubMenu.MOTION, Key.L) { nudgeMotion(+1) }
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

                // ── Background: reposition sprites ────────────────────────────
                engine.islands.forEachIndexed { i, isl ->
                    islandViews[i].position(isl.x.toDouble(), isl.y.toDouble())
                }
                engine.clouds.forEachIndexed { i, cloud ->
                    cloudViews[i].position(cloud.x.toDouble(), cloud.y.toDouble())
                }

                // ── Power-ups: pool sprites, scale per pickup, hide unused ────
                powerupPool.forEach { it.visible = false }
                engine.powerUps.forEachIndexed { i, pu ->
                    if (i >= powerupPool.size) return@forEachIndexed
                    val s = (pu.width.toDouble() / AtlasSpec.POWERUP_REF_W) * SpriteAtlas.INV_SCALE
                    powerupPool[i].apply {
                        visible = true
                        position(pu.x.toDouble(), pu.y.toDouble())
                        scaleX = s; scaleY = s
                    }
                }

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
                            atlas.image(this, AtlasSpec.enemy(e.type))
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

                // ── Particles: ECS-driven pool — scale + colorMul, no tessellation ──
                particlePool.forEach { it.visible = false }
                var partIdx = 0
                engine.world.store<Particle>().each { e, p ->
                    if (partIdx >= particlePool.size) return@each
                    val pos     = engine.world.get<Position>(e) ?: return@each
                    val lifePct = p.currentLife.toFloat() / p.maxLife.toFloat()
                    val alpha   = (lifePct * 255).toInt().coerceIn(0, 255)
                    val base    = argbToRgba(p.color)
                    val radius  = (p.size * (0.5f + lifePct * 0.5f)).toDouble()
                    particlePool[partIdx].apply {
                        visible = true
                        position(pos.x.toDouble(), pos.y.toDouble())
                        scaleX = radius; scaleY = radius
                        colorMul = RGBA(base.r, base.g, base.b, alpha)
                    }
                    partIdx++
                }
            }

            // ── Player ── movement/roll/blink are cheap transforms every frame; only
            // the *which-sprite* choice changes when the shape state actually changes.
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
                val rolling = player.rollProgress > 0f
                playerImgRoll.visible    = rolling
                playerImgEscorts.visible = !rolling && player.escortsActive
                playerImgNormal.visible  = !rolling && !player.escortsActive
                lastPlayerShapeKey = playerShapeKey
            }

            // ── Debug overlay ─────────────────────────────────────────────────
            // Visibility is driven from the single source of truth (engine.debugSubMenu):
            // base readout only at BASE; motion panel only at MOTION. Opening a sub-menu
            // hides the base readout. FPS accumulator runs always so the counter is correct
            // the instant it's shown; the (costly) per-field text updates only run when visible.
            debugContainer.visible = engine.showDebugOverlay && engine.debugSubMenu == DebugSubMenu.NONE
            motionPanel.visible    = engine.showDebugOverlay && engine.debugSubMenu == DebugSubMenu.MOTION
            fpsAccumMs += dt.inWholeMilliseconds.toDouble(); fpsFrames++
            if (fpsAccumMs >= 500.0) {
                fpsDisplay     = (fpsFrames * 1000.0 / fpsAccumMs).toInt()
                frameMsDisplay = fpsAccumMs / fpsFrames
                fpsAccumMs     = 0.0; fpsFrames = 0
            }
            if (debugContainer.visible) {
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
                dbgValues[6].text = "${engine.world.store<Particle>().size}"
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
            if (lives != lastLives && lives > 0) {
                livesText.text = "FIGHTERS: ${"* ".repeat(lives).trim()}"; lastLives = lives
            }
            val fuelPct = player.fuel.toInt()
            if (fuelPct != lastFuelPct) {
                fuelText.text = "${fuelPct}%"; lastFuelPct = fuelPct
            }
            val paused = state == GameState.PAUSED
            if (paused != lastPaused) {
                pauseLabel.text = if (paused) "RESUME" else "PAUSE"; lastPaused = paused
            }

            // Seed UI is shown only while paused.
            val showSeed = state == GameState.PAUSED
            seedLabel.visible = showSeed; seedValue.visible = showSeed
            copySeedRect.visible = showSeed; copySeedLabel.visible = showSeed
            if (!showSeed) pauseCopyFlash = 0
            // Center COPY SEED label (re-center after font layout each frame).
            copySeedLabel.x = 500.0 + (320.0 - copySeedLabel.width) / 2.0

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
                    seedValue.text = SeedField.value
                    if (pauseCopyFlash > 0) {
                        pauseCopyFlash--
                        copySeedRect.color = RGBA(0, 255, 136, 40)
                        copySeedLabel.text = "COPIED"; copySeedLabel.color = Colors["#00FF88"]
                    } else {
                        copySeedRect.color = RGBA(255, 255, 255, 20)
                        copySeedLabel.text = "[ C ]  COPY SEED"; copySeedLabel.color = Colors.WHITE
                    }
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

// ── Particle color helper ─────────────────────────────────────────────────────
// (The vector shape functions live in com.vitriolix.airwar2142.render.Shapes — shared
// with the build-time atlas bake.)

private fun argbToRgba(argb: Int) = RGBA(
    (argb ushr 16) and 0xFF,
    (argb ushr  8) and 0xFF,
     argb          and 0xFF,
    (argb ushr 24) and 0xFF
)
