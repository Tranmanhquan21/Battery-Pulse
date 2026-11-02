package com.example.batterypulse.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.batterypulse.BatteryPulseApplication

class BatteryStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED || intent?.action == Intent.ACTION_POWER_CONNECTED || intent?.action == Intent.ACTION_POWER_DISCONNECTED) {
            val repository = (context.applicationContext as BatteryPulseApplication).batteryRepository
            repository.refresh()
        }
    }
}
