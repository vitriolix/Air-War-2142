package com.vitriolix.airwar2142.audio

import com.vitriolix.airwar2142.interfaces.SoundPlayer

class NoOpSoundPlayer : SoundPlayer {
    override fun playShoot() {}
    override fun playExplosion() {}
    override fun playRoll() {}
    override fun playPowerup() {}
    override fun toggleMute(muted: Boolean) {}
}
