package com.saurabh.skipad.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalyticsDao {
    @Insert
    suspend fun insertSession(session: UsageSession)

    @Query("SELECT * FROM usage_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<UsageSession>>

    @Query("SELECT packageName, appName, SUM(durationMs) as totalDuration FROM usage_sessions GROUP BY packageName")
    fun getAggregateUsage(): Flow<List<AppUsageStats>>
}

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val totalDuration: Long
)
