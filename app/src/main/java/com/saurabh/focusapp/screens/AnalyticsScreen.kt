package com.saurabh.focusapp.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saurabh.focusapp.BuildConfig
import com.saurabh.focusapp.data.db.AppUsageStats
import com.saurabh.focusapp.data.db.UsageSession
import com.saurabh.focusapp.ui.component.AppIcon
import com.saurabh.focusapp.viewmodel.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * AnalyticsScreen — visual layer only.
 * All ViewModel reads (state.totalTodayMs, sessionCountToday, avgSessionMs,
 * weeklyData, topApp, recentSessions, usageStats) and derived values
 * (maxDuration) are unchanged.
 */

private object AnalyticsPalette {
    val Accent = Color(0xFFE8A33D)
    val AccentSoft = Color(0xFFFCE9C7)
    val OnAccent = Color(0xFF241A05)
    val InkSurface = Color(0xFF14161A)
    val InkSurfaceAlt = Color(0xFF1D2026)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    analyticsViewModel: AnalyticsViewModel = hiltViewModel()
) {
    if (BuildConfig.DEBUG) {
        SideEffect { Log.d("AnalyticsScreen", "AnalyticsScreen recomposed") }
    }

    val state by analyticsViewModel.uiState.collectAsStateWithLifecycle()

    val maxDuration = remember(state.usageStats) {
        state.usageStats.maxOfOrNull { it.totalDuration } ?: 1L
    }

    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Focus session insights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Summary hero ──
            item {
                SummaryHero(
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
// SUMMARY HERO — dark gradient card, three metrics
// ══════════════════════════════════════════════════════

@Composable
private fun SummaryHero(totalMs: Long, sessionCount: Int, avgMs: Long) {
    val totalFormatted = remember(totalMs) { totalMs.toReadable() }
    val avgFormatted = remember(avgMs) { avgMs.toReadable() }

    val bgBrush = remember {
        Brush.linearGradient(
            listOf(
                AnalyticsPalette.InkSurface,
                AnalyticsPalette.InkSurfaceAlt
            )
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier
            .background(bgBrush)
            .padding(20.dp)) {
            Column {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    letterSpacing = 1.sp
                )
                Text(
                    text = totalFormatted,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    MiniMetric(label = "Sessions", value = "$sessionCount")
                    MiniMetric(label = "Avg session", value = avgFormatted)
                }
            }
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = AnalyticsPalette.Accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f)
        )
    }
}

// ══════════════════════════════════════════════════════
// WEEKLY CHART
// ══════════════════════════════════════════════════════

@Composable
private fun WeeklyChart(weekData: List<Long>) {
    val days = remember { listOf("M", "T", "W", "T", "F", "S", "S") }
    val maxVal = remember(weekData) { max(weekData.maxOrNull() ?: 1L, 1L) }
    val todayIndex =
        remember { (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7 }
    val peakFormatted = remember(maxVal) { maxVal.toReadable() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weekData.forEachIndexed { index, ms ->
                    val fraction = if (maxVal > 0) ms.toFloat() / maxVal else 0f
                    val isToday = index == todayIndex
                    val isPeak = ms == maxVal && ms > 0

                    val barBrush = when {
                        isPeak -> Brush.verticalGradient(
                            listOf(
                                AnalyticsPalette.Accent,
                                AnalyticsPalette.Accent.copy(alpha = 0.7f)
                            )
                        )

                        isToday -> Brush.verticalGradient(
                            listOf(
                                AnalyticsPalette.Accent.copy(alpha = 0.55f),
                                AnalyticsPalette.Accent.copy(alpha = 0.35f)
                            )
                        )

                        else -> Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            )
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(barBrush)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(if (isToday) AnalyticsPalette.AccentSoft else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            modifier = Modifier.padding(vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isToday) AnalyticsPalette.OnAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(10.dp))
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
                    "Peak · $peakFormatted",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AnalyticsPalette.Accent
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
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        color = AnalyticsPalette.AccentSoft.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, AnalyticsPalette.Accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        AnalyticsPalette.Accent.copy(alpha = 0.6f),
                        RoundedCornerShape(15.dp)
                    )
            ) {
                AppIcon(
                    packageName = stat.packageName,
                    drawable = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEvents,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AnalyticsPalette.OnAccent
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Top focus app",
                        style = MaterialTheme.typography.labelSmall,
                        color = AnalyticsPalette.OnAccent.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = stat.appName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = AnalyticsPalette.OnAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AnalyticsPalette.OnAccent
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
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = timeAgoFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AnalyticsPalette.AccentSoft
            ) {
                Text(
                    text = durationFormatted,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AnalyticsPalette.OnAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
    val fraction = remember(stat.totalDuration, maxDuration) {
        if (maxDuration > 0) stat.totalDuration.toFloat() / maxDuration else 0f
    }
    val durationFormatted = remember(stat.totalDuration) { stat.totalDuration.toReadable() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(AnalyticsPalette.Accent)
                    )
                }
            }
            Text(
                text = durationFormatted,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
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
            .padding(vertical = 72.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "No data yet",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 10.dp)
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