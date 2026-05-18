package com.phonecall.dialcontacts.calldialer.repository

import android.accounts.AccountManager
import android.content.ContentProviderOperation
import android.content.Context
import android.net.Uri
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import com.phonecall.dialcontacts.calldialer.models.AccountModel
import com.phonecall.dialcontacts.calldialer.models.ContactDetail
import com.phonecall.dialcontacts.calldialer.models.FullContactData
import com.phonecall.dialcontacts.calldialer.utils.Common
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
        contactData: FullContactData,
        selectedImageUri: Uri?,
        accountModel: AccountModel,
        isContactSaved: Boolean,
        contactId: String? = null,
        onCallBack: (String, String?) -> Unit
    ) {
        val ops = ArrayList<ContentProviderOperation>()

        try {
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
                val isTargetLocal = accountModel.name == "Device Only" || accountModel.email.isBlank()

                if (isCurrentLocal != isTargetLocal || (!isCurrentLocal && currentAccount != accountModel.email)) {
                    shouldRecreate = true
                }
            }

            if (existingRawId != null && !shouldRecreate) {
                val rawId = existingRawId

                // 👉 Update Name
                ops.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
                            arrayOf(rawId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contactData.middleName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.surname)
                        .build()
                )

                // 👉 Organization
                updateOrganization(ops, rawId, contactData.company)
                
                // 👉 Phone Numbers
                updatePhoneNumbers(ops, rawId, contactData.phones)

                // 👉 Emails
                updateEmails(ops, rawId, contactData.emails)

                // 👉 Addresses
                updateAddresses(ops, rawId, contactData.addresses)

                // 👉 Events/Birthdays
                updateEvents(ops, rawId, contactData.events)

                // 👉 Websites
                updateWebsites(ops, rawId, contactData.websites)

                // 👉 Relations
                updateRelations(ops, rawId, contactData.relations)

                // 👉 Notes
                updateNotes(ops, rawId, contactData.notes)

                // 👉 Photo
                updatePhoto(ops, rawId, selectedImageUri)

                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                onCallBack("Contact Updated ✅", null)

            } else {
                // =========================
                // 🆕 NEW OR MOVE CONTACT
                // =========================
                val accountValues = ContentValues()
                if (accountModel.name != "Device Only" && accountModel.email.isNotBlank()) {
                    val type = accountModel.accountType ?: if (accountModel.email.contains("@")) "com.google" else "com.android.localphone" 
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, type)
                    accountValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, accountModel.email)
                } else {
                    val localAccount = findLocalAccount()
                    if (localAccount != null) {
                        accountValues.put(ContactsContract.RawContacts.ACCOUNT_TYPE, localAccount.first)
                        accountValues.put(ContactsContract.RawContacts.ACCOUNT_NAME, localAccount.second)
                    } else {
                        accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_TYPE)
                        accountValues.putNull(ContactsContract.RawContacts.ACCOUNT_NAME)
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
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, contactData.firstName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME, contactData.middleName)
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, contactData.surname)
                        .build()
                )

                // 👉 Company
                if (contactData.company.isNotEmpty()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, contactData.company)
                            .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                            .build()
                    )
                }

                // 👉 Multiple Phones
                contactData.phones.forEach { phone ->
                    if (phone.value.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.value)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                                .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                                .build()
                        )
                    }
                }

                // 👉 Multiple Emails
                contactData.emails.forEach { email ->
                    if (email.value.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.value)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                                .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.label)
                                .build()
                        )
                    }
                }

                // 👉 Addresses
                contactData.addresses.forEach { addr ->
                    if (addr.value.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, addr.value)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addr.type)
                                .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, addr.label)
                                .build()
                        )
                    }
                }

                // 👉 Events
                contactData.events.forEach { ev ->
                    if (ev.value.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, ev.value)
                                .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ev.type)
                                .withValue(ContactsContract.CommonDataKinds.Event.LABEL, ev.label)
                                .build()
                        )
                    }
                }

                // 👉 Websites
                contactData.websites.forEach { web ->
                    if (web.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Website.URL, web)
                                .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                                .build()
                        )
                    }
                }

                // 👉 Relations
                contactData.relations.forEach { rel ->
                    if (rel.value.isNotEmpty()) {
                        ops.add(
                            ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Relation.NAME, rel.value)
                                .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, rel.type)
                                .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, rel.label)
                                .build()
                        )
                    }
                }

                // 👉 Notes
                if (contactData.notes.isNotEmpty()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Note.NOTE, contactData.notes)
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
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, photoBytes)
                                .build()
                        )
                    }
                }

                if (shouldRecreate && contactId != null) {
                    ops.add(
                        ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                            .withSelection("${ContactsContract.RawContacts.CONTACT_ID}=?", arrayOf(contactId))
                            .build()
                    )
                }

                val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                var newContactId: String? = null
                if (results != null && results.isNotEmpty() && results[0]?.uri != null) {
                    try {
                        val newRawId = ContentUris.parseId(results[0].uri!!)
                        newContactId = getContactIdFromRawId(newRawId.toString())
                    } catch (e: Exception) {
                        Log.e("TAG", "Error parsing new contact ID: ${e.message}")
                    }
                }

                onCallBack(if (shouldRecreate) "Contact Moved & Saved" else "Contact Saved", newContactId)
            }
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

    private fun updateOrganization(ops: ArrayList<ContentProviderOperation>, rawId: String, company: String) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE))
                .build()
        )
        if (company.isNotEmpty()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, company)
                    .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                    .build()
            )
        }
    }

    private fun updatePhoneNumbers(ops: ArrayList<ContentProviderOperation>, rawId: String, phones: List<ContactDetail>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE))
                .build()
        )
        phones.forEach { phone ->
            if (phone.value.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.value)
                        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                        .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                        .build()
                )
            }
        }
    }

    private fun updateEmails(ops: ArrayList<ContentProviderOperation>, rawId: String, emails: List<ContactDetail>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE))
                .build()
        )
        emails.forEach { email ->
            if (email.value.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.value)
                        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                        .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.label)
                        .build()
                )
            }
        }
    }

    private fun updateAddresses(ops: ArrayList<ContentProviderOperation>, rawId: String, addresses: List<ContactDetail>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE))
                .build()
        )
        addresses.forEach { addr ->
            if (addr.value.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, addr.value)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, addr.type)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, addr.label)
                        .build()
                )
            }
        }
    }

    private fun updateEvents(ops: ArrayList<ContentProviderOperation>, rawId: String, events: List<ContactDetail>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE))
                .build()
        )
        events.forEach { ev ->
            if (ev.value.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, ev.value)
                        .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ev.type)
                        .withValue(ContactsContract.CommonDataKinds.Event.LABEL, ev.label)
                        .build()
                )
            }
        }
    }

    private fun updateWebsites(ops: ArrayList<ContentProviderOperation>, rawId: String, websites: List<String>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE))
                .build()
        )
        websites.forEach { web ->
            if (web.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Website.URL, web)
                        .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
                        .build()
                )
            }
        }
    }

    private fun updateRelations(ops: ArrayList<ContentProviderOperation>, rawId: String, relations: List<ContactDetail>) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE))
                .build()
        )
        relations.forEach { rel ->
            if (rel.value.isNotEmpty()) {
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Relation.NAME, rel.value)
                        .withValue(ContactsContract.CommonDataKinds.Relation.TYPE, rel.type)
                        .withValue(ContactsContract.CommonDataKinds.Relation.LABEL, rel.label)
                        .build()
                )
            }
        }
    }

    private fun updateNotes(ops: ArrayList<ContentProviderOperation>, rawId: String, notes: String) {
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                    arrayOf(rawId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE))
                .build()
        )
        if (notes.isNotEmpty()) {
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                    .build()
            )
        }
    }

    private fun updatePhoto(ops: ArrayList<ContentProviderOperation>, rawId: String, selectedImageUri: Uri?) {
        if (selectedImageUri != null) {
            val photoBytes = Common.getPhotoBytes(selectedImageUri, context)
            if (photoBytes != null) {
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection("${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?", 
                            arrayOf(rawId, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE))
                        .build()
                )
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

    fun getFullContactData(contactId: String): FullContactData? {
        val rawId = getRawContactIdFromContactId(contactId) ?: return null
        
        // 1. Name & Company
        val nameCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
                ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
                ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.Data.MIMETYPE
            ),
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE} IN (?, ?)",
            arrayOf(rawId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null
        )
        
        var firstName = ""
        var middleName = ""
        var surname = ""
        var company = ""
        
        nameCursor?.use { cursor ->
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE))
                if (mimeType == ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE) {
                    firstName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME)) ?: ""
                    middleName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME)) ?: ""
                    surname = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME)) ?: ""
                } else if (mimeType == ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE) {
                    company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)) ?: ""
                }
            }
        }

        // 2. Phones
        val phones = mutableListOf<ContactDetail>()
        queryData(rawId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL)) { cursor ->
            val number = cursor.getString(0)
            val type = cursor.getInt(1)
            val label = cursor.getString(2)
            phones.add(ContactDetail(number, type, label))
        }

        // 3. Emails
        val emails = mutableListOf<ContactDetail>()
        queryData(rawId, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.LABEL)) { cursor ->
            val address = cursor.getString(0)
            val type = cursor.getInt(1)
            val label = cursor.getString(2)
            emails.add(ContactDetail(address, type, label))
        }

        // 4. Addresses
        val addresses = mutableListOf<ContactDetail>()
        queryData(rawId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, ContactsContract.CommonDataKinds.StructuredPostal.TYPE, ContactsContract.CommonDataKinds.StructuredPostal.LABEL)) { cursor ->
            val address = cursor.getString(0)
            val type = cursor.getInt(1)
            val label = cursor.getString(2)
            addresses.add(ContactDetail(address, type, label))
        }

        // 5. Events
        val events = mutableListOf<ContactDetail>()
        queryData(rawId, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Event.START_DATE, ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.LABEL)) { cursor ->
            val date = cursor.getString(0)
            val type = cursor.getInt(1)
            val label = cursor.getString(2)
            events.add(ContactDetail(date, type, label))
        }

        // 6. Websites
        val websites = mutableListOf<String>()
        queryData(rawId, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Website.URL)) { cursor ->
            websites.add(cursor.getString(0))
        }

        // 7. Relations
        val relations = mutableListOf<ContactDetail>()
        queryData(rawId, ContactsContract.CommonDataKinds.Relation.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Relation.NAME, ContactsContract.CommonDataKinds.Relation.TYPE, ContactsContract.CommonDataKinds.Relation.LABEL)) { cursor ->
            val name = cursor.getString(0)
            val type = cursor.getInt(1)
            val label = cursor.getString(2)
            relations.add(ContactDetail(name, type, label))
        }

        // 8. Notes
        var notes = ""
        queryData(rawId, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, arrayOf(ContactsContract.CommonDataKinds.Note.NOTE)) { cursor ->
            notes = cursor.getString(0) ?: ""
        }

        // 9. Photo URI
        var photoUri: String? = null
        val contactCursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.PHOTO_URI),
            "${ContactsContract.Contacts._ID}=?",
            arrayOf(contactId),
            null
        )
        contactCursor?.use {
            if (it.moveToFirst()) {
                photoUri = it.getString(0)
            }
        }

        return FullContactData(firstName, middleName, surname, company, phones, emails, addresses, events, websites, relations, notes, photoUri)
    }

    private fun queryData(rawId: String, mimeType: String, projection: Array<String>, onResult: (Cursor) -> Unit) {
        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            "${ContactsContract.Data.RAW_CONTACT_ID}=? AND ${ContactsContract.Data.MIMETYPE}=?",
            arrayOf(rawId, mimeType),
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                onResult(it)
            }
        }
    }

    fun getContactIdFromRawId(rawContactId: String): String? {
        var contactId: String? = null
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
            try { Thread.sleep(300) } catch (e: Exception) {}
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
                if (type != "com.whatsapp" && !type.contains("whatsapp", true) && type != "org.telegram.messenger" && !type.contains("telegram", true)) {
                    return rawId
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
                if (type != "com.whatsapp" && !type.contains("whatsapp", true) && type != "org.telegram.messenger" && !type.contains("telegram", true)) {
                    return Pair(name, type)
                }
                if (bestMatch.first == null) bestMatch = Pair(name, type)
            }
        }
        return bestMatch
    }

    fun findLocalAccount(): Pair<String, String>? {
        val accountManager = AccountManager.get(context)
        val accounts = accountManager.accounts
        val localTypes = arrayOf("com.android.localphone", "vnd.sec.contact.phone", "com.phone.contacts", "com.android.contacts.default", "com.samsung.android.core.apps.contact", "com.sonyericsson.localcontacts", "com.google.android.gms.primary", "default", "local", "phone")
        for (type in localTypes) {
            for (acc in accounts) {
                if (acc.type.equals(type, ignoreCase = true)) return Pair(acc.type, acc.name)
            }
        }
        for (acc in accounts) {
            if (acc.name.equals("Phone", true) || acc.name.equals("Device", true)) return Pair(acc.type, acc.name)
        }
        for (acc in accounts) {
            val type = acc.type.lowercase()
            if (type.contains("local") || type.contains("phone") || type.contains("device")) return Pair(acc.type, acc.name)
        }
        return null
    }
}