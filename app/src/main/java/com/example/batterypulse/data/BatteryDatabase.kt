package com.example.batterypulse.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BatterySampleEntity::class, ChargingSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class BatteryDatabase : RoomDatabase() {
    abstract fun batteryDao(): BatteryDao
}
