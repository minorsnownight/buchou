package com.buchou.app.sync

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class WebDavConfig(
    val url: String = "",
    val username: String = "",
    val password: String = "",
)

data class SyncState(
    val config: WebDavConfig = WebDavConfig(),
    val lastSyncEpochMillis: Long? = null,
    val lastError: String? = null,
    val isSyncing: Boolean = false,
)

class SyncPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("webdav", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())

    val state: StateFlow<SyncState> = mutableState

    fun saveConfig(config: WebDavConfig) {
        preferences.edit {
            putString(KEY_URL, config.url)
            putString(KEY_USERNAME, config.username)
            putString(KEY_PASSWORD, config.password)
        }
        mutableState.value = mutableState.value.copy(config = config, lastError = null)
    }

    fun setSyncing(syncing: Boolean) {
        mutableState.value = mutableState.value.copy(isSyncing = syncing)
    }

    fun setSyncResult(epochMillis: Long?, error: String?) {
        mutableState.value = mutableState.value.copy(
            lastSyncEpochMillis = epochMillis,
            lastError = error,
            isSyncing = false,
        )
    }

    private fun read(): SyncState = SyncState(
        config = WebDavConfig(
            url = preferences.getString(KEY_URL, "") ?: "",
            username = preferences.getString(KEY_USERNAME, "") ?: "",
            password = preferences.getString(KEY_PASSWORD, "") ?: "",
        ),
        lastSyncEpochMillis = preferences.getLong(KEY_LAST_SYNC, 0).takeIf { it > 0 },
    )

    private companion object {
        const val KEY_URL = "url"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_LAST_SYNC = "last_sync"
    }
}
