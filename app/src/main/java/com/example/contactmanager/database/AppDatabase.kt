package com.example.contactmanager.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.models.TagModel

@Database(entities = [TagModel::class, BlockModel::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
}
