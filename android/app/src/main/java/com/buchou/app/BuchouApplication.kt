package com.buchou.app

import android.app.Application
import com.buchou.app.alarm.AlarmScheduler
import com.buchou.app.alarm.ReminderPreferences
import com.buchou.app.data.BuchouRepository
import com.buchou.app.data.local.BuchouDatabase
import com.buchou.app.sync.SyncManager
import com.buchou.app.sync.SyncPreferences
import com.buchou.app.ui.CurrencyPreferences
import com.buchou.app.ui.HomeModulePreferences
import com.buchou.app.ui.LanguagePreferences
import com.buchou.app.ui.ThemePreferences
import com.buchou.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BuchouApplication : Application() {
    val homeModulePreferences: HomeModulePreferences by lazy {
        HomeModulePreferences(this)
    }

    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(this)
    }

    val currencyPreferences: CurrencyPreferences by lazy {
        CurrencyPreferences(this)
    }

    val languagePreferences: LanguagePreferences by lazy {
        LanguagePreferences(this)
    }

    val syncPreferences: SyncPreferences by lazy {
        SyncPreferences(this)
    }

    val syncManager: SyncManager by lazy {
        SyncManager(database, syncPreferences)
    }

    val reminderPreferences: ReminderPreferences by lazy {
        ReminderPreferences(this)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(this, reminderPreferences)
    }

    private val database: BuchouDatabase by lazy {
        BuchouDatabase.create(this)
    }

    val repository: BuchouRepository by lazy {
        BuchouRepository(database)
    }

    override fun onCreate() {
        super.onCreate()
        // 监听数据变化，数据任何写入（打卡/重置/重启/同步）完成后推一次 widget。
        // 前台执行，不经 JobScheduler，不被厂商后台冻结。
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            repository.data.collect { WidgetUpdater.updateAll(this@BuchouApplication) }
        }
    }
}
