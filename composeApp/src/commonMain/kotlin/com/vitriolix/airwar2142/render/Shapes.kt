package com.vitriolix.airwar2142.render

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.image.paint.LinearGradientPaint
import korlibs.image.paint.RadialGradientPaint
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

// ── Player — twin-boom P-38 interceptor ───────────────────────────────────────
//
// Re-authored from the Design handoff (design/incoming/design_handoff_player_plane).
// The art is authored in the handoff's own coordinate system (centreline cx=140,
// nose up, fixed Y bands) and mapped to the engine's centred-at-origin, facing-up
// convention by [drawAirframe]. Colorway is fully derived from a [PlanePalette].
//
// Scope note: this is the clean "New" airframe with always-on weathering. Battle-
// damage states, the 3/4 bank pose and every animated FX (exhaust backfire, fire/
// smoke, the death/crash sequence, prop spin) are deliberately deferred — props are
// drawn as a static motion-blurred disc; no engine flame is baked in. `tick` is
// unused for now (kept so the bake/runtime signature is stable).

// Handoff coordinate constants.
private const val DESIGN_CX = 140.0        // centreline
private const val DESIGN_CY = 188.0        // vertical pivot → engine origin (≈ airframe centroid)
private const val DESIGN_FULL_SPAN = 372.0 // full wingspan (2 * span 186) → maps to PlayerState.width

@Suppress("UNUSED_PARAMETER")
internal fun ShapeBuilder.drawPlayer(p: PlayerState, tick: Long) {
    val pal = PlanePalette.DEFAULT
    // px-per-design-unit so the full wingspan renders at PlayerState.width.
    val u = p.width / DESIGN_FULL_SPAN

    // Escort wingmen (0.5 scale) flank the leader, drawn behind it.
    if (p.escortsActive && p.rollProgress == 0f) {
        val ex = p.width * 0.82
        val ey = p.width * 0.10
        drawAirframe(pal, -ex, ey, u * 0.5)
        drawAirframe(pal,  ex, ey, u * 0.5)
    }

    drawAirframe(pal, 0.0, 0.0, u)
}

// Draws one whole airframe centred at (ox,oy) with [u] px per handoff design-unit.
private fun ShapeBuilder.drawAirframe(pal: PlanePalette, ox: Double, oy: Double, u: Double) {
    val sb = this
    fun X(x: Double) = (x - DESIGN_CX) * u + ox
    fun Y(y: Double) = (y - DESIGN_CY) * u + oy
    // Filled polygon from flat design-coord pairs (x0,y0,x1,y1,…).
    fun poly(vararg c: Double) {
        sb.moveTo(X(c[0]), Y(c[1]))
        var i = 2
        while (i < c.size) { sb.lineTo(X(c[i]), Y(c[i + 1])); i += 2 }
        sb.close()
    }
    fun dcircle(x: Double, y: Double, r: Double) = sb.circle(X(x), Y(y), r * u)
    fun dellipse(x: Double, y: Double, rx: Double, ry: Double) = sb.ellipse(X(x), Y(y), rx * u, ry * u)
    fun drrect(x: Double, y: Double, w: Double, h: Double, r: Double) =
        sb.roundRect(X(x), Y(y), w * u, h * u, r * u, r * u)

    val gap = 64.0; val span = 186.0
    val lbx = DESIGN_CX - gap; val rbx = DESIGN_CX + gap
    val wingLE = 138.0; val wingTE = 184.0
    val spinTipY = 70.0; val noseTipY = 62.4

    // Horizontal cylinder gradient across one boom (edge→spec core→ao), per handoff gBoom.
    fun boomGrad(xc: Double) = LinearGradientPaint(X(xc - 14), Y(150.0), X(xc + 14), Y(150.0))
        .addColorStop(0.0, pal.edge).addColorStop(0.14, pal.dark).addColorStop(0.30, pal.light)
        .addColorStop(0.42, pal.spec).addColorStop(0.50, pal.core40).addColorStop(0.58, pal.spec)
        .addColorStop(0.72, pal.base).addColorStop(0.88, pal.dark2).addColorStop(1.0, pal.ao)
    // Fuselage cross-section gradient across the gondola pod (gFuse).
    val fuseGrad = LinearGradientPaint(X(DESIGN_CX - 17), Y(132.0), X(DESIGN_CX + 17), Y(132.0))
        .addColorStop(0.0, pal.ao).addColorStop(0.16, pal.dark).addColorStop(0.32, pal.light)
        .addColorStop(0.44, pal.core38).addColorStop(0.56, pal.spec).addColorStop(0.72, pal.base)
        .addColorStop(0.88, pal.dark2).addColorStop(1.0, pal.edge)
    // Vertical wing gradient (lite top → dark trailing), gWing.
    val wingGrad = LinearGradientPaint(X(DESIGN_CX), Y(wingLE), X(DESIGN_CX), Y(wingTE))
        .addColorStop(0.0, pal.lite2).addColorStop(0.3, pal.base).addColorStop(1.0, pal.dark)
    val tailGrad = LinearGradientPaint(X(DESIGN_CX), Y(300.0), X(DESIGN_CX), Y(316.0))
        .addColorStop(0.0, pal.lite2).addColorStop(0.3, pal.base).addColorStop(1.0, pal.dark)

    // ── Ambient-occlusion contact shadows (blur approximated by low-alpha ellipses) ──
    fill(RGBA(0, 0, 0, 52)) { dellipse(DESIGN_CX + 4, 176.0, 20.0, 30.0) }
    fill(RGBA(0, 0, 0, 46)) {
        dellipse(lbx + 3, 178.0, 13.0, 22.0)
        dellipse(rbx + 3, 178.0, 13.0, 22.0)
    }

    // ── Always-on weathering: exhaust soot streaks washing back over the booms ──
    fill(RGBA(0x16, 0x18, 0x0f, 66)) {
        for (x in listOf(lbx, rbx)) for (s in listOf(1.0, -1.0))
            poly(x + s * 11, 250.0, x + s * 17, 250.0, x + s * 15, 304.0, x + s * 9, 304.0)
    }
    fill(RGBA(0x10, 0x0f, 0x0a, 50)) {
        poly(DESIGN_CX - 4, 148.0, DESIGN_CX - 1, 148.0, DESIGN_CX - 2, 184.0, DESIGN_CX - 5, 184.0)
        poly(DESIGN_CX + 2, 154.0, DESIGN_CX + 5, 154.0, DESIGN_CX + 4, 184.0, DESIGN_CX + 1, 184.0)
    }

    // ── Wings (tapered: root chord 46 → narrow tip), with LE specular + TE shadow ──
    for (dir in listOf(-1.0, 1.0)) {
        val tipLE = wingLE + 14; val tipTE = wingTE - 20
        fill(wingGrad) {
            poly(DESIGN_CX, wingLE, DESIGN_CX + dir * (gap + 6), wingLE - 1, DESIGN_CX + dir * span, tipLE,
                 DESIGN_CX + dir * span, tipTE, DESIGN_CX + dir * (gap + 6), wingTE, DESIGN_CX, wingTE)
        }
        fill(RGBA(pal.spec.r, pal.spec.g, pal.spec.b, 140)) {
            poly(DESIGN_CX, wingLE, DESIGN_CX + dir * (gap + 6), wingLE - 1, DESIGN_CX + dir * span, tipLE,
                 DESIGN_CX + dir * span, tipLE + 3, DESIGN_CX + dir * (gap + 6), wingLE + 2, DESIGN_CX, wingLE + 3)
        }
        fill(RGBA(pal.ao.r, pal.ao.g, pal.ao.b, 102)) {
            poly(DESIGN_CX, wingTE, DESIGN_CX + dir * (gap + 6), wingTE, DESIGN_CX + dir * span, tipTE,
                 DESIGN_CX + dir * span, tipTE - 2.5, DESIGN_CX + dir * (gap + 6), wingTE - 2.5, DESIGN_CX, wingTE - 2.5)
        }
        // Yellow wingtip accent cap.
        fill(pal.accent) {
            poly(DESIGN_CX + dir * span, tipLE, DESIGN_CX + dir * (span - 15), tipLE + 2,
                 DESIGN_CX + dir * (span - 15), tipTE - 2, DESIGN_CX + dir * span, tipTE)
        }
    }

    // ── Tailplane (horizontal stabiliser joining the booms) ──
    fill(tailGrad) {
        poly(lbx - 6, 300.0, rbx + 6, 300.0, rbx + 8, 308.0, rbx + 4, 316.0, lbx - 4, 316.0, lbx - 8, 308.0)
    }

    // ── Engine booms (slender nacelles) + cowl ring + exhaust scoops ──
    for (dir in listOf(-1.0, 1.0)) {
        val x = DESIGN_CX + dir * gap
        fill(boomGrad(x)) {
            poly(x, 86.0, x - 13, 100.0, x - 14, 150.0, x - 12, 200.0, x - 10, 252.0, x - 9, 290.0,
                 x, 300.0, x + 9, 290.0, x + 10, 252.0, x + 12, 200.0, x + 14, 150.0, x + 13, 100.0)
        }
        fill(pal.dark) { poly(x - 12, 100.0, x + 12, 100.0, x + 11, 116.0, x - 11, 116.0) } // cowl ring
        for (s in listOf(1.0, -1.0)) {
            fill(pal.dark2) { poly(x + s * 11, 226.0, x + s * 20, 235.0, x + s * 20, 258.0, x + s * 11, 266.0) }
            fill(RGBA(0x0b, 0x0e, 0x0a, 224)) {
                poly(x + s * 12, 238.0, x + s * 18, 240.0, x + s * 18, 253.0, x + s * 12, 255.0)
            }
        }
    }

    // ── Fins (boom tail fairings) + vertical stabilisers with yellow tip bands ──
    for (dir in listOf(-1.0, 1.0)) {
        val x = DESIGN_CX + dir * gap
        fill(boomGrad(x)) {
            poly(x - 11, 286.0, x - 12, 308.0, x - 6, 320.0, x + 6, 320.0, x + 12, 308.0, x + 11, 286.0)
        }
        fill(fuseGrad) { // vertical-stab blade
            poly(x - 4, 282.0, x + 4, 282.0, x + 5, 304.0, x + 4, 322.0, x, 328.0, x - 4, 322.0, x - 5, 304.0)
        }
        fill(RGBA(pal.accent.r, pal.accent.g, pal.accent.b, 230)) {
            poly(x - 4, 282.0, x + 4, 282.0, x + 4, 292.0, x - 4, 292.0)
        }
    }

    // ── Spinners (dark cone + yellow tip) ──
    for (dir in listOf(-1.0, 1.0)) {
        val x = DESIGN_CX + dir * gap
        fill(pal.dark2) { poly(x - 10, 92.0, x, spinTipY, x + 10, 92.0) }
        fill(pal.tip)   { poly(x - 5, 82.0, x, spinTipY, x + 5, 82.0) }
    }

    // ── Propellers — static motion-blurred disc (spin is deferred FX) ──
    for (dir in listOf(-1.0, 1.0)) {
        val x = DESIGN_CX + dir * gap; val pcy = 88.0
        fill(RGBA(0xcf, 0xe3, 0xea, 33)) { dellipse(x, pcy, 40.0, 8.0) }
        fill(RGBA(0xea, 0xf3, 0xf7, 46)) { dellipse(x, pcy, 40.0, 3.0) }
        fill(RGBA(pal.metal.r, pal.metal.g, pal.metal.b, 150)) { dellipse(x, pcy, 38.0, 6.5) }
        fill(pal.dark2) { dellipse(x, pcy, 8.0, 5.0) }
        fill(pal.edge)  { dellipse(x, pcy, 3.0, 2.0) }
    }

    // ── Gondola pod + canopy bubble + nose guns ──
    val canG = RadialGradientPaint(X(139.0), Y(143.0), 0.0, X(DESIGN_CX), Y(152.0), 22.0 * u)
        .addColorStop(0.0, pal.canopyHi)
        .addColorStop(0.55, shade(pal.canopyInner, 6.0))
        .addColorStop(1.0, shade(pal.canopyInner, -10.0))
    val n = noseTipY
    fill(fuseGrad) {
        poly(DESIGN_CX, n, DESIGN_CX + 4, n + 4, DESIGN_CX + 10, n + 17, DESIGN_CX + 16, 96.0,
             DESIGN_CX + 17, 132.0, DESIGN_CX + 14, 160.0, DESIGN_CX + 8, 176.0, DESIGN_CX, 184.0,
             DESIGN_CX - 8, 176.0, DESIGN_CX - 14, 160.0, DESIGN_CX - 17, 132.0, DESIGN_CX - 16, 96.0,
             DESIGN_CX - 10, n + 17, DESIGN_CX - 4, n + 4)
    }
    fill(RGBA(pal.dark.r, pal.dark.g, pal.dark.b, 107)) { // darker rounded nose cap
        poly(DESIGN_CX - 10, n + 17, DESIGN_CX - 4, n + 4, DESIGN_CX, n, DESIGN_CX + 4, n + 4,
             DESIGN_CX + 10, n + 17, DESIGN_CX + 9, n + 27, DESIGN_CX - 9, n + 27)
    }
    fill(RGBA(pal.spec.r, pal.spec.g, pal.spec.b, 128)) { // centreline sheen
        poly(DESIGN_CX - 3, n + 8, DESIGN_CX, n + 2, DESIGN_CX + 3, n + 8,
             DESIGN_CX + 3, 120.0, DESIGN_CX, 128.0, DESIGN_CX - 3, 120.0)
    }
    fill(pal.edge) { // twin nose-gun barrels
        drrect(DESIGN_CX - 4, n - 8, 2.6, 13.0, 1.3)
        drrect(DESIGN_CX + 1.4, n - 8, 2.6, 13.0, 1.3)
    }
    // Canopy bubble with two white reflection slivers.
    fill(canG) {
        poly(DESIGN_CX - 9, 136.0, DESIGN_CX, 132.0, DESIGN_CX + 9, 136.0,
             DESIGN_CX + 10, 164.0, DESIGN_CX, 172.0, DESIGN_CX - 10, 164.0)
    }
    fill(RGBA(255, 255, 255, 102)) {
        poly(DESIGN_CX - 6, 139.0, DESIGN_CX - 2, 137.0, DESIGN_CX - 3, 150.0, DESIGN_CX - 7, 151.0)
    }
    fill(RGBA(255, 255, 255, 41)) {
        poly(DESIGN_CX + 3, 156.0, DESIGN_CX + 6, 156.0, DESIGN_CX + 5, 167.0, DESIGN_CX + 2, 166.0)
    }
}
