package com.vitriolix.airwar2142.render

import korlibs.image.bitmap.BmpSlice
import korlibs.image.bitmap.sliceWithSize
import korlibs.image.format.readBitmap
import korlibs.io.file.std.resourcesVfs
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.image
import korlibs.math.geom.Anchor

// Runtime loader for the build-time sprite atlas (sprites.png + sprites.txt, produced
// by the jvmMain BakeAtlas task). Loads the single packed bitmap once and exposes named
// slices plus a hotspot-derived anchor for each, so views can be positioned by the same
// origin the vector art used. See [AtlasSpec] for the shared bake/runtime contract.
class SpriteAtlas private constructor(
    private val slices: Map<String, BmpSlice>,
    private val anchors: Map<String, Anchor>,
) {
    fun slice(name: String): BmpSlice = slices[name] ?: error("atlas: no slice '$name'")
    fun anchor(name: String): Anchor = anchors[name] ?: error("atlas: no anchor '$name'")

    // Create an Image for [name], anchored at its baked hotspot and scaled back down to
    // logical pixels (1 / BAKE_SCALE). Callers may apply further transforms on top.
    fun image(parent: Container, name: String): Image =
        parent.image(slice(name), anchor(name)) {
            scaleX = INV_SCALE
            scaleY = INV_SCALE
        }

    companion object {
        const val INV_SCALE = 1.0 / AtlasSpec.BAKE_SCALE

        suspend fun load(): SpriteAtlas {
            val bmp = resourcesVfs[AtlasSpec.PNG].readBitmap().toBMP32()
            val slices = LinkedHashMap<String, BmpSlice>()
            val anchors = LinkedHashMap<String, Anchor>()
            val meta = resourcesVfs[AtlasSpec.TXT].readBytes().decodeToString()
            meta.lineSequence().forEach { raw ->
                val line = raw.trim()
                if (line.isEmpty()) return@forEach
                val p = line.split(" ")
                val name = p[0]
                val x = p[1].toInt(); val y = p[2].toInt()
                val w = p[3].toInt(); val h = p[4].toInt()
                val hx = p[5].toInt(); val hy = p[6].toInt()
                slices[name] = bmp.sliceWithSize(x, y, w, h, name)
                anchors[name] = Anchor(hx.toDouble() / w, hy.toDouble() / h)
            }
            return SpriteAtlas(slices, anchors)
        }
    }
}
