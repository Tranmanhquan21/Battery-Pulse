package com.example.batterypulse.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.batterypulse.BatteryPulseApplication
import com.example.batterypulse.R
import com.example.batterypulse.data.AppLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BatteryMonitorService : Service() {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val english = kotlinx.coroutines.runBlocking {
            (application as BatteryPulseApplication).preferencesRepository.preferences.first().language == AppLanguage.ENGLISH
        }
        createChannel(if (english) "Battery monitoring" else "Theo dõi pin")
        startForeground(NOTIFICATION_ID, notification(if (english) "Battery monitoring is active" else "Đang theo dõi pin"))
        monitorJob = scope.launch {
            while (isActive) {
                val repository = (application as BatteryPulseApplication).batteryRepository
                val status = repository.refresh()
                repository.checkAlerts(status)
                val interval = when {
                    status.chargingState != "Đang sạc" -> 30_000L
                    status.powerWatts >= 30f -> 10_000L
                    else -> 15_000L
                }
                delay(interval)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.coroutineContext.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel(name: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_LOW)
        )
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Battery Pulse")
            .setContentText(text)
            .setOngoing(true)
            .build()

    companion object {
        private const val CHANNEL_ID = "battery_monitor"
        private const val NOTIFICATION_ID = 1001
    }
}
