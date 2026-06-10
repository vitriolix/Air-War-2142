package com.example.clone1942.interfaces

interface SoundPlayer {
    fun playShoot()
    fun playExplosion()
    fun playRoll()
    fun playPowerup()
    fun toggleMute(muted: Boolean)
}

interface SensorInput {
    fun getTiltValues(): Pair<Float, Float> // Returns (xSteer, ySteer) where values are normalized roughly -1f to 1f
    fun startListening()
    fun stopListening()
}
