package com.saurabh.focusapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saurabh.focusapp.data.db.AnalyticsDao
import com.saurabh.focusapp.data.db.AppUsageStats
import com.saurabh.focusapp.data.db.UsageSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class AnalyticsUiState(
    val usageStats: List<AppUsageStats> = emptyList(),
    val recentSessions: List<UsageSession> = emptyList(),
    val topApp: AppUsageStats? = null,
    val totalTodayMs: Long = 0L,
    val sessionCountToday: Int = 0,
    val avgSessionMs: Long = 0L,
    val weeklyData: List<Long> = List(7) { 0L }
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsDao: AnalyticsDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        observeAggregateStats()
        observeRecentSessions()
        observeToday()
        observeWeekly()
    }

    private fun observeAggregateStats() {
        analyticsDao.getAggregateUsage()
            .onEach { stats ->
                _uiState.update {
                    it.copy(
                        usageStats = stats,
                        topApp = stats.firstOrNull()  // already sorted DESC by totalDuration
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeRecentSessions() {
        analyticsDao.getAllSessions()
            .onEach { sessions ->
                _uiState.update { it.copy(recentSessions = sessions.take(10)) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeToday() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        analyticsDao.getSessionsAfter(startOfDay)
            .onEach { sessions ->
                val total = sessions.sumOf { it.durationMs }
                val count = sessions.size
                val avg = if (count > 0) total / count else 0L
                _uiState.update {
                    it.copy(
                        totalTodayMs = total,
                        sessionCountToday = count,
                        avgSessionMs = avg
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeWeekly() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val weekStart = cal.timeInMillis

        analyticsDao.getSessionsAfter(weekStart)
            .onEach { sessions ->
                val dayTotals = MutableList(7) { 0L }
                sessions.forEach { session ->
                    cal.timeInMillis = session.startTime
                    val dow = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    if (dow in 0..6) dayTotals[dow] += session.durationMs
                }
                _uiState.update { it.copy(weeklyData = dayTotals) }
            }
            .launchIn(viewModelScope)
    }
}