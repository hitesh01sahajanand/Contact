package com.phonecall.dialcontacts.calldialer.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.phonecall.dialcontacts.calldialer.models.CallLogEntry
import com.phonecall.dialcontacts.calldialer.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactDetailsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tagRepository: TagRepository
) {

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
                            stringType = Common.getCallType(context, type),
                            dateData = date,
                            stringDuration = duration,
                            stringCallName = name,
                            stringDateCategory = date.toString(),
                            stringPhotoUri = photo,
                            intType = type
                        ).apply {
                            resetCallIds(id)
                            // If name is null/empty, try to fetch from tags
                            if (stringCallName.isNullOrEmpty()) {
                                val cleaned = Common.cleanNumber(numberDb ?: "")
                                kotlinx.coroutines.runBlocking {
                                    val tag = tagRepository.getTag(cleaned)
                                    if (!tag.isNullOrEmpty()) {
                                        stringCallName = tag
                                    }
                                }
                            }
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

    fun deleteCallHistoryForNumber(number: String) {
        val contentResolver = context.contentResolver
        val normalizedInput = number.replace(Regex("[^0-9]"), "")

        val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.NUMBER)
        val selection = "${CallLog.Calls.NUMBER} LIKE ?"
        val selectionArgs = arrayOf("%$normalizedInput%")

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = it.getColumnIndex(CallLog.Calls.NUMBER)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val numberDb = it.getString(numberIdx)
                    val normDb = numberDb?.replace(Regex("[^0-9]"), "") ?: ""

                    if (Common.compareNumbers(normDb, normalizedInput)) {
                        contentResolver.delete(
                            CallLog.Calls.CONTENT_URI,
                            "${CallLog.Calls._ID} = ?",
                            arrayOf(id.toString())
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
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

            // 👉 Get name parts from Data table (more immediate than aggregated DISPLAY_NAME)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                    ContactsContract.Data.PHOTO_URI
                ),
                "${ContactsContract.Data.CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                arrayOf(
                    contactId,
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                ),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val firstName = cursor.getString(0) ?: ""
                    val lastName = cursor.getString(1) ?: ""
                    name = "$firstName $lastName".trim()
                    photoUri = cursor.getString(2)
                }
            }

            // 👉 If name still null, fallback to Contacts table
            if (name == null) {
                resolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    arrayOf(
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.Contacts.PHOTO_URI
                    ),
                    "${ContactsContract.Contacts._ID}=?",
                    arrayOf(contactId),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(0)
                        photoUri = cursor.getString(1)
                    }
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

    fun deleteContact(contactId: String) {
        try {
            val contentResolver = context.contentResolver
            val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}