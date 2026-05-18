package com.phonecall.dialcontacts.calldialer.repository

import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import com.phonecall.dialcontacts.calldialer.models.CallLogEntry
import com.phonecall.dialcontacts.calldialer.models.ContactCacheData
import com.phonecall.dialcontacts.calldialer.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class RecentRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun getCallHistory(
        offset: Int,
        limit: Int
    ): ArrayList<CallLogEntry> {

        val list = ArrayList<CallLogEntry>()

        val contentResolver = context.contentResolver
        var cursor: Cursor? = null

        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                "subscription_id",
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                "name",
                "photo_uri"
            )

            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return list

            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val subscriptionIdIdx = cursor.getColumnIndex("subscription_id")

            val entriesToProcess = mutableListOf<Triple<Long, String, Int>>()
            if (cursor.moveToPosition(offset)) {
                var count = 0
                do {
                    if (count >= limit) break
                    val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                    val number = if (numberIdx != -1) cursor.getString(numberIdx) else ""
                    entriesToProcess.add(Triple(id, number, cursor.position))
                    count++
                } while (cursor.moveToNext())
            }

            if (entriesToProcess.isEmpty()) return list

            val uniqueNumbers =
                entriesToProcess.map { it.second }.filter { it.isNotEmpty() }.toSet()
            val contactCache = HashMap<String, ContactCacheData>()

            if (uniqueNumbers.isNotEmpty()) {
                try {
                    // Optimized: query only the unique numbers found in this page
                    val selection = uniqueNumbers.joinToString(",") { "'$it'" }
                    contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                        ),
                        "${ContactsContract.CommonDataKinds.Phone.NUMBER} IN ($selection) OR ${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} IN ($selection)",
                        null,
                        null
                    )?.use { contactsCursor ->
                        val numIdx =
                            contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val nameIdx2 =
                            contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val photoIdx2 =
                            contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                        val idIdx2 =
                            contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                        val normIdx =
                            contactsCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)

                        while (contactsCursor.moveToNext()) {
                            val data = ContactCacheData().apply {
                                name = contactsCursor.getString(nameIdx2)
                                photoUri = contactsCursor.getString(photoIdx2)
                                contactId = contactsCursor.getString(idIdx2)
                            }
                            contactsCursor.getString(numIdx)
                                ?.let { contactCache[Common.cleanNumber(it)] = data }
                            contactsCursor.getString(normIdx)?.let { contactCache[it] = data }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            for (item in entriesToProcess) {
                cursor.moveToPosition(item.third)
                try {
                    val number = item.second
                    val type = if (typeIdx != -1) cursor.getString(typeIdx) else "3"
                    val dateStr =
                        if (dateIdx != -1) cursor.getString(dateIdx) else System.currentTimeMillis()
                            .toString()
                    val duration = if (durationIdx != -1) cursor.getString(durationIdx) else "0"
                    val simId = if (subscriptionIdIdx != -1) cursor.getInt(subscriptionIdIdx) else -1

                    val normNum = Common.cleanNumber(number)
                    val cacheData = contactCache[normNum] ?: ContactCacheData()

                    val date = Date(dateStr.toLong())
                    val rawType = type.toInt()
                    val callType = Common.getCallType(context,rawType)
                    val id = item.first

                    list.add(
                        CallLogEntry(
                            stringNumber = number,
                            stringType = callType,
                            dateData = date,
                            stringDuration = duration,
                            stringCallName = cacheData.name,
                            stringDateCategory = dateStr,
                            stringPhotoUri = cacheData.photoUri,
                            contactId = cacheData.contactId,
                            isBlocked = false, // Will be set in ViewModel
                            intType = rawType,
                            simId = simId
                        ).apply {
                            resetCallIds(id)
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        } catch (_: SecurityException) {
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }

        return list
    }

    fun deleteCallHistory(callIds: List<Long>) {
        if (callIds.isEmpty()) return
        try {
            val selection = "${CallLog.Calls._ID} IN (${callIds.joinToString(",")})"
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, selection, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteAllCallHistory() {
        try {
            context.contentResolver.delete(CallLog.Calls.CONTENT_URI, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}