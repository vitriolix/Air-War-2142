package com.example.clone1942.bake

import korlibs.image.color.Colors
import korlibs.image.format.PNG
import korlibs.image.vector.buildShape
import korlibs.image.vector.renderWithHotspot
import korlibs.math.geom.Point
import java.io.File

// Build-time sprite-atlas bake. Runs headlessly (korim `native = false` → pure-Kotlin
// rasterization, no AWT/GPU) from the `bakeAtlas` Gradle task.
//
// SPIKE STAGE: proves the headless vector→bitmap→PNG pipeline works end-to-end in a
// Gradle JavaExec context, at the chosen 3× resolution. Will grow into the real atlas
// (packing every game shape into one PNG + metadata under resources).
private const val BAKE_SCALE = 3.0

fun main() {
    // A representative island-style shape (gradient-free circles) to exercise the pipeline.
    val ir = 100.0
    val shape = buildShape {
        fill(Colors["#E2D4A8"]) { circle(Point(0.0, 0.0), ir) }
        fill(Colors["#2E8B57"]) { circle(Point(0.0, 0.0), ir * 0.85) }
        fill(Colors["#1E5B37"]) {
            circle(Point(-ir * 0.2, -ir * 0.1), ir * 0.4)
            circle(Point(ir * 0.3, ir * 0.2), ir * 0.3)
        }
    }

    val rendered = shape.renderWithHotspot(scale = BAKE_SCALE, native = false)
    val out = File("/tmp/bake_spike.png")
    out.writeBytes(PNG.encode(rendered.bitmap))
    println("BAKE-SPIKE: wrote ${out.absolutePath}  ${rendered.bitmap.width}x${rendered.bitmap.height}px  hotspot=${rendered.hotspot}  (scale=$BAKE_SCALE)")
}
