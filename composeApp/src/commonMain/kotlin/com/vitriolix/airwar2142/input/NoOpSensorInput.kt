package com.vitriolix.airwar2142.input

import com.vitriolix.airwar2142.interfaces.SensorInput

class NoOpSensorInput : SensorInput {
    override fun getTiltValues(): Pair<Float, Float> = Pair(0f, 0f)
    override fun startListening() {}
    override fun stopListening() {}
}
