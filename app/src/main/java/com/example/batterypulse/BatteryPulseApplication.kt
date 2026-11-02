package com.example.batterypulse

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.batterypulse.data.BatteryDatabase
import com.example.batterypulse.data.BatteryRepository
import com.example.batterypulse.data.PreferencesRepository

class BatteryPulseApplication : Application() {
    private val databaseMigration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN inputPowerWatts REAL")
            database.execSQL("ALTER TABLE battery_samples ADD COLUMN powerSource TEXT NOT NULL DEFAULT 'BATTERY_SIDE'")
        }
    }

    val database: BatteryDatabase by lazy {
        Room.databaseBuilder(this, BatteryDatabase::class.java, "battery_pulse.db")
            .addMigrations(databaseMigration)
            .build()
    }
    val batteryRepository: BatteryRepository by lazy { BatteryRepository(this, database) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }

    override fun onTerminate() {
        batteryRepository.close()
        database.close()
        super.onTerminate()
    }
}
