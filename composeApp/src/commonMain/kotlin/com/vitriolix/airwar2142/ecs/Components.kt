package com.vitriolix.airwar2142.ecs

// First components. Position/Velocity are generic "core" standard-vocabulary
// components (reused as bullets/enemies migrate); Particle is the per-particle
// visual data. Components are plain mutable data — no logic.
data class Position(var x: Float, var y: Float)
data class Velocity(var x: Float, var y: Float)
data class Particle(val size: Float, val color: Int, val maxLife: Int, var currentLife: Int)
