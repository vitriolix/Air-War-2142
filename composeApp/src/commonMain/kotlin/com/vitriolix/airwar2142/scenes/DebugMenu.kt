package com.vitriolix.airwar2142.scenes

import korlibs.event.Key
import korlibs.korge.input.KeysEvents
import com.vitriolix.airwar2142.logic.DebugSubMenu
import com.vitriolix.airwar2142.logic.GameEngine

/**
 * Bind a key that only fires while [menu] is the active debug sub-menu (and the debug
 * overlay is up). This is the *single, uniform* gating idiom for every debug-menu key —
 * a menu's keys are effectively "bound" only while it's visible, without juggling
 * AutoCloseables (which would also create same-key re-entrancy hazards, e.g. the key
 * that opens a menu vs. the one that closes it). Used identically by the JUMP picker
 * (main.kt) and the MOTION panel (GameScene). Menu *toggles* (J/G/ESC) are transitions
 * handled at the call site; this covers the per-menu action keys.
 */
fun KeysEvents.onSubMenu(engine: GameEngine, menu: DebugSubMenu, key: Key, action: suspend () -> Unit) =
    down(key) { if (engine.showDebugOverlay && engine.debugSubMenu == menu) action() }
