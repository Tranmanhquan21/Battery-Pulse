package com.example.batterypulse.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.batterypulse.R
import com.example.batterypulse.domain.BatteryStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

internal class BatteryAlertNotifier(private val context: Context) {
    private val preferencesRepository = PreferencesRepository(context)
    private var batteryAlertSent = false
    private var temperatureAlertSent = false
    private var wasCharging = false

    fun evaluate(status: BatteryStatus) {
        val manager = context.getSystemService(NotificationManager::class.java)
        runBlocking {
            val preferences = preferencesRepository.preferences.first()
            val charging = status.chargingState == "Đang sạc"
            if (charging != wasCharging) {
                batteryAlertSent = false
                temperatureAlertSent = false
                wasCharging = charging
            }
            val english = preferences.language == AppLanguage.ENGLISH
            manager.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, if (english) "Battery alerts" else "Cảnh báo pin", NotificationManager.IMPORTANCE_HIGH)
                    .apply { enableVibration(true) }
            )
            if (charging && status.percent >= preferences.batteryAlertPercent) {
                if (preferences.batteryAlertEnabled && !batteryAlertSent) {
                    notify(
                        ALERT_BATTERY_ID,
                        if (english) "Battery reached ${status.percent}%" else "Pin đã đạt ${status.percent}%",
                        if (english) "Battery reached ${preferences.batteryAlertPercent}% while charging." else "Pin đã đạt ngưỡng ${preferences.batteryAlertPercent}% khi đang sạc."
                    )
                    batteryAlertSent = true
                }
            }
            if (status.temperatureCelsius >= preferences.temperatureAlertCelsius) {
                if (preferences.temperatureAlertEnabled && !temperatureAlertSent) {
                    notify(
                        ALERT_TEMPERATURE_ID,
                        if (english) "Battery temperature is high" else "Pin đang quá nhiệt",
                        if (english) "Battery temperature is ${"%.1f".format(status.temperatureCelsius)}°C; alert threshold is ${preferences.temperatureAlertCelsius}°C." else "Nhiệt độ pin ${"%.1f".format(status.temperatureCelsius)}°C, ngưỡng cảnh báo là ${preferences.temperatureAlertCelsius}°C."
                    )
                    temperatureAlertSent = true
                }
            }
        }
    }

    private fun notify(id: Int, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    companion object {
        const val ALERT_CHANNEL_ID = "battery_alerts"
        private const val ALERT_BATTERY_ID = 2001
        private const val ALERT_TEMPERATURE_ID = 2002
    }
}
