package com.example.batterypulse.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.batterypulse.data.BatteryRepository
import com.example.batterypulse.data.PreferencesRepository

class DashboardViewModelFactory(
    private val repository: BatteryRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, preferencesRepository) as T
        }
        error("Unknown ViewModel: ${modelClass.name}")
    }
}
