package com.example.contactmanager.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.contactmanager.models.TagModel
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagModel)

    @Query("SELECT * FROM tags WHERE phoneNumber = :number")
    suspend fun getTagByNumber(number: String): TagModel?

    @Query("SELECT * FROM tags")
    fun getAllTags(): Flow<List<TagModel>>

    @Query("DELETE FROM tags WHERE phoneNumber = :number")
    suspend fun deleteTag(number: String)
}
