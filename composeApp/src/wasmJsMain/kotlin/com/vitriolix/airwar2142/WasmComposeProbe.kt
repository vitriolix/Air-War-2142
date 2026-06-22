package com.vitriolix.airwar2142

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// SPIKE (spike/compose-korge-interop): does Compose-MP UI compile for the Wasm target?
@Composable
fun WasmComposeProbe() {
    Box(Modifier.fillMaxSize()) { }
}
