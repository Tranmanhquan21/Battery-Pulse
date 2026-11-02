package com.example.batterypulse.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage { ENGLISH, VIETNAMESE }
enum class AppThemeChoice { DARK, LIGHT }

data class AppPreferences(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val theme: AppThemeChoice = AppThemeChoice.DARK,
    val batteryAlertEnabled: Boolean = true,
    val temperatureAlertEnabled: Boolean = true,
    val batteryAlertPercent: Int = 80,
    val temperatureAlertCelsius: Float = 43f,
    val designCapacityMah: Int = 0
)

private val Context.preferencesDataStore by preferencesDataStore(name = "app_preferences")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
        val batteryAlertEnabled = booleanPreferencesKey("battery_alert_enabled")
        val temperatureAlertEnabled = booleanPreferencesKey("temperature_alert_enabled")
        val batteryAlertPercent = androidx.datastore.preferences.core.intPreferencesKey("battery_alert_percent")
        val temperatureAlertCelsius = androidx.datastore.preferences.core.floatPreferencesKey("temperature_alert_celsius")
        val designCapacityMah = intPreferencesKey("design_capacity_mah")
    }

    val preferences: Flow<AppPreferences> = context.preferencesDataStore.data.map { values ->
        AppPreferences(
            language = runCatching { AppLanguage.valueOf(values[Keys.language] ?: "ENGLISH") }
                .getOrDefault(AppLanguage.ENGLISH),
            theme = runCatching { AppThemeChoice.valueOf(values[Keys.theme] ?: "DARK") }
                .getOrDefault(AppThemeChoice.DARK),
            batteryAlertEnabled = values[Keys.batteryAlertEnabled] ?: true,
            temperatureAlertEnabled = values[Keys.temperatureAlertEnabled] ?: true,
            batteryAlertPercent = (values[Keys.batteryAlertPercent] ?: 80).coerceIn(50, 100),
            temperatureAlertCelsius = (values[Keys.temperatureAlertCelsius] ?: 43f).coerceIn(30f, 60f),
            designCapacityMah = (values[Keys.designCapacityMah] ?: 0).coerceIn(0, 20_000)
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.preferencesDataStore.edit { it[Keys.language] = language.name }
    }

    suspend fun setTheme(theme: AppThemeChoice) {
        context.preferencesDataStore.edit { it[Keys.theme] = theme.name }
    }

    suspend fun setBatteryAlertEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[Keys.batteryAlertEnabled] = enabled }
    }

    suspend fun setBatteryAlertPercent(percent: Int) {
        context.preferencesDataStore.edit { it[Keys.batteryAlertPercent] = percent.coerceIn(50, 100) }
    }

    suspend fun setTemperatureAlertCelsius(celsius: Float) {
        context.preferencesDataStore.edit { it[Keys.temperatureAlertCelsius] = celsius.coerceIn(30f, 60f) }
    }

    suspend fun setTemperatureAlertEnabled(enabled: Boolean) {
        context.preferencesDataStore.edit { it[Keys.temperatureAlertEnabled] = enabled }
    }

    suspend fun setDesignCapacityMah(capacity: Int) {
        context.preferencesDataStore.edit { it[Keys.designCapacityMah] = capacity.coerceIn(0, 20_000) }
    }
}
