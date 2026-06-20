package com.vitriolix.airwar2142.worldgen

// ─────────────────────────────────────────────────────────────────────────────
// Deterministic, cross-platform PRNG primitives for worldgen (see TASKS #21 +
// the deterministic-worldgen-seeding design).
//
// HARD RULE: never use kotlin.random.Random.Default / java.util.Random / JS
// Math.random() for anything that must reproduce from a seed — their streams are
// NOT bit-identical across JVM / Native / JS, so the same seed yields different
// worlds per platform. The generators here are pure integer ops (xor / shift /
// multiply / rotate) on ULong, which Kotlin/JS emulates deterministically, so a
// given seed produces the SAME stream on every backend. Floating point is the
// trap: IEEE-754 add/mul is fine, but sin/cos/pow/sqrt are not cross-platform
// bit-identical — keep generation integer-only; convert to Double only at the
// consumption edge (nextDouble/nextFloat below), never feed a transcendental back
// into generation.
//
// These primitives are deliberately "dumb": they know nothing about WorldSeed,
// Domain, levels, or authored content. Higher layers compose them (see
// WorldSeed.kt). Cross-platform identity is pinned by golden tests run on both
// JVM and JS in commonTest.
// ─────────────────────────────────────────────────────────────────────────────

// SplitMix64 golden gamma (0x9E3779B97F4A7C15) and finalizer multipliers, as the
// canonical reference values. Top-level vals (not const) so the unsigned literals
// can be written verbatim and read back as the published constants.
private val SPLITMIX_GAMMA = 0x9E3779B97F4A7C15uL
private val SPLITMIX_MUL_1 = 0xBF58476D1CE4E5B9uL
private val SPLITMIX_MUL_2 = 0x94D049BB133111EBuL

/** SplitMix64 finalizer — mixes one 64-bit word into a well-distributed result. */
private fun splitMix64Mix(z0: ULong): ULong {
    var z = z0
    z = (z xor (z shr 30)) * SPLITMIX_MUL_1
    z = (z xor (z shr 27)) * SPLITMIX_MUL_2
    return z xor (z shr 31)
}

/**
 * SplitMix64 generator. Tiny, fast, used for **seeding** other generators (its job
 * is to expand a single seed into a well-distributed sequence of state words).
 */
class SplitMix64(seed: Long) {
    private var state: ULong = seed.toULong()

    fun nextLong(): Long {
        state += SPLITMIX_GAMMA
        return splitMix64Mix(state).toLong()
    }
}

/**
 * Order-dependent hash of N longs into one seed (SplitMix64 finalizer per part).
 * This is how higher layers derive an independent stream from a tuple of inputs —
 * e.g. mix(master, genVersion, domain, chunkX, chunkY). Deterministic and
 * well-distributed; the same arguments always yield the same seed.
 */
fun mix(vararg parts: Long): Long {
    var h = SPLITMIX_GAMMA
    for (p in parts) {
        h = splitMix64Mix(h xor p.toULong())
    }
    return h.toLong()
}

private fun rotl(x: ULong, k: Int): ULong = (x shl k) or (x shr (64 - k))

/**
 * xoshiro256** — the working generator. Excellent statistical quality, fast, all
 * integer ops. 256-bit state seeded from SplitMix64 (the recommended way to avoid
 * degenerate all-zero state).
 */
class Xoshiro256(seed: Long) {
    private val s = ULongArray(4)

    init {
        val sm = SplitMix64(seed)
        for (i in 0 until 4) s[i] = sm.nextLong().toULong()
    }

    /** Next raw 64-bit value (full range, including negatives). */
    fun nextLong(): Long {
        val result = rotl(s[1] * 5uL, 7) * 9uL
        val t = s[1] shl 17
        s[2] = s[2] xor s[0]
        s[3] = s[3] xor s[1]
        s[1] = s[1] xor s[2]
        s[0] = s[0] xor s[3]
        s[2] = s[2] xor t
        s[3] = rotl(s[3], 45)
        return result.toLong()
    }

    /** Next 32 bits as an Int (full range). */
    fun nextInt(): Int = (nextLong() ushr 32).toInt()

    /**
     * Uniform-ish Int in [0, bound). Lemire multiply-shift (a negligible bias, no
     * rejection loop) — deterministic across platforms, which is what matters here.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        val bits = (nextLong() ushr 32).toULong() // top 32 bits, unsigned, in [0, 2^32)
        return ((bits * bound.toULong()) shr 32).toInt()
    }

    /** Int in [min, untilExclusive). */
    fun nextInt(min: Int, untilExclusive: Int): Int {
        require(untilExclusive > min) { "empty range: [$min, $untilExclusive)" }
        return min + nextInt(untilExclusive - min)
    }

    /**
     * Double in [0, 1). Uses the top 53 bits — IEEE-754 multiply by a constant is
     * cross-platform deterministic (no transcendentals), so this is replay-safe.
     */
    fun nextDouble(): Double = (nextLong() ushr 11).toDouble() * (1.0 / (1L shl 53))

    /** Double in [min, max). */
    fun nextDouble(min: Double, max: Double): Double = min + nextDouble() * (max - min)

    /** Float in [0, 1). */
    fun nextFloat(): Float = nextDouble().toFloat()

    fun nextBoolean(): Boolean = (nextLong() ushr 63) != 0L
}
