package com.example.batterypulse

import android.app.Application
import androidx.room.Room
import com.example.batterypulse.data.BatteryDatabase
import com.example.batterypulse.data.BatteryRepository
import com.example.batterypulse.data.PreferencesRepository

class BatteryPulseApplication : Application() {
    val database: BatteryDatabase by lazy {
        Room.databaseBuilder(this, BatteryDatabase::class.java, "battery_pulse.db").build()
    }
    val batteryRepository: BatteryRepository by lazy { BatteryRepository(this, database) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }

    override fun onTerminate() {
        batteryRepository.close()
        database.close()
        super.onTerminate()
    }
}
