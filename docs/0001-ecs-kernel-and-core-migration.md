# 0001 — ECS Kernel & Incremental Core Migration

**Status:** Proposed (design — not yet implemented)
**Branch:** `ecs-kernel-particles` (first slice)
**Related:** plugin/ECS architecture spec (see project memory `plugin-architecture.md`); sprite-atlas migration (`0000`-era, already merged on `main`)

> This is the first entry in `docs/`. We're starting a habit: **big/breaking changes get a design doc here before the code lands** — what's changing, why, code sketches, and how we'll verify (incl. perf). Docs are numbered ADR-style.

---

## 1. Why this change

We're re-architecting the game so that **plugins are the core way game logic is structured** (3rd-party mods get near-total control). The agreed foundation is an **Entity–Component–System (ECS)** kernel with the base game itself authored as plugins. The full set of locked decisions lives in project memory (`plugin-architecture.md`); the load-bearing ones for *this* doc:

- **Everything is a plugin; the engine is a thin kernel** (decision #2).
- **ECS** is the structuring model — entities (ids), components (data), systems (logic over component sets) (#4).
- **Incremental migration** — stand up the kernel and move systems onto it one at a time, *game playable at every merge* (#3).
- **Perf gate** — no frame-time regression vs. the measured baseline (#14).
- **No backward-compat burden** — no shipped user-data to preserve; free to make breaking changes (#3).

ECS is what makes "a mod adds behavior to existing things without touching them" possible — e.g. the EMP feature becomes *a new `Stun` component + a `StunSystem`*, with zero edits to enemies or the engine.

---

## 2. Scope of this task

"Make the core app changes" is **not one PR** — per the incremental decision it's a *sequence* of small, atomic, individually-mergeable PRs, each green and each behind the perf gate, with the game playable throughout.

### PR decomposition (core)

| PR | Content | Touches |
|----|---------|---------|
| **A** *(this doc)* | ECS kernel skeleton + migrate **particles** as the first proof | new `ecs/` pkg, `GameEngine` particle code, `GameScene` particle render |
| B | Plugin host + compiled-plugin registration; existing systems become *registered* systems | `ecs/`, scene wiring |
| C…N | Migrate one subsystem per PR: background → bullets → enemies/spawn → player + input-mapping (lifting input layer into kernel **intact**) → collision → scoring → power-ups → HUD | one subsystem each |

**Then (separate branch/PR):** the **EMP** feature as the first dogfooded plugin — and a key simplification: EMP can be a *compiled first-party* plugin, so it needs **no scripting interpreter** (that whole VM track is later).

### Out of scope (explicitly later)
Scripting VM / interpreter · plugin manifest loader · distribution / `plugins/` folder baking · maps/shaders/audio plugin formats · EMP itself · iOS target.

---

## 3. PR-A in detail

**Goal:** prove the ECS kernel works *and integrates with the real game*, using particles as the guinea pig, with **behavior-identical** output and **no perf regression**. Everything else in the game is untouched.

Particles are the right first target: they're already pooled data (`Particle(x,y,vx,vy,size,color,maxLife,life)`), purely visual, and high-count — a good exercise of storage + iteration without risking gameplay.

### 3.1 The kernel skeleton (new package `com.example.clone1942.ecs`)

A minimal **sparse-set ECS**: dense component arrays for cache-friendly iteration, an entity→slot index for joins/removal. (The perf gate requires typed dense storage, *not* `HashMap<Entity,Component>` per component.)

```kotlin
data class Entity(val id: Int)   // plain data class — cross-platform (@JvmInline is JVM-only)

/** Dense storage for one component type: packed array + entity↔slot index. */
class ComponentStore<T> {
    @PublishedApi internal val dense  = ArrayList<T>()       // packed components
    @PublishedApi internal val owners = ArrayList<Entity>()  // entity per slot
    private val slotOf = HashMap<Entity, Int>()              // entity -> slot

    val size get() = dense.size

    fun add(e: Entity, c: T) {
        slotOf[e]?.let { dense[it] = c; return }
        slotOf[e] = dense.size; dense.add(c); owners.add(e)
    }
    fun get(e: Entity): T? = slotOf[e]?.let { dense[it] }
    fun has(e: Entity) = e in slotOf
    fun remove(e: Entity) {                                  // O(1) swap-remove
        val i = slotOf.remove(e) ?: return
        val last = dense.size - 1
        if (i != last) { dense[i] = dense[last]; owners[i] = owners[last]; slotOf[owners[i]] = i }
        dense.removeAt(last); owners.removeAt(last)
    }
    /** Allocation-free iteration over the packed array. */
    inline fun each(action: (Entity, T) -> Unit) {
        for (i in dense.indices) action(owners[i], dense[i])
    }
}

interface System { val order: Int; fun update(world: World, dt: Float) }

class World {
    private var next = 0
    private val stores  = HashMap<KClass<*>, ComponentStore<*>>()
    private val systems = ArrayList<System>()

    fun create(): Entity = Entity(next++)
    fun destroy(e: Entity) { stores.values.forEach { it.remove(e) } }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> store(): ComponentStore<T> =
        storeFor(T::class) as ComponentStore<T>
    @PublishedApi internal fun storeFor(k: KClass<*>) =
        stores.getOrPut(k) { ComponentStore<Any>() }

    inline fun <reified T : Any> add(e: Entity, c: T) = store<T>().add(e, c)
    inline fun <reified T : Any> get(e: Entity): T?   = store<T>().get(e)
    inline fun <reified T : Any> has(e: Entity)       = store<T>().has(e)

    fun addSystem(s: System) { systems.add(s); systems.sortBy { it.order } } // before/after via `order`
    fun update(dt: Float) { for (s in systems) s.update(this, dt) }
}
```

> `order` is the first cut of the `before/after` system scheduling from the contract (#5); it graduates to named hints later. `ComponentStore<Any>` + reified casts keep us reflection-free in the hot path (only `KClass` *tokens* as map keys, looked up once per system per frame — not per entity).

### 3.2 The particle components (first blessed `core.*` components)

```kotlin
data class Position(var x: Float, var y: Float)            // blessed standard component
data class Velocity(var x: Float, var y: Float)            // blessed standard component
data class Particle(val size: Float, val color: Int, val maxLife: Int, var life: Int)
```

### 3.3 The systems

`MovementSystem` is deliberately generic (`Position + Velocity`) — it's reused by bullets/enemies in later PRs. This is the ECS payoff demonstrated on day one.

```kotlin
class MovementSystem : System {                 // anything that moves
    override val order = 100
    override fun update(world: World, dt: Float) {
        world.store<Velocity>().each { e, v ->
            world.get<Position>(e)?.let { it.x += v.x; it.y += v.y }   // per-tick step, matches current engine
        }
    }
}

class ParticleSystem(private val dead: ArrayList<Entity> = ArrayList()) : System {
    override val order = 200
    override fun update(world: World, dt: Float) {
        dead.clear()                            // reused buffer — no per-frame alloc (perf gate)
        world.store<Particle>().each { e, p -> if (++p.life >= p.maxLife) dead.add(e) }
        for (e in dead) world.destroy(e)
    }
}
```

### 3.4 Integration points (the "messy middle" of incremental migration)

**World ownership:** `GameEngine` gains `val world = World()` and registers the two systems; `tick()` calls `world.update(dt)` where it currently calls `updateParticles()`.

**Spawning — before → after:**
```kotlin
// before (GameEngine, ~10 call sites)
particles.add(Particle(p.x, p.y + p.height/2f, 0f, 5f, 6f, 0xFFFFAA00.toInt(), 5, 0))

// after — a helper on the engine creates the entity + components
fun spawnParticle(x: Float, y: Float, vx: Float, vy: Float, size: Float, color: Int, maxLife: Int) {
    val e = world.create()
    world.add(e, Position(x, y)); world.add(e, Velocity(vx, vy))
    world.add(e, Particle(size, color, maxLife, 0))
}
// → world.spawnParticle(p.x, p.y + p.height/2f, 0f, 5f, 6f, 0xFFFFAA00.toInt(), 5)
```

**Removed:** `engine.particles: MutableList<Particle>`, `updateParticles()`, and the data class `Particle`'s old home (moves into `ecs` as a component). Debug overlay `PARTS` now reads `world.store<Particle>().size`.

**Rendering — `GameScene` particle loop, before → after:** the existing 120-slot pool stays; only its data source changes.
```kotlin
// before: engine.particles.forEachIndexed { i, p -> pool[i].apply { ... } }
// after:
particlePool.forEach { it.visible = false }
var i = 0
world.store<Particle>().each { e, p ->
    if (i >= particlePool.size) return@each
    val pos = world.get<Position>(e) ?: return@each
    val lifePct = 1f - p.life.toFloat() / p.maxLife
    val radius  = (p.size * (0.5f + lifePct * 0.5f)).toDouble()
    val base    = argbToRgba(p.color)
    particlePool[i].apply {
        visible = true; position(pos.x.toDouble(), pos.y.toDouble())
        scaleX = radius; scaleY = radius
        colorMul = RGBA(base.r, base.g, base.b, (lifePct * 255).toInt().coerceIn(0, 255))
    }
    i++
}
```

### Data-flow (after PR-A)

```
            spawn (hits/deaths/exhaust)
GameEngine ───────────────► World ── Position ─┐
   │ tick()                  (entities)  Velocity│
   │  └─ world.update(dt):                Particle│
   │       MovementSystem  (Pos += Vel)          │
   │       ParticleSystem  (age, destroy dead)   │
   ▼                                             ▼
GameScene render ── reads world ──► existing 120-slot pool (unchanged)
```

Everything *else* in `GameEngine.tick()` / `GameScene` is byte-for-byte the same as today.

---

## 4. Verification & the perf gate (#14)

**Functional (the `verify` discipline — run it, don't just compile):** launch the JVM app, drive it to generate particles (kill enemies → explosion bursts; player exhaust), screenshot, and confirm particle look/behaviour is **identical** to `main`.

**Perf A/B** against the measured baseline (always `pmset -g` first — Low Power Mode throttles browser rAF and invalidates web numbers):

| Metric (web, headed Chromium, `/tmp/measure.js`) | `main` baseline | PR-A target |
|---|---|---|
| Median frame | **3.9 ms** | ≤ 3.9 ms |
| p95 | **7.3 ms** | ≤ ~7.3 ms |
| Throughput (uncapped) | **~199 fps** | ≥ ~199 fps |
| JVM | ~58 fps / 17 ms | unchanged |

```
frame-time budget (16.7ms @60fps)
main  sprites  ▏median 3.9  ███▉················  p95 7.3 ███████▎
PR-A  target   ▏median ≤3.9 (no regression — particles are a small slice of load)
                0ms                          16.7ms
```

> Particles are a small fraction of total load, so PR-A's perf delta should be ~noise. The point of measuring here isn't that particles are heavy — it's to **establish the A/B ritual** every migration PR will follow, and to catch storage/allocation mistakes early (the dense store + reused `dead` buffer exist precisely to keep this flat).

**Gate rule:** if a step regresses, fix storage/allocation *before* moving on — never accumulate a mystery slowdown.

---

## 5. Risks & notes

- **GC/allocation on JS/Wasm** is the historical foot-gun. Mitigations baked into the design: dense stores, allocation-free `each`, a reused `dead` buffer, no per-frame component/lambda garbage.
- **Cross-component joins** (`get<Position>` inside a `Velocity` loop) do a map lookup per entity. Fine at this scale (≤ a few hundred entities); if a future hot system needs it, we move to archetype iteration. Not needed now.
- **Two sources of truth during migration** is inherent to incremental work — acceptable because each PR fully migrates its subsystem (no half-states left dangling).
- **Determinism** (seeded RNG + fixed step) isn't required for PR-A but the kernel shouldn't introduce wall-clock/entropy — it doesn't.

---

## 6. Definition of done (PR-A)

- [ ] `ecs/` kernel skeleton (`Entity`, `ComponentStore`, `World`, `System`) compiles on all targets.
- [ ] Particles fully migrated: spawns → entities, `MovementSystem` + `ParticleSystem`, render reads the world; `engine.particles`/`updateParticles` removed; `PARTS` debug wired to the store.
- [ ] JVM run shows **identical** particle behaviour (screenshot evidence).
- [ ] Perf A/B shows **no regression** vs. baseline (LPM checked).
- [ ] Squash-merged to `main`; next PR (B) opens fresh.

---

<!-- TASKS:auto START -->
## Tasks (from TASKS.md)

<!-- Generated from TASKS.md by `./gradlew syncDocTasks` — edit tasks there, not here. -->

- [x] **PR-A** — ECS kernel + migrate particles (PR #2)
- [ ] **PR-B** — plugin host + turn existing systems into *registered* systems
- [ ] **PR-C…N** — migrate one subsystem per PR: background → bullets → enemies/spawn → player + input-mapping (lift input layer **intact**) → collision → scoring → power-ups → HUD
- [x] #3 **Incremental** migration; lift input layer intact; no backward-compat burden
- [x] #4 Kernel↔plugin boundary; ECS; standard-lib blessed components
- [ ] #15 **ECS dev-tooling** — entity inspector, system trace/profiler, deterministic replay, per-system toggles; **VM error reporting = hard requirement**
- [ ] #19 **Deterministic command-log + replay** (elaborates #15's "deterministic replay"; needs its own `docs/` design doc before code — next free number; `0002`–`0004` are taken). Goal: log every player command + (optionally) plugin/system actions so a bug session is byte-reproducible. **Analysis done:** sim is already near-deterministic — seeded `Random(42)` (all RNG routed through it) + fixed-timestep `tick()` (ignores wall-clock `dt`; ECS gets `world.update(1f)`). So a session = **seed + ordered command stream pinned to tick**; replay regenerates all system/particle output, so logging plugin actions is only a *verification* trace, not needed to reproduce. The "commands" = `GameEngine`'s mutation surface: `startGame`/`proceedToNextLevel`/`togglePause`/`returnToMenu`/`setControlMode`, per-frame `updateKeyboardInputs`/`updateTouchTarget` + the tilt read inside `tick()`, `triggerRoll`, and `tick()` itself — funneled in from `GameScene.kt` (updater/touch/keys) + `main.kt` (stage keys). **3 gaps to close:** (1) tilt is read live from the sensor inside the tick → must record per-tick as data; (2) event-driven cmds (roll/pause/touch) need a tick stamp to preserve ordering; (3) cross-platform float math (`sin`/`sqrt`) isn't bit-identical JVM/JS/Wasm → honest guarantee is *same-platform* replay. **Open forks (parked — ask before building):** (a) refactor depth: full sealed `GameCommand` + `engine.dispatch()` *[leaning yes — matches the "command pattern" framing + future plugin/VM kernel boundary; must preserve macOS HeldKey/touch hacks]* vs minimal tap recorder; (b) log scope: commands + cheap per-tick state checksum *[leaning — checksum localizes divergence, perf-safe]* vs commands-only vs full per-system trace *[avoid — perf gate]*; (c) replay form: headless harness/test *[leaning]* vs in-app ghost replay vs recorder+format-only. **Perf gate:** recorder must be off by default + delta-encode (log on change), near-zero cost when disabled; A/B it. Log sink is multiplatform (expect/actual): JVM→file, web→console/download, Android→logcat+files.
- [x] **PR #2** — ECS kernel + particle migration → **merged** (first ECS slice on `main`)

<!-- TASKS:auto END -->
