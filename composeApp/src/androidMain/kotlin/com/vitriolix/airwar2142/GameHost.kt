package com.vitriolix.airwar2142

import korlibs.image.color.Colors
import korlibs.korge.KorgeConfig
import korlibs.korge.scene.SceneContainer
import korlibs.korge.view.addUpdater
import korlibs.math.geom.Size
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.scenes.GameScene
import kotlinx.coroutines.flow.MutableStateFlow

// ── SPIKE (spike/compose-overlay) ────────────────────────────────────────────
// Boots the real KorGE game (straight into GameScene) as a KorgeConfig, for embedding
// in a KorgeAndroidView under a transparent Compose HUD. The same `engine` instance is
// shared with the Compose overlay (single KMP process), so the overlay reads the live
// StateFlows directly. `frameMs` is an EMA of the KorGE frame delta — the number the
// overlay-perf test cares about (does a transparent Compose layer on top regress it?).
//
// No keyboard input layer is wired here: startGame() + GameScene's own updater drive the
// sim (enemies/bullets/particles spawn and render), which is the render load we're
// measuring. Player input isn't needed for a frame-time test.
fun buildGameConfig(engine: GameEngine, frameMs: MutableStateFlow<Double>): KorgeConfig =
    KorgeConfig(
        windowSize = Size(1000.0, CANVAS_HEIGHT.toDouble()),
        virtualSize = Size(1000.0, CANVAS_HEIGHT.toDouble()),
        backgroundColor = Colors["#0F2027"],
        main = {
            engine.startGame()
            val sc = SceneContainer(views)
            addChild(sc)
            sc.changeTo { GameScene(engine, sc) }
            var ema = 16.0
            addUpdater { dt ->
                val ms = dt.inWholeMicroseconds / 1000.0
                if (ms in 0.1..100.0) {            // ignore the first/huge boot frames
                    ema = ema * 0.9 + ms * 0.1
                    frameMs.value = ema
                }
            }
        }
    )
