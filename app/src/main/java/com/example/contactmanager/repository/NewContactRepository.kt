package com.example.contactmanager.repository

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.content.ContentUris
import android.provider.ContactsContract
import android.util.Log
import com.example.contactmanager.models.AccountModel
import com.example.contactmanager.utils.Common
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NewContactRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun getGoogleAccounts(): List<Pair<String, String>> {

        val list = mutableListOf<Pair<String, String>>()

        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType("com.google")

        for (account in accounts) {
            val email = account.name

            val name = email.substringBefore("@")

            list.add(Pair(name, email))
        }

        return list
    }


    fun saveOrUpdateContact(
        name: String,
        number: String,
        email: String?,
        selectedImageUri: Uri?,
        accountModel: AccountModel,
        isContactSaved: Boolean,
        contactId: String? = null,
        onCallBack: (String, String?) -> Unit
    ) {

        val ops = ArrayList<ContentProviderOperation>()

        try {

            val nameParts = name.trim().split(" ")
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = nameParts.drop(1).joinToString(" ")

            val existingRawId = if (isContactSaved) {
                contactId?.let { getRawContactIdFromContactId(it) }
            } else {
                null
            }

            // 👉 Determine if we need to MOVE the contact (Account changed)
            var shouldRecreate = false
            if (isContactSaved && contactId != null) {
                val currentAccount = getContactAccountName(contactId)
                val isCurrentLocal = currentAccount.isNullOrBlank()
                val isTargetLocal =
                    accountModel.name == "Device Only" || accountModel.email.isBlank()

                if (isCurrentLocal != isTargetLocal || (!isCurrentLocal && currentAccount != accountModel.email)) {
                    shouldRecreate = true
                }
            }

            // =========================
            // 🔄 UPDATE OR MOVE
            // =========================
            if (existingRawId != null && !shouldRecreate) {
                val rawId = existingRawId

                // 👉 Account (Same account, just ensure it's set - though usually unnecessary)
                // We skip updating account fields if they haven't changed to avoid potential crashes

                // 👉 Update Name
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(
                                rawId,
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            firstName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                            lastName
                        )
                        .build()
                )

                // 👉 Update Number
                if (isPhoneExists(rawId)) {
                    ops.add(
                        ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                arrayOf(
                                    rawId,
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                )
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                            )
                            .build()
                    )
                }

                // 👉 Email (Update existing or insert if not found)
                if (!email.isNullOrEmpty()) {
                    if (isEmailExists(rawId)) {
                        ops.add(
                            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(
                                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                    arrayOf(
                                        rawId,
                                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                    )
                                )
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .build()
                        )
                    } else {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                )
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(
                                    ContactsContract.CommonDataKinds.Email.TYPE,
                                    ContactsContract.CommonDataKinds.Email.TYPE_HOME
                                )
                                .build()
                        )
                    }
                }

                // 👉 Photo
                selectedImageUri?.let {
                    val photoBytes = Common.getPhotoBytes(it, context)
                    if (photoBytes != null) {
                        if (isPhotoExists(rawId)) {
                            ops.add(
                                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(
                                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                        arrayOf(
                                            rawId,
                                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                        )
                                    )
                                    .withValue(
                                        ContactsContract.CommonDataKinds.Photo.PHOTO,
                                        photoBytes
                                    )
                                    .build()
                            )
                        } else {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                    .withValue(
                                        ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                    )
                                    .withValue(
                                        ContactsContract.CommonDataKinds.Photo.PHOTO,
                                        photoBytes
                                    )
                                    .build()
                            )
                        }
                    }
                }

                onCallBack("Contact Updated ✅", null)
            } else {

                // =========================
                // 🆕 NEW OR MOVE CONTACT
                // =========================

                // 👉 Account
                val accountValues = android.content.ContentValues()
                if (accountModel.name != "Device Only" && accountModel.email.isNotBlank()) {
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, accountModel.email)
                } else {
                    accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
                }

                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValues(accountValues)
                        .build()
                )

                // 👉 Name
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                            firstName
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                            lastName
                        )
                        .build()
                )

                // 👉 Number (required)
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            number
                        )
                        .withValue(
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                        )
                        .build()
                )

                // 👉 Email (optional)
                if (!email.isNullOrEmpty()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                email
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.TYPE,
                                ContactsContract.CommonDataKinds.Email.TYPE_HOME
                            )
                            .build()
                    )
                }

                // 👉 Photo (optional)
                selectedImageUri?.let {
                    val photoBytes = Common.getPhotoBytes(it, context)
                    if (photoBytes != null) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(
                                    ContactsContract.Data.MIMETYPE,
                                    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                                )
                                .withValue(
                                    ContactsContract.CommonDataKinds.Photo.PHOTO,
                                    photoBytes
                                )
                                .build()
                        )
                    }
                }

                // 🔥 If moving, delete the old raw contact
                if (shouldRecreate && existingRawId != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.RawContacts._ID}=?",
                                arrayOf(existingRawId)
                            )
                            .build()
                    )
                }

                // 👉 APPLY
                val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

                // 👉 Get NEW Contact ID
                var newContactId: String? = null
                if (results.isNotEmpty() && results[0].uri != null) {
                    val newRawId = ContentUris.parseId(results[0].uri!!)
                    newContactId = getContactIdFromRawId(newRawId.toString())
                }

                onCallBack(
                    if (shouldRecreate) "Contact Moved & Saved" else "Contact Saved",
                    newContactId
                )
            }

            // 👉 APPLY (Only if not already applied in the Recreate branch)
            if (existingRawId != null && !shouldRecreate) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onCallBack(e.message ?: "Error", null)
        }
    }

    fun getContactIdFromRawId(rawContactId: String): String? {
        var contactId: String? = null
        // 👉 Aggregation can take a few ms. Try 5 times with delay.
        for (i in 0 until 5) {
            val cursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts._ID}=?",
                arrayOf(rawContactId),
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    contactId = it.getString(0)
                }
            }
            if (contactId != null) break
            try {
                Thread.sleep(200)
            } catch (e: Exception) {
                Log.e("TAG", "getContactIdFromRawId: ${e.message}")
            }
        }
        return contactId
    }

    fun getRawContactIdFromContactId(contactId: String): String? {
        val rawCursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )
        rawCursor?.use { rc ->
            if (rc.moveToFirst()) {
                return rc.getString(0)
            }
        }
        return null
    }

    fun getContactAccountName(contactId: String): String? {
        val rawCursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME),
            "${ContactsContract.RawContacts.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )
        rawCursor?.use { rc ->
            if (rc.moveToFirst()) {
                return rc.getString(0)
            }
        }
        return null
    }

    fun getContactEmail(contactId: String): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
            arrayOf(contactId),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return null
    }

    fun isEmailExists(rawContactId: String): Boolean {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(rawContactId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE),
            null
        )
        cursor?.use { return it.count > 0 }
        return false
    }

    fun isPhoneExists(rawContactId: String): Boolean {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(rawContactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
            null
        )
        cursor?.use { return it.count > 0 }
        return false
    }

    fun isPhotoExists(rawContactId: String): Boolean {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(
                rawContactId,
                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
            ),
            null
        )

        cursor?.use {
            return it.count > 0
        }
        return false
    }
}