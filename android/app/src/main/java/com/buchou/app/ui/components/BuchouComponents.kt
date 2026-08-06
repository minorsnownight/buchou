package com.buchou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.buchou.app.R
import com.buchou.app.ui.theme.BuchouRadius
import com.buchou.app.ui.theme.BuchouSpacing

enum class BuchouTab(
    val labelRes: Int,
) {
    Home(R.string.tab_home),
    Records(R.string.tab_records),
    Settings(R.string.tab_settings),
}

@Composable
fun BuchouScaffold(
    selectedTab: BuchouTab,
    onTabSelected: (BuchouTab) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BuchouTabBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
        },
        content = content,
    )
}

@Composable
fun BuchouTabBar(
    selectedTab: BuchouTab,
    onTabSelected: (BuchouTab) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(64.dp)
                .padding(horizontal = BuchouSpacing.sm, vertical = BuchouSpacing.xs),
        ) {
            BuchouTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = androidx.compose.ui.semantics.Role.Tab,
                        ) {
                            onTabSelected(tab)
                        }
                        .padding(vertical = BuchouSpacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .animateContentSize(animationSpec = tween(200))
                            .background(
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                                shape = RoundedCornerShape(BuchouRadius.full),
                            )
                            .padding(horizontal = BuchouSpacing.xl, vertical = BuchouSpacing.sm),
                    ) {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val headerModifier = if (onLongClick != null) {
        modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
            onLongClick = onLongClick,
        )
    } else modifier
    Column(modifier = headerModifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (subtitle != null) {
            Spacer(Modifier.height(BuchouSpacing.sm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun BuchouCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(BuchouSpacing.lg),
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(BuchouRadius.card),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsGroup(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = BuchouSpacing.xs),
        )
        Spacer(Modifier.height(BuchouSpacing.sm))
        BuchouCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Column { content() }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
    showDivider: Boolean = true,
) {
    val textColor = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(56.dp)
            .padding(horizontal = BuchouSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BuchouSpacing.md),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = textColor)
            if (subtitle != null) {
                Spacer(Modifier.height(BuchouSpacing.xxs))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = BuchouSpacing.lg),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
fun BuchouSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
            disabledCheckedThumbColor = MaterialTheme.colorScheme.surface,
            disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
            disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
        ),
    )
}
