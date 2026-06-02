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

    @Query(
        """
        SELECT packageName, appName, SUM(durationMs) as totalDuration 
        FROM usage_sessions 
        GROUP BY packageName 
        ORDER BY totalDuration DESC
    """
    )
    fun getAggregateUsage(): Flow<List<AppUsageStats>>

    // ── Naya: date filter ke saath sessions ──
    @Query("SELECT * FROM usage_sessions WHERE startTime >= :fromMs ORDER BY startTime DESC")
    fun getSessionsAfter(fromMs: Long): Flow<List<UsageSession>>

    @Query("DELETE FROM usage_sessions WHERE packageName = :packageName")
    suspend fun deleteSessionsForApp(packageName: String)

    @Query("DELETE FROM usage_sessions")
    suspend fun clearAll()
}

data class AppUsageStats(
    val packageName: String,
    val appName: String,
    val totalDuration: Long
)