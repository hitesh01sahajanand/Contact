package com.example.contactmanager.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.models.ContactCacheData
import com.example.contactmanager.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class RecentRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /*fun getRecentList(): List<CallLogModel> {

        val map = linkedMapOf<String, CallLogModel>()
        val phoneUtil = PhoneNumberUtil.getInstance()

        try {

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {

                    var number = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    ) ?: continue

                    val name = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    )

                    val type = it.getInt(
                        it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    )

                    val date = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    )

                    val duration = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    )

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
                    )

                    val isMissed = type == CallLog.Calls.MISSED_TYPE
                    val isIncoming = type == CallLog.Calls.INCOMING_TYPE
                    val isOutgoing = type == CallLog.Calls.OUTGOING_TYPE
                    val isRejected = type == CallLog.Calls.REJECTED_TYPE

                    val nameTemp = if (name.isNullOrEmpty()) number else name
                    val imageAvatar = Common.generateAvatar(nameTemp)

                    // 🔥 Normalize number (global)
                    var normalizedNumber: String? = null

                    try {
                        val parsed = phoneUtil.parse(number, null)
                        normalizedNumber = phoneUtil.format(
                            parsed,
                            PhoneNumberUtil.PhoneNumberFormat.E164
                        )
                    } catch (e: Exception) {
                        normalizedNumber = number.replace("\\D".toRegex(), "")
                    }

                    if (normalizedNumber.isNullOrEmpty()) continue

                    // 🔥 GROUPING LOGIC
                    if (map.containsKey(normalizedNumber)) {

                        val existing = map[normalizedNumber]!!

                        // count++
                        existing.callCount += 1

                        // latest call update (important 🔥)
                        if (date > existing.date) {
                            existing.date = date
                            existing.type = type
                            existing.duration = duration
                            existing.isMissed = isMissed
                            existing.isIncoming = isIncoming
                            existing.isOutgoing = isOutgoing
                            existing.isRejected = isRejected
                        }

                    } else {

                        map[normalizedNumber] = CallLogModel(
                            name = name,
                            number = normalizedNumber,
                            type = type,
                            date = date,
                            duration = duration,
                            isMissed = isMissed,
                            isIncoming = isIncoming,
                            isOutgoing = isOutgoing,
                            photoUri = photoUri,
                            callCount = 1,
                            avatar = imageAvatar,
                            isRejected = isRejected
                        )
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("TAG", "getRecentList: ${e.message}")
        }

        // 🔥 FINAL SORT (latest first)
        return map.values.sortedByDescending { it.date }
    }



    fun getMissedCalls(): List<CallLogModel> {

        val callList = mutableListOf<CallLogModel>()

        try {

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                "${CallLog.Calls.TYPE} = ?", // 👈 filter only missed
                arrayOf(CallLog.Calls.MISSED_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {

                    val number = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    )

                    val name = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                    )

                    val type = it.getInt(
                        it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                    )

                    val date = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                    )

                    val duration = it.getLong(
                        it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                    )

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
                    )
                    val nameTemp = if (name.isNullOrEmpty()) number else name
                    val imageAvatar = Common.generateAvatar(nameTemp)

                    val isIncoming = type == CallLog.Calls.INCOMING_TYPE
                    val isOutgoing = type == CallLog.Calls.OUTGOING_TYPE
                    val isMissed = type == CallLog.Calls.MISSED_TYPE
                    val isRejected = type == CallLog.Calls.REJECTED_TYPE

                    callList.add(
                        CallLogModel(
                            name = name,
                            number = number,
                            type = type,
                            date = date,
                            duration = duration,
                            isMissed = isMissed,
                            isIncoming = isIncoming,
                            isOutgoing = isOutgoing,
                            photoUri = photoUri,
                            callCount = 1,
                            avatar = imageAvatar,
                            isRejected = isRejected
                        )
                    )
                }
            }

        } catch (e: Exception) {
            Log.e("TAG", "getMissedCalls: ${e.message}")
        }
        return callList
    }*/


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
            val nameIdx = cursor.getColumnIndex("name")
            val photoIdx = cursor.getColumnIndex("photo_uri")

            val contactCache = HashMap<String, ContactCacheData>()

            try {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                    ),
                    null,
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

                        val number = contactsCursor.getString(numIdx)
                        val normalized = contactsCursor.getString(normIdx)

                        number?.replace(Regex("[^0-9+]"), "")?.let {
                            contactCache[it] = data
                        }

                        normalized?.let {
                            contactCache[it] = data
                        }
                    }
                }
            } catch (_: Exception) {
            }

            val blockedNumbers = HashSet<String>()
            val blockedSuffixes = HashSet<String>()

            try {
                contentResolver.query(
                    BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                    arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                    null,
                    null,
                    null
                )?.use { blockedCursor ->

                    val idx = blockedCursor.getColumnIndex(
                        BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER
                    )

                    while (blockedCursor.moveToNext()) {
                        val bNum = blockedCursor.getString(idx) ?: continue
                        val clean = bNum.replace(Regex("[^0-9]"), "")

                        blockedNumbers.add(clean)

                        val suffix = when {
                            clean.length >= 7 -> clean.takeLast(7)
                            clean.isNotEmpty() -> clean
                            else -> null
                        }

                        suffix?.let { blockedSuffixes.add(it) }
                    }
                }
            } catch (_: Exception) {
            }

            // Pagination
            if (cursor.moveToPosition(offset)) {
                var processed = 0

                do {
                    if (processed >= limit) break
                    processed++

                    try {
                        val number = if (numberIdx != -1) cursor.getString(numberIdx) else ""
                        val type = if (typeIdx != -1) cursor.getString(typeIdx) else "3"
                        val dateStr =
                            if (dateIdx != -1) cursor.getString(dateIdx) else System.currentTimeMillis()
                                .toString()
                        val duration = if (durationIdx != -1) cursor.getString(durationIdx) else "0"

                        val normNum = Common.cleanNumber(number)
                        var cacheData = contactCache[normNum]

                        if (cacheData == null) {
                            cacheData = ContactCacheData()
                        }

                        val date = Date(dateStr.toLong())
                        val rawType = type.toInt()
                        val callType = Common.getCallType(rawType)
                        val id = if (idIdx != -1) cursor.getLong(idIdx) else 0L

                        var isBlocked = false

                        if (normNum.isNotEmpty()) {
                            val suffix = if (normNum.length >= 7) normNum.takeLast(7) else normNum

                            if (blockedSuffixes.contains(suffix)) {
                                for (b in blockedNumbers) {
                                    if (Common.compareNumbers(normNum, b)) {
                                        isBlocked = true
                                        break
                                    }
                                }
                            }
                        }

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
                                isBlocked = isBlocked,
                                intType = rawType
                            ).apply {
                                resetCallIds(id)
                            }
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                } while (cursor.moveToNext())
            }

        } catch (_: SecurityException) {
        } catch (_: Exception) {
        } finally {
            cursor?.close()
        }

        return list
    }

}