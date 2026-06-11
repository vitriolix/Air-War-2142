package com.example.clone1942.ecs

// Moves anything with Position + Velocity, one fixed step per tick (matches the
// engine's integer-step integration: x += vx, y += vy). Generic on purpose — it
// will move bullets and enemies too as those subsystems migrate.
class MovementSystem : System {
    override val order = 100
    override fun update(world: World, dt: Float) {
        world.store<Velocity>().each { e, v ->
            world.get<Position>(e)?.let { it.x += v.x; it.y += v.y }
        }
    }
}

// Ages particles and culls them at end of life (matches the old updateParticles():
// currentLife++ then remove where currentLife >= maxLife).
class ParticleSystem : System {
    override val order = 200
    private val dead = ArrayList<Entity>()   // reused buffer — no per-frame allocation
    override fun update(world: World, dt: Float) {
        dead.clear()
        world.store<Particle>().each { e, p -> if (++p.currentLife >= p.maxLife) dead.add(e) }
        for (e in dead) world.destroy(e)
    }
}
