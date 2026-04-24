package com.example.contactmanager.repository

import com.example.contactmanager.database.QuickResponseDao
import com.example.contactmanager.models.QuickResponseModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class QuickResponseRepository @Inject constructor(
    private val quickResponseDao: QuickResponseDao
) {
    fun getAllMessages(): Flow<List<QuickResponseModel>> = quickResponseDao.getAllMessages()

    suspend fun insertMessage(message: String) {
        quickResponseDao.insertMessage(QuickResponseModel(message = message))
    }

    suspend fun insertInitialMessages(messages: List<String>) {
        if (quickResponseDao.getMessageCount() == 0) {
            val models = messages.map { QuickResponseModel(message = it) }
            quickResponseDao.insertMessages(models)
        }
    }

    suspend fun deleteMessageById(id: Int) {
        quickResponseDao.deleteMessageById(id)
    }
}
