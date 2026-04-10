package com.example.contactmanager.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import com.example.contactmanager.models.ContactModel
import com.example.contactmanager.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class FavoriteRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

   /* fun getContacts(): List<ContactModel> {
        val contactList = mutableListOf<ContactModel>()
        val uniqueNumbers = mutableSetOf<String>()

        try {

            val resolver = context.contentResolver
            val cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                while (it.moveToNext()) {

                    val contactId = it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        )
                    )

                    val name = it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    )

                    var number = it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )

                    number = number.replace("\\s".toRegex(), "")
                        .replace("+91", "")
                        .replace("-", "")

                    if (uniqueNumbers.contains(number)) continue
                    uniqueNumbers.add(number)

                    val isFavorite = it.getInt(
                        it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)
                    ) == 1


                    val (isRecent, isMissed, lastCallDateTime) = getCallStatus(number)

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    )

                    val nameTemp = if (name.isNullOrEmpty()) number else name
                    val imageAvatar = Common.generateAvatar(nameTemp)

                    contactList.add(
                        ContactModel(
                            _id = contactId,
                            name = name,
                            number = number,
                            isFavorite = isFavorite,
                            isRecent = isRecent,
                            isMissed = isMissed,
                            lastCallDateTime = lastCallDateTime,
                            photoUri = photoUri,
                            avatar = imageAvatar
                        )
                    )
                }
            }

        } catch (e: Exception) {

            Log.e("TAG", "getContacts: ${e.message}")
        }

        return contactList
    }*/

    fun getCallStatus(phoneNumber: String): Triple<Boolean, Boolean, Long?> {
        var isRecent = false
        var isMissed = false
        var lastCallTime: Long? = null

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

                    var dbNumber = it.getString(
                        it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                    )


                    dbNumber = dbNumber.replace("\\s".toRegex(), "")
                        .replace("+91", "")
                        .replace("-", "")

                    if (dbNumber == phoneNumber) {

                        val callDate = it.getLong(
                            it.getColumnIndexOrThrow(CallLog.Calls.DATE)
                        )

                        if (lastCallTime == null) {
                            lastCallTime = callDate
                        }

                        isRecent = true

                        val type = it.getInt(
                            it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                        )

                        if (type == CallLog.Calls.MISSED_TYPE) {
                            isMissed = true
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("TAG", "getCallStatus: ${e.message}")
        }

        return Triple(isRecent, isMissed, lastCallTime)
    }


    fun addToFavoriteUnFavorite(contactId: String?, makeFavorite: Boolean) {
        val id = contactId?.toLongOrNull() ?: return

        try {
            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (makeFavorite) 1 else 0)
            }

            val uri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI,
                id
            )

            context.contentResolver.update(uri, values, null, null)

        } catch (e: Exception) {
            Log.e("TAG", "addToFavoriteUnFavorite: ${e.message}")
        }
    }


    fun getAllFavoriteContacts(): ArrayList<ContactModel> {
        val favoritesList = ArrayList<ContactModel>()

        try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            val cursor = contentResolver.query(
                uri,
                null,
                "starred=?",
                arrayOf("1"),
                "display_name ASC"
            )

            cursor?.use { query ->
                val contactIdIndex = query.getColumnIndex("contact_id")
                val nameIndex = query.getColumnIndex("display_name")
                val numberIndex = query.getColumnIndex("data1")
                val photoIndex = query.getColumnIndex("photo_uri")

                val addedIds = HashSet<String>() // 🔥 better duplicate handling

                while (query.moveToNext()) {
                    val contactId = query.getString(contactIdIndex)

                    // Skip duplicates
                    if (addedIds.contains(contactId)) continue
                    addedIds.add(contactId)

                    val displayName = query.getString(nameIndex)
                    val phoneNumber = query.getString(numberIndex)
                    val photoUri = query.getString(photoIndex)

                    val split = displayName?.split("\\s+".toRegex()) ?: listOf("")
                    val firstName = split.getOrNull(0) ?: ""
                    val middleName = split.getOrNull(1) ?: ""
                    val surname = split.getOrNull(2) ?: ""

                    val contact = ContactModel().apply {
                        this.contactId = contactId
                        this.displayName = displayName
                        this.number = phoneNumber
                        this.userThumbnail = photoUri
                        this.firstName = firstName
                        this.middleName = middleName
                        this.surname = surname
                        this.isFavourite = 1
                    }

                    favoritesList.add(contact)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return favoritesList
    }

}