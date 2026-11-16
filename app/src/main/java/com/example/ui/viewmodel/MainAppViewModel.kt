package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SettingsDataStore
import com.example.data.repository.HistoryRepository
import com.example.domain.model.AppSettings
import com.example.domain.model.CalculatorMode
import com.example.domain.model.DecimalSeparator
import com.example.domain.model.FractionDisplayPreference
import com.example.domain.model.GroupingStyle
import com.example.domain.model.HistoryItem
import com.example.domain.model.HistoryMode
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainAppViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val historyItems: StateFlow<List<HistoryItem>> = historyRepository.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentMode = MutableStateFlow(CalculatorMode.STANDARD)
    val currentMode: StateFlow<CalculatorMode> = _currentMode.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _historyFilter = MutableStateFlow<HistoryMode?>(null)
    val historyFilter: StateFlow<HistoryMode?> = _historyFilter.asStateFlow()

    fun selectMode(mode: CalculatorMode) {
        _isSettingsOpen.value = false
        _currentMode.value = mode
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun setHistoryFilter(filter: HistoryMode?) {
        _historyFilter.value = filter
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteEntry(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clearAll()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    }

    fun setDecimalPrecision(precision: Int) {
        viewModelScope.launch { settingsDataStore.setDecimalPrecision(precision) }
    }

    fun setDecimalSeparator(separator: DecimalSeparator) {
        viewModelScope.launch { settingsDataStore.setDecimalSeparator(separator) }
    }

    fun setGroupingStyle(style: GroupingStyle) {
        viewModelScope.launch { settingsDataStore.setGroupingStyle(style) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setHapticEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSoundEnabled(enabled) }
    }

    fun setFractionDisplayPreference(pref: FractionDisplayPreference) {
        viewModelScope.launch { settingsDataStore.setFractionDisplayPreference(pref) }
    }
}
