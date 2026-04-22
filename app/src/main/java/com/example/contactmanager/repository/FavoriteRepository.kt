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

    fun updateFavoriteStatusBatch(contacts: List<ContactModel>) {
        val operations = ArrayList<android.content.ContentProviderOperation>()

        for (contact in contacts) {
            val contactIdLong = contact.contactId?.toLongOrNull() ?: continue
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactIdLong)

            operations.add(
                android.content.ContentProviderOperation.newUpdate(uri)
                    .withValue(ContactsContract.Contacts.STARRED, if (contact.isFavourite == 1) 1 else 0)
                    .build()
            )

            // To avoid TransactionTooLargeException, apply in chunks if necessary
            if (operations.size >= 100) {
                try {
                    context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
                    operations.clear()
                } catch (e: Exception) {
                    Log.e("TAG", "updateFavoriteStatusBatch error: ${e.message}")
                }
            }
        }

        if (operations.isNotEmpty()) {
            try {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            } catch (e: Exception) {
                Log.e("TAG", "updateFavoriteStatusBatch error: ${e.message}")
            }
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