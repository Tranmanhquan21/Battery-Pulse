package com.example.batterypulse.domain

data class BatteryStatus(
    val percent: Int,
    val chargingState: String,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val currentAmps: Float,
    val powerWatts: Float
)

data class BatterySample(
    val timestampMillis: Long,
    val status: BatteryStatus
)
