package com.example.clone1942.audio

import com.example.clone1942.interfaces.SoundPlayer

class NoOpSoundPlayer : SoundPlayer {
    override fun playShoot() {}
    override fun playExplosion() {}
    override fun playRoll() {}
    override fun playPowerup() {}
    override fun toggleMute(muted: Boolean) {}
}
