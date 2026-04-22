package com.example.contactmanager.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.contactmanager.models.BlockModel
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockNumber(blockModel: BlockModel)

    @Delete
    suspend fun unblockNumber(blockModel: BlockModel)

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE phoneNumber = :number)")
    suspend fun isBlocked(number: String): Boolean

    @Query("SELECT * FROM blocked_numbers")
    fun getAllBlockedNumbers(): Flow<List<BlockModel>>

    @Query("DELETE FROM blocked_numbers WHERE phoneNumber = :number")
    suspend fun deleteByNumber(number: String)
}
