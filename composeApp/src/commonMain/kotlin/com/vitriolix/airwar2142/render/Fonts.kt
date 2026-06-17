package com.vitriolix.airwar2142.render

import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.io.file.std.resourcesVfs

// Shipped UI fonts, imported from the Claude Design round-trip (design/design-tokens.json →
// typography.fonts): Wallpoet for titles/heads, Chakra Petch for content. Both SIL OFL 1.1 —
// see resources/fonts/*-OFL.txt. Design self-hosts them as web woff2; KorGE loads the TTF.
//
// Loaded once from resources/fonts/ and cached (like SpriteAtlas, scenes call load() in
// sceneMain). Scenes pass Fonts.title / Fonts.content to text(); the debug overlay keeps the
// default bitmap font. Content is unified to SemiBold here — Design uses several Chakra weights
// (per-element weight fidelity is a refinement follow-up).
object Fonts {
    lateinit var title: Font      // Wallpoet — hero title, overlay heads, settings title
        private set
    lateinit var content: Font    // Chakra Petch (SemiBold) — HUD, buttons, body, settings rows, hints
        private set

    private var loaded = false

    suspend fun load() {
        if (loaded) return
        title = resourcesVfs["fonts/Wallpoet-Regular.ttf"].readTtfFont()
        content = resourcesVfs["fonts/ChakraPetch-SemiBold.ttf"].readTtfFont()
        loaded = true
    }
}
