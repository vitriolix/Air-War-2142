package com.vitriolix.airwar2142.logic

import com.vitriolix.airwar2142.worldgen.SplitMix64

/**
 * MOCK seed store for the Menu/Pause seed UI (Design round 3). This is a deliberate placeholder
 * until the real plumbing lands: the string→64-bit registry (TASKS task-21) and the engine-seed
 * thread-through (task-23). It holds the player-facing seed *string* the editor shows/edits; nothing
 * here drives worldgen yet — `startGame()` does not consume it.
 *
 * `roll()` uses the merged worldgen [SplitMix64] (not a platform RNG) to fabricate placeholder
 * values, so even the mock honors the determinism discipline.
 */
object SeedField {
    const val MAX_LEN = 16

    var value: String = "4815162342"
        private set

    private var nonce = 0L

    fun set(v: String) { value = v.filter { it.isDigit() }.take(MAX_LEN) }

    fun append(c: Char) { if (c.isDigit() && value.length < MAX_LEN) value += c }

    fun backspace() { if (value.isNotEmpty()) value = value.dropLast(1) }

    /** Fabricate a new placeholder seed (mock "randomize"). Deterministic per call-count. */
    fun roll() {
        val r = SplitMix64(nonce++ xor value.hashCode().toLong()).nextLong()
        val n = (r and Long.MAX_VALUE) % 10_000_000_000L
        value = n.toString().padStart(10, '0')
    }
}
