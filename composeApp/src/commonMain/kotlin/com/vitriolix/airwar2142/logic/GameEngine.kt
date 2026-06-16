package com.vitriolix.airwar2142.logic

import com.vitriolix.airwar2142.CANVAS_HEIGHT
import com.vitriolix.airwar2142.ecs.*
import com.vitriolix.airwar2142.interfaces.SoundPlayer
import com.vitriolix.airwar2142.interfaces.SensorInput
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class GameState {
    MENU, PLAYING, PAUSED, GAME_OVER, LEVEL_COMPLETE
}

enum class ControlMode {
    TILT, TOUCH, KEYBOARD
}

enum class PowerUpType {
    DOUBLE_SHOT, EXTRA_ROLL, FUEL_REFILL, ESCORTS, SCREEN_BOMB
}

enum class EnemyType(val width: Float, val height: Float, val baseHealth: Int, val scoreValue: Int) {
    SCOUT(60f, 60f, 1, 100),
    DIVER(70f, 70f, 1, 150),
    SQUADRON_RED(60f, 60f, 1, 200),
    HEAVY_FIGHTER(120f, 100f, 5, 500),
    BOSS(250f, 180f, 50, 2000)
}

data class PlayerState(
    var x: Float = 500f,
    var y: Float = 1200f,
    var width: Float = 90f,
    var height: Float = 90f,
    var score: Int = 0,
    var lives: Int = 3,
    var fuel: Float = 100f, // 0f to 100f
    var rollsLeft: Int = 3,
    var rollProgress: Float = 0f, // 0f (not rolling) to 1.0f (completed roll)
    var doubleShotActive: Boolean = false,
    var doubleShotTimeLeft: Int = 0, // Frame counts or ticks
    var escortsActive: Boolean = false,
    var escortsTimeLeft: Int = 0,
    var invulnTicks: Int = 0
)

data class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var health: Int,
    var spawnTick: Long,
    var pathProgress: Float = 0f,
    val initialX: Float = x
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val isPlayerOwned: Boolean,
    val isHeavy: Boolean = false
)

data class PowerUp(
    val type: PowerUpType,
    var x: Float,
    var y: Float,
    var vy: Float = 3f,
    var width: Float = 50f,
    var height: Float = 50f
)

data class BackgroundIsland(
    var x: Float,
    var y: Float,
    val radius: Float,
    val colorType: Int, // Used to draw slightly different islands
    val seed: Long
)

data class BackgroundCloud(
    var x: Float,
    var y: Float,
    val scaleX: Float,
    val scaleY: Float,
    val speed: Float
)

class GameEngine(
    private val soundPlayer: SoundPlayer,
    private val sensorInput: SensorInput
) {
    // Logical screen dimensions — height MUST match the platform canvas
    // (CANVAS_HEIGHT: 1500 on JVM/JS, 2200 on Android). Hardcoding 1500 made the
    // engine wrap/clamp at 1500 while Android renders 2200 tall, so islands/clouds
    // popped ~700px above the real bottom and the plane couldn't reach the lower screen.
    val playWidth = 1000f
    val playHeight = CANVAS_HEIGHT.toFloat()

    // Game state tracking
    private val _gameState = MutableStateFlow(GameState.MENU)
    val gameState: StateFlow<GameState> = _gameState

    private val _controlMode = MutableStateFlow(ControlMode.KEYBOARD)
    val controlMode: StateFlow<ControlMode> = _controlMode

    private val _player = MutableStateFlow(PlayerState())
    val player: StateFlow<PlayerState> = _player

    // Mutable game lists updated on each tick
    val enemies = mutableListOf<Enemy>()
    val bullets = mutableListOf<Bullet>()
    val powerUps = mutableListOf<PowerUp>()

    // ECS world — particles live here (first migrated subsystem; see docs/0001).
    val world = World().apply {
        addSystem(MovementSystem())
        addSystem(ParticleSystem())
    }
    val islands = mutableListOf<BackgroundIsland>()
    val clouds = mutableListOf<BackgroundCloud>()

    // Game Stats
    var level = 1
    var kills = 0
    var activeRedSquadronDestroyed = 0
    private var enemyIdCounter = 0
    private var tickCount = 0L
    private val rand = Random(42)

    // Current keyboard key state (set directly from polling each frame)
    private var keyLeft  = false
    private var keyRight = false
    private var keyUp    = false
    private var keyDown  = false

    // Persistent velocity for keyboard inertia movement
    private var velX = 0f
    private var velY = 0f
    var keyShoot = false
    var keyRollTriggered = false

    // Debug-readable state
    val debugKeyLeft:  Boolean get() = keyLeft
    val debugKeyRight: Boolean get() = keyRight
    val debugKeyUp:    Boolean get() = keyUp
    val debugKeyDown:  Boolean get() = keyDown
    val debugTickCount: Long get() = tickCount

    // Drag touch control offset target
    private var touchTargetX: Float? = null
    private var touchTargetY: Float? = null

    // Configuration
    var sfxEnabled = true
    var sensitivity = 1.0f
    var showDebugOverlay = false

    init {
        generateBackgrounds()
    }

    private fun generateBackgrounds() {
        islands.clear()
        clouds.clear()
        // Generate initial islands placed randomly on vertical axis
        for (i in 0..6) {
            islands.add(
                BackgroundIsland(
                    x = rand.nextFloat() * playWidth,
                    y = rand.nextFloat() * playHeight,
                    radius = 80f + rand.nextFloat() * 120f,
                    colorType = rand.nextInt(3),
                    seed = rand.nextLong()
                )
            )
        }
        // Generate initial clouds
        for (i in 0..8) {
            clouds.add(
                BackgroundCloud(
                    x = rand.nextFloat() * playWidth,
                    y = rand.nextFloat() * playHeight,
                    scaleX = 1f + rand.nextFloat() * 1.5f,
                    scaleY = 0.8f + rand.nextFloat() * 0.8f,
                    speed = 2f + rand.nextFloat() * 3f
                )
            )
        }
    }

    fun startGame() {
        _player.value = PlayerState(
            x = playWidth / 2f,
            y = playHeight * 0.8f,
            score = 0,
            lives = 3,
            fuel = 100f,
            rollsLeft = 3
        )
        enemies.clear()
        bullets.clear()
        powerUps.clear()
        world.clear()
        level = 1
        kills = 0
        tickCount = 0
        touchTargetX = null
        touchTargetY = null
        keyLeft = false; keyRight = false; keyUp = false; keyDown = false
        velX = 0f; velY = 0f
        keyShoot = false
        sensorInput.startListening()
        _gameState.value = GameState.PLAYING
    }

    fun setControlMode(mode: ControlMode) {
        _controlMode.value = mode
    }

    fun updateKeyboardInputs(left: Boolean, right: Boolean, up: Boolean, down: Boolean, shoot: Boolean) {
        keyLeft  = left
        keyRight = right
        keyUp    = up
        keyDown  = down
        keyShoot = shoot
    }

    fun updateTouchTarget(x: Float?, y: Float?) {
        touchTargetX = x
        touchTargetY = y
    }

    fun triggerRoll() {
        val p = _player.value.copy()
        if (p.rollsLeft > 0 && p.rollProgress == 0f && _gameState.value == GameState.PLAYING) {
            p.rollsLeft--
            p.rollProgress = 0.01f
            p.invulnTicks = 60 // ~1 second of invulnerability
            if (sfxEnabled) soundPlayer.playRoll()
            _player.value = p
        }
    }

    fun togglePause() {
        if (_gameState.value == GameState.PLAYING) {
            _gameState.value = GameState.PAUSED
            sensorInput.stopListening()
        } else if (_gameState.value == GameState.PAUSED) {
            _gameState.value = GameState.PLAYING
            sensorInput.startListening()
        }
    }

    fun returnToMenu() {
        _gameState.value = GameState.MENU
        sensorInput.stopListening()
    }

    // Main 60FPS tick logic
    fun tick() {
        if (_gameState.value != GameState.PLAYING) return
        tickCount++

        updateBackgrounds()
        updatePlayerMovement()
        updateRollState()
        updatePlayerShooting()
        updateEnemies()
        updateBullets()
        updatePowerups()
        world.update(1f)   // ECS: MovementSystem + ParticleSystem (was updateParticles())
        checkCollisions()
        spawnEnemiesWave()
    }

    private fun updateBackgrounds() {
        // Island scrolling (slow parallax)
        islands.forEach { island ->
            island.y += 1f
            if (island.y > playHeight + island.radius) {
                island.y = -island.radius
                island.x = rand.nextFloat() * playWidth
            }
        }
        // Cloud scrolling (faster parallax)
        clouds.forEach { cloud ->
            cloud.y += cloud.speed
            if (cloud.y > playHeight + 200f) {
                cloud.y = -200f
                cloud.x = rand.nextFloat() * playWidth
            }
        }
    }

    private fun updatePlayerMovement() {
        val p = _player.value.copy()
        if (p.rollProgress > 0f) {
            // Cannot manually move while executing a loop-the-loop roll
            return
        }

        var dx = 0f
        var dy = 0f

        when (_controlMode.value) {
            ControlMode.KEYBOARD -> {
                // Each held key adds acceleration; friction decays velocity every tick.
                // Equilibrium speed = accel / (1 - friction) = 3 / 0.2 = 15 → clamped to 12.
                val accel   = 3.0f
                val friction = 0.80f
                val maxSpeed = 12f

                if (keyLeft)  velX -= accel
                if (keyRight) velX += accel
                if (keyUp)    velY -= accel
                if (keyDown)  velY += accel

                velX *= friction
                velY *= friction

                // Clamp total speed — also normalises diagonals automatically
                val speed = kotlin.math.sqrt(velX * velX + velY * velY)
                if (speed > maxSpeed) {
                    val scale = maxSpeed / speed
                    velX *= scale
                    velY *= scale
                }

                dx = velX
                dy = velY
            }
            ControlMode.TILT -> {
                val moveSpeed = 12f
                val (tiltX, tiltY) = sensorInput.getTiltValues()
                dx = tiltX * moveSpeed * 1.5f * sensitivity
                dy = -tiltY * moveSpeed * 1.5f * sensitivity
            }
            ControlMode.TOUCH -> {
                val targetX = touchTargetX
                val targetY = touchTargetY
                if (targetX != null && targetY != null) {
                    val speed = 20f
                    val diffX = targetX - p.x
                    val diffY = targetY - p.y
                    val dist = kotlin.math.sqrt(diffX * diffX + diffY * diffY)
                    if (dist > 5f) {
                        dx = (diffX / dist) * kotlin.math.min(speed, dist)
                        dy = (diffY / dist) * kotlin.math.min(speed, dist)
                    }
                }
            }
        }

        p.x = (p.x + dx).coerceIn(p.width / 2f, playWidth - p.width / 2f)
        p.y = (p.y + dy).coerceIn(p.height / 2f, playHeight - p.height / 2f)

        // Fuel slowly depletes
        p.fuel = (p.fuel - 0.015f).coerceAtLeast(0f)
        if (p.fuel <= 0f) {
            // Out of fuel damage
            p.fuel = 0f
            if (tickCount % 30 == 0L) {
                damagePlayer(1)
            }
        }

        if (p.invulnTicks > 0) p.invulnTicks--

        _player.value = p
    }

    private fun updateRollState() {
        val p = _player.value.copy()
        if (p.rollProgress > 0f) {
            p.rollProgress += 0.02f // Completes roll in 50 frames (~0.8 seconds)
            if (p.rollProgress >= 1f) {
                p.rollProgress = 0f
            }
            _player.value = p
        }
    }

    private fun updatePlayerShooting() {
        val p = _player.value.copy()
        val shouldShoot = keyShoot || (_controlMode.value == ControlMode.TOUCH && touchTargetX != null) || _controlMode.value == ControlMode.TILT

        // Automatic or key shooting at constrained rate (every 12 frames)
        if (shouldShoot && tickCount % 10 == 0L && p.rollProgress == 0f) {
            spawnPlayerBullet(p)
        }

        if (p.doubleShotActive) {
            p.doubleShotTimeLeft--
            if (p.doubleShotTimeLeft <= 0) p.doubleShotActive = false
        }
        if (p.escortsActive) {
            p.escortsTimeLeft--
            if (p.escortsTimeLeft <= 0) p.escortsActive = false
        }
        _player.value = p
    }

    private fun spawnPlayerBullet(p: PlayerState) {
        if (p.doubleShotActive) {
            bullets.add(Bullet(p.x - 20f, p.y - p.height / 2f, 0f, -22f, isPlayerOwned = true))
            bullets.add(Bullet(p.x + 20f, p.y - p.height / 2f, 0f, -22f, isPlayerOwned = true))
        } else {
            bullets.add(Bullet(p.x, p.y - p.height / 2f, 0f, -22f, isPlayerOwned = true))
        }

        if (p.escortsActive) {
            // Left escort fires
            bullets.add(Bullet(p.x - 80f, p.y + 20f, 0f, -22f, isPlayerOwned = true))
            // Right escort fires
            bullets.add(Bullet(p.x + 80f, p.y + 20f, 0f, -22f, isPlayerOwned = true))
        }

        // Add visual propulsion spark at exhaust
        spawnParticle(p.x, p.y + p.height / 2f, 0f, 5f, 6f, 0xFFFFAA00.toInt(), 5)

        if (sfxEnabled) soundPlayer.playShoot()
    }

    private fun updateEnemies() {
        enemies.forEach { e ->
            e.pathProgress += 0.01f

            when (e.type) {
                EnemyType.SCOUT -> {
                    // Simple straight down
                    e.y += e.vy
                }
                EnemyType.DIVER -> {
                    // Moves straight down then dives towards player's horizontal position
                    e.y += e.vy
                    val p = _player.value
                    if (e.y > playHeight * 0.3f && e.y < playHeight * 0.7f) {
                        val dx = p.x - e.x
                        e.x += (dx * 0.03f).coerceIn(-6f, 6f)
                    }
                }
                EnemyType.SQUADRON_RED -> {
                    // Weaves in a sine wave path
                    e.y += e.vy
                    e.x = e.initialX + kotlin.math.sin(e.pathProgress * 6f) * 150f
                }
                EnemyType.HEAVY_FIGHTER -> {
                    // Moves slowly down, occasionally stopping or shooting
                    e.y += e.vy
                    if (tickCount % 80 == 0L && e.y < playHeight * 0.6f) {
                        spawnEnemyBullet(e.x, e.y + e.type.height / 2f, 0f, 10f)
                    }
                }
                EnemyType.BOSS -> {
                    // Hover near top, moves left and right
                    if (e.y < 250f) {
                        e.y += 2f
                    } else {
                        e.x += e.vx
                        if (e.x < 150f || e.x > playWidth - 150f) {
                            e.vx = -e.vx
                        }
                    }
                    // Heavy multi-bullet firing patterns
                    if (tickCount % 50 == 0L) {
                        spawnEnemyBullet(e.x - 60f, e.y + 40f, -3f, 12f)
                        spawnEnemyBullet(e.x, e.y + 50f, 0f, 12f)
                        spawnEnemyBullet(e.x + 60f, e.y + 40f, 3f, 12f)
                    }
                }
            }

            // Engine visual smoke particle for heavy enemy and boss
            if (e.type == EnemyType.HEAVY_FIGHTER && tickCount % 5 == 0L) {
                spawnParticle(e.x, e.y - 30f, 0f, -2f, 8f, 0xFF777777.toInt(), 20)
            } else if (e.type == EnemyType.BOSS && tickCount % 3 == 0L) {
                spawnParticle(e.x - 70f, e.y - 50f, 0f, -2f, 12f, 0xFF444444.toInt(), 30)
                spawnParticle(e.x + 70f, e.y - 50f, 0f, -2f, 12f, 0xFF444444.toInt(), 30)
            }
        }

        // Remove out-of-screen enemies
        enemies.removeAll { e ->
            val out = e.y > playHeight + 200f || e.y < -300f
            if (out && e.type == EnemyType.SQUADRON_RED) {
                // If a red squadron plane escapes, we reset active squadron tracker
                activeRedSquadronDestroyed = 0
            }
            out
        }
    }

    private fun spawnEnemyBullet(x: Float, y: Float, vx: Float, vy: Float) {
        bullets.add(Bullet(x, y, vx, vy, isPlayerOwned = false))
    }

    private fun updateBullets() {
        bullets.forEach { b ->
            b.x += b.vx
            b.y += b.vy
        }
        // Remove off-screen bullets
        bullets.removeAll { b -> b.y < -50f || b.y > playHeight + 50f || b.x < -50f || b.x > playWidth + 50f }
    }

    private fun updatePowerups() {
        powerUps.forEach { p ->
            p.y += p.vy
        }
        powerUps.removeAll { p -> p.y > playHeight + 50f }
    }

    // Spawn a particle as an ECS entity (Position + Velocity + Particle). Movement +
    // aging are handled by MovementSystem/ParticleSystem on world.update().
    fun spawnParticle(x: Float, y: Float, vx: Float, vy: Float, size: Float, color: Int, maxLife: Int) {
        val e = world.create()
        world.add(e, Position(x, y))
        world.add(e, Velocity(vx, vy))
        world.add(e, Particle(size, color, maxLife, 0))
    }

    private fun checkCollisions() {
        val p = _player.value
        val isPlayerInvuln = p.rollProgress > 0f || p.invulnTicks > 0

        // 1. Player Bullets hitting Enemies
        val bulletsToRemove = mutableListOf<Bullet>()
        val enemiesToHit = mutableMapOf<Enemy, Int>() // Enemy to damage count

        bullets.filter { it.isPlayerOwned }.forEach { bullet ->
            enemies.forEach { enemy ->
                if (checkOverlap(bullet.x, bullet.y, 10f, 10f, enemy.x, enemy.y, enemy.type.width, enemy.type.height)) {
                    bulletsToRemove.add(bullet)
                    enemiesToHit[enemy] = (enemiesToHit[enemy] ?: 0) + 1
                }
            }
        }

        bullets.removeAll(bulletsToRemove)

        enemiesToHit.forEach { (enemy, damage) ->
            enemy.health -= damage
            // Spawn spark particles on hit
            for (i in 1..4) {
                spawnParticle(
                    x = enemy.x,
                    y = enemy.y,
                    vx = (rand.nextFloat() - 0.5f) * 8f,
                    vy = (rand.nextFloat() - 0.5f) * 8f,
                    size = 4f + rand.nextFloat() * 4f,
                    color = 0xFFFFFF00.toInt(), // yellow sparks
                    maxLife = 10 + rand.nextInt(10)
                )
            }

            if (enemy.health <= 0) {
                destroyEnemy(enemy)
            }
        }

        // 2. Enemy Bullets hitting Player
        if (!isPlayerInvuln) {
            val enemyBulletsToRemove = mutableListOf<Bullet>()
            bullets.filter { !it.isPlayerOwned }.forEach { bullet ->
                if (checkOverlap(bullet.x, bullet.y, 12f, 12f, p.x, p.y, p.width, p.height)) {
                    enemyBulletsToRemove.add(bullet)
                    damagePlayer(1)
                }
            }
            bullets.removeAll(enemyBulletsToRemove)
        }

        // 3. Enemies colliding directly with Player
        if (!isPlayerInvuln) {
            val collidingEnemies = enemies.filter { enemy ->
                checkOverlap(enemy.x, enemy.y, enemy.type.width, enemy.type.height, p.x, p.y, p.width, p.height)
            }
            collidingEnemies.forEach { enemy ->
                if (enemy.type != EnemyType.BOSS && enemy.type != EnemyType.HEAVY_FIGHTER) {
                    destroyEnemy(enemy)
                }
                damagePlayer(2)
            }
        }

        // 4. Player collecting Power-ups
        val powerupsToRemove = mutableListOf<PowerUp>()
        powerUps.forEach { pu ->
            if (checkOverlap(pu.x, pu.y, pu.width, pu.height, p.x, p.y, p.width, p.height)) {
                powerupsToRemove.add(pu)
                collectPowerup(pu)
            }
        }
        powerUps.removeAll(powerupsToRemove)
    }

    private fun checkOverlap(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return kotlin.math.abs(x1 - x2) < (w1 + w2) / 2f &&
               kotlin.math.abs(y1 - y2) < (h1 + h2) / 2f
    }

    private fun destroyEnemy(enemy: Enemy) {
        enemies.remove(enemy)
        kills++
        val p = _player.value.copy()
        p.score += enemy.type.scoreValue
        _player.value = p

        // Squadron tracking logic
        if (enemy.type == EnemyType.SQUADRON_RED) {
            activeRedSquadronDestroyed++
            if (activeRedSquadronDestroyed == 5) {
                // Whole red squadron destroyed! Spawn powerup!
                spawnPowerUp(enemy.x, enemy.y)
                activeRedSquadronDestroyed = 0
            }
        }

        // Trigger heavy particle explosion
        val particleCount = when (enemy.type) {
            EnemyType.SCOUT -> 8
            EnemyType.DIVER -> 10
            EnemyType.SQUADRON_RED -> 12
            EnemyType.HEAVY_FIGHTER -> 20
            EnemyType.BOSS -> 50
        }

        val colors = listOf(0xFFFF3300.toInt(), 0xFFFF9900.toInt(), 0xFFFFFF00.toInt(), 0xFFFFFFFF.toInt())
        for (i in 1..particleCount) {
            spawnParticle(
                x = enemy.x,
                y = enemy.y,
                vx = (rand.nextFloat() - 0.5f) * 16f,
                vy = (rand.nextFloat() - 0.5f) * 16f,
                size = 6f + rand.nextFloat() * 12f,
                color = colors[rand.nextInt(colors.size)],
                maxLife = 20 + rand.nextInt(20)
            )
        }

        if (sfxEnabled) soundPlayer.playExplosion()

        if (enemy.type == EnemyType.BOSS) {
            // Beat the level!
            _gameState.value = GameState.LEVEL_COMPLETE
            sensorInput.stopListening()
        }
    }

    private fun damagePlayer(amount: Int) {
        val p = _player.value.copy()
        p.invulnTicks = 45 // iframe for ~0.7 seconds
        p.lives -= amount

        // visual screen shake particles / heavy sparks
        val colors = listOf(0xFFFF4444.toInt(), 0xFFFFAAAA.toInt(), 0xFFFFFFFF.toInt())
        for (i in 1..30) {
            spawnParticle(
                x = p.x,
                y = p.y,
                vx = (rand.nextFloat() - 0.5f) * 20f,
                vy = (rand.nextFloat() - 0.5f) * 20f,
                size = 8f + rand.nextFloat() * 10f,
                color = colors[rand.nextInt(colors.size)],
                maxLife = 30 + rand.nextInt(20)
            )
        }

        if (sfxEnabled) soundPlayer.playExplosion()

        if (p.lives <= 0) {
            p.lives = 0
            _gameState.value = GameState.GAME_OVER
            sensorInput.stopListening()
        }
        _player.value = p
    }

    private fun spawnPowerUp(x: Float, y: Float) {
        val types = PowerUpType.values()
        val selectedType = types[rand.nextInt(types.size)]
        powerUps.add(PowerUp(type = selectedType, x = x, y = y))
    }

    private fun collectPowerup(pu: PowerUp) {
        val p = _player.value.copy()
        p.score += 500 // bonus points!

        when (pu.type) {
            PowerUpType.DOUBLE_SHOT -> {
                p.doubleShotActive = true
                p.doubleShotTimeLeft = 600 // 10 seconds of double shot
            }
            PowerUpType.EXTRA_ROLL -> {
                p.rollsLeft = (p.rollsLeft + 1).coerceAtMost(5)
            }
            PowerUpType.FUEL_REFILL -> {
                p.fuel = (p.fuel + 40f).coerceAtMost(100f)
            }
            PowerUpType.ESCORTS -> {
                p.escortsActive = true
                p.escortsTimeLeft = 600 // 10 seconds of escorts
            }
            PowerUpType.SCREEN_BOMB -> {
                // Clear all normal screen enemies
                val enemiesToRemove = enemies.filter { it.type != EnemyType.BOSS }.toList()
                enemiesToRemove.forEach { e ->
                    destroyEnemy(e)
                }
            }
        }

        // collection sparkles
        for (i in 1..15) {
            spawnParticle(
                x = pu.x,
                y = pu.y,
                vx = (rand.nextFloat() - 0.5f) * 12f,
                vy = (rand.nextFloat() - 0.5f) * 12f,
                size = 5f + rand.nextFloat() * 5f,
                color = 0xFF00FFFF.toInt(), // Cyan sparkle
                maxLife = 15 + rand.nextInt(15)
            )
        }

        if (sfxEnabled) soundPlayer.playPowerup()
        _player.value = p
    }

    // Dynamic wave spawner based on ticks
    private fun spawnEnemiesWave() {
        // Standard levels logic
        // Wave timing is based on tickCount
        if (tickCount % 120 == 0L) {
            // Spawn a standard scout from the top
            spawnEnemy(EnemyType.SCOUT, rand.nextFloat() * (playWidth - 100f) + 50f, -50f, 0f, 5f)
        }

        if (tickCount % 350 == 0L) {
            // Spawn a diver diving towards player
            spawnEnemy(EnemyType.DIVER, rand.nextFloat() * (playWidth - 100f) + 50f, -50f, 0f, 7f)
        }

        if (tickCount % 600 == 0L) {
            // Spawn a Red Squadron of 5 planes weaving together
            val startX = 200f + rand.nextFloat() * 200f
            for (i in 0..4) {
                spawnEnemy(
                    EnemyType.SQUADRON_RED,
                    x = startX,
                    y = -50f - (i * 90f), // staggered spacing
                    vx = 0f,
                    vy = 6f,
                    spawnDelayTicks = i * 15
                )
            }
            activeRedSquadronDestroyed = 0
        }

        if (tickCount % 1000 == 0L) {
            // Spawn a heavy fighter
            spawnEnemy(EnemyType.HEAVY_FIGHTER, rand.nextFloat() * (playWidth - 200f) + 100f, -100f, 0f, 3f)
        }

        // Spawn Level Boss at tick 2500 (approx. 40 seconds in)
        if (tickCount == 2200L) {
            spawnEnemy(EnemyType.BOSS, playWidth / 2f, -250f, 4f, 0f)
        }
    }

    private fun spawnEnemy(type: EnemyType, x: Float, y: Float, vx: Float, vy: Float, spawnDelayTicks: Int = 0) {
        enemyIdCounter++
        enemies.add(
            Enemy(
                id = enemyIdCounter,
                type = type,
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                health = type.baseHealth,
                spawnTick = tickCount + spawnDelayTicks
            )
        )
    }

    fun proceedToNextLevel() {
        level++
        startGame()
    }
}
