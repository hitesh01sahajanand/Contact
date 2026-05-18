package com.phonecall.dialcontacts.calldialer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val color: String,
    val reminder_date_time: Long,
    val reminder_done: Int = 0
)
