package com.buchou.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.buchou.app.alarm.AlarmScheduler
import com.buchou.app.alarm.ReminderPreferences
import com.buchou.app.alarm.ReminderSettings
import com.buchou.app.ui.AppTheme
import com.buchou.app.ui.CurrencyPreferences
import com.buchou.app.ui.ThemePreferences
import com.buchou.app.data.BuchouData
import com.buchou.app.sync.SyncManager
import com.buchou.app.sync.SyncPreferences
import com.buchou.app.sync.SyncState
import android.app.Application
import com.buchou.app.sync.WebDavConfig
import com.buchou.app.data.BuchouRepository
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BuchouUiState {
    data object Loading : BuchouUiState
    data object NeedsOnboarding : BuchouUiState
    data class Ready(val data: BuchouData) : BuchouUiState
}

class BuchouViewModel(
    private val repository: BuchouRepository,
    private val reminderPreferences: ReminderPreferences,
    private val alarmScheduler: AlarmScheduler,
    private val homeModulePreferences: HomeModulePreferences,
    private val themePreferences: ThemePreferences,
    private val currencyPreferences: CurrencyPreferences,
    private val languagePreferences: LanguagePreferences,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences,
) : ViewModel() {
    init {
        viewModelScope.launch {
            repository.data.first { it.profile != null && it.journey != null }
            repository.ensureFiveReasons()
            repository.refreshAchievements()
        }
    }

    val reminderSettings: StateFlow<ReminderSettings> = reminderPreferences.settings
    val homeModuleConfigs: StateFlow<List<HomeModuleConfig>> = homeModulePreferences.configs
    val appTheme: StateFlow<AppTheme> = themePreferences.theme
    val currencyCode: StateFlow<String> = currencyPreferences.currencyCode
    val language: StateFlow<AppLanguage> = languagePreferences.language
    val syncState: StateFlow<SyncState> = syncPreferences.state
    val uiState: StateFlow<BuchouUiState> = repository.data
        .map { data ->
            if (data.profile == null || data.journey == null) {
                BuchouUiState.NeedsOnboarding
            } else {
                BuchouUiState.Ready(data)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BuchouUiState.Loading,
        )

    fun completeOnboarding(
        quitStartedAt: Instant,
        cigarettesPerDay: Int,
        cigarettesPerPack: Int?,
        pricePerPack: Double?,
        reasons: List<String>,
    ) {
        viewModelScope.launch {
            repository.initialize(
                quitStartedAt = quitStartedAt,
                cigarettesPerDay = cigarettesPerDay,
                cigarettesPerPack = cigarettesPerPack,
                pricePerPack = pricePerPack,
                customReasons = reasons,
            )
        }
    }

    fun markSmokeFree(cravingIntensity: Int? = null) {
        viewModelScope.launch {
            repository.markSmokeFree(cravingIntensity = cravingIntensity)
            alarmScheduler.skipToday()
        }
    }

    fun recordSmoking(cigaretteCount: Int, cravingIntensity: Int? = null) {
        viewModelScope.launch {
            repository.recordSmoking(
                cigaretteCount = cigaretteCount,
                cravingIntensity = cravingIntensity,
            )
            alarmScheduler.skipToday()
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        reminderPreferences.update { it.copy(enabled = enabled) }
        if (enabled) scheduleNextRespectingToday() else alarmScheduler.cancel()
    }

    fun setReminderTime(hour: Int, minute: Int) {
        reminderPreferences.update { it.copy(hour = hour, minute = minute) }
        scheduleNextRespectingToday()
    }

    fun toggleReminderWeekday(day: DayOfWeek) {
        reminderPreferences.update { current ->
            val updated = if (day in current.weekdays) current.weekdays - day else current.weekdays + day
            if (updated.isEmpty()) current else current.copy(weekdays = updated)
        }
        scheduleNextRespectingToday()
    }

    fun setReminderSound(uri: String?, name: String?) {
        reminderPreferences.update { it.copy(soundUri = uri, soundName = name) }
    }

    fun updateProfile(cigarettesPerDay: Int, cigarettesPerPack: Int?, pricePerPack: Double?) {
        viewModelScope.launch {
            repository.updateProfile(cigarettesPerDay, cigarettesPerPack, pricePerPack)
        }
    }

    fun updateReasons(reasons: List<String>) {
        viewModelScope.launch {
            repository.updateReasons(reasons)
        }
    }

    fun restartQuitJourney() {
        viewModelScope.launch {
            alarmScheduler.cancel()
            repository.restartQuitJourney()
            alarmScheduler.scheduleNext()
        }
    }

    fun injectTestData() {
        viewModelScope.launch {
            repository.injectTestData()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            alarmScheduler.cancel()
            reminderPreferences.reset()
            homeModulePreferences.reset()
            repository.resetAllData()
        }
    }

    fun setHomeModuleVisible(module: HomeModule, visible: Boolean) {
        homeModulePreferences.setVisible(module, visible)
    }

    fun setAppTheme(theme: AppTheme) {
        themePreferences.setTheme(theme)
    }

    fun setCurrency(code: String) {
        currencyPreferences.setCurrency(code)
    }

    fun setLanguage(language: AppLanguage) {
        languagePreferences.setLanguage(language)
    }

    fun saveWebDavConfig(config: WebDavConfig) {
        syncPreferences.saveConfig(config)
    }

    fun syncUpload() {
        viewModelScope.launch {
            syncManager.upload()
        }
    }

    fun syncDownload() {
        viewModelScope.launch {
            syncManager.download()
        }
    }

    fun moveHomeModule(module: HomeModule, direction: Int) {
        homeModulePreferences.move(module, direction)
    }

    private fun scheduleNextRespectingToday() {
        val data = (uiState.value as? BuchouUiState.Ready)?.data
        if (data?.hasRecordForDate(LocalDate.now()) == true) {
            alarmScheduler.skipToday()
        } else {
            alarmScheduler.scheduleNext()
        }
    }

    companion object {
        fun factory(
            application: Application,
            repository: BuchouRepository,
            reminderPreferences: ReminderPreferences,
            alarmScheduler: AlarmScheduler,
            homeModulePreferences: HomeModulePreferences,
            themePreferences: ThemePreferences,
            currencyPreferences: CurrencyPreferences,
            languagePreferences: LanguagePreferences,
            syncManager: SyncManager,
            syncPreferences: SyncPreferences,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BuchouViewModel(repository, reminderPreferences, alarmScheduler, homeModulePreferences, themePreferences, currencyPreferences, languagePreferences, syncManager, syncPreferences)
            }
        }
    }
}
