package com.buchou.app.ui

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.buchou.app.R

enum class HomeModule(val labelRes: Int) {
    Health(R.string.module_health),
    Reasons(R.string.module_reasons),
    Achievements(R.string.module_achievements),
}

data class HomeModuleConfig(
    val module: HomeModule,
    val visible: Boolean,
)

class HomeModulePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("home_modules", Context.MODE_PRIVATE)
    private val mutableConfigs = MutableStateFlow(read())

    val configs: StateFlow<List<HomeModuleConfig>> = mutableConfigs

    fun setVisible(module: HomeModule, visible: Boolean) {
        save(mutableConfigs.value.map { if (it.module == module) it.copy(visible = visible) else it })
    }

    fun move(module: HomeModule, direction: Int) {
        val current = mutableConfigs.value.toMutableList()
        val from = current.indexOfFirst { it.module == module }
        val to = (from + direction).coerceIn(current.indices)
        if (from < 0 || from == to) return
        current.add(to, current.removeAt(from))
        save(current)
    }

    fun reset() {
        save(defaultConfigs())
    }

    private fun read(): List<HomeModuleConfig> {
        if (preferences.getInt(KEY_VERSION, 0) < CURRENT_VERSION) {
            return defaultConfigs().also(::write)
        }
        val orderedModules = preferences.getString(KEY_ORDER, null)
            ?.split(',')
            ?.mapNotNull { name -> HomeModule.entries.firstOrNull { it.name == name } }
            ?.takeIf { it.toSet() == HomeModule.entries.toSet() }
            ?: HomeModule.entries
        val hidden = preferences.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()
        return orderedModules.map { HomeModuleConfig(it, it.name !in hidden) }
    }

    private fun save(configs: List<HomeModuleConfig>) {
        write(configs)
        mutableConfigs.value = configs
    }

    private fun write(configs: List<HomeModuleConfig>) {
        preferences.edit {
            putInt(KEY_VERSION, CURRENT_VERSION)
            putString(KEY_ORDER, configs.joinToString(",") { it.module.name })
            putStringSet(KEY_HIDDEN, configs.filterNot(HomeModuleConfig::visible).mapTo(mutableSetOf()) { it.module.name })
        }
    }

    private fun defaultConfigs(): List<HomeModuleConfig> = listOf(
        HomeModuleConfig(HomeModule.Health, true),
        HomeModuleConfig(HomeModule.Reasons, false),
        HomeModuleConfig(HomeModule.Achievements, false),
    )

    private companion object {
        const val CURRENT_VERSION = 2
        const val KEY_VERSION = "version"
        const val KEY_ORDER = "order"
        const val KEY_HIDDEN = "hidden"
    }
}
