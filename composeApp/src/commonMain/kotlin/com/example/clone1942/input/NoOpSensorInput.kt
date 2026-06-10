package com.example.clone1942.input

import com.example.clone1942.interfaces.SensorInput

class NoOpSensorInput : SensorInput {
    override fun getTiltValues(): Pair<Float, Float> = Pair(0f, 0f)
    override fun startListening() {}
    override fun stopListening() {}
}
