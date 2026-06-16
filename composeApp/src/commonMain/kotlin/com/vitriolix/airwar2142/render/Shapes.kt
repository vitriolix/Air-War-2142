package com.vitriolix.airwar2142.render

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.paint.LinearGradientPaint
import korlibs.image.vector.ShapeBuilder
import korlibs.math.geom.Point
import korlibs.math.geom.Size2D
import com.vitriolix.airwar2142.logic.BackgroundCloud
import com.vitriolix.airwar2142.logic.BackgroundIsland
import com.vitriolix.airwar2142.logic.Enemy
import com.vitriolix.airwar2142.logic.EnemyType
import com.vitriolix.airwar2142.logic.GameEngine
import com.vitriolix.airwar2142.logic.PlayerState

// ─────────────────────────────────────────────────────────────────────────────
// Vector art — the single authoring source for every game object.
//
// These are pure korim `ShapeBuilder` extensions with NO korge-view dependency,
// so they can run BOTH at runtime (via `Graphics.updateShape`) and headlessly at
// build time (the `bakeAtlas` task rasterizes them into a sprite atlas). Keep this
// file free of any `korlibs.korge.*` import so the bake stays AWT/GPU-free.
//
// Every object is authored centered at origin (0,0) and facing up (−y = forward).
// `internal` (not private) so the jvmMain bake in the same module can call them.
// ─────────────────────────────────────────────────────────────────────────────

// ── ShapeBuilder helpers ──────────────────────────────────────────────────────

internal fun ShapeBuilder.circle(x: Double, y: Double, r: Double) = circle(Point(x, y), r)

internal fun ShapeBuilder.ellipse(cx: Double, cy: Double, rx: Double, ry: Double) {
    ellipse(Point(cx, cy), Size2D(rx * 2.0, ry * 2.0))
}

// ── Background ────────────────────────────────────────────────────────────────

internal fun ShapeBuilder.drawOcean(engine: GameEngine, canvasH: Double) {
    fill(LinearGradientPaint(0, 0, 0, canvasH)
        .addColorStop(0.0, Colors["#0F2027"])
        .addColorStop(0.5, Colors["#203A43"])
        .addColorStop(1.0, Colors["#2C5364"])
    ) { rect(0.0, 0.0, engine.playWidth.toDouble(), canvasH) }
}

internal fun ShapeBuilder.drawIslandCentered(isl: BackgroundIsland) {
    val ir = isl.radius.toDouble()
    fill(Colors["#D4C490"]) { circle(0.0, 0.0, ir) }
    fill(Colors["#4CAF50"]) { circle(0.0, 0.0, ir * 0.78) }
    fill(Colors["#2E7D32"]) { circle(0.0, 0.0, ir * 0.55) }
    fill(Colors["#1B5E20"]) {
        circle(-ir * 0.25, -ir * 0.15, ir * 0.28)
        circle( ir * 0.20,  ir * 0.20, ir * 0.22)
    }
    fill(RGBA(255, 255, 220, 30)) { circle(-ir * 0.15, -ir * 0.15, ir * 0.9) }
}

internal fun ShapeBuilder.drawCloudCentered(cloud: BackgroundCloud) {
    val cw = 90.0 * cloud.scaleX; val ch = 50.0 * cloud.scaleY
    fill(RGBA(255, 255, 255, 55)) {
        ellipse(0.0,        0.0,         cw * 0.5,  ch * 0.5)
        ellipse(-cw * 0.4, -ch * 0.1,   cw * 0.35, ch * 0.38)
        ellipse( cw * 0.3,  ch * 0.05,  cw * 0.32, ch * 0.32)
    }
}

// ── Power-ups ─────────────────────────────────────────────────────────────────

// Single power-up centered at origin (bake target). The batch version below draws
// each live power-up at its world position and is still used by the runtime until
// power-ups migrate to atlas sprites.
internal fun ShapeBuilder.drawPowerupCentered(w: Double, h: Double) {
    fill(RGBA(0, 20, 40, 200))  { roundRect(-w/2, -h/2, w, h, 10.0, 10.0) }
    fill(RGBA(0, 229, 255, 80)) { circle(0.0, 0.0, w * 0.32) }
    fill(Colors["#00FFFF"])     { circle(0.0, 0.0, w * 0.14) }
    fill(Colors["#00E5FF"]) {
        roundRect(-w/2,       -h/2,       w,   3.0, 1.0, 1.0)
        roundRect(-w/2,        h/2 - 3.0, w,   3.0, 1.0, 1.0)
        roundRect(-w/2,       -h/2,        3.0, h,   1.0, 1.0)
        roundRect( w/2 - 3.0, -h/2,        3.0, h,   1.0, 1.0)
    }
}

internal fun ShapeBuilder.drawPowerupsCentered(engine: GameEngine) {
    engine.powerUps.forEach { pu ->
        val x = pu.x.toDouble(); val y = pu.y.toDouble()
        val w = pu.width.toDouble(); val h = pu.height.toDouble()
        fill(RGBA(0, 20, 40, 200))  { roundRect(x - w/2, y - h/2, w, h, 10.0, 10.0) }
        fill(RGBA(0, 229, 255, 80)) { circle(x, y, w * 0.32) }
        fill(Colors["#00FFFF"])     { circle(x, y, w * 0.14) }
        fill(Colors["#00E5FF"]) {
            roundRect(x - w/2,       y - h/2,       w,   3.0, 1.0, 1.0)
            roundRect(x - w/2,       y + h/2 - 3.0, w,   3.0, 1.0, 1.0)
            roundRect(x - w/2,       y - h/2,        3.0, h,   1.0, 1.0)
            roundRect(x + w/2 - 3.0, y - h/2,        3.0, h,   1.0, 1.0)
        }
    }
}

// ── Enemies ───────────────────────────────────────────────────────────────────

internal fun ShapeBuilder.drawEnemyCentered(e: Enemy) {
    val hw = e.type.width / 2.0; val hh = e.type.height / 2.0
    when (e.type) {

        EnemyType.SCOUT -> {
            fill(Colors["#2E7D32"]) { roundRect(-6.0, -hh * 0.9, 12.0, hh * 1.8, 6.0, 6.0) }
            fill(Colors["#388E3C"]) { ellipse(0.0, hh * 0.1, hw * 0.95, 9.0) }
            fill(Colors["#1B5E20"]) { circle(0.0, -hh * 0.65, 9.0) }
            fill(RGBA(160, 230, 160, 65)) { circle(0.0, -hh * 0.9, 11.0) }
            fill(RGBA(60, 180, 100, 210)) { ellipse(0.0, -hh * 0.2, 4.5, 8.0) }
            fill(Colors["#1B5E20"]) { roundRect(-3.0, hh * 0.6, 6.0, 9.0, 2.0, 2.0) }
        }

        EnemyType.DIVER -> {
            fill(Colors["#1565C0"]) { roundRect(-9.0, -hh * 0.88, 18.0, hh * 1.76, 8.0, 8.0) }
            fill(Colors["#1976D2"]) { ellipse(0.0, hh * 0.08, hw, 12.0) }
            fill(Colors["#0D47A1"]) { circle(0.0, -hh * 0.6, 12.0) }
            fill(RGBA(130, 185, 255, 65)) { circle(0.0, -hh * 0.88, 13.0) }
            fill(Colors["#0D47A1"]) {
                roundRect(-24.0, hh * 0.1, 9.0, 14.0, 4.0, 4.0)
                roundRect( 15.0, hh * 0.1, 9.0, 14.0, 4.0, 4.0)
            }
            fill(RGBA(70, 150, 255, 195)) { ellipse(0.0, -hh * 0.1, 6.0, 11.0) }
            fill(Colors["#0D47A1"]) { roundRect(-4.0, hh * 0.6, 8.0, 10.0, 2.0, 2.0) }
        }

        EnemyType.SQUADRON_RED -> {
            fill(Colors["#B71C1C"]) { roundRect(-5.5, -hh * 0.9, 11.0, hh * 1.8, 5.0, 5.0) }
            fill(Colors["#C62828"]) { ellipse(0.0, hh * 0.05, hw * 0.9, 8.0) }
            fill(Colors["#7F0000"]) { circle(0.0, -hh * 0.62, 8.0) }
            fill(RGBA(255, 150, 150, 65)) { circle(0.0, -hh * 0.88, 10.0) }
            fill(Colors["#7F0000"]) {
                roundRect(-hw * 0.72, -3.0, 8.0, 7.0, 2.0, 2.0)
                roundRect( hw * 0.58, -3.0, 8.0, 7.0, 2.0, 2.0)
            }
            fill(RGBA(255, 70, 70, 205)) { ellipse(0.0, -hh * 0.18, 4.0, 7.5) }
            fill(Colors["#7F0000"]) { roundRect(-3.0, hh * 0.62, 6.0, 8.0, 2.0, 2.0) }
        }

        EnemyType.HEAVY_FIGHTER -> {
            fill(Colors["#37474F"]) { roundRect(-11.0, -hh * 0.88, 22.0, hh * 1.76, 8.0, 8.0) }
            fill(Colors["#455A64"]) { roundRect(-hw, -10.0, hw * 2, 20.0, 6.0, 6.0) }
            fill(Colors["#263238"]) {
                roundRect(-50.0, -hh * 0.55, 16.0, hh * 0.9, 6.0, 6.0)
                roundRect( 34.0, -hh * 0.55, 16.0, hh * 0.9, 6.0, 6.0)
            }
            fill(RGBA(130, 165, 180, 60)) {
                circle(-42.0, -hh * 0.7, 13.0)
                circle( 42.0, -hh * 0.7, 13.0)
            }
            fill(RGBA(80, 95, 105, 120)) {
                roundRect(-48.0, hh * 0.25, 12.0, 10.0, 3.0, 3.0)
                roundRect( 36.0, hh * 0.25, 12.0, 10.0, 3.0, 3.0)
            }
            fill(RGBA(60, 130, 170, 195)) { ellipse(0.0, -hh * 0.38, 7.0, 13.0) }
            fill(Colors["#37474F"]) { roundRect(-8.0, hh * 0.68, 16.0, 9.0, 3.0, 3.0) }
            // HP bar drawn as SolidRect siblings in the enemy container
        }

        EnemyType.BOSS -> {
            fill(Colors["#1A1A1A"]) { roundRect(-40.0, -hh * 0.9, 80.0, hh * 1.8, 10.0, 10.0) }
            fill(Colors["#212121"]) { roundRect(-hw, -12.0, hw * 2, 24.0, 6.0, 6.0) }
            fill(Colors["#111111"]) {
                roundRect(-100.0, -hh * 0.5, 18.0, hh * 0.85, 6.0, 6.0)
                roundRect( -52.0, -hh * 0.5, 16.0, hh * 0.8,  5.0, 5.0)
                roundRect(  36.0, -hh * 0.5, 16.0, hh * 0.8,  5.0, 5.0)
                roundRect(  82.0, -hh * 0.5, 18.0, hh * 0.85, 6.0, 6.0)
            }
            fill(RGBA(80, 100, 110, 50)) {
                circle(-91.0, -hh * 0.72, 15.0)
                circle(-44.0, -hh * 0.72, 14.0)
                circle( 44.0, -hh * 0.72, 14.0)
                circle( 91.0, -hh * 0.72, 15.0)
            }
            fill(Colors["#2A2E32"]) {
                circle(-22.0, -30.0, 9.0)
                circle( 22.0, -30.0, 9.0)
                circle(  0.0,  40.0,  9.0)
            }
            fill(Colors["#111111"]) {
                roundRect(-24.0, -48.0, 4.0, 20.0, 2.0, 2.0)
                roundRect( 20.0, -48.0, 4.0, 20.0, 2.0, 2.0)
                roundRect(  -2.0,  42.0, 4.0, 22.0, 2.0, 2.0)
            }
            fill(RGBA(25, 110, 160, 185)) { roundRect(-20.0, -hh * 0.28, 40.0, 28.0, 6.0, 6.0) }
            fill(RGBA(255, 20, 20, 215))  { ellipse(0.0, hh * 0.62, 15.0, 5.5) }
            fill(RGBA(255, 140, 0, 180))  { ellipse(0.0, hh * 0.62, 7.0, 2.5) }
            // HP bar drawn as SolidRect siblings in the enemy container
        }
    }
}

// ── Bullets ───────────────────────────────────────────────────────────────────

internal fun ShapeBuilder.drawPlayerBulletShape() {
    fill(RGBA(0, 255, 255, 180))   { roundRect(-4.0, -14.0, 8.0, 28.0, 4.0, 4.0) }
    fill(RGBA(180, 255, 255, 220)) { roundRect(-2.0, -12.0, 4.0, 24.0, 2.0, 2.0) }
    fill(Colors.WHITE)             { roundRect(-1.0,  -8.0, 2.0, 16.0, 1.0, 1.0) }
}

internal fun ShapeBuilder.drawEnemyBulletShape() {
    fill(RGBA(200, 0, 0, 220))     { circle(0.0, 0.0, 8.0) }
    fill(RGBA(255, 80, 80, 255))   { circle(0.0, 0.0, 5.0) }
    fill(RGBA(255, 200, 200, 255)) { circle(0.0, 0.0, 2.5) }
}

// ── Player — P-38 Lightning, centered at (0,0), facing up (−y = forward) ───────

internal fun ShapeBuilder.drawPlayer(p: PlayerState, tick: Long) {
    val hw = p.width / 2.0; val hh = p.height / 2.0
    val bx = 20.0; val bw = 8.0

    if (p.escortsActive && p.rollProgress == 0f) {
        fill(Colors["#546E7A"]) {
            roundRect(-hw - 46.0, -6.0, 26.0, 12.0, 3.0, 3.0)
            roundRect( hw + 20.0, -6.0, 26.0, 12.0, 3.0, 3.0)
        }
        fill(Colors["#607D8B"]) {
            roundRect(-hw - 54.0, -2.0, 42.0, 4.0, 2.0, 2.0)
            roundRect( hw + 12.0, -2.0, 42.0, 4.0, 2.0, 2.0)
        }
    }

    if (p.rollProgress == 0f) {
        val fl = 7.0 + (tick % 6) * 1.5
        fill(RGBA(255, 70, 0, 200))  { circle(-bx, hh + fl * 0.35, fl) }
        fill(RGBA(255, 70, 0, 200))  { circle( bx, hh + fl * 0.35, fl) }
        fill(RGBA(255, 190, 0, 180)) { circle(-bx, hh + fl * 0.15, fl * 0.5) }
        fill(RGBA(255, 190, 0, 180)) { circle( bx, hh + fl * 0.15, fl * 0.5) }
    }

    fill(Colors["#546E7A"]) {
        roundRect(-bx - bw/2, -hh, bw, hh * 2, 4.0, 4.0)
        roundRect( bx - bw/2, -hh, bw, hh * 2, 4.0, 4.0)
    }
    fill(Colors["#607D8B"]) {
        roundRect(-bx - 14.0, hh - 9.0, 28.0, 6.0, 3.0, 3.0)
        roundRect( bx - 14.0, hh - 9.0, 28.0, 6.0, 3.0, 3.0)
    }
    fill(Colors["#90A4AE"]) { roundRect(-hw, -7.0, hw * 2, 14.0, 5.0, 5.0) }
    fill(Colors["#B71C1C"]) {
        roundRect(-hw,       -6.0, 10.0, 12.0, 3.0, 3.0)
        roundRect(hw - 10.0, -6.0, 10.0, 12.0, 3.0, 3.0)
    }
    fill(Colors["#B0BEC5"]) { roundRect(-7.0, -hh * 0.73, 14.0, hh * 0.82, 5.0, 5.0) }
    fill(Colors["#90A4AE"]) { roundRect(-5.0, -hh * 0.42, 10.0, 2.0, 1.0, 1.0) }
    fill(RGBA(0, 210, 240, 220))  { ellipse(0.0, -hh * 0.55, 5.0, 9.0) }
    fill(RGBA(220, 250, 255, 90)) { ellipse(-1.0, -hh * 0.58, 2.0, 4.0) }
    fill(RGBA(170, 215, 235, 75)) {
        circle(-bx, -hh + 7.0, 12.0)
        circle( bx, -hh + 7.0, 12.0)
    }
    fill(Colors["#37474F"]) {
        circle(-bx, -hh + 7.0, 2.5)
        circle( bx, -hh + 7.0, 2.5)
    }
}
