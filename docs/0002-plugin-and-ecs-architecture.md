# 0002 — Plugin & ECS Architecture (reference)

**Status:** Reference / architecture overview. The design is **locked** (decisions #1–#13, see
project memory `plugin-architecture`); most of it is **not yet implemented** — the ECS kernel
+ particle migration (`docs/0001`, PR #2) is the only slice on `main` so far.
**Related:** [`0001` — ECS Kernel & Core Migration](0001-ecs-kernel-and-core-migration.md) ·
[`../TASKS.md`](../TASKS.md) (roadmap & decision index) · `plugin-architecture` project memory.

> This is the map of where the engine is going: **the whole game becomes plugins over a thin
> kernel.** `0001` is the first step on the ground; this doc is the destination and the
> vocabulary (objects, data, events, contract) everything else is built against. It is a
> living reference — each migration PR should update the status markers below as slices land.

---

## 0. What exists today vs. what's designed

Read this legend everywhere in the doc — it's the difference between "you can call this now" and
"this is the plan."

| Marker | Meaning |
|:--:|---|
| ✅ | **Implemented** on `main` (you can read/run it today) |
| 🟡 | **Partial** — exists but not in its final, kernelized form |
| ⬜ | **Designed, not built** — locked spec, no code yet |

| Area | State | Where |
|---|:--:|---|
| ECS kernel — `Entity`, `ComponentStore`, `World`, `System` | ✅ | `ecs/Ecs.kt` |
| Core components `Position`, `Velocity`, `Particle` | ✅ | `ecs/Components.kt` |
| Systems `MovementSystem`, `ParticleSystem` | ✅ | `ecs/Systems.kt` |
| System ordering (integer `order`) | ✅ | `ecs/Ecs.kt` |
| Seeded RNG + fixed-step tick (determinism floor) | ✅ | `logic/GameEngine.kt` |
| Input layer (macOS `HeldKey`, `singleTouch` *Anywhere, key-enum divergence) | 🟡 | `scenes/GameScene.kt`, `main.kt` — works, but lives in the scene, not lifted into a kernel **input service** |
| Sprite atlas (build-time vector→atlas bake, batched draws) | 🟡 | `render/` — first-party bake done; per-plugin namespaced atlases ⬜ |
| Sound bridge | 🟡 | `interfaces/SoundPlayer` (NoOp impls) |
| Gameplay as plugins (player, weapons, enemies, collision, scoring, power-ups, bg, hud) | ⬜ | still in the `GameEngine` monolith (`logic/GameEngine.kt`) |
| Plugin host (manifest, load order, registration, feature flags) | ⬜ | — |
| Standard component library beyond the 3 above (`Health`, `Collider`, `Stun`, …) | ⬜ | — |
| Event bus + standard events | ⬜ | — |
| Scripting VM / Kotlin-subset DSL interpreter | ⬜ | separate later track (#16) |
| `wrap` / `replace`, namespacing, contract enforcement | ⬜ | — |
| Distribution (bundled `plugins/` bake, .zip modpacks) | ⬜ | — |
| Sandbox (instruction budget, crash isolation) | ⬜ | — |

So: **today the game is a conventional KorGE app with a small ECS island (particles).** The rest
of this doc describes the target it's being driven toward, one perf-gated PR at a time.

---

## 1. Why plugins

The goal (josh): make art, maps/paths, sprites, sound, music, shaders, **and gameplay logic**
all extensible — with plugins as the *core* way game logic is structured, so 3rd-party mods get
**near-total control** (read/modify/replace any entity, component, or system).

The lever that makes this possible is **ECS**: when behavior is "a system over a component set,"
a mod adds behavior to existing things *without touching them*. The worked example is **EMP**: it
becomes a new `Stun` component + a `StunSystem`, with **zero edits** to enemies or the engine.

Two consequences drive the whole design:

- **#2 Everything is a plugin.** The engine is a thin **kernel**; the base game "Air War 2142"
  ships as plugin bundles that are **peers to mods** — no privileged core. Anything a mod can do
  to the base game, the base game already does to itself.
- **#1 Dual-backend plugins.** Real Kotlin at runtime is impossible (the compiler is JVM-only;
  Wasm is sealed). So plugins are **data + a sandboxed Kotlin-*flavored* scripting VM** for
  3rd-party, while **first-party/base-game is authored in a real-Kotlin DSL and compiled in**.
  Both are written against the *same* strict Kotlin subset, so a plugin is promotable from
  interpreted → compiled with **zero rewrite**.

---

## 2. The big picture

```
 ┌──────────────────────────────────────────────────────────────────────────┐
 │  PLATFORM  (KorGE 6 / korlibs)         JVM · Android · JS · Wasm  [· iOS]  │
 │  window, GL, AWT/touch events, audio device, file I/O, sensors            │
 └───────────────────────────────┬──────────────────────────────────────────┘
                                  │  expect/actual Platform.kt (CANVAS_HEIGHT…)
 ┌───────────────────────────────▼──────────────────────────────────────────┐
 │  KERNEL  (thin, game-agnostic — the only thing that talks to the platform)│
 │                                                                            │
 │   ECS runtime ✅      Plugin host ⬜      Scripting VM ⬜                    │
 │   World/stores       load order,         Kotlin-subset                     │
 │   systems, order     registration        tree-walker                       │
 │                                                                            │
 │   Input service 🟡   Asset loader 🟡    Render bridge 🟡   Core services ⬜  │
 │   (lift HeldKey      (atlases,          (sprite-atlas      dt, seeded RNG,  │
 │    intact) +         sprites.txt)        batching/pool)    event bus, hud,  │
 │    action bindings                                         sound, shader,   │
 │                                                           spawn/query/mutate│
 └───────────────────────────────┬──────────────────────────────────────────┘
                                  │  Host API  (the ONLY surface plugins see)
        ┌─────────────────────────┼─────────────────────────────────┐
        ▼                         ▼                                 ▼
 ┌───────────────┐        ┌───────────────┐                 ┌───────────────┐
 │ BASE GAME ⬜  │        │ BASE GAME ⬜  │      …           │  3rd-party    │
 │ plugin bundle │        │ plugin bundle │                 │  mod (EMP) ⬜ │
 │ player/weapons│        │ enemies/spawn │                 │ Stun + System │
 │ (compiled)    │        │ collision…    │                 │ (interpreted) │
 └───────────────┘        └───────────────┘                 └───────────────┘
   first-party = real-Kotlin DSL, compiled-in        3rd-party = data + VM
        (peers — same Host API, no privilege)
```

**The boundary is the contract.** The kernel never knows what "an enemy" is; plugins never touch
GL or the keyboard. Everything crosses at the Host API.

### 2.1 Kernel vs. plugin responsibilities (#4)

| The **kernel** owns (mechanism) | **Plugins** own (policy / all gameplay) |
|---|---|
| ECS runtime (entities, component stores, system scheduler) | Player, weapons, escorts, roll |
| Plugin host (scan, deps, load order, register, feature flags) | Enemies + spawn waves, boss patterns |
| Scripting VM (interpret the Kotlin subset, enforce budgets) | **Collision**, **scoring**, power-ups |
| Input service (platform key/touch/tilt → action bindings) | Levels, background/parallax |
| Asset loader (namespaced atlases, audio) | HUD *content*, audio cues, shaders (EMP ripple) |
| Render bridge (sprite-atlas batching + view pooling) | Anything composed from core.* components/events |
| Core services: `dt`, **seeded RNG**, event bus, sound, shader, hud, spawn/query/mutate | |

Rule of thumb: **if it touches the device or the frame loop, it's kernel; if it's a game rule,
it's a plugin.**

---

## 3. The ECS model ✅ (this part is real)

Entities are ids, components are plain data, systems are logic over a component set. The kernel is
a **sparse-set ECS**: dense per-type arrays for cache-friendly, allocation-free iteration, plus an
entity→slot index for O(1) random access and removal. (Perf gate: typed dense arrays, **never** a
`HashMap<Entity, Component>` per component.)

```
 World ✅
 ├─ entities: just ints      Entity(0) Entity(1) Entity(2) Entity(3) …
 │
 ├─ component stores (one dense array per type)
 │    Position │ (5,1) │ (9,3) │ (2,7) │        owners ─┐ slotOf: entity→slot
 │    Velocity │ (0,-22)│ (0,5)│       │               │ (O(1) get / swap-remove)
 │    Particle │  …    │       │       │               ▼
 │                                            packed → inline each{} = no alloc
 │
 └─ systems (run each tick in ascending `order`)
      order 100  MovementSystem   Position += Velocity
      order 200  ParticleSystem   age, cull at end-of-life
                       │
                  world.update(dt)  ── called once per GameEngine.tick()
```

The actual kernel (`ecs/Ecs.kt`, abridged):

```kotlin
data class Entity(val id: Int)            // plain data class — @JvmInline is JVM-only

class ComponentStore<T> {                 // dense storage for ONE component type
    @PublishedApi internal val dense  = ArrayList<T>()       // packed components
    @PublishedApi internal val owners = ArrayList<Entity>()  // entity per slot
    private val slotOf = HashMap<Entity, Int>()              // entity → slot

    fun add(e: Entity, c: T) { /* set-or-append */ }
    fun get(e: Entity): T?   = slotOf[e]?.let { dense[it] }
    fun remove(e: Entity)    { /* O(1) swap-remove */ }
    inline fun each(action: (Entity, T) -> Unit) {           // allocation-free
        for (i in dense.indices) action(owners[i], dense[i])
    }
}

interface System { val order: Int; fun update(world: World, dt: Float) }

class World {
    fun create(): Entity                                     // sequential ids
    fun destroy(e: Entity)
    inline fun <reified T : Any> add(e: Entity, c: T)
    inline fun <reified T : Any> get(e: Entity): T?
    inline fun <reified T : Any> store(): ComponentStore<T>
    fun addSystem(s: System)                                 // kept sorted by `order`
    fun update(dt: Float) { for (s in systems) s.update(this, dt) }
}
```

A system is just logic over a join (`ecs/Systems.kt`):

```kotlin
class MovementSystem : System {                 // generic — reused by bullets/enemies later
    override val order = 100
    override fun update(world: World, dt: Float) {
        world.store<Velocity>().each { e, v ->
            world.get<Position>(e)?.let { it.x += v.x; it.y += v.y }
        }
    }
}
```

> **`order` is the first cut of the contract's before/after scheduling** (§6). Today it's a raw
> integer; it graduates to named, dependency-based hints when the plugin host lands.

---

## 4. Components — the shared vocabulary

Components are **plain mutable data, no logic**. The kernel blesses a **standard library** of
`core.*` components (decision #4) so unrelated plugins interoperate (EMP's `StunSystem` can find
*any* enemy because every enemy carries `core.Health`/`core.Team`, no matter who authored it).

| `core.*` component | Purpose | State |
|---|---|:--:|
| `Position` | x, y | ✅ `ecs/Components.kt` |
| `Velocity` | per-tick delta | ✅ |
| `Particle` | size, color, maxLife, currentLife (visual) | ✅ |
| `Sprite` | atlas ref + transform | ⬜ |
| `Health` | hp / max | ⬜ |
| `Collider` | shape/size for overlap tests | ⬜ |
| `Stun` | EMP's marker — "frozen for N ticks" | ⬜ |
| `Lifetime` | auto-despawn timer (generalizes `Particle.maxLife`) | ⬜ |
| `Team` | player / enemy / neutral (collision filtering) | ⬜ |
| `Damage` | amount a projectile deals | ⬜ |

Implemented today:

```kotlin
data class Position(var x: Float, var y: Float)   // blessed core
data class Velocity(var x: Float, var y: Float)    // blessed core
data class Particle(val size: Float, val color: Int, val maxLife: Int, var currentLife: Int)
```

Plugins may also declare **their own** namespaced components (`emp.Charge`) — the standard set is
the *lingua franca*, not a cage.

---

## 5. From today's monolith → components (the migration map)

The current `GameEngine` (`logic/GameEngine.kt`) holds gameplay as **fat mutable objects** and
hand-written `update*()` methods. The migration (`0001` §2, PRs C…N) decomposes each object into
core components and each `update*()` into a registered system. This table is the Rosetta stone:

| Today (`GameEngine`) ⬜→ | Becomes entity with components | Driven by system(s) |
|---|---|---|
| `PlayerState(x,y,health,fuel,roll,invuln,…)` | `Position`,`Velocity`,`Health`,`Collider`,`Sprite` + `player.*` (fuel, roll, escorts) | `InputSystem`→`PlayerMoveSystem`, `RollSystem`, `ShootSystem` |
| `Enemy(type,x,y,vx,vy,health,path…)` | `Position`,`Velocity`,`Health`,`Collider`,`Team`,`Sprite` + `enemy.AI` | `EnemyAISystem`, `SpawnSystem` |
| `Bullet(x,y,vx,vy,owner,heavy)` | `Position`,`Velocity`,`Collider`,`Team`,`Damage`,`Lifetime` | `MovementSystem` ✅, `CollisionSystem` |
| `PowerUp(type,x,y,vy)` | `Position`,`Velocity`,`Collider`,`Sprite` + `powerup.Kind` | `PowerUpSystem`, `CollisionSystem` |
| particles | `Position`,`Velocity`,`Particle` ✅ | `MovementSystem` ✅, `ParticleSystem` ✅ |
| `islands`/`clouds` | `Position`,`Velocity`,`Sprite` | `BackgroundSystem` |
| `tick()` body (ordered calls) | — | the **system schedule** (replaces the hardcoded call list) |
| `checkCollisions()` | — | `CollisionSystem` (emits `collide`/`damage`/`death` events) |
| `destroyEnemy()` scoring | — | `ScoringSystem` (listens for `death`) |

The endpoint of `tick()` is illustrative: today it's a fixed call list…

```kotlin
// logic/GameEngine.kt  — today (⬜ to be replaced by the schedule)
fun tick() {
    updateBackgrounds(); updatePlayerMovement(); updateRollState()
    updatePlayerShooting(); updateEnemies(); updateBullets(); updatePowerups()
    world.update(1f)            // ✅ the ECS island (particles) already lives here
    checkCollisions(); spawnEnemiesWave()
}
```

…and the destination is `world.update(dt)` running an ordered set of *registered* systems
contributed by plugins — nothing hardcoded.

---

## 6. The contract (#5) — how plugins compose without chaos

Composition is the whole point, so the kernel makes ordering and conflicts **explicit**.

**Two orderings, two mechanisms:**

```
 LOAD ORDER  (which plugin registers first)        SYSTEM ORDER (run each tick)
 = topological sort of declared deps               = before/after hints (today: int `order`)

   core ──► base.enemies ──► base.collision           InputSystem
        └─► base.player  ──┘        │                     ▼  before: collision
                                    ▼                  MovementSystem (100) ✅
                              emp (depends on             ▼
                              base.enemies)            CollisionSystem  ── after: movement
                                                          ▼
                                                       ScoringSystem   ── after: collision
                                                          ▼
                                                       ParticleSystem (200) ✅
```

**Versioning:** a single integer **Host API level** (kernel capability) + per-plugin **semver**.
A plugin declares the API level it needs; the host refuses to load incompatible plugins.

**Namespacing:** everything is namespaced by plugin id; `core.*` is reserved. `emp.Stun` and
`weather.Stun` never collide.

**Extending existing behavior — `wrap` vs `replace`:**

```
 wrap  (composable, many allowed)            replace  (exclusive, one allowed)
 ───────────────────────────────             ───────────────────────────────
   base.ShootSystem                            base.ScoringSystem
        ▲ wrapped by                                ╳ replaced by
   rapidfire.wrap{ before(); base(); }         doublepoints.replace{ … }

   ✔ stacks: rapidfire + spread both wrap     ✘ two replaces of the same target
                                                 = HARD ERROR (explicit resolution
                                                   required — never silent last-wins)
```

> **`replace`-collision is a hard error**, by design. Two mods silently fighting over scoring is
> exactly the bug class this architecture refuses to ship. The user resolves it (disable one, or
> a `wrap` that orders them).

---

## 7. The plugin model & DSL (#1, #13)

⬜ **Designed, not built.** A plugin is a **bundle**: an entry `plugin.kt` (the DSL) + namespaced assets (atlas PNG +
`sprites.txt`, audio, shaders). The DSL is a **strict subset of real Kotlin** — `val`/`var`,
`fun`, lambdas, `if`/`when`/`for`/`while`, arithmetic/bool, `data class`, lists/maps. **Excluded:**
coroutines, reflection, arbitrary classes, non-trivial generics. Manifest lives *in* the DSL.

Sketch of EMP as a worked example (⬜ — illustrative DSL, not real code):

```kotlin
plugin("emp") {
    meta { name = "EMP Blast"; version = "0.1.0"; hostApi = 1; needs("base.enemies") }

    component("Stun") { var ticksLeft = 0 }          // emp.Stun

    // New behavior — added without editing a single enemy definition.
    system("StunDecay") {
        after("core.movement")
        each(has = ["emp.Stun"]) { e ->              // join: every entity with a Stun
            val s = get(e, "emp.Stun")
            if (--s.ticksLeft <= 0) remove(e, "emp.Stun")
        }
    }

    on("inputAction") { a ->                         // event-driven trigger
        if (a.name == "emp" && spend(energy = 1)) {
            query(has = ["core.Team"], where = { team(it) == ENEMY }).forEach { e ->
                add(e, "emp.Stun") { ticksLeft = 180 }
                emit("emp.ripple", at = position(e))  // a shader plugin can listen
            }
        }
    }
}
```

The **same source** is either interpreted by the VM (3rd-party) or compiled as real Kotlin
(first-party). Typing is dynamic at runtime, but every plugin is **static-checked at packaging
time by compiling it with the real Kotlin compiler** — so authors get real errors before ship.

### 7.1 The Host API (the only surface a plugin sees) ⬜

```kotlin
interface Host {                       // sketch — the sandbox boundary
    val dt: Float
    val rng: Random                    // SEEDED — determinism (no wall-clock, no entropy)
    fun spawn(): Entity
    fun add(e: Entity, comp: String, init: () -> Unit)
    fun get(e: Entity, comp: String): Component?
    fun query(has: List<String>, where: ((Entity) -> Boolean)? = null): Sequence<Entity>
    fun emit(event: String, payload: Any? = null)
    fun on(event: String, handler: (Any?) -> Unit)
    fun sound(id: String); fun shader(id: String)   // namespaced assets only
    // NO file / network / env / wall-clock / reflection / native.
}
```

---

## 8. Events (#4) ⬜ — the loose-coupling layer

Beyond systems iterating components, plugins coordinate through a **kernel event bus**. A
`death` event fired by `CollisionSystem` is heard by `ScoringSystem` (add score), an audio plugin
(play explosion), and a particle plugin (spawn burst) — none of which know about each other.

**Standard events:** `tick`, `spawn`, `despawn`, `collide`, `damage`, `death`, `pickup`,
`inputAction`, `levelStart`/`levelEnd`, `scoreChange`.

```
                       ┌───────────────► ScoringSystem   (+score on death)
 CollisionSystem ──emit("death", e)──┼──► audio plugin    (playExplosion)
                       └───────────────┼─► particle plugin (burst at position)
                                       └─► emp plugin      (chain reaction?)
        (publishers don't know subscribers — add a listener, not an edit)
```

Determinism note: event delivery is **ordered and synchronous within a tick** (seeded, fixed
step) so a replay reproduces the exact fan-out — see TASKS #19 (deterministic command-log).

---

## 9. Lifecycle & distribution (#6) ⬜

**Bundled-only, near-term.** A curated set (incl. curated 3rd-party) is **baked into the build** —
baking into the reviewed binary also sidesteps iOS App Store rule 2.5.2. Live sideloading and a
plugin "store" are **deferred** (#17).

```
 BUILD TIME                              RUNTIME (startup)                 PER TICK
 ──────────                              ────────────────                 ────────
 plugins/                               scan bundles                      world.update(dt):
   emp/        ─┐                       → read manifests                    for system in
     plugin.kt  │  Gradle bakes         → resolve deps (topo sort)            schedule:
     sprites.*  │  (like the            → deterministic LOAD order             system.update()
   weather.zip ─┘  committed            → register comps/systems/events     event bus drains
                   sprites.png)         → apply Settings FEATURE FLAGS        (ordered)
                                          (ALL plugins OFF by default)
```

- **Feature flags** are the universal enable/disable mechanism (Settings UI); everything is off by
  default until explicitly enabled.
- **Drop-in structure** is identical for a folder or a `.zip` modpack: entry `plugin.kt` + assets.
- Dev **hot-reload** is a tooling nicety; full live hot-swap is deferred.

---

## 10. Trust & sandbox (#7) ⬜

The sandbox guards the **device and app — not gameplay.** Plugins are *meant* to read/modify any
entity or component (that's composition + near-total control); gameplay trust comes from
**curation**, not the sandbox.

```
 ┌─ Sandbox by construction ────────────────────────────────────────────────┐
 │  • VM exposes ONLY the Host API (no file/network/env/wall-clock/           │
 │    reflection/native)                                                      │
 │  • per-tick INSTRUCTION BUDGET + memory/recursion caps                     │
 │  • crash isolation: catch → report → DISABLE the plugin, never crash the   │
 │    game  (VM error reporting = hard requirement, TASKS #15)               │
 │  • determinism: seeded RNG ✅ + fixed step ✅ (already true in GameEngine)  │
 └───────────────────────────────────────────────────────────────────────────┘
```

---

## 11. Status, risks, and where to go next

- **Implemented:** the ECS kernel and the particle slice (✅ in §3). Determinism floor (seeded
  RNG + fixed step) is already real. The hard-won input layer works (🟡, in the scene).
- **Next slices** (per `TASKS.md` / `0001`): **PR-B** plugin host + turn existing systems into
  *registered* systems → then migrate one subsystem per PR (background → bullets → enemies →
  player+input → collision → scoring → power-ups → HUD), **lifting the input layer intact**.
- **Risks** (carried from `0001` §5): GC/allocation on JS/Wasm (mitigated by dense stores +
  allocation-free `each`), and the inherent "two sources of truth" during incremental migration.
  Each step is **A/B'd against the perf gate** (no regression vs. web median 3.9 ms / p95 7.3 ms).
- **Open / shelved** (revisit when the slice arrives): maps/paths format (#9), shader format (#10,
  lean KorGE fragment DSL), audio format (#11), and the EMP **gameplay** forks (#12 — energy
  economy, trigger, freeze-vs-stop-fire). The interpreter stack (#16) is a separate, later track.

> **Maintenance:** when a slice lands, flip its ✅/🟡/⬜ markers here and link the PR. This doc is
> the shared mental model — keep it true.

---

<!-- TASKS:auto START -->
## Tasks (from TASKS.md)

<!-- Generated from TASKS.md by `./gradlew syncDocTasks` — edit tasks there, not here. -->

- [ ] **PR-B** — plugin host + turn existing systems into *registered* systems
- [ ] **EMP** — first dogfooded plugin (compiled first-party; no interpreter needed). Mock spec written; gameplay forks still open (see below)
- [ ] Scripting VM / interpreter for 3rd-party plugins (separate, later track)
- [x] #1 Plugin model = **B**: data + Kotlin-flavored sandboxed VM (3rd-party) + real-Kotlin compiled-in (first-party)
- [x] #2 **Everything is a plugin**; engine = thin kernel; base game = plugins, peer to mods
- [x] #3 **Incremental** migration; lift input layer intact; no backward-compat burden
- [x] #4 Kernel↔plugin boundary; ECS; standard-lib blessed components
- [x] #5 API contract: API-level + semver; in-DSL manifest; namespacing; `wrap`/`replace` (collision = hard error)
- [x] #6 Distribution = bundled-only near-term (drop folder/zip into `plugins/`); Settings feature-flags off by default
- [x] #7 Sandbox guards device/app, not gameplay; trust via curation
- [x] #13 DSL: strict Kotlin subset, dynamic runtime + static-check via real-Kotlin compile
- [~] #9 Maps/paths format — lean: DSL content + standard path primitives in `core.*`
- [~] #11 Sound/music format — lean: namespaced audio files, event-hooked
- [~] #12 EMP as first dogfooded plugin — also still-unanswered EMP **gameplay** forks: economy (repurpose SCREEN_BOMB vs new), stun behavior (freeze vs stop-fire), distortion scope
- [ ] #15 **ECS dev-tooling** — entity inspector, system trace/profiler, deterministic replay, per-system toggles; **VM error reporting = hard requirement**
- [ ] #16 Interpreter tech stack (reference) — antlr-kotlin (Wasm-proven parse) + hand-rolled tree-walker; luak/TENUM refs
- [~] #17 Plugin "store"/registry — deferred (much later); iOS App Store 2.5.2 means curated bundled-only there

<!-- TASKS:auto END -->
