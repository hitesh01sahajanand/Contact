package com.example.contactmanager.repository

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.provider.BlockedNumberContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import com.example.contactmanager.database.BlockDao
import com.example.contactmanager.models.BlockModel
import com.example.contactmanager.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepository @Inject constructor(
    private val blockDao: BlockDao,
    @param:ApplicationContext private val context: Context
) {
    suspend fun blockNumber(number: String) {
        val clean = Common.cleanNumber(number)
        
        // 1. Save to local database
        blockDao.blockNumber(BlockModel(clean))
        
        // 2. Save to system storage (if supported and permitted)
        try {
            if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                val values = ContentValues().apply {
                    put(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER, clean)
                }
                context.contentResolver.insert(BlockedNumberContract.BlockedNumbers.CONTENT_URI, values)
            }
        } catch (e: Exception) {
            Log.e("BlockRepository", "Failed to block number in system storage: ${e.message}")
        }
    }

    suspend fun unblockNumber(number: String) {
        val clean = Common.cleanNumber(number)
        
        // 1. Delete from local database
        blockDao.deleteByNumber(clean)
        
        // 2. Delete from system storage
        try {
            if (BlockedNumberContract.canCurrentUserBlockNumbers(context)) {
                context.contentResolver.delete(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                    arrayOf(clean)
                )
            }
        } catch (e: Exception) {
            Log.e("BlockRepository", "Failed to unblock number in system storage: ${e.message}")
        }
    }

    suspend fun isBlocked(number: String): Boolean {
        // First check system storage if available
        try {
            if (BlockedNumberContract.isBlocked(context, number)) {
                return true
            }
        } catch (e: Exception) {
            Log.e("BlockRepository", "Error checking system blocked numbers: ${e.message}")
        }

        // Fallback to local database
        val blockedList = blockDao.getAllBlockedNumbers().first()
        return blockedList.any { blocked ->
            PhoneNumberUtils.compare(blocked.phoneNumber, number) || 
            Common.cleanNumber(blocked.phoneNumber).takeLast(10) == Common.cleanNumber(number).takeLast(10)
        }
    }

    fun getAllBlockedNumbers(): Flow<List<BlockModel>> {
        return callbackFlow {
            val observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    launch {
                        send(fetchBlockedNumbers())
                    }
                }
            }

            context.contentResolver.registerContentObserver(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                true,
                observer
            )

            // Initial fetch
            launch {
                send(fetchBlockedNumbers())
            }

            awaitClose {
                context.contentResolver.unregisterContentObserver(observer)
            }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun fetchBlockedNumbers(): List<BlockModel> {
        val list = mutableListOf<BlockModel>()
        
        // 1. Fetch from system storage
        try {
            val cursor = context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null, null, null
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val number = it.getString(it.getColumnIndexOrThrow(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER))
                    val contact = Common.getContactByNumber(context, number)
                    list.add(
                        BlockModel(
                            phoneNumber = number,
                            name = contact?.displayName,
                            photoUri = contact?.userThumbnail
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("BlockRepository", "Error fetching system blocked numbers: ${e.message}")
        }

        // 2. Merge with local database to ensure all user-blocked numbers are captured
        try {
            val localList = blockDao.getAllBlockedNumbers().first()
            list.addAll(localList)
        } catch (e: Exception) {
            Log.e("BlockRepository", "Error fetching local blocked numbers: ${e.message}")
        }
        
        val uniqueList = list.distinctBy { it.phoneNumber }
        
        // Resolve contact details for any missing names/photos
        return uniqueList.map { item ->
            if (item.name == null) {
                val contact = Common.getContactByNumber(context, item.phoneNumber)
                item.copy(
                    name = contact?.displayName,
                    photoUri = contact?.userThumbnail
                )
            } else {
                item
            }
        }
    }
}
