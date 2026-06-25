package com.vitriolix.airwar2142

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import korlibs.korge.android.KorgeAndroidView
import com.vitriolix.airwar2142.audio.NoOpSoundPlayer
import com.vitriolix.airwar2142.input.NoOpSensorInput
import com.vitriolix.airwar2142.logic.GameEngine
import kotlinx.coroutines.flow.MutableStateFlow

// ── SPIKE (spike/compose-overlay) ────────────────────────────────────────────
// "Compose everywhere" stress test: a transparent Compose HUD COMPOSITED over live
// KorGE gameplay (not the handoff model — both render every frame). Layout is a Box:
// the KorGE game (KorgeAndroidView) on the bottom, a transparent Compose HUD on top.
// Both share one GameEngine, so the HUD reads the live StateFlows. The question this
// answers (on-device): does the second render layer composite transparently over the
// GL SurfaceView, and what does it cost per frame (the `frameMs` readout)?
//
// To RUN: point the launcher at this Activity (AndroidManifest android:name=
// ".ComposeHostActivity") and `./gradlew playAndroid` on an emulator/device.
class ComposeHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = GameEngine(NoOpSoundPlayer(), NoOpSensorInput())
        val frameMs = MutableStateFlow(0.0)
        val gameConfig = buildGameConfig(engine, frameMs)
        setContent {
            MaterialTheme {
                var inGame by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    if (!inGame) {
                        Button(onClick = { inGame = true }, modifier = Modifier.align(Alignment.Center)) {
                            Text("START CAMPAIGN")
                        }
                    } else {
                        // Bottom layer: the live KorGE game.
                        AndroidView(modifier = Modifier.fillMaxSize(),
                            factory = { ctx -> KorgeAndroidView(ctx).apply { loadModule(gameConfig) } })
                        // Top layer: transparent Compose HUD over gameplay.
                        HudOverlay(engine, frameMs)
                    }
                }
            }
        }
    }
}

@Composable
private fun HudOverlay(engine: GameEngine, frameMs: MutableStateFlow<Double>) {
    val player by engine.player.collectAsState()
    val ms by frameMs.collectAsState()
    Box(Modifier.fillMaxSize()) {   // no background → gameplay shows through
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("SCORE  ${player.score}", color = Color(0xFF00E5FF), fontSize = 22.sp)
            Text("FUEL  ${player.fuel.toInt()}%", color = Color(0xFF00FF88), fontSize = 18.sp)
            Text("FIGHTERS  ${player.lives.coerceAtLeast(0)}", color = Color.White, fontSize = 18.sp)
        }
        Text(
            "KorGE  ${(ms * 10).toInt() / 10.0} ms",
            color = Color(0xFFFFCC00), fontSize = 20.sp,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        )
    }
}
