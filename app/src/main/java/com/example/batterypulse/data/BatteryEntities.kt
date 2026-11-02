package com.example.batterypulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_samples")
data class BatterySampleEntity(
    @PrimaryKey val timestampMillis: Long,
    val percent: Int,
    val chargingState: String,
    val temperatureCelsius: Float,
    val voltageMillivolts: Int,
    val currentAmps: Float,
    val powerWatts: Float
)

@Entity(tableName = "charging_sessions")
data class ChargingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val startPercent: Int,
    val endPercent: Int?,
    val maxTemperatureCelsius: Float,
    val energyPercentGained: Int
)
