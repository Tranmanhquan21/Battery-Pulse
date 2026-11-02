package com.example.batterypulse.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.batterypulse.domain.BatterySample
import com.example.batterypulse.domain.BatteryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BatteryRepository(private val context: Context, private val database: BatteryDatabase) {
    private var activeSession: ChargingSessionEntity? = null
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val alertNotifier = BatteryAlertNotifier(context)
    private val initialStatus = readStatus()
    private val _status = MutableStateFlow(initialStatus)
    val status: StateFlow<BatteryStatus> = _status.asStateFlow()

    private val _samples = MutableStateFlow(listOf(BatterySample(System.currentTimeMillis(), initialStatus)))
    val samples: StateFlow<List<BatterySample>> = _samples.asStateFlow()
    val sessions: Flow<List<ChargingSessionEntity>> = database.batteryDao().observeAllSessions()

    init {
        ioScope.launch {
            val stored = database.batteryDao().recentSamples(MAX_SAMPLES).asReversed().map { it.toDomain() }
            if (stored.isNotEmpty()) _samples.value = stored
        }
    }

    fun refresh(): BatteryStatus {
        val timestamp = System.currentTimeMillis()
        val value = readStatus()
        _status.value = value
        val sample = BatterySample(timestamp, value)
        _samples.value = (_samples.value + sample).takeLast(MAX_SAMPLES)
        ioScope.launch {
            database.batteryDao().insertSample(sample.toEntity())
            updateChargingSession(timestamp, value)
            checkAlerts(value)
        }
        return value
    }

    fun checkAlerts(status: BatteryStatus) = alertNotifier.evaluate(status)

    fun startMonitoring() {
        ContextCompat.startForegroundService(context, Intent(context, BatteryMonitorService::class.java))
    }

    fun close() {
        ioScope.cancel()
    }

    private suspend fun updateChargingSession(timestamp: Long, status: BatteryStatus) {
        if (activeSession == null) activeSession = database.batteryDao().activeSession()
        val charging = status.chargingState == "Đang sạc"
        val current = activeSession
        if (charging && current == null) {
            val started = ChargingSessionEntity(
                startedAtMillis = timestamp,
                endedAtMillis = null,
                startPercent = status.percent,
                endPercent = null,
                maxTemperatureCelsius = status.temperatureCelsius,
                energyPercentGained = 0
            )
            database.batteryDao().insertSession(started)
            activeSession = database.batteryDao().activeSession()
        } else if (charging && current != null) {
            val updated = current.copy(
                maxTemperatureCelsius = maxOf(current.maxTemperatureCelsius, status.temperatureCelsius),
                energyPercentGained = (status.percent - current.startPercent).coerceAtLeast(0)
            )
            database.batteryDao().updateSession(updated)
            activeSession = updated
        } else if (!charging && current != null) {
            database.batteryDao().updateSession(current.copy(endedAtMillis = timestamp, endPercent = status.percent))
            activeSession = null
        }
    }

    private fun BatterySampleEntity.toDomain() = BatterySample(
        timestampMillis = timestampMillis,
        status = BatteryStatus(percent, chargingState, temperatureCelsius, voltageMillivolts, currentAmps, powerWatts)
    )

    private fun BatterySample.toEntity() = BatterySampleEntity(
        timestampMillis = timestampMillis,
        percent = status.percent,
        chargingState = status.chargingState,
        temperatureCelsius = status.temperatureCelsius,
        voltageMillivolts = status.voltageMillivolts,
        currentAmps = status.currentAmps,
        powerWatts = status.powerWatts
    )

    private fun readStatus(): BatteryStatus {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percent = if (scale > 0) level * 100 / scale else 0
        val temperature = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val statusCode = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            ?: BatteryManager.BATTERY_STATUS_UNKNOWN
        val status = when (statusCode) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Đang sạc"
            BatteryManager.BATTERY_STATUS_FULL -> "Đã đầy"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Đang xả"
            else -> "Chờ"
        }
        val currentMicroAmps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            context.getSystemService(BatteryManager::class.java)
                ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
        } else 0
        val currentAmps = currentMicroAmps / 1_000_000f
        val powerWatts = voltage / 1000f * kotlin.math.abs(currentAmps)
        return BatteryStatus(percent, status, temperature, voltage, currentAmps, powerWatts)
    }

    companion object {
        private const val MAX_SAMPLES = 2_000
    }
}
