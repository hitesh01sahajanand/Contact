package com.phonecall.dialcontacts.calldialer.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import androidx.room.Room
import com.phonecall.dialcontacts.calldialer.database.AppDatabase
import com.phonecall.dialcontacts.calldialer.database.BlockDao
import com.phonecall.dialcontacts.calldialer.database.TagDao
import com.phonecall.dialcontacts.calldialer.utils.Constance
import com.phonecall.dialcontacts.calldialer.database.QuickResponseDao
import com.phonecall.dialcontacts.calldialer.database.ReminderDao
import com.phonecall.dialcontacts.calldialer.database.SpeedDialDao

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context, AppDatabase::class.java, Constance.DB_NAME
        ).fallbackToDestructiveMigration(true) // Added for database version upgrade
            .build()
    }

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    fun provideBlockDao(database: AppDatabase): BlockDao {
        return database.blockDao()
    }

    @Provides
    fun provideSpeedDialDao(database: AppDatabase): SpeedDialDao {
        return database.speedDialDao()
    }

    @Provides
    fun provideQuickResponseDao(database: AppDatabase): QuickResponseDao {
        return database.quickResponseDao()
    }

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao {
        return database.reminderDao()
    }
}