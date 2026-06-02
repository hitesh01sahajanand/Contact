package com.phonecall.dialcontacts.calldialer.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickResponseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(quickResponse: QuickResponseModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessages(quickResponses: List<QuickResponseModel>)

    @Query("SELECT * FROM quick_response_table ORDER BY id ASC")
    fun getAllMessages(): Flow<List<QuickResponseModel>>

    @Query("SELECT * FROM quick_response_table ORDER BY id ASC")
    suspend fun getAllMessagesSync(): List<QuickResponseModel>

    @Query("DELETE FROM quick_response_table WHERE id = :id")
    suspend fun deleteMessageById(id: Int)
}
