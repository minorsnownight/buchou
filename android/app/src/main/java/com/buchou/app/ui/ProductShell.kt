package com.buchou.app.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.buchou.app.alarm.NextAlarmCalculator
import com.buchou.app.alarm.ReminderSettings
import com.buchou.app.data.BuchouData
import com.buchou.app.domain.DailySummary
import com.buchou.app.domain.model.AchievementId
import com.buchou.app.R
import com.buchou.app.sync.SyncState
import com.buchou.app.sync.WebDavConfig
import com.buchou.app.domain.model.DailyStatus
import com.buchou.app.ui.components.BuchouCard
import com.buchou.app.ui.components.BuchouScaffold
import com.buchou.app.ui.components.BuchouSwitch
import com.buchou.app.ui.components.BuchouTab
import com.buchou.app.ui.components.PageHeader
import com.buchou.app.ui.components.SectionHeader
import com.buchou.app.ui.components.SettingsGroup
import com.buchou.app.ui.components.SettingsRow
import com.buchou.app.ui.theme.BuchouRadius
import com.buchou.app.ui.theme.BuchouSpacing
import com.buchou.app.ui.theme.AchievementBlue
import com.buchou.app.ui.theme.AchievementCoral
import com.buchou.app.ui.theme.AchievementGold
import com.buchou.app.ui.theme.AchievementViolet
import com.buchou.app.ui.theme.SmokeFree
import com.buchou.app.ui.theme.Smoked
import com.buchou.app.ui.theme.Unrecorded
import com.buchou.app.ui.AppLanguage
import com.buchou.app.ui.AppTheme
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
fun ProductShell(
    data: BuchouData,
    openSmokingOnStart: Boolean,
    onSmokeFree: (Int?) -> Unit,
    onSmoked: (Int, Int?) -> Unit,
    settings: ReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onSoundChange: (String?, String?) -> Unit,
    onUpdateProfile: (Int, Int?, Double?) -> Unit,
    onUpdateReasons: (List<String>) -> Unit,
    onRestartJourney: () -> Unit,
    onResetAllData: () -> Unit,
    homeModuleConfigs: List<HomeModuleConfig>,
    onHomeModuleVisible: (HomeModule, Boolean) -> Unit,
    onMoveHomeModule: (HomeModule, Int) -> Unit,
    onInjectTestData: () -> Unit,
    appTheme: AppTheme,
    onSetTheme: (AppTheme) -> Unit,
    currencyCode: String,
    onSetCurrency: (String) -> Unit,
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
    syncState: SyncState,
    onSaveWebDavConfig: (WebDavConfig) -> Unit,
    onSyncUpload: () -> Unit,
    onSyncDownload: () -> Unit,
    onLanguageChanged: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(BuchouTab.Home) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    BuchouScaffold(selectedTab = selectedTab, onTabSelected = { selectedTab = it }) { padding ->
        Box(Modifier.fillMaxSize()) {
            when (selectedTab) {
                BuchouTab.Home -> ProductHomeScreen(
                    data = data,
                    openSmokingOnStart = openSmokingOnStart,
                    onSmokeFree = onSmokeFree,
                    onSmoked = onSmoked,
                    homeModuleConfigs = homeModuleConfigs,
                    modifier = Modifier.padding(padding),
                )
                BuchouTab.Records -> ProductRecordsScreen(data, onInjectTestData, Modifier.padding(padding))
                BuchouTab.Settings -> ProductSettingsScreen(
                    settings = settings,
                    onEnabledChange = onEnabledChange,
                    onTimeChange = onTimeChange,
                    onWeekdayToggle = onWeekdayToggle,
                    onSoundChange = onSoundChange,
                    data = data,
                    onUpdateProfile = onUpdateProfile,
                    onUpdateReasons = onUpdateReasons,
                    onRestartJourney = onRestartJourney,
                    onResetAllData = onResetAllData,
                    homeModuleConfigs = homeModuleConfigs,
                    onHomeModuleVisible = onHomeModuleVisible,
                    onMoveHomeModule = onMoveHomeModule,
                    appTheme = appTheme,
                    onSetTheme = onSetTheme,
                    currencyCode = currencyCode,
                    onSetCurrency = onSetCurrency,
                    language = language,
                    onSetLanguage = onSetLanguage,
                    syncState = syncState,
                    onSaveWebDavConfig = onSaveWebDavConfig,
                    onSyncUpload = onSyncUpload,
                    onSyncDownload = onSyncDownload,
                    onLanguageChanged = onLanguageChanged,
                    onAboutClick = { showAbout = true },
                    modifier = Modifier.padding(padding),
                )
            }
            AnimatedVisibility(visible = showAbout, enter = fadeIn(), exit = fadeOut()) {
                AboutScreen(onBack = { showAbout = false })
            }
        }
    }
}

@Composable
private fun ProductHomeScreen(
    data: BuchouData,
    openSmokingOnStart: Boolean,
    onSmokeFree: (Int?) -> Unit,
    onSmoked: (Int, Int?) -> Unit,
    homeModuleConfigs: List<HomeModuleConfig>,
    modifier: Modifier = Modifier,
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showSmokeFreeDialog by rememberSaveable { mutableStateOf(false) }
    var showSmokingDialog by rememberSaveable { mutableStateOf(openSmokingOnStart) }
    LaunchedEffect(Unit) {
        while (true) {
            val currentTime = System.currentTimeMillis()
            nowMillis = currentTime
            delay(60_000L - currentTime % 60_000L)
        }
    }
    val now = Instant.ofEpochMilli(nowMillis)
    val profile = checkNotNull(data.profile)
    val stats = checkNotNull(data.journeyStats(now))
    val todayStatus = data.statusForDate(LocalDate.now())

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BuchouSpacing.page),
    ) {
        Spacer(Modifier.height(BuchouSpacing.xl))
        PageHeader(stringResource(R.string.page_home))
        Spacer(Modifier.height(BuchouSpacing.xl))
        HeroStatusCard(stats.currentSmokeFreeDuration)
        Spacer(Modifier.height(BuchouSpacing.md))
        TodayActions(
            status = todayStatus,
            onSmokeFree = { showSmokeFreeDialog = true },
            onSmoked = { showSmokingDialog = true },
        )
        Spacer(Modifier.height(BuchouSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
            MetricCard(stringResource(R.string.metric_avoided), "${stats.avoidedCigarettes.toInt()}", stringResource(R.string.day_unit_short), Modifier.weight(1f))
            MetricCard(
                stringResource(R.string.metric_saved),
                stats.savedMoney?.let { String.format(Locale.getDefault(), "%.2f", it) } ?: "—",
                if (stats.savedMoney == null) stringResource(R.string.metric_no_price) else profile.currencyCode,
                Modifier.weight(1f),
            )
        }
        homeModuleConfigs.filter(HomeModuleConfig::visible).forEach { config ->
            when (config.module) {
                HomeModule.Reasons -> if (data.reasons.isNotEmpty()) {
                    Spacer(Modifier.height(BuchouSpacing.section))
                    ReasonCarousel(data.reasons.take(5).map { it.content })
                }
                HomeModule.Health -> {
                    Spacer(Modifier.height(BuchouSpacing.section))
                    HealthTimeline(stats.currentSmokeFreeDuration)
                }
                HomeModule.Achievements -> {
                    Spacer(Modifier.height(BuchouSpacing.section))
                    AchievementsModule(data)
                }
            }
        }
        Spacer(Modifier.height(BuchouSpacing.section))
    }
    if (showSmokeFreeDialog) {
        ProductCravingDialog(
            onDismiss = { showSmokeFreeDialog = false },
            onConfirm = { craving ->
                onSmokeFree(craving)
                showSmokeFreeDialog = false
            },
        )
    }
    if (showSmokingDialog) {
        ProductSmokingDialog(
            onDismiss = { showSmokingDialog = false },
            onConfirm = { count, craving ->
                onSmoked(count, craving)
                showSmokingDialog = false
            },
        )
    }
}

@Composable
private fun HeroStatusCard(duration: Duration) {
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    BuchouCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(BuchouSpacing.xl),
    ) {
        Column {
            Text(stringResource(R.string.duration_label), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(BuchouSpacing.sm))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(days.toString(), style = MaterialTheme.typography.displayLarge)
                Text(
                    stringResource(R.string.day_unit_short),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = BuchouSpacing.sm, bottom = 7.dp),
                )
                Text(
                    "$hours ${stringResource(R.string.hour_unit_short)} $minutes ${stringResource(R.string.minute_unit_short)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = BuchouSpacing.lg, bottom = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun ReasonCarousel(reasons: List<String>) {
    var currentIndex by rememberSaveable(reasons) { mutableIntStateOf(0) }
    LaunchedEffect(reasons) {
        currentIndex = 0
        if (reasons.size > 1) {
            while (true) {
                delay(4_000)
                currentIndex = (currentIndex + 1) % reasons.size
            }
        }
    }
    SectionHeader(stringResource(R.string.module_reasons))
    Spacer(Modifier.height(BuchouSpacing.md))
    BuchouCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column {
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "quitReasonCarousel",
            ) { index ->
                Box(
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = reasons[index],
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckInStatusBadge(recorded: Boolean) {
    val color = if (recorded) MaterialTheme.colorScheme.primary else Unrecorded
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BuchouRadius.full))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = BuchouSpacing.md, vertical = BuchouSpacing.sm),
    ) {
        Text(if (recorded) stringResource(R.string.checked_in) else stringResource(R.string.not_checked_in), style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun TodayActions(status: DailyStatus, onSmokeFree: () -> Unit, onSmoked: () -> Unit) {
    BuchouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.check_in_prompt), style = MaterialTheme.typography.bodyMedium)
                CheckInStatusBadge(recorded = status != DailyStatus.UNRECORDED)
            }
            Spacer(Modifier.height(BuchouSpacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
                CheckInButton(
                    text = stringResource(R.string.btn_smoked),
                    selected = status == DailyStatus.SMOKED,
                    selectedColor = Smoked,
                    onClick = onSmoked,
                    modifier = Modifier.weight(1f),
                )
                CheckInButton(
                    text = stringResource(R.string.btn_smoke_free),
                    selected = status == DailyStatus.SMOKE_FREE,
                    selectedColor = SmokeFree,
                    enabled = status != DailyStatus.SMOKED,
                    onClick = onSmokeFree,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CheckInButton(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.42f)
        selected -> selectedColor
        else -> MaterialTheme.colorScheme.outline
    }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(BuchouRadius.control),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) selectedColor.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (selected) selectedColor else MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.52f),
        ),
    ) {
        Text(text, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@Composable
private fun MetricCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    BuchouCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(BuchouSpacing.sm))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(BuchouSpacing.xs))
            Text(unit, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HealthTimeline(duration: Duration) {
    val milestones = listOf(
        Duration.ofMinutes(20) to stringResource(R.string.health_heart_rate),
        Duration.ofDays(1) to stringResource(R.string.health_nicotine_zero),
        Duration.ofDays(2) to stringResource(R.string.health_taste_smell),
        Duration.ofDays(14) to stringResource(R.string.health_circulation),
    )
    SectionHeader(stringResource(R.string.health_recovery))
    Spacer(Modifier.height(BuchouSpacing.md))
    BuchouCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(BuchouSpacing.lg)) {
            milestones.forEach { (required, label) ->
                val reached = duration >= required
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
                    Box(
                        Modifier.size(12.dp).background(
                            if (reached) SmokeFree else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    )
                    Column {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            formatMilestone(required),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                stringResource(R.string.health_disclaimer),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun formatMilestone(duration: Duration): String = when {
    duration.toDays() > 0 -> "${duration.toDays()} ${stringResource(R.string.day_unit_short)}"
    duration.toHours() > 0 -> "${duration.toHours()} ${stringResource(R.string.hour_unit_short)}"
    else -> "${duration.toMinutes()} ${stringResource(R.string.minute_unit_short)}"
}

@Composable
private fun AchievementsModule(data: BuchouData) {
    val unlocked = data.achievementUnlocks.map { it.achievementId }.toSet()
    SectionHeader(stringResource(R.string.achievements), trailing = "${unlocked.size} / ${AchievementId.entries.size}")
    Spacer(Modifier.height(BuchouSpacing.lg))
    Column(verticalArrangement = Arrangement.spacedBy(BuchouSpacing.xl)) {
        AchievementId.entries.chunked(3).forEach { rowAchievements ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
            ) {
                rowAchievements.forEach { achievement ->
                    AchievementMedal(
                        achievement = achievement,
                        unlocked = achievement.name in unlocked,
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowAchievements.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AchievementMedal(
    achievement: AchievementId,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = when (achievement.ordinal % 4) {
        0 -> AchievementGold
        1 -> AchievementCoral
        2 -> AchievementBlue
        else -> AchievementViolet
    }
    val lockedColor = MaterialTheme.colorScheme.outline
    val medalBrush = Brush.sweepGradient(
        listOf(
            accent.copy(alpha = 0.6f),
            accent,
            Color.White.copy(alpha = 0.9f),
            accent,
            accent.copy(alpha = 0.6f),
        ),
    )
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val outerStroke = 7.dp.toPx()
                if (unlocked) {
                    drawCircle(brush = medalBrush, radius = size.minDimension * 0.42f, style = Stroke(outerStroke))
                    drawCircle(color = accent.copy(alpha = 0.18f), radius = size.minDimension * 0.31f)
                    drawCircle(color = accent, radius = size.minDimension * 0.29f, style = Stroke(1.5.dp.toPx()))
                } else {
                    drawCircle(color = lockedColor.copy(alpha = 0.34f), radius = size.minDimension * 0.42f, style = Stroke(5.dp.toPx()))
                    drawCircle(color = lockedColor.copy(alpha = 0.12f), radius = size.minDimension * 0.31f)
                }
                repeat(12) { index ->
                    val angle = Math.toRadians(index * 30.0 - 90.0)
                    val startRadius = size.minDimension * 0.455f
                    val endRadius = size.minDimension * 0.49f
                    val start = Offset(
                        center.x + cos(angle).toFloat() * startRadius,
                        center.y + sin(angle).toFloat() * startRadius,
                    )
                    val end = Offset(
                        center.x + cos(angle).toFloat() * endRadius,
                        center.y + sin(angle).toFloat() * endRadius,
                    )
                    drawLine(
                        color = if (unlocked) accent else lockedColor.copy(alpha = 0.28f),
                        start = start,
                        end = end,
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    achievement.requiredDays.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
                Text(
                    stringResource(R.string.day_unit_short),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                )
            }
        }
        Spacer(Modifier.height(BuchouSpacing.sm))
        Text(
            achievementTitle(achievement),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (unlocked) FontWeight.SemiBold else FontWeight.Medium,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
        Text(
            if (unlocked) stringResource(R.string.achievement_unlocked) else stringResource(R.string.achievement_locked),
            style = MaterialTheme.typography.labelMedium,
            color = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun achievementTitle(achievement: AchievementId): String = when (achievement) {
    AchievementId.ONE_DAY -> stringResource(R.string.achievement_one_day)
    AchievementId.THREE_DAYS -> stringResource(R.string.achievement_three_days)
    AchievementId.SEVEN_DAYS -> stringResource(R.string.achievement_seven_days)
    AchievementId.FOURTEEN_DAYS -> stringResource(R.string.achievement_fourteen_days)
    AchievementId.THIRTY_DAYS -> stringResource(R.string.achievement_thirty_days)
    AchievementId.NINETY_DAYS -> stringResource(R.string.achievement_ninety_days)
    AchievementId.ONE_HUNDRED_EIGHTY_DAYS -> stringResource(R.string.achievement_180_days)
    AchievementId.ONE_YEAR -> stringResource(R.string.achievement_one_year)
}

@Composable
private fun ProductSmokingDialog(onDismiss: () -> Unit, onConfirm: (Int, Int?) -> Unit) {
    var count by rememberSaveable { mutableStateOf("") }
    var craving by rememberSaveable { mutableFloatStateOf(2f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_smoked)) },
        text = {
            Column {
                OutlinedTextField(
                    value = count,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 3) count = it },
                    label = { Text(stringResource(R.string.cigarette_count_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(BuchouRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(BuchouSpacing.lg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.craving_intensity), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${craving.toInt()} / 5",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(value = craving, onValueChange = { craving = it }, valueRange = 0f..5f, steps = 4)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(6) { value ->
                        Text(value.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(
                enabled = (count.toIntOrNull() ?: 0) > 0,
                onClick = { onConfirm(checkNotNull(count.toIntOrNull()), craving.toInt()) },
            ) { Text(stringResource(R.string.record)) }
        },
    )
}

@Composable
private fun ProductCravingDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    var craving by rememberSaveable { mutableFloatStateOf(2f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.btn_smoke_free)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.craving_intensity), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${craving.toInt()} / 5",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(value = craving, onValueChange = { craving = it }, valueRange = 0f..5f, steps = 4)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(6) { value ->
                        Text(value.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(onClick = { onConfirm(craving.toInt()) }) { Text(stringResource(R.string.record)) }
        },
    )
}

@Composable
private fun ProductRecordsScreen(data: BuchouData, onInjectTestData: () -> Unit, modifier: Modifier = Modifier) {
    var rangeDays by rememberSaveable { mutableIntStateOf(30) }
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val month = YearMonth.now().plusMonths(monthOffset.toLong())
    val rangeStart = today.minusDays((rangeDays - 1).toLong())
    val summaries = remember(data, rangeDays, today) {
        (0 until rangeDays).map { index -> data.dailySummary(rangeStart.plusDays(index.toLong())) }
    }
    val recordStats = data.recordStats(rangeStart, today, today, Instant.now())

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BuchouSpacing.page),
    ) {
        Spacer(Modifier.height(BuchouSpacing.xl))
        PageHeader(stringResource(R.string.page_records), onLongClick = onInjectTestData)
        Spacer(Modifier.height(BuchouSpacing.xl))
        CalendarCard(month, data, onPrevious = { monthOffset-- }, onNext = { if (monthOffset < 0) monthOffset++ })
        Spacer(Modifier.height(BuchouSpacing.section))
        Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.sm)) {
            listOf(7, 30, 90).forEach { days ->
                val selected = days == rangeDays
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(BuchouRadius.full))
                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { rangeDays = days }
                        .padding(horizontal = BuchouSpacing.lg, vertical = BuchouSpacing.sm),
                ) {
                    Text("$days " + stringResource(R.string.day_unit_short), style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(BuchouSpacing.md))
        CigaretteChart(summaries)
        Spacer(Modifier.height(BuchouSpacing.md))
        CravingChart(summaries)
        Spacer(Modifier.height(BuchouSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
            MetricCard(
                stringResource(R.string.smoke_free_rate),
                recordStats?.smokeFreeRate?.let { "${(it * 100).toInt()}" } ?: "—",
                if (recordStats?.smokeFreeRate == null) stringResource(R.string.no_records) else "%",
                Modifier.weight(1f),
            )
            MetricCard(
                stringResource(R.string.record_completeness),
                "${((recordStats?.recordCompleteness ?: 0.0) * 100).toInt()}",
                "%",
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(BuchouSpacing.md))
        MetricCard(
            stringResource(R.string.longest_smoke_free),
            (recordStats?.longestSmokeFreeDuration?.toDays() ?: 0).toString(),
            stringResource(R.string.day_unit_short),
            Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(BuchouSpacing.section))
    }
}

@Composable
private fun CalendarCard(
    month: YearMonth,
    data: BuchouData,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy年 M月") }
    val journeyStartDate = checkNotNull(data.journey)
        .startedAtEpochMillis
        .let { Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    BuchouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(month.format(formatter), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = onPrevious) { Text("‹", style = MaterialTheme.typography.titleLarge) }
                TextButton(onClick = onNext, enabled = month < YearMonth.now()) { Text("›", style = MaterialTheme.typography.titleLarge) }
            }
            Spacer(Modifier.height(BuchouSpacing.md))
            Row(Modifier.fillMaxWidth()) {
                listOf(stringResource(R.string.weekday_monday), stringResource(R.string.weekday_tuesday), stringResource(R.string.weekday_wednesday), stringResource(R.string.weekday_thursday), stringResource(R.string.weekday_friday), stringResource(R.string.weekday_saturday), stringResource(R.string.weekday_sunday)).forEach {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(BuchouSpacing.sm))
            val leading = month.atDay(1).dayOfWeek.value - 1
            val cells = leading + month.lengthOfMonth()
            repeat(ceil(cells / 7f).toInt()) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { weekday ->
                        val day = week * 7 + weekday - leading + 1
                        if (day in 1..month.lengthOfMonth()) {
                            val date = month.atDay(day)
                            val isInJourney = !date.isBefore(journeyStartDate) && !date.isAfter(LocalDate.now())
                            CalendarDay(
                                day = day,
                                status = data.statusForDate(date).takeIf { isInJourney },
                                isToday = date == LocalDate.now(),
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(BuchouSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.lg)) {
                CalendarLegend(stringResource(R.string.calendar_legend_smoke_free), SmokeFree)
                CalendarLegend(stringResource(R.string.calendar_legend_smoked), Smoked)
                CalendarLegend(stringResource(R.string.calendar_legend_unrecorded), Unrecorded)
            }
        }
    }
}

@Composable
private fun CalendarDay(day: Int, status: DailyStatus?, isToday: Boolean, modifier: Modifier = Modifier) {
    val color = when (status) {
        DailyStatus.SMOKE_FREE -> SmokeFree
        DailyStatus.SMOKED -> Smoked
        DailyStatus.UNRECORDED, null -> Color.Transparent
    }
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val dayModifier = when (status) {
            DailyStatus.SMOKE_FREE, DailyStatus.SMOKED -> Modifier.background(color, CircleShape)
            DailyStatus.UNRECORDED -> Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            null -> Modifier
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .then(dayModifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = when (status) {
                    null -> MaterialTheme.colorScheme.outline
                    DailyStatus.UNRECORDED -> MaterialTheme.colorScheme.onSurface
                    else -> Color.White
                },
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.xs)) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CigaretteChart(summaries: List<DailySummary>) {
    val barColor = Smoked
    val gridColor = MaterialTheme.colorScheme.outline
    BuchouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionHeader(stringResource(R.string.chart_daily_cigarettes), trailing = stringResource(R.string.chart_range_days, summaries.size))
            Spacer(Modifier.height(BuchouSpacing.lg))
            Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                val max = (summaries.mapNotNull { it.cigaretteCount }.maxOrNull() ?: 1).coerceAtLeast(1)
                repeat(4) { line ->
                    val y = size.height * line / 3f
                    drawLine(gridColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val slot = size.width / summaries.size.coerceAtLeast(1)
                val width = (slot * 0.56f).coerceAtMost(12.dp.toPx())
                summaries.forEachIndexed { index, summary ->
                    val value = summary.cigaretteCount
                    if (value != null && value > 0) {
                        val height = size.height * value / max
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(index * slot + (slot - width) / 2, size.height - height),
                            size = androidx.compose.ui.geometry.Size(width, height),
                            cornerRadius = CornerRadius(width / 2, width / 2),
                        )
                    } else if (summary.status == DailyStatus.SMOKE_FREE) {
                        drawLine(
                            color = SmokeFree,
                            start = Offset(index * slot + slot * 0.3f, size.height - 2.dp.toPx()),
                            end = Offset(index * slot + slot * 0.7f, size.height - 2.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CravingChart(summaries: List<DailySummary>) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline
    BuchouCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionHeader(stringResource(R.string.chart_craving_trend))
            Spacer(Modifier.height(BuchouSpacing.lg))
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                repeat(3) { line ->
                    val y = size.height * line / 2f
                    drawLine(gridColor.copy(alpha = 0.45f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val slot = if (summaries.size <= 1) size.width else size.width / (summaries.size - 1)
                var previous: Offset? = null
                summaries.forEachIndexed { index, summary ->
                    val craving = summary.cravingIntensity
                    if (craving == null) {
                        previous = null
                    } else {
                        val point = Offset(index * slot, size.height * (1f - craving / 5f))
                        previous?.let { drawLine(lineColor, it, point, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round) }
                        drawCircle(lineColor, radius = 2.5.dp.toPx(), center = point)
                        previous = point
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSettingsScreen(
    data: BuchouData,
    settings: ReminderSettings,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (Int, Int) -> Unit,
    onWeekdayToggle: (DayOfWeek) -> Unit,
    onSoundChange: (String?, String?) -> Unit,
    onUpdateProfile: (Int, Int?, Double?) -> Unit,
    onUpdateReasons: (List<String>) -> Unit,
    onRestartJourney: () -> Unit,
    onResetAllData: () -> Unit,
    homeModuleConfigs: List<HomeModuleConfig>,
    onHomeModuleVisible: (HomeModule, Boolean) -> Unit,
    onMoveHomeModule: (HomeModule, Int) -> Unit,
    appTheme: AppTheme,
    onSetTheme: (AppTheme) -> Unit,
    currencyCode: String,
    onSetCurrency: (String) -> Unit,
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
    syncState: SyncState,
    onSaveWebDavConfig: (WebDavConfig) -> Unit,
    onSyncUpload: () -> Unit,
    onSyncDownload: () -> Unit,
    onLanguageChanged: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ctx = context
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    var enablingReminder by rememberSaveable { mutableStateOf(false) }
    var permissionAttempt by remember { mutableIntStateOf(0) }
    var permissionError by rememberSaveable { mutableStateOf(false) }
    val exactAlarmLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (productCanScheduleExactAlarms(context)) permissionAttempt++ else {
            enablingReminder = false
            permissionError = true
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) permissionAttempt++ else {
            enablingReminder = false
            permissionError = true
        }
    }
    val fullScreenLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (productCanUseFullScreenIntent(context)) permissionAttempt++ else {
            enablingReminder = false
            permissionError = true
        }
    }
    val soundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val name = context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: uri.lastPathSegment
            onSoundChange(uri.toString(), name)
        }
    }
    LaunchedEffect(enablingReminder, permissionAttempt) {
        if (!enablingReminder) return@LaunchedEffect
        val packageUri = "package:${context.packageName}".toUri()
        when {
            !productCanScheduleExactAlarms(context) -> exactAlarmLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri),
            )
            !productCanPostNotifications(context) -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !productCanUseFullScreenIntent(context) -> fullScreenLauncher.launch(
                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri),
            )
            else -> {
                onEnabledChange(true)
                permissionError = false
                enablingReminder = false
            }
        }
    }
    LaunchedEffect(settings.enabled) {
        if (settings.enabled && (!productCanScheduleExactAlarms(context) || !productCanPostNotifications(context) || !productCanUseFullScreenIntent(context))) {
            onEnabledChange(false)
        }
    }
    val nextReminder = remember(settings) {
        val today = LocalDate.now()
        val earliestDate = if (data.hasRecordForDate(today)) today.plusDays(1) else today
        NextAlarmCalculator.next(ZonedDateTime.now(), settings, earliestDate)
            ?.format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = BuchouSpacing.page),
    ) {
        Spacer(Modifier.height(BuchouSpacing.xl))
        PageHeader(stringResource(R.string.page_settings))
        Spacer(Modifier.height(BuchouSpacing.xl))
        SettingsGroup(stringResource(R.string.group_data)) {
            SettingsRow(stringResource(R.string.setting_profile), trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Profile })
            SettingsRow(stringResource(R.string.setting_reasons), trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Reasons })
            val syncSubtitle = syncState.lastError ?: syncState.lastSyncEpochMillis?.let { ts ->
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(java.util.Date(ts))
    } ?: if (syncState.config.url.isNotBlank()) stringResource(R.string.sync_configured) else stringResource(R.string.sync_not_configured)
            SettingsRow(stringResource(R.string.setting_sync), if (syncState.isSyncing) stringResource(R.string.sync_in_progress) else syncSubtitle, trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Sync })
            SettingsRow(stringResource(R.string.setting_restart), onClick = { dialog = SettingsDialog.Restart }, destructive = true)
            SettingsRow(stringResource(R.string.setting_reset_all), onClick = { dialog = SettingsDialog.DeleteAll }, destructive = true, showDivider = false)
        }
        Spacer(Modifier.height(BuchouSpacing.section))
        SettingsGroup(stringResource(R.string.group_reminder)) {
            SettingsRow(
                stringResource(R.string.setting_reminder),
                when {
                    permissionError -> stringResource(R.string.reminder_permission_error)
                    enablingReminder -> stringResource(R.string.reminder_checking_perm)
                    settings.enabled && nextReminder != null -> stringResource(R.string.reminder_next, nextReminder)
                    else -> null
                },
                trailing = {
                    BuchouSwitch(
                        checked = settings.enabled,
                        enabled = !enablingReminder,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                permissionError = false
                                enablingReminder = true
                                permissionAttempt++
                            } else {
                                onEnabledChange(false)
                            }
                        },
                    )
                },
            )
            SettingsRow(
                stringResource(R.string.setting_reminder_time),
                formatReminderTime(context, settings.hour, settings.minute),
                trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = {
                    TimePickerDialog(context, { _, h, m -> onTimeChange(h, m) }, settings.hour, settings.minute, true).show()
                },
            )
            SettingsRow(stringResource(R.string.setting_repeat), weekdaySummary(settings.weekdays, LocalContext.current), trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Weekdays })
            SettingsRow(
                stringResource(R.string.setting_alarm_sound),
                settings.soundName ?: stringResource(R.string.system_default_alarm),
                trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = { soundLauncher.launch(arrayOf("audio/*")) },
                showDivider = settings.soundUri != null,
            )
            if (settings.soundUri != null) {
                SettingsRow(
                    stringResource(R.string.restore_default_sound),
                    onClick = { onSoundChange(null, null) },
                    showDivider = false,
                )
            }
        }
        Spacer(Modifier.height(BuchouSpacing.section))
        SettingsGroup(stringResource(R.string.group_other)) {
            val visibleModuleLabels = homeModuleConfigs.filter { it.visible }.map { ctx.getString(it.module.labelRes) }
            val appearanceSubtitle = ctx.getString(appTheme.labelRes) + if (visibleModuleLabels.isNotEmpty()) " · ${visibleModuleLabels.joinToString()}" else ""
            SettingsRow(stringResource(R.string.setting_appearance), appearanceSubtitle, trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Appearance })
            SettingsRow(stringResource(R.string.setting_language), when (language) { AppLanguage.System -> stringResource(R.string.theme_system); AppLanguage.Chinese -> stringResource(R.string.lang_chinese); AppLanguage.English -> stringResource(R.string.lang_english) }, trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Language })
            SettingsRow(stringResource(R.string.setting_currency), currencyCode, trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) }, onClick = { dialog = SettingsDialog.Currency })
            SettingsRow(
                stringResource(R.string.setting_about),
                stringResource(R.string.about_version, "0.3.0-alpha10"),
                trailing = { Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                onClick = onAboutClick,
                showDivider = false,
            )
        }
        Spacer(Modifier.height(BuchouSpacing.section))
    }
    when (dialog) {
        SettingsDialog.Profile -> ProfileDialog(
            data = data,
            onDismiss = { dialog = null },
            onConfirm = { daily, perPack, price ->
                onUpdateProfile(daily, perPack, price)
                dialog = null
            },
        )
        SettingsDialog.Restart -> DestructiveDialog(
            title = stringResource(R.string.restart_title),
            message = stringResource(R.string.restart_message),
            confirm = stringResource(R.string.restart_confirm),
            onDismiss = { dialog = null },
            onConfirm = onRestartJourney,
        )
        SettingsDialog.DeleteAll -> DestructiveDialog(
            title = stringResource(R.string.reset_title),
            message = stringResource(R.string.reset_message),
            confirm = stringResource(R.string.reset_confirm),
            onDismiss = { dialog = null },
            onConfirm = onResetAllData,
        )
        SettingsDialog.Weekdays -> WeekdayDialog(
            selected = settings.weekdays,
            onToggle = onWeekdayToggle,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Appearance -> AppearanceDialog(
            appTheme = appTheme,
            onSetTheme = onSetTheme,
            homeModuleConfigs = homeModuleConfigs,
            onHomeModuleVisible = onHomeModuleVisible,
            onMoveHomeModule = onMoveHomeModule,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Currency -> CurrencyDialog(
            currencyCode = currencyCode,
            onSetCurrency = onSetCurrency,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Language -> LanguageDialog(
            language = language,
            onSetLanguage = { lang ->
                onSetLanguage(lang)
                dialog = null
                onLanguageChanged()
            },
            onDismiss = { dialog = null },
        )
        SettingsDialog.Sync -> SyncDialog(
            syncState = syncState,
            onSaveConfig = onSaveWebDavConfig,
            onUpload = onSyncUpload,
            onDownload = onSyncDownload,
            onDismiss = { dialog = null },
        )
        SettingsDialog.Reasons -> ReasonsDialog(
            data = data,
            onDismiss = { dialog = null },
            onConfirm = { reasons ->
                onUpdateReasons(reasons)
                dialog = null
            },
        )
        null -> Unit
    }
}

private enum class SettingsDialog { Profile, Restart, DeleteAll, Weekdays, Appearance, Reasons, Currency, Language, Sync }

private const val ABOUT_URL = "https://raw.githubusercontent.com/minorsnownight/buchou/refs/heads/main/ABOUT.md"

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = BuchouSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = stringResource(R.string.setting_about),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    loadUrl(ABOUT_URL)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ReasonsDialog(
    data: BuchouData,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var reasons by rememberSaveable {
        mutableStateOf(List(5) { index -> data.reasons.getOrNull(index)?.content.orEmpty() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reasons_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BuchouSpacing.sm),
            ) {
                reasons.forEachIndexed { index, reason ->
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { updated ->
                            reasons = reasons.toMutableList().also { it[index] = updated.take(60) }
                        },
                        label = { Text(stringResource(R.string.reason_slot, index + 1)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(BuchouRadius.control),
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { Button(onClick = { onConfirm(reasons) }) { Text(stringResource(R.string.save)) } },
    )
}

@Composable
private fun SyncDialog(
    syncState: SyncState,
    onSaveConfig: (WebDavConfig) -> Unit,
    onUpload: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    var url by rememberSaveable { mutableStateOf(syncState.config.url) }
    var username by rememberSaveable { mutableStateOf(syncState.config.username) }
    var password by rememberSaveable { mutableStateOf(syncState.config.password) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sync_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
            ) {
                Text(stringResource(R.string.sync_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.sync_url)) },
                    singleLine = true,
                    shape = RoundedCornerShape(BuchouRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.sync_username)) },
                    singleLine = true,
                    shape = RoundedCornerShape(BuchouRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.sync_password)) },
                    singleLine = true,
                    shape = RoundedCornerShape(BuchouRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Spacer(Modifier.height(BuchouSpacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
                    OutlinedButton(onClick = { onSaveConfig(WebDavConfig(url, username, password)); onUpload() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(BuchouRadius.control)) { Text(stringResource(R.string.sync_upload)) }
                    OutlinedButton(onClick = { onSaveConfig(WebDavConfig(url, username, password)); onDownload() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(BuchouRadius.control)) { Text(stringResource(R.string.sync_download)) }
                }
                if (syncState.isSyncing) {
                    Text(stringResource(R.string.sync_in_progress), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else if (syncState.lastError != null) {
                    Text(syncState.lastError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else if (syncState.lastSyncEpochMillis != null) {
                    Text(stringResource(R.string.sync_success), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { Button(onClick = { onSaveConfig(WebDavConfig(url, username, password)); onDismiss() }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LanguageDialog(
    language: AppLanguage,
    onSetLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.setting_language)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
            ) {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = BuchouSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.sm),
                    ) {
                        Text(when (lang) { AppLanguage.System -> stringResource(R.string.theme_system); AppLanguage.Chinese -> stringResource(R.string.lang_chinese); AppLanguage.English -> stringResource(R.string.lang_english) }, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        BuchouSwitch(
                            checked = lang == language,
                            onCheckedChange = { if (it) { onSetLanguage(lang); onDismiss() } },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun CurrencyDialog(
    currencyCode: String,
    onSetCurrency: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val currencies = remember { listOf("🇨🇳 CNY" to "CNY", "🇺🇸 USD" to "USD", "🇪🇺 EUR" to "EUR", "🇯🇵 JPY" to "JPY", "🇬🇧 GBP" to "GBP", "🇭🇰 HKD" to "HKD") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
            ) {
                currencies.forEach { (label, code) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = BuchouSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.sm),
                    ) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        BuchouSwitch(
                            checked = code == currencyCode,
                            onCheckedChange = { if (it) onSetCurrency(code) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun AppearanceDialog(
    appTheme: AppTheme,
    onSetTheme: (AppTheme) -> Unit,
    homeModuleConfigs: List<HomeModuleConfig>,
    onHomeModuleVisible: (HomeModule, Boolean) -> Unit,
    onMoveHomeModule: (HomeModule, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
            ) {
                Text(stringResource(R.string.appearance_theme), style = MaterialTheme.typography.titleMedium)
                AppTheme.entries.forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = BuchouSpacing.xxs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.sm),
                    ) {
                        Text(stringResource(theme.labelRes), modifier = Modifier.weight(1f))
                        BuchouSwitch(
                            checked = theme == appTheme,
                            onCheckedChange = { if (it) onSetTheme(theme) },
                        )
                    }
                }
                Spacer(Modifier.height(BuchouSpacing.sm))
                Text(stringResource(R.string.appearance_home_modules), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.appearance_modules_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                homeModuleConfigs.forEachIndexed { index, config ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = BuchouSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.xs),
                    ) {
                        Text(stringResource(config.module.labelRes), modifier = Modifier.weight(1f))
                        TextButton(onClick = { onMoveHomeModule(config.module, -1) }, enabled = index > 0) { Text(stringResource(R.string.move_up)) }
                        TextButton(onClick = { onMoveHomeModule(config.module, 1) }, enabled = index < homeModuleConfigs.lastIndex) { Text(stringResource(R.string.move_down)) }
                        BuchouSwitch(config.visible, onCheckedChange = { onHomeModuleVisible(config.module, it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun WeekdayDialog(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weekday_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BuchouSpacing.sm)) {
                DayOfWeek.entries.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BuchouRadius.control))
                            .clickable { if (day !in selected || selected.size > 1) onToggle(day) }
                            .padding(vertical = BuchouSpacing.md, horizontal = BuchouSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${day.value}", modifier = Modifier.weight(1f))
                        BuchouSwitch(
                            checked = day in selected,
                            onCheckedChange = { if (day !in selected || selected.size > 1) onToggle(day) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun ProfileDialog(
    data: BuchouData,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int?, Double?) -> Unit,
) {
    val profile = checkNotNull(data.profile)
    var daily by rememberSaveable { mutableStateOf(profile.cigarettesPerDay.toString()) }
    var perPack by rememberSaveable { mutableStateOf(profile.cigarettesPerPack?.toString().orEmpty()) }
    var price by rememberSaveable { mutableStateOf(profile.pricePerPack?.toString().orEmpty()) }
    val valid = (daily.toIntOrNull() ?: 0) > 0 &&
        (perPack.isBlank() || (perPack.toIntOrNull() ?: 0) > 0) &&
        (price.isBlank() || (price.toDoubleOrNull() ?: -1.0) >= 0.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(BuchouSpacing.md)) {
                CompactNumberField(daily, { daily = it }, stringResource(R.string.profile_daily_label))
                CompactNumberField(perPack, { perPack = it }, stringResource(R.string.profile_per_pack_label))
                OutlinedTextField(
                    value = price,
                    onValueChange = { if (it.length <= 8 && it.count { char -> char == '.' } <= 1 && it.all { char -> char.isDigit() || char == '.' }) price = it },
                    label = { Text(stringResource(R.string.profile_price_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(BuchouRadius.control),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { Button(enabled = valid, onClick = { onConfirm(checkNotNull(daily.toIntOrNull()), perPack.toIntOrNull(), price.toDoubleOrNull()) }) { Text(stringResource(R.string.save)) } },
    )
}

@Composable
private fun CompactNumberField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) onValueChange(it) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(BuchouRadius.control),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DestructiveDialog(
    title: String,
    message: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        confirmButton = {
            Button(
                onClick = { onConfirm(); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(confirm) }
        },
    )
}

private fun weekdaySummary(days: Set<DayOfWeek>, context: Context): String = when {
    days.size == 7 -> context.getString(R.string.weekday_every_day)
    days == setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY) -> context.getString(R.string.weekday_workdays)
    else -> {
        val labels = context.resources.getStringArray(R.array.weekday_labels)
        days.sortedBy(DayOfWeek::getValue).joinToString(context.getString(R.string.weekday_separator)) { labels[it.value - 1] }
    }
}

private fun formatReminderTime(context: Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
}

private fun productCanScheduleExactAlarms(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

private fun productCanPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun productCanUseFullScreenIntent(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
        context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
