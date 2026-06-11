package com.example.clone1942.ecs

import kotlin.reflect.KClass

// ─────────────────────────────────────────────────────────────────────────────
// Minimal Entity–Component–System kernel.
//
// This is game-agnostic infrastructure (no logic/render/korge imports): entities
// are ids, components are plain data stored in dense per-type arrays, systems are
// logic that iterates a component set each tick. It is the foundation the game is
// incrementally migrated onto (see docs/0001). Storage is a "sparse set" — a packed
// array for cache-friendly, allocation-free iteration, plus an entity→slot index for
// O(1) random access and removal. (Perf gate: typed dense arrays, NOT a
// HashMap<Entity, Component> per component.)
// ─────────────────────────────────────────────────────────────────────────────

// A plain data class (equals/hashCode for use as a map key). Kept simple for
// cross-platform; an inline value class would need @JvmInline (JVM-only) and the
// boxing saved is negligible at our entity counts.
data class Entity(val id: Int)

/** Dense storage for one component type. */
class ComponentStore<T> {
    @PublishedApi internal val dense = ArrayList<T>()        // packed components
    @PublishedApi internal val owners = ArrayList<Entity>()  // entity owning each slot (parallel to dense)
    private val slotOf = HashMap<Entity, Int>()              // entity → slot index

    val size: Int get() = dense.size

    fun add(e: Entity, c: T) {
        val existing = slotOf[e]
        if (existing != null) { dense[existing] = c; return }
        slotOf[e] = dense.size
        dense.add(c); owners.add(e)
    }

    fun get(e: Entity): T? {
        val i = slotOf[e] ?: return null
        return dense[i]
    }

    fun has(e: Entity): Boolean = slotOf.containsKey(e)

    /** O(1) swap-remove: move the last slot into the hole. */
    fun remove(e: Entity) {
        val i = slotOf.remove(e) ?: return
        val last = dense.size - 1
        if (i != last) {
            dense[i] = dense[last]
            owners[i] = owners[last]
            slotOf[owners[i]] = i
        }
        dense.removeAt(last)
        owners.removeAt(last)
    }

    fun clear() { dense.clear(); owners.clear(); slotOf.clear() }

    /** Allocation-free iteration over the packed array (inline → no lambda alloc). */
    inline fun each(action: (Entity, T) -> Unit) {
        for (i in dense.indices) action(owners[i], dense[i])
    }
}

/** Logic over a component set, run once per [World.update] in ascending [order]. */
interface System {
    val order: Int
    fun update(world: World, dt: Float)
}

class World {
    private var nextId = 0
    private val stores = HashMap<KClass<*>, ComponentStore<*>>()
    private val systems = ArrayList<System>()

    fun create(): Entity = Entity(nextId++)

    fun destroy(e: Entity) { for (s in stores.values) s.remove(e) }

    /** Remove every entity/component (e.g. on game reset); systems are kept. */
    fun clear() { for (s in stores.values) s.clear() }

    @PublishedApi internal fun storeFor(k: KClass<*>): ComponentStore<*> =
        stores.getOrPut(k) { ComponentStore<Any?>() }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> store(): ComponentStore<T> =
        storeFor(T::class) as ComponentStore<T>

    inline fun <reified T : Any> add(e: Entity, c: T) = store<T>().add(e, c)
    inline fun <reified T : Any> get(e: Entity): T? = store<T>().get(e)
    inline fun <reified T : Any> has(e: Entity): Boolean = store<T>().has(e)
    inline fun <reified T : Any> remove(e: Entity) = store<T>().remove(e)

    /** `order` is the first cut of the contract's before/after scheduling (#5). */
    fun addSystem(s: System) {
        systems.add(s)
        systems.sortBy { it.order }
    }

    fun update(dt: Float) {
        for (s in systems) s.update(this, dt)
    }
}
