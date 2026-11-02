package com.example.batterypulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.batterypulse.data.AppLanguage
import com.example.batterypulse.data.AppPreferences
import com.example.batterypulse.data.AppThemeChoice
import com.example.batterypulse.data.BatteryRepository
import com.example.batterypulse.data.ChargingSessionEntity
import com.example.batterypulse.data.PreferencesRepository
import com.example.batterypulse.domain.BatteryStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: BatteryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val status: StateFlow<BatteryStatus> = repository.status.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repository.status.value
    )
    val samples: StateFlow<List<com.example.batterypulse.domain.BatterySample>> = repository.samples.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repository.samples.value
    )
    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        AppPreferences()
    )
    val sessions: StateFlow<List<ChargingSessionEntity>> = repository.sessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )


    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun startMonitoring() = repository.startMonitoring()
    fun setLanguage(language: AppLanguage) = viewModelScope.launch { preferencesRepository.setLanguage(language) }
    fun setTheme(theme: AppThemeChoice) = viewModelScope.launch { preferencesRepository.setTheme(theme) }
    fun setBatteryAlertEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setBatteryAlertEnabled(enabled) }
    fun setTemperatureAlertEnabled(enabled: Boolean) = viewModelScope.launch { preferencesRepository.setTemperatureAlertEnabled(enabled) }
    fun setBatteryAlertPercent(percent: Int) = viewModelScope.launch { preferencesRepository.setBatteryAlertPercent(percent) }
    fun setTemperatureAlertCelsius(celsius: Float) = viewModelScope.launch { preferencesRepository.setTemperatureAlertCelsius(celsius) }
    fun setDesignCapacityMah(capacity: Int) = viewModelScope.launch { preferencesRepository.setDesignCapacityMah(capacity) }
}
