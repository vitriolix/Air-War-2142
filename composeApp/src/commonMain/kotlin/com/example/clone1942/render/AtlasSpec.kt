package com.example.clone1942.render

import com.example.clone1942.logic.EnemyType

// Shared contract between the build-time bake (jvmMain BakeAtlas) and the runtime
// atlas loader (GameScene). Slice names, reference sizes and the bake scale MUST
// match on both ends, so they live here in commonMain.
object AtlasSpec {
    // Vector shapes are rasterized at this multiple for high-DPI headroom; the
    // runtime draws the resulting sprites at 1/BAKE_SCALE to get back to logical px.
    const val BAKE_SCALE = 3.0

    // Output files under src/commonMain/resources.
    const val PNG = "sprites.png"
    const val TXT = "sprites.txt"

    // Objects with continuous size are baked once at a reference size and scaled at
    // runtime (sprite scale = actual / reference). Shapes scale linearly with these.
    const val ISLAND_REF_RADIUS = 100.0
    const val CLOUD_REF_SCALE = 1.0
    const val POWERUP_REF_W = 50.0
    const val POWERUP_REF_H = 50.0

    // Slice names.
    const val PLAYER_NORMAL = "player_normal"
    const val PLAYER_ESCORTS = "player_escorts"
    const val PLAYER_ROLL = "player_roll"
    const val BULLET_PLAYER = "bullet_player"
    const val BULLET_ENEMY = "bullet_enemy"
    const val ISLAND = "island"
    const val CLOUD = "cloud"
    const val POWERUP = "powerup"

    fun enemy(type: EnemyType): String = "enemy_${type.name.lowercase()}"
}
