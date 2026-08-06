package com.buchou.app.alarm

import android.content.Context
import androidx.core.content.edit
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 21,
    val minute: Int = 0,
    val weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val soundUri: String? = null,
    val soundName: String? = null,
)

class ReminderPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("reminder", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())

    val settings: StateFlow<ReminderSettings> = mutableSettings

    fun current(): ReminderSettings = mutableSettings.value

    fun update(transform: (ReminderSettings) -> ReminderSettings) {
        val updated = transform(mutableSettings.value)
        require(updated.hour in 0..23)
        require(updated.minute in 0..59)
        require(updated.weekdays.isNotEmpty())
        preferences.edit {
            putBoolean(KEY_ENABLED, updated.enabled)
            putInt(KEY_HOUR, updated.hour)
            putInt(KEY_MINUTE, updated.minute)
            putInt(KEY_WEEKDAYS, updated.weekdays.toMask())
            putString(KEY_SOUND_URI, updated.soundUri)
            putString(KEY_SOUND_NAME, updated.soundName)
        }
        mutableSettings.value = updated
    }

    fun reset() {
        preferences.edit { clear() }
        mutableSettings.value = ReminderSettings()
    }

    private fun read(): ReminderSettings {
        val weekdays = preferences.getInt(KEY_WEEKDAYS, ALL_DAYS_MASK).toWeekdays()
        return ReminderSettings(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            hour = preferences.getInt(KEY_HOUR, 21),
            minute = preferences.getInt(KEY_MINUTE, 0),
            weekdays = weekdays.ifEmpty { DayOfWeek.entries.toSet() },
            soundUri = preferences.getString(KEY_SOUND_URI, null),
            soundName = preferences.getString(KEY_SOUND_NAME, null),
        )
    }

    private fun Set<DayOfWeek>.toMask(): Int = fold(0) { mask, day ->
        mask or (1 shl (day.value - 1))
    }

    private fun Int.toWeekdays(): Set<DayOfWeek> = DayOfWeek.entries
        .filterTo(linkedSetOf()) { day -> this and (1 shl (day.value - 1)) != 0 }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
        const val KEY_WEEKDAYS = "weekdays"
        const val KEY_SOUND_URI = "sound_uri"
        const val KEY_SOUND_NAME = "sound_name"
        const val ALL_DAYS_MASK = 0b1111111
    }
}
