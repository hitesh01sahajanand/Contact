package com.example.contactmanager.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.contactmanager.models.SpeedDialModel
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedDialDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeedDial(speedDial: SpeedDialModel)

    @Query("DELETE FROM speed_dial_table WHERE slot = :slot")
    suspend fun deleteSpeedDial(slot: Int)

    @Query("SELECT * FROM speed_dial_table ORDER BY slot ASC")
    fun getAllSpeedDials(): Flow<List<SpeedDialModel>>

    @Query("SELECT * FROM speed_dial_table WHERE slot = :slot LIMIT 1")
    suspend fun getSpeedDialBySlot(slot: Int): SpeedDialModel?
}
