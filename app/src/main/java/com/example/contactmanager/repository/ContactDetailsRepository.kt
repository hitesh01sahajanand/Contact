package com.example.contactmanager.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.contactmanager.models.CallLogEntry
import com.example.contactmanager.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class ContactDetailsRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    /*fun getCallHistoryByNumber(number: String): List<CallLogEntry> {

        val callList = mutableListOf<CallLogEntry>()

        val selection = "${CallLog.Calls.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$number%")

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                while (it.moveToNext()) {

                    val num = it.getString(
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

                    val isIncoming = type == CallLog.Calls.INCOMING_TYPE
                    val isOutgoing = type == CallLog.Calls.OUTGOING_TYPE
                    val isMissed = type == CallLog.Calls.MISSED_TYPE
                    val isRejected = type == CallLog.Calls.REJECTED_TYPE

                    callList.add(
                        CallLogEntry(

                        )
                    )
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return callList
    }*/


    fun getCallHistoryForNumber(
        number: String,
        offset: Int,
        limit: Int
    ): ArrayList<CallLogEntry> {

        val list = ArrayList<CallLogEntry>()
        val contentResolver = context.contentResolver

        val normalizedInput = number.replace(Regex("[^0-9]"), "")

        var cursor: Cursor? = null

        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.CACHED_PHOTO_URI
            )

            // ✅ IMPORTANT: Filter by number (LIKE for flexibility)
            val selection = "${CallLog.Calls.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$normalizedInput%")

            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return list

            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val photoIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_PHOTO_URI)

            if (cursor.moveToPosition(offset)) {
                var processed = 0

                do {
                    if (processed >= limit) break
                    processed++

                    val numberDb = cursor.getString(numberIdx)
                    val normDb = numberDb?.replace(Regex("[^0-9]"), "") ?: ""

                    // ✅ EXTRA SAFE: exact compare
                    if (!Common.compareNumbers(normDb, normalizedInput)) continue

                    val type = cursor.getInt(typeIdx)
                    val date = Date(cursor.getLong(dateIdx))
                    val duration = cursor.getString(durationIdx)
                    val name = cursor.getString(nameIdx)
                    val photo = cursor.getString(photoIdx)
                    val id = cursor.getLong(idIdx)

                    list.add(
                        CallLogEntry(
                            stringNumber = numberDb,
                            stringType = Common.getCallType(type),
                            dateData = date,
                            stringDuration = duration,
                            stringCallName = name,
                            stringDateCategory = date.toString(),
                            stringPhotoUri = photo,
                            intType = type
                        ).apply {
                            resetCallIds(id)
                        }
                    )

                } while (cursor.moveToNext())
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return list
    }


    fun getUpdatedContact(contactId: String?): CallLogEntry? {

        if (contactId.isNullOrEmpty()) return null

        // 👉 Permission check (VERY IMPORTANT)
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return try {

            val resolver = context.contentResolver

            var name: String? = null
            var photoUri: String? = null
            var number: String? = null

            // 👉 Get basic contact info
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.PHOTO_URI
                ),
                "${ContactsContract.Contacts._ID}=?",
                arrayOf(contactId),
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {

                    name = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                    )

                    photoUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
                    )
                }
            }

            // 👉 Get phone number
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?",
                arrayOf(contactId),
                null
            )?.use { cursor ->

                if (cursor.moveToFirst()) {
                    number = cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                    )
                }
            }

            // 👉 Return safely
            CallLogEntry(
                stringNumber = number,
                stringCallName = name,
                stringPhotoUri = photoUri,
                contactId = contactId
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}