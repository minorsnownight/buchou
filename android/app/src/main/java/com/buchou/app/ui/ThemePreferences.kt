package com.buchou.app.ui

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import com.buchou.app.R

enum class AppTheme(val labelRes: Int) {
    System(R.string.theme_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark),
}

class ThemePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("theme", Context.MODE_PRIVATE)
    private val mutableTheme = MutableStateFlow(read())

    val theme: StateFlow<AppTheme> = mutableTheme

    fun setTheme(theme: AppTheme) {
        preferences.edit { putString(KEY_THEME, theme.name) }
        mutableTheme.value = theme
    }

    private fun read(): AppTheme {
        val name = preferences.getString(KEY_THEME, null)
        return runCatching { AppTheme.valueOf(name!!) }.getOrDefault(AppTheme.System)
    }

    private companion object {
        const val KEY_THEME = "theme"
    }
}
