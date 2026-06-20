package com.vitriolix.airwar2142.worldgen

// ─────────────────────────────────────────────────────────────────────────────
// Named / spatial stream derivation (see TASKS #21).
//
// The core idea: NEVER pull worldgen from one shared, global RNG cursor. Doing so
// makes generation ORDER a hidden dependency — add one cosmetic feature and every
// enemy wave downstream shifts, so seeds stop being stable across code changes.
// Instead, derive an INDEPENDENT sub-stream per subsystem (Domain) and per spatial
// cell by hashing (master, genVersion, domain, coords) with mix(). Each stream is
// isolated, so:
//   • adding randomness in one Domain never perturbs another,
//   • any chunk / wave is reproducible standalone (lazy / infinite worlds),
//   • genVersion folded into the hash gives free versioning — bumping it
//     re-namespaces every stream, so old (genVersion, seed) pairs keep
//     reproducing their original worlds.
//
// LevelSpec (seed + per-domain authored overrides) builds ON TOP of this and is a
// later slice; this file is just the seed → stream derivation.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Generation algorithm version, folded into every derived stream. **Bump this when
 * a worldgen change should produce different output for existing seeds.** Store it
 * alongside the seed in world metadata as the pair (GEN_VERSION, seed).
 */
const val GEN_VERSION: Int = 1

/**
 * Independent worldgen subsystems. Each gets its own stream from a [WorldSeed], so
 * one subsystem's randomness can't disturb another's. Append new domains at the END
 * (ordinal is part of the derivation — reordering changes every downstream seed).
 */
enum class Domain { BIOME, TERRAIN, ISLANDS, WAVES, BOSS, LOOT, COSMETIC }

/**
 * A world's master seed plus the version it was generated under. Hands out
 * independent [Xoshiro256] streams keyed by [Domain] (and optionally by spatial
 * cell or an extra index such as a wave number).
 */
class WorldSeed(val master: Long, val genVersion: Int = GEN_VERSION) {

    /** Independent stream for a whole subsystem (no spatial/index keying). */
    fun stream(domain: Domain): Xoshiro256 =
        Xoshiro256(mix(master, genVersion.toLong(), domain.ordinal.toLong()))

    /**
     * Independent stream keyed by an extra index within a domain — e.g. enemy wave
     * number, so wave N is reproducible without simulating 1..N-1.
     */
    fun stream(domain: Domain, index: Int): Xoshiro256 =
        Xoshiro256(mix(master, genVersion.toLong(), domain.ordinal.toLong(), index.toLong()))

    /**
     * Independent stream for a spatial cell — chunk (cx, cy) generates identically
     * regardless of the order chunks are visited, enabling lazy / infinite worlds.
     */
    fun chunk(domain: Domain, cx: Int, cy: Int): Xoshiro256 =
        Xoshiro256(mix(master, genVersion.toLong(), domain.ordinal.toLong(), cx.toLong(), cy.toLong()))
}
