package com.vitriolix.airwar2142.logic

import com.vitriolix.airwar2142.worldgen.WorldSeed
import com.vitriolix.airwar2142.worldgen.mix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Golden + property tests for the WAVES generator (TASKS #21). The golden digest
// runs in commonTest, so it executes on JVM / JS / wasmJs — if any backend's plan
// diverged, it would fail there (the cross-platform guarantee, end to end through a
// real generator, not just the raw PRNG).

private fun digest(plan: LevelPlan): Long {
    var acc = mix(plan.level.toLong(), plan.seed, plan.genVersion.toLong(), plan.bossTick)
    for (s in plan.spawns) {
        acc = mix(
            acc, s.tick, s.type.ordinal.toLong(),
            s.x.toRawBits().toLong(), s.vx.toRawBits().toLong(), s.vy.toRawBits().toLong(),
            s.count.toLong(), s.spacingTicks.toLong(),
        )
    }
    return acc
}

// Pinned from a JVM run; verified identical on JS + wasmJs.
private const val GOLDEN_LEVEL1_SEED12345 = -6397650549828450055L

class WaveGeneratorTest {

    @Test
    fun plan_isPinned() {
        val plan = WaveGenerator(WorldSeed(master = 12345L)).plan(1)
        assertEquals(GOLDEN_LEVEL1_SEED12345, digest(plan))
    }

    @Test
    fun sameSeedAndLevel_samePlan() {
        val a = WaveGenerator(WorldSeed(master = 7L)).plan(3)
        val b = WaveGenerator(WorldSeed(master = 7L)).plan(3)
        assertEquals(a, b)
    }

    @Test
    fun differentSeed_differentPlan() {
        val a = WaveGenerator(WorldSeed(master = 1L)).plan(1)
        val b = WaveGenerator(WorldSeed(master = 2L)).plan(1)
        assertNotEquals(digest(a), digest(b))
    }

    @Test
    fun differentLevel_differentPlan() {
        val w = WorldSeed(master = 1L)
        assertNotEquals(digest(WaveGenerator(w).plan(1)), digest(WaveGenerator(w).plan(2)))
    }

    @Test
    fun genVersionChangesPlan() {
        val v1 = WaveGenerator(WorldSeed(master = 1L, genVersion = 1)).plan(1)
        val v2 = WaveGenerator(WorldSeed(master = 1L, genVersion = 2)).plan(1)
        assertNotEquals(digest(v1), digest(v2))
    }

    @Test
    fun spawnsAreChronological() {
        val plan = WaveGenerator(WorldSeed(master = 99L)).plan(2)
        for (i in 1 until plan.spawns.size) {
            assertTrue(plan.spawns[i].tick >= plan.spawns[i - 1].tick, "spawns not ordered by tick")
        }
    }

    @Test
    fun bossClosesTheLevel() {
        val plan = WaveGenerator(WorldSeed(master = 99L)).plan(2)
        val last = plan.spawns.last()
        assertEquals(EnemyType.BOSS, last.type)
        assertEquals(plan.bossTick, last.tick)
        // exactly one boss
        assertEquals(1, plan.spawns.count { it.type == EnemyType.BOSS })
    }

    @Test
    fun allSpawnsWithinPlayArea() {
        val plan = WaveGenerator(WorldSeed(master = 99L)).plan(5)
        for (s in plan.spawns) {
            assertTrue(s.x in 0f..WaveGenerator.PLAY_WIDTH, "x out of bounds: ${s.x}")
        }
    }

    @Test
    fun higherLevelsRunLongerAndDenser() {
        val w = WorldSeed(master = 5L)
        val l1 = WaveGenerator(w).plan(1)
        val l5 = WaveGenerator(w).plan(5)
        assertTrue(l5.bossTick > l1.bossTick, "later level should run longer")
        // Denser: more non-boss spawns per tick at level 5 than level 1.
        val d1 = (l1.spawns.size - 1).toDouble() / l1.bossTick
        val d5 = (l5.spawns.size - 1).toDouble() / l5.bossTick
        assertTrue(d5 > d1, "later level should be denser ($d5 vs $d1)")
    }
}
