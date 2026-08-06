package com.buchou.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.buchou.app.ui.BuchouApp
import com.buchou.app.widget.WidgetUpdater
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext((newBase.applicationContext as BuchouApplication).languagePreferences.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuchouApp(
                openSmokingOnStart = intent.getBooleanExtra(EXTRA_OPEN_SMOKING, false),
                onLanguageChanged = {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                    finish()
                },
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // 回前台时刷新时间快照；数据变化的推送由 Application 的流负责。
        MainScope().launch {
            WidgetUpdater.updateAll(this@MainActivity)
        }
    }

    companion object {
        const val EXTRA_OPEN_SMOKING = "open_smoking"
    }
}
