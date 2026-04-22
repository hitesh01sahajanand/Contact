package com.example.contactmanager.repository

import android.telephony.PhoneNumberUtils
import android.util.Log
import com.example.contactmanager.database.BlockDao
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.utils.Common
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val blockDao: BlockDao
) {
    suspend fun blockNumber(number: String) {
        val clean = Common.cleanNumber(number)
        blockDao.blockNumber(BlockModel(clean))
    }

    suspend fun unblockNumber(number: String) {
        val clean = Common.cleanNumber(number)
        blockDao.deleteByNumber(clean)
    }

    suspend fun isBlocked(number: String): Boolean {
        val blockedList = blockDao.getAllBlockedNumbers().first()
        val isBlocked = blockedList.any { blocked ->
            PhoneNumberUtils.compare(blocked.phoneNumber, number) || 
            Common.cleanNumber(blocked.phoneNumber).takeLast(10) == Common.cleanNumber(number).takeLast(10)
        }
        Log.d("BlockRepository", "Checking number: $number, isBlocked: $isBlocked")
        return isBlocked
    }

    fun getAllBlockedNumbers(): Flow<List<BlockModel>> {
        return blockDao.getAllBlockedNumbers()
    }
}
