package com.phonecall.dialcontacts.calldialer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phonecall.dialcontacts.calldialer.models.BlockModel
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.models.ReminderModel
import com.phonecall.dialcontacts.calldialer.models.SpeedDialModel
import com.phonecall.dialcontacts.calldialer.models.TagModel

@Database(entities = [TagModel::class, BlockModel::class, SpeedDialModel::class, QuickResponseModel::class, ReminderModel::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
    abstract fun speedDialDao(): SpeedDialDao
    abstract fun quickResponseDao(): QuickResponseDao
    abstract fun reminderDao(): ReminderDao
}
