package com.example.contactmanager.repository

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
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
        onCallBack: (String) -> Unit
    ) {

        val ops = ArrayList<ContentProviderOperation>()

        try {

            val nameParts = name.trim().split(" ")
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = nameParts.drop(1).joinToString(" ")

            val existingRawId = contactId?.let { getRawContactIdFromContactId(it) } 
                ?: getRawContactIdByNumber(number)

            // =========================
            // 🔄 UPDATE CONTACT
            // =========================
            if (existingRawId != null) {
                val rawId = existingRawId

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

                // 👉 Update Number (We update the first mobile number found, or insert if not exists)
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=? AND ${ContactsContract.Data.DATA2}=?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE.toString())
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
                        .build()
                )

                // 👉 Email (Update existing or insert if not found)
                if (!email.isNullOrEmpty()) {
                    if (isEmailExists(rawId)) {
                        ops.add(
                            ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                .withSelection(
                                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                    arrayOf(rawId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                )
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .build()
                        )
                    } else {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_HOME)
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
                                        arrayOf(rawId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    )
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build()
                            )
                        } else {
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                    .build()
                            )
                        }
                    }
                }

                onCallBack("Contact Updated ✅")
            } else {

                // =========================
                // 🆕 NEW CONTACT
                // =========================

                // 👉 Account
                if (accountModel.name == "Device Only") {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                            .withValue(
                                ContactsContract.RawContacts.ACCOUNT_NAME,
                                accountModel.email
                            )
                            .build()
                    )
                }

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

                onCallBack("Contact Saved ✅")
            }

            // 👉 APPLY
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

        } catch (e: Exception) {
            e.printStackTrace()
            onCallBack(e.message ?: "Error")
        }
    }


    /*fun saveContact(
        firstName: String,
        lastName: String,
        phoneList: List<Pair<String, String>>,
        emailList: List<Pair<String, String>>,
        selectedImageUri: Uri?,
        accountModel: AccountModel,
        onCallBack: (String) -> Unit
    ) {

        val ops = ArrayList<ContentProviderOperation>()

        try {

            // 👉 STEP 1: check existing contact
            val firstNumber = phoneList.firstOrNull()?.first
            val rawContactId = firstNumber?.let { getRawContactIdByNumber(it) }

            if (rawContactId != null) {
                Log.e("TAG", "saveContact: EXISTING")

                // =========================
                // 🔥 UPDATE EXISTING CONTACT
                // =========================

                // 👉 Update Name
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(
                                rawContactId,
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

                // 👉 Add new phones (duplicate check optional)
                for (phonePair in phoneList) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(
                                ContactsContract.Data.RAW_CONTACT_ID,
                                rawContactId
                            )
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                phonePair.first
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                Common.getPhoneType(phonePair.second)
                            )
                            .build()
                    )
                }

                // 👉 Emails
                for (emailPair in emailList) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(
                                ContactsContract.Data.RAW_CONTACT_ID,
                                rawContactId
                            )
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                emailPair.first
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.TYPE,
                                Common.getEmailType(emailPair.second)
                            )
                            .build()
                    )
                }


                selectedImageUri?.let {
                    val photoBytes = Common.getPhotoBytes(it, context)

                    if (photoBytes != null) {

                        if (isPhotoExists(rawContactId)) {
                            // 👉 UPDATE PHOTO
                            ops.add(
                                ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                                    .withSelection(
                                        "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                        arrayOf(
                                            rawContactId,
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
                            // 👉 INSERT PHOTO
                            ops.add(
                                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValue(
                                        ContactsContract.Data.RAW_CONTACT_ID,
                                        rawContactId
                                    )
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

                onCallBack("Contact Updated ✅")

            } else {

                Log.e("TAG", "saveContact: NEW CONTACT")

                // =========================
                // 🆕 INSERT NEW CONTACT
                // =========================

                // 👉 Account
                if (accountModel.name == "Device Only") {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build()
                    )
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, "com.google")
                            .withValue(
                                ContactsContract.RawContacts.ACCOUNT_NAME,
                                accountModel.email
                            )
                            .build()
                    )
                }

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

                // 👉 Phones
                for (phonePair in phoneList) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                phonePair.first
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Phone.TYPE,
                                Common.getPhoneType(phonePair.second)
                            )
                            .build()
                    )
                }

                // 👉 Emails
                for (emailPair in emailList) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(
                                ContactsContract.Data.MIMETYPE,
                                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.ADDRESS,
                                emailPair.first
                            )
                            .withValue(
                                ContactsContract.CommonDataKinds.Email.TYPE,
                                Common.getEmailType(emailPair.second)
                            )
                            .build()
                    )
                }

                // 👉 Photo
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

                onCallBack("Contact Saved ✅")
            }

            // 👉 APPLY
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

        } catch (e: Exception) {
            e.printStackTrace()
            onCallBack("${e.message}")
        }
    }*/

    fun getRawContactIdByNumber(number: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )

        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId = it.getString(0)

                // 🔥 Now get RAW_CONTACT_ID
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
            }
        }
        return null
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