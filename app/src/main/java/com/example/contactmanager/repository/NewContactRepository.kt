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

    fun getGoogleAccounts(): List<Triple<String, String, String>> {
        val list = mutableListOf<Triple<String, String, String>>()
        val accountManager = AccountManager.get(context)
        
        // Get Google Accounts
        val googleAccounts = accountManager.getAccountsByType("com.google")
        for (account in googleAccounts) {
            list.add(Triple(account.name.substringBefore("@"), account.name, "com.google"))
        }

        // Get other accounts that might be local/manufacturer specific
        val allAccounts = accountManager.accounts
        for (account in allAccounts) {
            if (account.type != "com.google" && !account.type.contains("whatsapp", ignoreCase = true)) {
                // If it looks like a local account (e.g., Samsung, Xiaomi, etc.)
                if (account.type.contains("local", ignoreCase = true) || 
                    account.type.contains("phone", ignoreCase = true) ||
                    account.type.contains("contact", ignoreCase = true)) {
                    list.add(Triple("Device (${account.type.substringAfterLast(".")})", account.name, account.type))
                }
            }
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
                val accountInfo = getContactAccountName(contactId)
                val currentAccount = accountInfo.first
                val currentAccountType = accountInfo.second
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
                } else {
                    // 👉 Email Cleared (Delete existing email)
                    if (isEmailExists(rawId)) {
                        ops.add(
                            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                                .withSelection(
                                    "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                                    arrayOf(
                                        rawId,
                                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE
                                    )
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

                // 👉 APPLY
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                onCallBack("Contact Updated ✅", null)
            } else {

                // =========================
                // 🆕 NEW OR MOVE CONTACT
                // =========================

                // 👉 Account
                val accountValues = android.content.ContentValues()
                if (accountModel.name != "Device Only" && accountModel.email.isNotBlank()) {
                    // It's a cloud or specific local account selected from the list
                    val type = accountModel.accountType ?: if (accountModel.email.contains("@")) "com.google" else "com.android.localphone" 
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, type)
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, accountModel.email)
                    Log.d("TAG", "Saving to account: ${accountModel.email} (Type: $type)")
                } else {
                    // For local contacts on Android 14+, null/null might be rejected if a cloud account is default.
                    val localAccount = findLocalAccount()
                    if (localAccount != null) {
                        accountValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, localAccount.first)
                        accountValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, localAccount.second)
                        Log.d("TAG", "Saving to Detected Local account: ${localAccount.first}")
                    } else {
                        // If no local account type is found, we use putNull.
                        // This honors the user's "Device Only" request.
                        // If this fails with IllegalArgumentException on Android 14+, 
                        // it will be caught and the user will be informed.
                        accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                        accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
                        Log.d("TAG", "Saving to Device Only (using null/null)")
                    }
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

                // 🔥 If moving, delete ALL old raw contacts associated with this contact
                if (shouldRecreate && contactId != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection(
                                "${ContactsContract.RawContacts.CONTACT_ID}=?",
                                arrayOf(contactId)
                            )
                            .build()
                    )
                }

                // 👉 APPLY
                val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

                // 👉 Get NEW Contact ID
                var newContactId: String? = null
                if (results != null && results.isNotEmpty() && results[0]?.uri != null) {
                    try {
                        val newRawId = ContentUris.parseId(results[0].uri!!)
                        newContactId = getContactIdFromRawId(newRawId.toString())
                    } catch (e: Exception) {
                        Log.e("TAG", "Error parsing new contact ID: ${e.message}")
                    }
                }

                onCallBack(
                    if (shouldRecreate) "Contact Moved & Saved" else "Contact Saved",
                    newContactId
                )
            }

            // 👉 APPLY (Already handled in branches above)
            /*if (existingRawId != null && !shouldRecreate) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }*/

        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = if (e.message?.contains("local or SIM accounts", ignoreCase = true) == true) {
                "System Error: Cannot save to Device while a Google account is set as default. Please select your Google account from the list above. ⚠️"
            } else {
                e.message ?: "Error"
            }
            onCallBack(errorMsg, null)
        }
    }

    fun getContactIdFromRawId(rawContactId: String): String? {
        var contactId: String? = null
        // 👉 Aggregation can take a few ms. Try 10 times with delay.
        for (i in 0 until 10) {
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
                Thread.sleep(300)
            } catch (e: Exception) {
                Log.e("TAG", "getContactIdFromRawId: ${e.message}")
            }
        }
        return contactId
    }

    fun getRawContactIdFromContactId(contactId: String): String? {
        val rawCursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts._ID, ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId),
            null
        )
        var bestRawId: String? = null
        rawCursor?.use { rc ->
            while (rc.moveToNext()) {
                val rawId = rc.getString(0)
                val type = rc.getString(1) ?: ""
                
                val isWhatsApp = type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                val isTelegram = type == "org.telegram.messenger" || type.contains("telegram", ignoreCase = true)
                
                if (!isWhatsApp && !isTelegram) {
                    // This is a "real" account (Google, Device, etc.)
                    return rawId // Prioritize the first real account found
                }
                if (bestRawId == null) bestRawId = rawId
            }
        }
        return bestRawId
    }

    fun getContactAccountName(contactId: String): Pair<String?, String?> {
        val rawCursor = context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.CONTACT_ID}=? AND ${ContactsContract.RawContacts.DELETED}=0",
            arrayOf(contactId),
            null
        )
        var bestMatch: Pair<String?, String?> = Pair(null, null)
        rawCursor?.use { rc ->
            while (rc.moveToNext()) {
                val name = rc.getString(0)
                val type = rc.getString(1) ?: ""
                
                val isWhatsApp = type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                val isTelegram = type == "org.telegram.messenger" || type.contains("telegram", ignoreCase = true)
                
                if (!isWhatsApp && !isTelegram) {
                    return Pair(name, type) // Prioritize real accounts
                }
                if (bestMatch.first == null) bestMatch = Pair(name, type)
            }
        }
        return bestMatch
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

    fun findLocalAccount(): Pair<String, String>? {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.accounts
        
        Log.d("TAG", "--- Available Accounts ---")
        for (acc in accounts) {
            Log.d("TAG", "Account: Name=${acc.name}, Type=${acc.type}")
        }

        // 1. Check for well-known local/phone account types
        val localTypes = arrayOf(
            "com.android.localphone",
            "vnd.sec.contact.phone",
            "com.phone.contacts",
            "com.android.contacts.default",
            "com.samsung.android.core.apps.contact",
            "com.sonyericsson.localcontacts",
            "com.google.android.gms.primary",
            "default",
            "local",
            "phone"
        )

        for (type in localTypes) {
            for (acc in accounts) {
                if (acc.type.equals(type, ignoreCase = true)) return Pair(acc.type, acc.name)
            }
        }
        
        // 2. Check account names for "Phone" or "Device"
        for (acc in accounts) {
            if (acc.name.equals("Phone", ignoreCase = true) || acc.name.equals("Device", ignoreCase = true)) {
                return Pair(acc.type, acc.name)
            }
        }

        // 3. Search for any account containing "local", "phone", or "device" in type
        for (acc in accounts) {
            val type = acc.type.lowercase()
            if (type.contains("local") || type.contains("phone") || type.contains("device")) {
                return Pair(acc.type, acc.name)
            }
        }

        return null
    }
}