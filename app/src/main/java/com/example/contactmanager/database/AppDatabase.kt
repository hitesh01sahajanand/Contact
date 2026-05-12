package com.example.contactmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.models.SpeedDialModel
import com.example.contactmanager.models.TagModel
import com.example.contactmanager.models.QuickResponseModel

import com.example.contactmanager.models.ReminderModel
import com.example.contactmanager.database.ReminderDao

@Database(entities = [TagModel::class, BlockModel::class, SpeedDialModel::class, QuickResponseModel::class, ReminderModel::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun quickResponseDao(): QuickResponseDao
    abstract fun reminderDao(): ReminderDao
}
