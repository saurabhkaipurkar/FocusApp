package com.saurabh.skipad.di

import android.content.Context
import androidx.room.Room
import com.saurabh.skipad.data.db.AnalyticsDao
import com.saurabh.skipad.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "focus_app_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideAnalyticsDao(database: AppDatabase): AnalyticsDao {
        return database.analyticsDao()
    }
}
