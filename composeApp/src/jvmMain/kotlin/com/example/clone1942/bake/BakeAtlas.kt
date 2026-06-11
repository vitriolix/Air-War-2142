package com.example.clone1942.bake

import com.example.clone1942.logic.BackgroundCloud
import com.example.clone1942.logic.BackgroundIsland
import com.example.clone1942.logic.Enemy
import com.example.clone1942.logic.EnemyType
import com.example.clone1942.logic.PlayerState
import com.example.clone1942.render.AtlasSpec
import com.example.clone1942.render.drawCloudCentered
import com.example.clone1942.render.drawEnemyBulletShape
import com.example.clone1942.render.drawEnemyCentered
import com.example.clone1942.render.drawIslandCentered
import com.example.clone1942.render.drawPlayer
import com.example.clone1942.render.drawPlayerBulletShape
import com.example.clone1942.render.drawPowerupCentered
import korlibs.image.atlas.MutableAtlas
import korlibs.image.atlas.add
import korlibs.image.format.PNG
import korlibs.image.vector.ShapeBuilder
import korlibs.image.vector.buildShape
import korlibs.image.vector.renderWithHotspot
import korlibs.math.geom.Vector2I
import java.io.File

// Build-time sprite-atlas bake. Runs headlessly (korim `native = false` → pure-Kotlin
// rasterization, no AWT/GPU) from the `bakeAtlas` Gradle task. Rasterizes every game
// shape (the SAME ShapeBuilder functions the runtime uses, from render/Shapes.kt) at
// AtlasSpec.BAKE_SCALE, packs them into one PNG, and writes a sidecar metadata file
// mapping each slice name → its rect in the PNG + its hotspot (the origin pivot).
//
// Output: <resources>/sprites.png + <resources>/sprites.txt
//   sprites.txt line format:  name x y w h hotspotX hotspotY
//
// arg[0] (optional): output directory. Defaults to composeApp/src/commonMain/resources.

fun main(args: Array<String>) {
    val outDir = File(if (args.isNotEmpty()) args[0] else "composeApp/src/commonMain/resources")
    outDir.mkdirs()

    // GROW_IMAGE keeps everything on a single page (doubling if it overflows) so the
    // runtime only ever loads one texture.
    val atlas = MutableAtlas<Unit>(
        width = 1024, height = 1024, border = 2,
        growMethod = MutableAtlas.GrowMethod.GROW_IMAGE
    )
    val hotspots = LinkedHashMap<String, Vector2I>()

    fun bake(name: String, block: ShapeBuilder.() -> Unit) {
        val bwh = buildShape(builder = block)
            .renderWithHotspot(scale = AtlasSpec.BAKE_SCALE, native = false)
        hotspots[name] = bwh.hotspot
        atlas.add(bwh.bitmap.toBMP32(), name)
    }

    // ── Player (3 shape variants; movement/roll/blink are runtime transforms) ──
    bake(AtlasSpec.PLAYER_NORMAL)  { drawPlayer(PlayerState(escortsActive = false, rollProgress = 0f), 0L) }
    bake(AtlasSpec.PLAYER_ESCORTS) { drawPlayer(PlayerState(escortsActive = true,  rollProgress = 0f), 0L) }
    bake(AtlasSpec.PLAYER_ROLL)    { drawPlayer(PlayerState(escortsActive = false, rollProgress = 1f), 0L) }

    // ── Enemies (one slice per type) ──
    var idCounter = 0
    EnemyType.entries.forEach { type ->
        bake(AtlasSpec.enemy(type)) {
            drawEnemyCentered(
                Enemy(id = idCounter++, type = type, x = 0f, y = 0f, vx = 0f, vy = 0f,
                      health = type.baseHealth, spawnTick = 0L)
            )
        }
    }

    // ── Bullets ──
    bake(AtlasSpec.BULLET_PLAYER) { drawPlayerBulletShape() }
    bake(AtlasSpec.BULLET_ENEMY)  { drawEnemyBulletShape() }

    // ── Background / pickups (baked at reference size, scaled at runtime) ──
    bake(AtlasSpec.ISLAND) {
        drawIslandCentered(BackgroundIsland(0f, 0f, AtlasSpec.ISLAND_REF_RADIUS.toFloat(), 0, 0L))
    }
    bake(AtlasSpec.CLOUD) {
        val s = AtlasSpec.CLOUD_REF_SCALE.toFloat()
        drawCloudCentered(BackgroundCloud(0f, 0f, s, s, 0f))
    }
    bake(AtlasSpec.POWERUP) {
        drawPowerupCentered(AtlasSpec.POWERUP_REF_W, AtlasSpec.POWERUP_REF_H)
    }

    check(atlas.allBitmaps.size == 1) {
        "Atlas overflowed to ${atlas.allBitmaps.size} pages — runtime loader expects one."
    }

    // ── Write PNG ──
    val pngFile = File(outDir, AtlasSpec.PNG)
    pngFile.writeBytes(PNG.encode(atlas.bitmap))

    // ── Write metadata sidecar ──
    val txt = buildString {
        atlas.entries.forEach { e ->
            val s = e.slice
            val hs = hotspots[s.name] ?: error("missing hotspot for ${s.name}")
            appendLine("${s.name} ${s.left} ${s.top} ${s.width} ${s.height} ${hs.x} ${hs.y}")
        }
    }
    val txtFile = File(outDir, AtlasSpec.TXT)
    txtFile.writeText(txt)

    println("BAKE: ${atlas.entries.size} slices → ${pngFile.name} (${atlas.bitmap.width}x${atlas.bitmap.height}px) + ${txtFile.name}")
    println("  $pngFile")
    print(txt.prependIndent("  "))
}
