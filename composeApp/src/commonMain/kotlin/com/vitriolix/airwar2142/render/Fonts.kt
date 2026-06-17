package com.vitriolix.airwar2142.render

import korlibs.image.font.Font
import korlibs.image.font.readTtfFont
import korlibs.io.file.std.resourcesVfs

// Shipped UI fonts, imported from the Claude Design round-trip (design/design-tokens.json →
// typography.fonts): Wallpoet for titles/heads, Chakra Petch for content. Both SIL OFL 1.1 —
// see resources/fonts/*-OFL.txt. These are Design's own TTFs (filenames mirror the project's
// assets/fonts/), so the bytes the game ships match the bytes Design ships.
//
// Loaded once from resources/fonts/ and cached (like SpriteAtlas, scenes call load() in
// sceneMain). Scenes pass Fonts.title / Fonts.content to text(); the debug overlay keeps the
// default bitmap font. Content is unified to weight 600 here — Design ships several Chakra
// weights (400/500/600/700); per-element weight fidelity is a refinement follow-up.
object Fonts {
    lateinit var title: Font      // Wallpoet — hero title, overlay heads, settings title
        private set
    lateinit var content: Font    // Chakra Petch 600 — HUD, buttons, body, settings rows, hints
        private set

    private var loaded = false

    suspend fun load() {
        if (loaded) return
        title = resourcesVfs["fonts/Wallpoet-400.ttf"].readTtfFont()
        content = resourcesVfs["fonts/ChakraPetch-600.ttf"].readTtfFont()
        loaded = true
    }
}
