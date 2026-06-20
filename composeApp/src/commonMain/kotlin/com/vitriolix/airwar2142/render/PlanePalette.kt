package com.vitriolix.airwar2142.render

import korlibs.image.color.Colors
import korlibs.image.color.RGBA

// ─────────────────────────────────────────────────────────────────────────────
// Player-plane colorway — ported from the Design handoff
// (design/incoming/design_handoff_player_plane).
//
// Every shade in the airframe is derived from ONE base hull hue by HSL lightness
// shifts, so a colorway is fully described by {hull, accent, canopy}. The shift
// amounts below are the handoff's spec (README "Colorways": +30 spec … −46 AO).
// `shade()` reproduces the reference's rgb→hsl→Δlightness→rgb exactly so the baked
// sprite matches the design's hex values.
// ─────────────────────────────────────────────────────────────────────────────

/** Lighten/darken [c] by [dl] points of HSL lightness (−100..100). Exact port of the handoff's `shade()`. */
internal fun shade(c: RGBA, dl: Double): RGBA {
    val r = c.r / 255.0; val g = c.g / 255.0; val b = c.b / 255.0
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    var h = 0.0; var s = 0.0; val l = (max + min) / 2.0
    if (max != min) {
        val d = max - min
        s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
        h = when (max) {
            r -> (g - b) / d + (if (g < b) 6.0 else 0.0)
            g -> (b - r) / d + 2.0
            else -> (r - g) / d + 4.0
        } / 6.0
    }
    val l2 = ((l * 100.0 + dl).coerceIn(0.0, 100.0)) / 100.0
    fun hue(p: Double, q: Double, t0: Double): Double {
        var t = t0
        if (t < 0) t += 1.0
        if (t > 1) t -= 1.0
        return when {
            t < 1.0 / 6 -> p + (q - p) * 6 * t
            t < 1.0 / 2 -> q
            t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
            else -> p
        }
    }
    val nr: Double; val ng: Double; val nb: Double
    if (s == 0.0) {
        nr = l2; ng = l2; nb = l2
    } else {
        val q = if (l2 < 0.5) l2 * (1 + s) else l2 + s - l2 * s
        val p = 2 * l2 - q
        nr = hue(p, q, h + 1.0 / 3); ng = hue(p, q, h); nb = hue(p, q, h - 1.0 / 3)
    }
    return RGBA((nr * 255).toInt(), (ng * 255).toInt(), (nb * 255).toInt(), 255)
}

/** A fully-derived plane colorway. Shade names match the handoff's `palette()` keys. */
internal class PlanePalette(
    val base: RGBA,
    accent: RGBA,
    val canopyInner: RGBA,
    val canopyHi: RGBA,
) {
    val light = shade(base, 8.0)
    val lite2 = shade(base, 16.0)
    val dark = shade(base, -9.0)
    val dark2 = shade(base, -17.0)
    val line = shade(base, -26.0)
    val edge = shade(base, -34.0)
    val spec = shade(base, 30.0)
    val ao = shade(base, -46.0)
    // Brighter cylinder cores used in the boom/fuselage cross-section gradients.
    val core40 = shade(base, 40.0)
    val core38 = shade(base, 38.0)

    val accent = accent
    val tip = TIP
    val metal = METAL

    companion object {
        // Selectable colorways (handoff "Colorways"). Defaults = Olive Drab / yellow / Smoke.
        val HULLS = listOf(
            Colors["#5A6038"], // Olive Drab (default)
            Colors["#47532F"], // Field Green
            Colors["#54606A"], // Gunmetal
            Colors["#8B9AA3"], // Bare Steel
            Colors["#B49A63"], // Desert Tan
            Colors["#3C5670"], // Sea Blue
            Colors["#3A3F3A"], // Charcoal
        )
        val ACCENTS = listOf(
            Colors["#E7B73C"], Colors["#C0392B"], Colors["#ECEFF1"],
            Colors["#D98A2B"], Colors["#2E7D46"], Colors["#1F6F8B"],
        )
        // Canopy glass as (inner, highlight) pairs.
        val CANOPIES = listOf(
            Colors["#2E3A2A"] to Colors["#9DB79A"], // Field Glass
            Colors["#00D2F0"] to Colors["#5DEAF6"], // Cyan
            Colors["#C79A2E"] to Colors["#FFE082"], // Gold
            Colors["#2A323A"] to Colors["#8090A0"], // Smoke (default)
            Colors["#9C5A33"] to Colors["#FFAB91"], // Amber
            Colors["#4A3A7A"] to Colors["#B388FF"], // Violet
        )

        val TIP = Colors["#E7B73C"]   // yellow spinner/stab tip
        val METAL = Colors["#222a30"] // near-black prop metal

        /** The default WWII olive-drab livery shown in screenshot 01. */
        val DEFAULT = PlanePalette(
            base = HULLS[0],
            accent = ACCENTS[0],
            canopyInner = CANOPIES[3].first,
            canopyHi = CANOPIES[3].second,
        )
    }
}
