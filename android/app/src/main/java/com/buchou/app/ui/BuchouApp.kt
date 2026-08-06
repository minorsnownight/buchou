package com.buchou.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import com.buchou.app.BuchouApplication
import com.buchou.app.R
import com.buchou.app.alarm.ReminderSettings
import com.buchou.app.data.BuchouData
import com.buchou.app.data.local.ProfileEntity
import com.buchou.app.data.local.QuitJourneyEntity
import com.buchou.app.domain.model.DailyStatus
import com.buchou.app.ui.theme.BuchouTheme
import com.buchou.app.ui.components.BuchouSwitch
import java.text.DateFormat
import java.time.Duration
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.util.Calendar
import kotlinx.coroutines.delay

@Composable
fun BuchouApp(openSmokingOnStart: Boolean = false, onLanguageChanged: () -> Unit = {}) {
    val application = LocalContext.current.applicationContext as BuchouApplication
    val viewModel: BuchouViewModel = viewModel(
        factory = BuchouViewModel.factory(
            application,
            application.repository,
            application.reminderPreferences,
            application.alarmScheduler,
            application.homeModulePreferences,
            application.themePreferences,
            application.currencyPreferences,
            application.languagePreferences,
            application.syncManager,
            application.syncPreferences,
        ),
    )
    val uiState by viewModel.uiState.collectAsState()
    val reminderSettings by viewModel.reminderSettings.collectAsState()
    val homeModuleConfigs by viewModel.homeModuleConfigs.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val currencyCode by viewModel.currencyCode.collectAsState()
    val language by viewModel.language.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    BuchouTheme(appTheme = appTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                BuchouUiState.Loading -> LoadingScreen()
                BuchouUiState.NeedsOnboarding -> OnboardingScreen(
                    onComplete = { quitStartedAt, daily, perPack, price, reasons ->
                        viewModel.completeOnboarding(quitStartedAt, daily, perPack, price, reasons)
                    },
                )
                is BuchouUiState.Ready -> ProductShell(
                        data = state.data,
                        openSmokingOnStart = openSmokingOnStart,
                        onSmokeFree = viewModel::markSmokeFree,
                        onSmoked = viewModel::recordSmoking,
                        settings = reminderSettings,
                        onEnabledChange = viewModel::setReminderEnabled,
                        onTimeChange = viewModel::setReminderTime,
                        onWeekdayToggle = viewModel::toggleReminderWeekday,
                        onSoundChange = viewModel::setReminderSound,
                        onUpdateProfile = viewModel::updateProfile,
                        onUpdateReasons = viewModel::updateReasons,
                        onRestartJourney = viewModel::restartQuitJourney,
                        onResetAllData = viewModel::resetAllData,
                        homeModuleConfigs = homeModuleConfigs,
                        onHomeModuleVisible = viewModel::setHomeModuleVisible,
                        onMoveHomeModule = viewModel::moveHomeModule,
                        onInjectTestData = viewModel::injectTestData,
                        appTheme = appTheme,
                        onSetTheme = viewModel::setAppTheme,
                        currencyCode = currencyCode,
                        onSetCurrency = viewModel::setCurrency,
                        language = language,
                        onSetLanguage = viewModel::setLanguage,
                        syncState = syncState,
                        onSaveWebDavConfig = viewModel::saveWebDavConfig,
                        onSyncUpload = viewModel::syncUpload,
                        onSyncDownload = viewModel::syncDownload,
                        onLanguageChanged = onLanguageChanged,
                )
            }
        }
    }
}

private enum class NavDestination(val labelRes: Int) {
    Home(R.string.nav_home),
    History(R.string.nav_history),
    Achievements(R.string.nav_achievements),
    Settings(R.string.nav_settings),
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun OnboardingScreen(
    onComplete: (Instant, Int, Int?, Double?, List<String>) -> Unit,
) {
    val context = LocalContext.current
    var quitStartedAtMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var cigarettesPerDay by rememberSaveable { mutableStateOf("") }
    var cigarettesPerPack by rememberSaveable { mutableStateOf("20") }
    var pricePerPack by rememberSaveable { mutableStateOf("") }
    val defaultReasons = listOf(
        stringResource(R.string.reason_default_1),
        stringResource(R.string.reason_default_2),
        stringResource(R.string.reason_default_3),
        stringResource(R.string.reason_default_4),
        stringResource(R.string.reason_default_5),
    )
    var reasons by rememberSaveable { mutableStateOf(defaultReasons) }
    var validationMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    val dateText = remember(quitStartedAtMillis) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(quitStartedAtMillis)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.quit_start_time), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                TextButton(onClick = {
                    val initial = Calendar.getInstance().apply { timeInMillis = quitStartedAtMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    quitStartedAtMillis = Calendar.getInstance().apply {
                                        set(year, month, day, hour, minute, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                },
                                initial.get(Calendar.HOUR_OF_DAY),
                                initial.get(Calendar.MINUTE),
                                true,
                            ).show()
                        },
                        initial.get(Calendar.YEAR),
                        initial.get(Calendar.MONTH),
                        initial.get(Calendar.DAY_OF_MONTH),
                    ).show()
                }) { Text(stringResource(R.string.select)) }
            },
        )
        Spacer(Modifier.height(12.dp))
        NumberField(
            value = cigarettesPerDay,
            onValueChange = { cigarettesPerDay = it },
            label = stringResource(R.string.cigarettes_per_day),
        )
        Spacer(Modifier.height(12.dp))
        NumberField(
            value = cigarettesPerPack,
            onValueChange = { cigarettesPerPack = it },
            label = stringResource(R.string.cigarettes_per_pack),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pricePerPack,
            onValueChange = { value ->
                if (value.count { it == '.' } <= 1 && value.all { it.isDigit() || it == '.' }) {
                    pricePerPack = value
                }
            },
            label = { Text(stringResource(R.string.price_per_pack)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.quit_reason), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        reasons.forEachIndexed { index, reason ->
            OutlinedTextField(
                value = reason,
                onValueChange = { updated ->
                    reasons = reasons.toMutableList().also { it[index] = updated.take(60) }
                },
                label = { Text(stringResource(R.string.quit_reason_slot, index + 1)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
            )
            if (index < reasons.lastIndex) Spacer(Modifier.height(10.dp))
        }
        validationMessage?.let { message ->
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(message),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = {
                val daily = cigarettesPerDay.toIntOrNull()
                val perPack = cigarettesPerPack.takeIf(String::isNotBlank)?.toIntOrNull()
                val price = pricePerPack.takeIf(String::isNotBlank)?.toDoubleOrNull()
                validationMessage = when {
                    daily == null || daily <= 0 -> R.string.invalid_required_fields
                    cigarettesPerPack.isNotBlank() && (perPack == null || perPack <= 0) ->
                        R.string.invalid_required_fields
                    pricePerPack.isNotBlank() && (price == null || price < 0.0) ->
                        R.string.invalid_required_fields
                    quitStartedAtMillis > System.currentTimeMillis() -> R.string.future_start_time
                    else -> null
                }
                if (validationMessage == null) {
                    onComplete(
                        Instant.ofEpochMilli(quitStartedAtMillis),
                        checkNotNull(daily),
                        perPack,
                        price,
                        reasons,
                    )
                }
            },
        ) {
            Text(stringResource(R.string.start_button), modifier = Modifier.padding(vertical = 5.dp))
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { candidate ->
            if (candidate.all(Char::isDigit) && candidate.length <= 3) onValueChange(candidate)
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun HomeScreen(
    data: BuchouData,
    openSmokingOnStart: Boolean = false,
    onSmokeFree: (Int) -> Unit,
    onSmoked: (Int, Int) -> Unit,
    onNavigate: (NavDestination) -> Unit,
) {
    var showSmokeFreeDialog by rememberSaveable { mutableStateOf(false) }
    var showSmokingDialog by rememberSaveable { mutableStateOf(openSmokingOnStart) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(data.currentStreakStartedAtEpochMillis()) {
        while (true) {
            delay(60_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val profile = checkNotNull(data.profile)
    val stats = checkNotNull(data.journeyStats(Instant.ofEpochMilli(nowMillis)))
    val todayStatus = data.statusForDate(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        BrandHeader()
        Spacer(Modifier.height(34.dp))
        DurationBlock(stats.currentSmokeFreeDuration)
        Spacer(Modifier.height(22.dp))
        CheckInCard(
            todayStatus = todayStatus,
            onSmokeFree = { showSmokeFreeDialog = true },
            onSmoked = { showSmokingDialog = true },
        )
        if (data.reasons.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            ReasonCard(data.reasons.first().content)
        }
        Spacer(Modifier.height(14.dp))
        StatsCard(
            avoidedCigarettes = stats.avoidedCigarettes.toInt(),
            savedMoney = stats.savedMoney,
            currencyCode = profile.currencyCode,
        )
        Spacer(Modifier.weight(1f))
        BottomNavigation(selected = NavDestination.Home, onNavigate = onNavigate)
    }

    if (showSmokeFreeDialog) {
        CravingDialog(
            title = stringResource(R.string.craving_title),
            confirmLabel = stringResource(R.string.confirm_check_in),
            onDismiss = { showSmokeFreeDialog = false },
            onConfirm = { intensity ->
                onSmokeFree(intensity)
                showSmokeFreeDialog = false
            },
        )
    }
    if (showSmokingDialog) {
        SmokingDialog(
            onDismiss = { showSmokingDialog = false },
            onConfirm = { count, intensity ->
                onSmoked(count, intensity)
                showSmokingDialog = false
            },
        )
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(stringResource(R.string.brand_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text(stringResource(R.string.current_attempt), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun DurationBlock(duration: Duration) {
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    Column {
        Text(
            stringResource(R.string.quit_duration_label),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(days.toString(), style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(R.string.day_unit),
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(R.string.duration_detail, hours, minutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CheckInCard(
    todayStatus: DailyStatus,
    onSmokeFree: () -> Unit,
    onSmoked: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                when (todayStatus) {
                    DailyStatus.SMOKE_FREE -> stringResource(R.string.checked_smoke_free)
                    DailyStatus.SMOKED -> stringResource(R.string.checked_smoked)
                    DailyStatus.UNRECORDED -> stringResource(R.string.check_in_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSmokeFree,
                    enabled = todayStatus != DailyStatus.SMOKED,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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

@Composable
private fun ReasonCard(content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Text(
            stringResource(R.string.your_reason),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(content, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun StatsCard(
    avoidedCigarettes: Int,
    savedMoney: Double?,
    currencyCode: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatPill(stringResource(R.string.avoided_count, avoidedCigarettes), Modifier.weight(1f))
        if (savedMoney != null) {
            StatPill(
                stringResource(R.string.saved_money, savedMoney, currencyCode),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatPill(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CravingDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var intensity by rememberSaveable { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.craving_value, intensity.toInt()))
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(onClick = { onConfirm(intensity.toInt()) }) { Text(confirmLabel) }
        },
    )
}

@Composable
private fun SmokingDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var count by rememberSaveable { mutableStateOf("") }
    var intensity by rememberSaveable { mutableFloatStateOf(0f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.smoking_dialog_title)) },
        text = {
            Column {
                NumberField(
                    value = count,
                    onValueChange = { count = it },
                    label = stringResource(R.string.cigarette_count),
                )
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.craving_value, intensity.toInt()))
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 0f..5f,
                    steps = 4,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(
                enabled = (count.toIntOrNull() ?: 0) > 0,
                onClick = { onConfirm(checkNotNull(count.toIntOrNull()), intensity.toInt()) },
            ) {
                Text(stringResource(R.string.confirm_restart))
            }
        },
    )
}

@Composable
private fun SettingsScreen(
    settings: ReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onSoundChange: (String?, String?) -> Unit,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    var enabling by rememberSaveable { mutableStateOf(false) }
    var permissionAttempt by remember { mutableIntStateOf(0) }
    var permissionError by rememberSaveable { mutableStateOf(false) }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (canScheduleExactAlarms(context)) permissionAttempt++ else {
            enabling = false
            permissionError = true
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) permissionAttempt++ else {
            enabling = false
            permissionError = true
        }
    }
    val fullScreenLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (canUseFullScreenIntent(context)) permissionAttempt++ else {
            enabling = false
            permissionError = true
        }
    }
    val soundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val name = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment
            onSoundChange(uri.toString(), name)
        }
    }

    LaunchedEffect(enabling, permissionAttempt) {
        if (!enabling) return@LaunchedEffect
        val packageUri = "package:${context.packageName}".toUri()
        when {
            !canScheduleExactAlarms(context) -> exactAlarmLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri),
            )
            !canPostNotifications(context) -> notificationLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
            !canUseFullScreenIntent(context) -> fullScreenLauncher.launch(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri),
            )
            else -> {
                onEnabledChange(true)
                permissionError = false
                enabling = false
            }
        }
    }

    LaunchedEffect(settings.enabled) {
        if (
            settings.enabled &&
            (!canScheduleExactAlarms(context) ||
                !canPostNotifications(context) ||
                !canUseFullScreenIntent(context))
        ) {
            onEnabledChange(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.settings_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.check_in_reminder),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                stringResource(R.string.reminder_system_alarm),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        BuchouSwitch(
                            checked = settings.enabled,
                            enabled = !enabling,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    permissionError = false
                                    enabling = true
                                    permissionAttempt++
                                } else {
                                    onEnabledChange(false)
                                }
                            },
                        )
                    }
                    if (enabling) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.reminder_permission_in_progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else if (permissionError) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.reminder_permission_denied),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.reminder_time),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> onTimeChange(hour, minute) },
                                settings.hour,
                                settings.minute,
                                true,
                            ).show()
                        },
                    ) {
                        Text(formatReminderTime(context, settings.hour, settings.minute))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.repeat_on),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    WeekdaySelector(
                        selected = settings.weekdays,
                        onToggle = onWeekdayToggle,
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.alarm_sound),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        settings.soundName ?: stringResource(R.string.system_default_sound),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { soundLauncher.launch(arrayOf("audio/*")) },
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(stringResource(R.string.choose_sound))
                        }
                        if (settings.soundUri != null) {
                            TextButton(onClick = { onSoundChange(null, null) }) {
                                Text(stringResource(R.string.use_system_default))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.reminder_behavior_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BottomNavigation(selected = NavDestination.Settings, onNavigate = onNavigate)
    }
}

@Composable
private fun WeekdaySelector(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
) {
    val labels = listOf(
        R.string.weekday_monday,
        R.string.weekday_tuesday,
        R.string.weekday_wednesday,
        R.string.weekday_thursday,
        R.string.weekday_friday,
        R.string.weekday_saturday,
        R.string.weekday_sunday,
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DayOfWeek.entries.chunked(4).forEach { days ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { day ->
                    FilterChip(
                        selected = day in selected,
                        onClick = { onToggle(day) },
                        label = { Text(stringResource(labels[day.value - 1])) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    destination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(destination.labelRes), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.coming_soon),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BottomNavigation(selected = destination, onNavigate = onNavigate)
    }
}

@Composable
private fun BottomNavigation(
    selected: NavDestination,
    onNavigate: (NavDestination) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        NavDestination.entries.forEach { destination ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigate(destination) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(destination.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (destination == selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (destination == selected) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun canUseFullScreenIntent(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

private fun formatReminderTime(context: Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun OnboardingPreview() {
    BuchouTheme(appTheme = com.buchou.app.ui.AppTheme.System) {
        OnboardingScreen { _, _, _, _, _ -> }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomePreview() {
    BuchouTheme(appTheme = com.buchou.app.ui.AppTheme.System) {
        HomeScreen(
            data = BuchouData(
                profile = ProfileEntity(
                    cigarettesPerDay = 10,
                    cigarettesPerPack = 20,
                    pricePerPack = 30.0,
                    currencyCode = "CNY",
                    createdAtEpochMillis = 0,
                ),
                journey = QuitJourneyEntity(startedAtEpochMillis = 0, createdAtEpochMillis = 0),
                checkIns = emptyList(),
                smokingEvents = emptyList(),
                reasons = emptyList(),
                achievementUnlocks = emptyList(),
            ),
            onSmokeFree = {},
            onSmoked = { _, _ -> },
            onNavigate = {},
        )
    }
}
