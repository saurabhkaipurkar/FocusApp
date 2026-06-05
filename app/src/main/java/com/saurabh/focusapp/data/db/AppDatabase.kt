package com.saurabh.focusapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UsageSession::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun analyticsDao(): AnalyticsDao
}
