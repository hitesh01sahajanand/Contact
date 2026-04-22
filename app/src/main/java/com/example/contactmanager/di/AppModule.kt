package com.example.contactmanager.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import androidx.room.Room
import com.example.contactmanager.database.AppDatabase
import com.example.contactmanager.database.BlockDao
import com.example.contactmanager.database.TagDao
import com.example.contactmanager.utils.Constance

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
}