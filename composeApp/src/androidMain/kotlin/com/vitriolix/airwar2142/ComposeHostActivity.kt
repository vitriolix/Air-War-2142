package com.vitriolix.airwar2142

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import korlibs.korge.android.KorgeAndroidView

// ── SPIKE (spike/compose-korge-interop) ──────────────────────────────────────
// Proves the "Compose-First (game as a component)" foundation on Android: a real
// Compose-Multiplatform UI shell that, on demand, embeds the KorGE engine as an
// AndroidView. This is the full-screen *handoff* model (menu and game are never
// co-resident) — not compositing. It exists only to validate that:
//   1) the Compose-MP + compose-compiler plugins coexist with the KorGE plugin, and
//   2) `KorgeAndroidView` is referenceable from a Compose `AndroidView` factory.
// It is NOT wired as the launcher (the manifest still points at KorGE's generated
// MainActivity); loadModule(...) wiring is the deliberate next step if we proceed.
class ComposeHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var inGame by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    if (!inGame) {
                        // Real Compose menu (stand-in for the migrated MenuScene).
                        Button(onClick = { inGame = true }) { Text("START CAMPAIGN") }
                    } else {
                        // KorGE engine embedded full-screen as a component.
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx -> KorgeAndroidView(ctx) }
                        )
                    }
                }
            }
        }
    }
}
