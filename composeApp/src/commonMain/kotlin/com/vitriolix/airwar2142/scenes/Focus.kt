package com.vitriolix.airwar2142.scenes

import korlibs.korge.view.View

/**
 * A scene that supports directional focus navigation (keyboard arrows now, gamepad
 * d-pad later). Central key dispatch (in main.kt) routes nav input here, so input
 * source and navigation logic stay decoupled.
 */
interface KeyboardNavigable {
    val focusController: FocusController?
}

/**
 * A scene that handles a "back"/escape request (ESC key, or Android back button).
 * Central dispatch (main.kt) routes ESC/BACK here. Return true if handled.
 */
interface EscapeHandler {
    suspend fun onEscape(): Boolean
}

/**
 * Drives focus across a list of selectable items, independent of input source.
 * A single [caret] view is moved to mark the focused item.
 *
 * `move(delta)` / `activate()` are the input-agnostic entry points — wire keyboard,
 * gamepad, or click/hover to them identically.
 */
class FocusController {
    private class Item(
        val caretX: Double,
        val caretY: Double,
        val activate: suspend () -> Unit,
        val leftAct: (suspend () -> Unit)? = null,
        val rightAct: (suspend () -> Unit)? = null,
    )

    /** The marker view; assign after items are registered so it can be created on top. */
    var caret: View? = null

    private val items = mutableListOf<Item>()
    var index = 0
        private set

    /** Register a focusable item; returns its index. caretX/Y is where the caret sits. */
    fun add(
        caretX: Double,
        caretY: Double,
        activate: suspend () -> Unit,
        leftAct: (suspend () -> Unit)? = null,
        rightAct: (suspend () -> Unit)? = null,
    ): Int {
        items += Item(caretX, caretY, activate, leftAct, rightAct)
        return items.size - 1
    }

    /** Place initial focus (clamped) and show the caret. */
    fun start(initialIndex: Int = 0) {
        index = if (items.isEmpty()) 0 else initialIndex.coerceIn(0, items.size - 1)
        refresh()
    }

    private fun refresh() {
        val c = caret ?: return
        val item = items.getOrNull(index)
        if (item == null) { c.visible = false; return }
        c.visible = true
        c.x = item.caretX
        c.y = item.caretY
    }

    fun move(delta: Int) {
        if (items.isEmpty()) return
        index = (index + delta + items.size) % items.size
        refresh()
    }

    suspend fun activate()      { items.getOrNull(index)?.activate?.invoke() }
    suspend fun activateLeft()  { items.getOrNull(index)?.leftAct?.invoke() }
    suspend fun activateRight() { items.getOrNull(index)?.rightAct?.invoke() }
}
