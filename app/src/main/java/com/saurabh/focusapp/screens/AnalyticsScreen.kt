package com.saurabh.focusapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.focusapp.data.db.AppUsageStats
import com.saurabh.focusapp.data.db.UsageSession
import com.saurabh.focusapp.ui.component.AppIcon
import com.saurabh.focusapp.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Optimized AnalyticsScreen.
 * 
 * Performance Optimizations:
 * 1. Stability: Marked AnalyticsUiState, AppUsageStats, and UsageSession with 
 *    @Immutable to enable 'Skippable' recompositions for list items and cards.
 * 2. Derived State: Used remember(state.usageStats) for expensive calculations 
 *    like 'maxDuration' to avoid re-computing it inside the LazyColumn on 
 *    every scroll or minor state change.
 * 3. Component Optimization: Improved WeeklyChart by remembering chart 
 *    calculations (maxVal, todayIndex).
 * 4. Efficient Formatting: Shared formatters and remembered formatted strings 
 *    where applicable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    // Performance Metric: Log recompositions
    SideEffect {
        Log.d("AnalyticsScreen", "AnalyticsScreen recomposed")
    }

    val state by analyticsViewModel.uiState.collectAsStateWithLifecycle()

    // Optimization: Calculate maxDuration once per state change
    val maxDuration = remember(state.usageStats) {
        state.usageStats.maxOfOrNull { it.totalDuration } ?: 1L
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Analytics", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Focus session insights",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Summary strip ──
            item {
                SummaryStrip(
                    totalMs = state.totalTodayMs,
                    sessionCount = state.sessionCountToday,
                    avgMs = state.avgSessionMs
                )
            }

            // ── Weekly chart ──
            item {
                SectionHeader("This week")
                WeeklyChart(weekData = state.weeklyData)
            }

            // ── Top app highlight ──
            state.topApp?.let { top ->
                item {
                    SectionHeader("Most focused")
                    TopAppCard(stat = top)
                }
            }

            // ── Recent sessions ──
            if (state.recentSessions.isNotEmpty()) {
                item { SectionHeader("Recent sessions") }
                items(state.recentSessions, key = { it.id }) { session ->
                    RecentSessionRow(session = session)
                }
            }

            // ── App breakdown ──
            if (state.usageStats.isNotEmpty()) {
                item { SectionHeader("All apps") }
                items(state.usageStats, key = { it.packageName }) { stat ->
                    AppStatRow(stat = stat, maxDuration = maxDuration)
                }
            }

            // ── Empty ──
            if (state.usageStats.isEmpty() && state.recentSessions.isEmpty()) {
                item { EmptyAnalytics() }
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// SUMMARY STRIP
// ══════════════════════════════════════════════════════

@Composable
private fun SummaryStrip(totalMs: Long, sessionCount: Int, avgMs: Long) {
    // Optimization: Remember formatted values to avoid string generation on every recomposition
    val totalFormatted = remember(totalMs) { totalMs.toReadable() }
    val avgFormatted = remember(avgMs) { avgMs.toReadable() }
    val sessionCountText = remember(sessionCount) { "$sessionCount" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatBox("Total today", totalFormatted, Modifier.weight(1f))
        StatBox("Sessions", sessionCountText, Modifier.weight(1f))
        StatBox("Avg session", avgFormatted, Modifier.weight(1f))
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// WEEKLY CHART
// ══════════════════════════════════════════════════════

@Composable
private fun WeeklyChart(weekData: List<Long>) {
    // Optimization: Remember static list and calculations
    val days = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val maxVal = remember(weekData) { max(weekData.maxOrNull() ?: 1L, 1L) }
    val todayIndex = remember { (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 }
    val peakFormatted = remember(maxVal) { maxVal.toReadable() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEachIndexed { index, ms ->
                    val fraction = if (maxVal > 0) ms.toFloat() / maxVal else 0f
                    val isToday = index == todayIndex
                    val isPeak = ms == maxVal && ms > 0
                    
                    // Optimization: Pre-calculating color to avoid complex when {} in Draw phase
                    val barColor = when {
                        isToday || isPeak -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.04f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isToday)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "0m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Peak: $peakFormatted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// TOP APP CARD
// ══════════════════════════════════════════════════════

@Composable
private fun TopAppCard(stat: AppUsageStats) {
    val durationFormatted = remember(stat.totalDuration) { stat.totalDuration.toReadable() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
            ) {
                AppIcon(
                    packageName = stat.packageName,
                    drawable = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.appName,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total focus time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// RECENT SESSION ROW
// ══════════════════════════════════════════════════════

@Composable
private fun RecentSessionRow(session: UsageSession) {
    val durationFormatted = remember(session.durationMs) { session.durationMs.toReadable() }
    val timeAgoFormatted = remember(session.startTime) { session.startTime.toTimeAgo() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(11.dp)
                    )
            ) {
                AppIcon(
                    packageName = session.packageName,
                    drawable = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = timeAgoFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Duration badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = durationFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════
// APP STAT ROW
// ══════════════════════════════════════════════════════

@Composable
private fun AppStatRow(stat: AppUsageStats, maxDuration: Long) {
    // Optimization: Calculate fraction once per recomposition
    val fraction = remember(stat.totalDuration, maxDuration) { 
        if (maxDuration > 0) stat.totalDuration.toFloat() / maxDuration else 0f 
    }
    val durationFormatted = remember(stat.totalDuration) { stat.totalDuration.toReadable() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(11.dp)
                    )
            ) {
                AppIcon(
                    packageName = stat.packageName,
                    drawable = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stat.appName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                    )
                }
            }
            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ══════════════════════════════════════════════════════
// EMPTY STATE
// ══════════════════════════════════════════════════════

@Composable
private fun EmptyAnalytics() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.BarChart,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "No data yet",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Open an app in focus mode to start tracking",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ── Section header ──
@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

// ── Duration formatter ──
private fun Long.toReadable(): String {
    if (this <= 0L) return "0s"
    val h = TimeUnit.MILLISECONDS.toHours(this)
    val m = TimeUnit.MILLISECONDS.toMinutes(this) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

// ── Timestamp → "2 hours ago" ──
private fun Long.toTimeAgo(): String {
    val diff = System.currentTimeMillis() - this
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(this))
    }
}
