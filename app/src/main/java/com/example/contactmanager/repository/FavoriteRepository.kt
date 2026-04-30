package com.example.contactmanager.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.example.contactmanager.models.ContactModel
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
        Log.d("FavoriteRepository", "updateFavoriteStatusBatch: updating ${contacts.size} contacts")
        for (contact in contacts) {
            val contactId = contact.contactId ?: continue
            val isFav = contact.isFavourite == 1

            Log.d("FavoriteRepository", "Updating contact $contactId to favorite=$isFav")
            addToFavoriteUnFavorite(contactId, isFav)
        }
    }


    fun getAllFavoriteContacts(): ArrayList<ContactModel> {
        val favoritesList = ArrayList<ContactModel>()
        val contactMap = LinkedHashMap<String, ContactModel>()

        try {
            val contentResolver = context.contentResolver

            // 🔹 1. Get Starred Contacts from the Contacts table
            val contactsUri = ContactsContract.Contacts.CONTENT_URI
            val contactsCursor = contentResolver.query(
                contactsUri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                "${ContactsContract.Contacts.STARRED} = 1",
                null,
                "display_name COLLATE NOCASE ASC"
            )

            val starredIds = mutableSetOf<String>()
            contactsCursor?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val displayName = cursor.getString(nameIndex) ?: ""
                    val photoUri = cursor.getString(photoIndex)

                    starredIds.add(contactId)

                    val contact = ContactModel().apply {
                        this.contactId = contactId
                        this.displayName = displayName
                        this.userThumbnail = photoUri
                        this.isFavourite = 1
                    }
                    contactMap[contactId] = contact
                }
            }

            if (starredIds.isEmpty()) return favoritesList

            // 🔹 2. Get Phone Numbers for these starred contacts
            val dataUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val phoneCursor = contentResolver.query(
                dataUri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} IN (${
                    starredIds.joinToString(
                        ","
                    )
                })",
                null,
                null
            )

            phoneCursor?.use { cursor ->
                val idIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val phoneNumber = cursor.getString(numberIndex) ?: continue

                    contactMap[contactId]?.let { contact ->
                        if (contact.number.isNullOrEmpty()) {
                            contact.number = phoneNumber
                        } else if (contact.number != phoneNumber) {
                            // If multiple numbers, we can either add a new entry or keep one.
                            // The UI seems to expect one entry per favorite.
                            // But let's follow the ContactRepository pattern if needed.
                            // For favorites, usually one representative number is enough.
                        }
                    }
                }
            }

            favoritesList.addAll(contactMap.values)

        } catch (e: Exception) {
            Log.e("FavoriteRepository", "Error loading favorites: ${e.message}")
            e.printStackTrace()
        }

        return favoritesList
    }

}