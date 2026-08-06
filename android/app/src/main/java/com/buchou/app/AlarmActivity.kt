package com.buchou.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.buchou.app.alarm.AlarmRingingService
import com.buchou.app.ui.theme.BuchouTheme
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmActivity : ComponentActivity() {
    private var reason by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val application = applicationContext as BuchouApplication
        lifecycleScope.launch {
            reason = application.repository.data.first().reasons.firstOrNull()?.content
        }
        setContent {
            BuchouTheme {
                AlarmScreen(
                    reason = reason,
                    onSmokeFree = { intensity ->
                        lifecycleScope.launch {
                            application.repository.markSmokeFree(intensity)
                            application.alarmScheduler.skipToday()
                            AlarmRingingService.stop(this@AlarmActivity)
                            finishAndRemoveTask()
                        }
                    },
                    onSmoked = {
                        AlarmRingingService.stop(this@AlarmActivity)
                        startActivity(
                            Intent(this@AlarmActivity, MainActivity::class.java)
                                .putExtra(MainActivity.EXTRA_OPEN_SMOKING, true),
                        )
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }
}

@Composable
private fun AlarmScreen(
    reason: String?,
    onSmokeFree: (Int) -> Unit,
    onSmoked: () -> Unit,
) {
    var intensity by rememberSaveable { mutableFloatStateOf(0f) }
    BackHandler { }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 26.dp, vertical = 34.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(54.dp))
                Text(
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
                    style = MaterialTheme.typography.displayLarge,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.alarm_question),
                    style = MaterialTheme.typography.headlineSmall,
                )
                reason?.let {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "“$it”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column {
                Text(
                    stringResource(R.string.craving_value, intensity.toInt()),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { onSmokeFree(intensity.toInt()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.smoke_free_today))
                    }
                    OutlinedButton(
                        onClick = onSmoked,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.smoked_today))
                    }
                }
            }
        }
    }
}
