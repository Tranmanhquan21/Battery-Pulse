package com.example.batterypulse.domain

enum class PowerSource {
    SYSFS_INPUT,
    BATTERY_SIDE,
    UNAVAILABLE
}

data class BatteryStatus(
    val percent: Int,
    val chargingState: String,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val currentAmps: Float,
    val powerWatts: Float,
    val inputPowerWatts: Float?,
    val powerSource: PowerSource
)

data class BatterySample(
    val timestampMillis: Long,
    val status: BatteryStatus
)
