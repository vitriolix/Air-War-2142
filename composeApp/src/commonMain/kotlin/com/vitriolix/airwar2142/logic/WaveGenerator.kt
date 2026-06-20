package com.vitriolix.airwar2142.logic

import com.vitriolix.airwar2142.worldgen.Domain
import com.vitriolix.airwar2142.worldgen.WorldSeed
import com.vitriolix.airwar2142.worldgen.Xoshiro256

// ─────────────────────────────────────────────────────────────────────────────
// First concrete worldgen generator: deterministic enemy-wave schedules (the WAVES
// Domain). See TASKS #21 + the deterministic-worldgen-seeding design.
//
// This is the seed-driven, authorable replacement for GameEngine.spawnEnemiesWave()
// — currently fixed tick-modulo cadence with rand.nextFloat() x-positions pulled
// from the engine's shared Random(42) (order-coupled to particles/backgrounds, the
// exact global-cursor anti-pattern #21 calls out). Here, every level's schedule is
// a pure function of (master seed, genVersion, level) via WorldSeed.stream(WAVES,
// level): the same seed always produces the same level, and a level can be inspected
// or authored ahead of play.
//
// PURE LOGIC — it produces a plan; it does NOT touch the live engine. Wiring the
// plan into tick() (perf-gated, verified by running the app) is a separate slice.
// ─────────────────────────────────────────────────────────────────────────────

/** One scheduled spawn. A SQUADRON_RED uses count/spacingTicks for its staggered formation. */
data class SpawnEvent(
    val tick: Long,
    val type: EnemyType,
    val x: Float,
    val vx: Float,
    val vy: Float,
    val count: Int = 1,
    val spacingTicks: Int = 0,
)

/** A fully-resolved schedule for one level: ordered spawns ending in the boss. */
data class LevelPlan(
    val level: Int,
    val seed: Long,
    val genVersion: Int,
    val bossTick: Long,
    val spawns: List<SpawnEvent>,
)

/**
 * Generates deterministic [LevelPlan]s from a [WorldSeed]. Difficulty scales with
 * level: spawns get denser and the enemy mix shifts toward tougher types.
 */
class WaveGenerator(private val world: WorldSeed) {

    companion object {
        // Matches GameEngine.playWidth (logical play area). Kept local to avoid a
        // dependency on an engine instance; if it ever drifts, both should move to
        // one shared constant.
        const val PLAY_WIDTH = 1000f
        const val BASE_DURATION = 2200L      // ticks until the boss on level 1
        const val LEVEL_DURATION_STEP = 300L // each level runs a little longer
        // x-position math is done in Double and narrowed to Float exactly ONCE (see
        // rollSpawn). Float arithmetic is NOT cross-platform bit-identical — Kotlin/JS
        // emulates float32 via Math.fround and diverges from JVM — but Double ops and a
        // single Double→Float narrowing are identical everywhere (the #21 float rule).
        private const val PLAY_WIDTH_D = 1000.0
        private const val SPAWN_MARGIN_D = 100.0
    }

    /** Build the schedule for [level] (1-based). */
    fun plan(level: Int): LevelPlan {
        require(level >= 1) { "level must be >= 1: $level" }
        val rng = world.stream(Domain.WAVES, level)
        val bossTick = BASE_DURATION + (level - 1) * LEVEL_DURATION_STEP
        val spawns = ArrayList<SpawnEvent>()

        // Walk the level timeline, emitting one wave per beat. The gap between beats
        // shrinks with level (denser) and is jittered by the stream.
        var tick = 60L
        val lastSpawnTick = bossTick - 200
        while (tick < lastSpawnTick) {
            spawns.add(rollSpawn(rng, level, tick))
            val baseGap = (140 - (level - 1) * 8).coerceAtLeast(50)
            tick += baseGap + rng.nextInt(0, 80)
        }

        // The boss always closes the level, centered.
        spawns.add(SpawnEvent(bossTick, EnemyType.BOSS, x = PLAY_WIDTH / 2f, vx = 4f, vy = 0f))
        return LevelPlan(level, world.master, world.genVersion, bossTick, spawns)
    }

    private fun rollSpawn(rng: Xoshiro256, level: Int, tick: Long): SpawnEvent {
        val type = pickType(rng, level)
        val x = (SPAWN_MARGIN_D + rng.nextDouble() * (PLAY_WIDTH_D - 2 * SPAWN_MARGIN_D)).toFloat()
        return when (type) {
            EnemyType.SCOUT -> SpawnEvent(tick, type, x, vx = 0f, vy = 5f)
            EnemyType.DIVER -> SpawnEvent(tick, type, x, vx = 0f, vy = 7f)
            EnemyType.SQUADRON_RED -> SpawnEvent(tick, type, x, vx = 0f, vy = 6f, count = 5, spacingTicks = 15)
            EnemyType.HEAVY_FIGHTER -> SpawnEvent(tick, type, x, vx = 0f, vy = 3f)
            EnemyType.BOSS -> SpawnEvent(tick, type, PLAY_WIDTH / 2f, vx = 4f, vy = 0f)
        }
    }

    /** Weighted enemy pick; weights shift toward tougher types as level rises. */
    private fun pickType(rng: Xoshiro256, level: Int): EnemyType {
        val l = (level - 1).coerceAtMost(8)
        val scout = (40 - l * 3).coerceAtLeast(10)
        val diver = 20 + l
        val squad = 15 + l * 2
        val heavy = 5 + l * 3
        var r = rng.nextInt(scout + diver + squad + heavy)
        if (r < scout) return EnemyType.SCOUT
        r -= scout
        if (r < diver) return EnemyType.DIVER
        r -= diver
        if (r < squad) return EnemyType.SQUADRON_RED
        return EnemyType.HEAVY_FIGHTER
    }
}
