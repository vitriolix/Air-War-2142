package com.vitriolix.airwar2142.worldgen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Golden + property tests for the worldgen PRNG primitives (TASKS #21).
//
// The golden constants below are PINNED outputs. Their job is twofold:
//   1. Regression guard — any accidental change to the generators is caught.
//   2. CROSS-PLATFORM guard — this is commonTest, so it runs on JVM *and* JS in
//      CI; if the JS backend ever diverges from JVM (the whole reason we own the
//      PRNG instead of using kotlin.random), the same pinned constant fails there.
//
// If you intentionally change a generator, regenerate the constants from a JVM run
// and bump GEN_VERSION where appropriate.

private fun digest(rng: Xoshiro256, n: Int): Long {
    var acc = 0L
    repeat(n) { acc = mix(acc, rng.nextLong()) }
    return acc
}

private const val GOLDEN_XOSHIRO_12345 = -5945707318809394554L  // pinned from JVM run
private const val GOLDEN_SPLITMIX_1 = 1121906010908155571L       // pinned from JVM run
private const val GOLDEN_MIX_TUPLE = -5165502572648270291L       // pinned from JVM run

class RngGoldenTest {

    @Test
    fun splitMix64_isPinned() {
        val sm = SplitMix64(1L)
        var acc = 0L
        repeat(8) { acc = mix(acc, sm.nextLong()) }
        assertEquals(GOLDEN_SPLITMIX_1, acc)
    }

    @Test
    fun xoshiro256_isPinned() {
        assertEquals(GOLDEN_XOSHIRO_12345, digest(Xoshiro256(12345L), 1000))
    }

    @Test
    fun mix_isPinned() {
        assertEquals(GOLDEN_MIX_TUPLE, mix(12345L, 1L, 3L, 5L, -7L))
    }

    @Test
    fun sameSeed_sameStream() {
        val a = Xoshiro256(999L)
        val b = Xoshiro256(999L)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
    }

    @Test
    fun differentSeed_differentStream() {
        assertNotEquals(digest(Xoshiro256(1L), 100), digest(Xoshiro256(2L), 100))
    }
}

class WorldSeedTest {

    @Test
    fun domainsAreIndependent() {
        val w = WorldSeed(master = 42L)
        // Each domain yields a distinct stream.
        val digests = Domain.entries.map { digest(w.stream(it), 50) }
        assertEquals(digests.size, digests.toSet().size, "domain streams collided")
    }

    @Test
    fun sameDomain_sameStream() {
        val w = WorldSeed(master = 42L)
        assertEquals(digest(w.stream(Domain.WAVES), 50), digest(w.stream(Domain.WAVES), 50))
    }

    @Test
    fun chunkIsOrderIndependent() {
        val w = WorldSeed(master = 7L)
        // Generating other chunks first must not change chunk (5, 5).
        val direct = digest(w.chunk(Domain.ISLANDS, 5, 5), 50)
        w.chunk(Domain.ISLANDS, 0, 0).nextLong()
        w.chunk(Domain.ISLANDS, 9, -3).nextLong()
        val afterOthers = digest(w.chunk(Domain.ISLANDS, 5, 5), 50)
        assertEquals(direct, afterOthers)
    }

    @Test
    fun genVersionChangesStreams() {
        val v1 = WorldSeed(master = 42L, genVersion = 1)
        val v2 = WorldSeed(master = 42L, genVersion = 2)
        assertNotEquals(digest(v1.stream(Domain.BIOME), 50), digest(v2.stream(Domain.BIOME), 50))
    }

    @Test
    fun waveIndexReproducibleStandalone() {
        val w = WorldSeed(master = 100L)
        assertEquals(digest(w.stream(Domain.WAVES, 40), 20), digest(w.stream(Domain.WAVES, 40), 20))
        assertNotEquals(digest(w.stream(Domain.WAVES, 40), 20), digest(w.stream(Domain.WAVES, 41), 20))
    }
}

class RngRangeTest {

    @Test
    fun nextIntBound_inRange() {
        val r = Xoshiro256(1234L)
        repeat(10_000) {
            val v = r.nextInt(7)
            assertTrue(v in 0 until 7, "out of range: $v")
        }
    }

    @Test
    fun nextIntRange_inRange() {
        val r = Xoshiro256(1234L)
        repeat(10_000) {
            val v = r.nextInt(-5, 5)
            assertTrue(v in -5 until 5, "out of range: $v")
        }
    }

    @Test
    fun nextDouble_inUnitInterval() {
        val r = Xoshiro256(1234L)
        repeat(10_000) {
            val d = r.nextDouble()
            assertTrue(d >= 0.0 && d < 1.0, "out of range: $d")
        }
    }
}
