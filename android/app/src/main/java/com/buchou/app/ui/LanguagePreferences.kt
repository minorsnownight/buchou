package com.buchou.app.ui

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

enum class AppLanguage(val code: String) {
    System("system"),
    Chinese("zh"),
    English("en"),
}

class LanguagePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("language", Context.MODE_PRIVATE)
    private val mutableLanguage = MutableStateFlow(read())

    val language: StateFlow<AppLanguage> = mutableLanguage

    private var pendingRestart = false

    fun setLanguage(language: AppLanguage) {
        preferences.edit { putString(KEY_LANGUAGE, language.code) }
        mutableLanguage.value = language
        pendingRestart = true
    }

    fun consumePendingRestart(): Boolean {
        val r = pendingRestart
        pendingRestart = false
        return r
    }

    fun wrapContext(base: Context): Context {
        val lang = read()
        if (lang == AppLanguage.System) return base
        val locale = when (lang) {
            AppLanguage.Chinese -> java.util.Locale.Builder().setLanguage("zh").setRegion("CN").build()
            AppLanguage.English -> java.util.Locale.Builder().setLanguage("en").build()
            AppLanguage.System -> Locale.getDefault()
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    private fun read(): AppLanguage {
        val code = preferences.getString(KEY_LANGUAGE, null) ?: "system"
        return AppLanguage.entries.firstOrNull { it.code == code } ?: AppLanguage.System
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}
