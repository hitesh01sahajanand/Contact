package com.phonecall.dialcontacts.calldialer.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.phonecall.dialcontacts.calldialer.models.ReminderModel

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE reminder_done = 0 ORDER BY reminder_date_time ASC")
    fun getAllPendingReminders(): LiveData<List<ReminderModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderModel): Long

    @Update
    suspend fun updateReminder(reminder: ReminderModel)

    @Delete
    suspend fun deleteReminder(reminder: ReminderModel)

    @Query("UPDATE reminders SET reminder_done = 1 WHERE id = :id")
    suspend fun markAsDone(id: Int)
}
