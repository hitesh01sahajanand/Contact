package com.phonecall.dialcontacts.calldialer.repository

import com.phonecall.dialcontacts.calldialer.database.TagDao
import com.phonecall.dialcontacts.calldialer.models.TagModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    suspend fun saveTag(phoneNumber: String, tagName: String) {
        tagDao.insertTag(TagModel(phoneNumber, tagName))
    }

    suspend fun getTag(phoneNumber: String): String? {
        return tagDao.getTagByNumber(phoneNumber)?.tagName
    }

    fun getAllTags(): Flow<List<TagModel>> {
        return tagDao.getAllTags()
    }

    suspend fun deleteTag(phoneNumber: String) {
        tagDao.deleteTag(phoneNumber)
    }
}
