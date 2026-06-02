package com.phonecall.dialcontacts.calldialer.repository

import com.phonecall.dialcontacts.calldialer.database.QuickResponseDao
import com.phonecall.dialcontacts.calldialer.models.QuickResponseModel
import com.phonecall.dialcontacts.calldialer.utils.QuickResponseDefaults
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuickResponseRepository @Inject constructor(
    private val quickResponseDao: QuickResponseDao
) {
    fun getAllMessages(): Flow<List<QuickResponseModel>> = quickResponseDao.getAllMessages()

    suspend fun insertMessage(message: String) {
        val existing = quickResponseDao.getAllMessagesSync()
        if (existing.isEmpty()) {
            // Keep 4 placeholder rows so custom messages stay after the default slots.
            val placeholders = List(QuickResponseDefaults.DEFAULT_MESSAGE_COUNT) {
                QuickResponseModel(message = "")
            }
            quickResponseDao.insertMessages(placeholders)
        }
        quickResponseDao.insertMessage(QuickResponseModel(message = message))
    }

    suspend fun deleteMessageById(id: Int) {
        quickResponseDao.deleteMessageById(id)
    }
}
