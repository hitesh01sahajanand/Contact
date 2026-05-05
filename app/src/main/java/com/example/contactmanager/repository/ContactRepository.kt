package com.example.contactmanager.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContactRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun getContactsByAccountWithHeaders(): MutableList<ContactListItem> {
        val contactMap = LinkedHashMap<String, ContactModel>()
        val result = mutableListOf<ContactListItem>()

        try {
            val resolver = context.contentResolver

            // 🔹 0. Get all valid contact_id's (Device and Google only)
            val validContactIds = mutableSetOf<Long>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(idIdx)) {
                        val contactId = cursor.getLong(idIdx)
                        val type = cursor.getString(typeIdx) ?: ""

                        val isWhatsApp = type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                        val isTelegram = type == "org.telegram.messenger" || type.contains("telegram", ignoreCase = true)

                        // Only add if it's NOT WhatsApp and NOT Telegram
                        if (!isWhatsApp && !isTelegram) {
                            validContactIds.add(contactId)
                        }
                    }
                }
            }

            // 🔹 1. Get ALL Contacts (With phone number filter)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
                null,
                "${ContactsContract.Contacts.SORT_KEY_PRIMARY} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactIdLong = cursor.getLong(idIndex)
                    if (contactIdLong in validContactIds) {
                        val contactId = contactIdLong.toString()
                        val displayName = cursor.getString(nameIndex) ?: ""
                        val photoUri = cursor.getString(photoIndex)
                        val starred = cursor.getInt(starredIndex)

                        val contact = ContactModel().apply {
                            this.contactId = contactId
                            this.displayName = displayName
                            this.userThumbnail = photoUri
                            this.isFavourite = starred
                            this.firstName = displayName
                        }
                        contactMap[contactId] = contact
                    }
                }
            }

            // 🔹 1.5 Get Phone Numbers
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val contactsWithMultipleNumbers = mutableListOf<ContactModel>()
                while (cursor.moveToNext()) {
                    if (cursor.isNull(idIndex)) continue
                    val contactId = cursor.getLong(idIndex).toString()
                    val phoneNumber = cursor.getString(numberIndex) ?: continue

                    contactMap[contactId]?.let { contact ->
                        if (contact.number.isNullOrEmpty()) {
                            contact.number = phoneNumber
                        } else if (contact.number != phoneNumber) {
                            val additionalContact = contact.copy().apply {
                                this.number = phoneNumber
                            }
                            contactsWithMultipleNumbers.add(additionalContact)
                        }
                    }
                }
                contactsWithMultipleNumbers.forEach {
                    val uniqueKey = "${it.contactId}_${it.number}"
                    contactMap[uniqueKey] = it
                }
            }

            // 🔹 2. Organization (Company)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/organization"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val orgIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val company = cursor.getString(orgIndex) ?: ""
                    contactMap[contactId]?.company = company
                }
            }

            // 🔹 3. Notes
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/note"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val noteIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val noteText = cursor.getString(noteIndex) ?: ""
                    contactMap[contactId]?.note = noteText
                }
            }

            // 🔹 4. Names
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "data2", "data5", "data3", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val firstName = cursor.getString(firstIndex)
                    val middleName = cursor.getString(middleIndex)
                    val surname = cursor.getString(lastIndex)

                    contactMap[contactId]?.let {
                        if (!firstName.isNullOrEmpty()) it.firstName = firstName
                        if (!middleName.isNullOrEmpty()) it.middleName = middleName
                        if (!surname.isNullOrEmpty()) it.surname = surname
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sortedList = contactMap.values
            .filter { !it.number.isNullOrBlank() && !it.contactId.isNullOrEmpty() }
            .sortedWith { c1, c2 ->
                val name1 = c1.displayName?.trim() ?: ""
                val name2 = c2.displayName?.trim() ?: ""
                if (name1.isEmpty() && name2.isEmpty()) return@sortedWith 0
                if (name1.isEmpty()) return@sortedWith 1
                if (name2.isEmpty()) return@sortedWith -1
                val char1 = name1.uppercase()[0]
                val char2 = name2.uppercase()[0]
                val isLetter1 = char1 in 'A'..'Z'
                val isLetter2 = char2 in 'A'..'Z'
                when {
                    isLetter1 && !isLetter2 -> -1
                    !isLetter1 && isLetter2 -> 1
                    else -> name1.compareTo(name2, ignoreCase = true)
                }
            }

        var lastHeader = ""
        for (contact in sortedList) {
            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()
            val header =
                if (firstChar != null && firstChar.isLetter()) firstChar.toString() else "#"
            if (header != lastHeader) {
                result.add(ContactListItem.Header(header))
                lastHeader = header
            }
            result.add(ContactListItem.Contact(contact))
        }
        return result
    }

    fun getContactsByGoogleAccount(accountEmail: String): MutableList<ContactListItem> {
        val contactMap = LinkedHashMap<String, ContactModel>()
        val result = mutableListOf<ContactListItem>()

        try {
            val resolver = context.contentResolver

            // 🔹 0. Get all raw_contact_id's and contact_id's belonging to this Google Account
            val validContactIds = mutableSetOf<Long>()
            val validRawContactIds = mutableSetOf<Long>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID, ContactsContract.RawContacts._ID),
                "${ContactsContract.RawContacts.DELETED} = 0 AND ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                arrayOf(accountEmail, "com.google"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (!cursor.isNull(0)) validContactIds.add(cursor.getLong(0))
                    if (!cursor.isNull(1)) validRawContactIds.add(cursor.getLong(1))
                }
            }

            // 🔹 1. Get Contacts from the system (With phone number filter)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
                null,
                "${ContactsContract.Contacts.SORT_KEY_PRIMARY} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex)
                    if (contactId in validContactIds) {
                        val displayName = cursor.getString(nameIndex) ?: ""
                        val photoUri = cursor.getString(photoIndex)
                        val starred = cursor.getInt(starredIndex)

                        val contact = ContactModel().apply {
                            this.contactId = contactId.toString()
                            this.displayName = displayName
                            this.userThumbnail = photoUri
                            this.isFavourite = starred
                            this.firstName = displayName
                        }
                        contactMap[contactId.toString()] = contact
                    }
                }
            }

            // 🔹 1.5 Get Phone Numbers
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.Data.RAW_CONTACT_ID
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val rawIdIndex = cursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)

                val contactsWithMultipleNumbers = mutableListOf<ContactModel>()
                while (cursor.moveToNext()) {
                    if (cursor.isNull(idIndex)) continue
                    val contactId = cursor.getLong(idIndex)
                    val phoneNumber = cursor.getString(numberIndex) ?: continue
                    val rawContactId =
                        if (!cursor.isNull(rawIdIndex)) cursor.getLong(rawIdIndex) else null

                    if (rawContactId != null && rawContactId in validRawContactIds) {
                        contactMap[contactId.toString()]?.let { contact ->
                            if (contact.number.isNullOrEmpty()) {
                                contact.number = phoneNumber
                            } else if (contact.number != phoneNumber) {
                                val additionalContact = contact.copy().apply {
                                    this.number = phoneNumber
                                }
                                contactsWithMultipleNumbers.add(additionalContact)
                            }
                        }
                    }
                }
                contactsWithMultipleNumbers.forEach {
                    val uniqueKey = "${it.contactId}_${it.number}"
                    contactMap[uniqueKey] = it
                }
            }

            // 🔹 2. Organization (Company)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/organization"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val orgIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val company = cursor.getString(orgIndex) ?: ""
                    contactMap[contactId]?.company = company
                }
            }

            // 🔹 3. Notes
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/note"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val noteIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val noteText = cursor.getString(noteIndex) ?: ""
                    contactMap[contactId]?.note = noteText
                }
            }

            // 🔹 4. Names
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "data2", "data5", "data3", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val firstName = cursor.getString(firstIndex)
                    val middleName = cursor.getString(middleIndex)
                    val surname = cursor.getString(lastIndex)

                    contactMap[contactId]?.let {
                        if (!firstName.isNullOrEmpty()) it.firstName = firstName
                        if (!middleName.isNullOrEmpty()) it.middleName = middleName
                        if (!surname.isNullOrEmpty()) it.surname = surname
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sortedList = contactMap.values
            .filter { !it.number.isNullOrBlank() && !it.contactId.isNullOrEmpty() }
            .sortedWith { c1, c2 ->
                val name1 = c1.displayName?.trim() ?: ""
                val name2 = c2.displayName?.trim() ?: ""
                if (name1.isEmpty() && name2.isEmpty()) return@sortedWith 0
                if (name1.isEmpty()) return@sortedWith 1
                if (name2.isEmpty()) return@sortedWith -1
                val char1 = name1.uppercase()[0]
                val char2 = name2.uppercase()[0]
                val isLetter1 = char1 in 'A'..'Z'
                val isLetter2 = char2 in 'A'..'Z'
                when {
                    isLetter1 && !isLetter2 -> -1
                    !isLetter1 && isLetter2 -> 1
                    else -> name1.compareTo(name2, ignoreCase = true)
                }
            }

        var lastHeader = ""
        for (contact in sortedList) {
            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()
            val header =
                if (firstChar != null && firstChar.isLetter()) firstChar.toString() else "#"
            if (header != lastHeader) {
                result.add(ContactListItem.Header(header))
                lastHeader = header
            }
            result.add(ContactListItem.Contact(contact))
        }
        return result
    }

    fun getContactsByDevice(): MutableList<ContactListItem> {
        val contactMap = LinkedHashMap<String, ContactModel>()
        val result = mutableListOf<ContactListItem>()

        try {
            val resolver = context.contentResolver

            // 🔹 0. Get all raw_contact_id's and contact_id's belonging to the Device
            val validContactIds = mutableSetOf<Long>()
            val validRawContactIds = mutableSetOf<Long>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts._ID
                ),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.isNull(0)) continue
                    val id = cursor.getLong(0)
                    val type = cursor.getString(1) ?: ""
                    val name = cursor.getString(2) ?: ""
                    val rawId = if (!cursor.isNull(3)) cursor.getLong(3) else null

                    val isGoogle = type == "com.google"
                    val isWhatsApp =
                        type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                    val isTelegram = type == "org.telegram.messenger" || type.contains(
                        "telegram",
                        ignoreCase = true
                    )
                    val isEmail = name.contains("@") && type.contains("exchange", ignoreCase = true)

                    if (!isGoogle && !isWhatsApp && !isTelegram && !isEmail) {
                        validContactIds.add(id)
                        if (rawId != null) validRawContactIds.add(rawId)
                    }
                }
            }

            // 🔹 1. Get Base Contacts (With phone number filter)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
                null,
                "${ContactsContract.Contacts.SORT_KEY_PRIMARY} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex =
                    cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex)
                    if (contactId in validContactIds) {
                        val displayName = cursor.getString(nameIndex) ?: ""
                        val photoUri = cursor.getString(photoIndex)
                        val starred = cursor.getInt(starredIndex)

                        val contact = ContactModel().apply {
                            this.contactId = contactId.toString()
                            this.displayName = displayName
                            this.userThumbnail = photoUri
                            this.isFavourite = starred
                            this.firstName = displayName
                        }
                        contactMap[contactId.toString()] = contact
                    }
                }
            }

            // 🔹 1.5 Get Phone Numbers
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val numberIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val contactsWithMultipleNumbers = mutableListOf<ContactModel>()
                while (cursor.moveToNext()) {
                    if (cursor.isNull(idIndex)) continue
                    val contactId = cursor.getLong(idIndex)
                    val phoneNumber = cursor.getString(numberIndex) ?: continue

                    // Use contactId (already filtered by validContactIds in step 1),
                    // not rawContactId — phone data may belong to a merged raw contact
                    // (e.g. Google) even when the contact itself is device-originated.
                    contactMap[contactId.toString()]?.let { contact ->
                        if (contact.number.isNullOrEmpty()) {
                            contact.number = phoneNumber
                        } else if (contact.number != phoneNumber) {
                            val additionalContact = contact.copy().apply {
                                this.number = phoneNumber
                            }
                            contactsWithMultipleNumbers.add(additionalContact)
                        }
                    }
                }
                contactsWithMultipleNumbers.forEach {
                    val uniqueKey = "${it.contactId}_${it.number}"
                    contactMap[uniqueKey] = it
                }
            }

            // 🔹 2. Organization (Company)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/organization"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val orgIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val company = cursor.getString(orgIndex) ?: ""
                    contactMap[contactId]?.company = company
                }
            }

            // 🔹 3. Notes
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/note"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val noteIndex = cursor.getColumnIndex("data1")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val noteText = cursor.getString(noteIndex) ?: ""
                    contactMap[contactId]?.note = noteText
                }
            }

            // 🔹 4. Names
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("data1", "data2", "data5", "data3", "contact_id"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")
                while (cursor.moveToNext()) {
                    val contactId = cursor.getLong(idIndex).toString()
                    val firstName = cursor.getString(firstIndex)
                    val middleName = cursor.getString(middleIndex)
                    val surname = cursor.getString(lastIndex)

                    contactMap[contactId]?.let {
                        if (!firstName.isNullOrEmpty()) it.firstName = firstName
                        if (!middleName.isNullOrEmpty()) it.middleName = middleName
                        if (!surname.isNullOrEmpty()) it.surname = surname
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val sortedList = contactMap.values
            .filter { !it.number.isNullOrBlank() && !it.contactId.isNullOrEmpty() }
            .sortedWith { c1, c2 ->
                val name1 = c1.displayName?.trim() ?: ""
                val name2 = c2.displayName?.trim() ?: ""
                if (name1.isEmpty() && name2.isEmpty()) return@sortedWith 0
                if (name1.isEmpty()) return@sortedWith 1
                if (name2.isEmpty()) return@sortedWith -1
                val char1 = name1.uppercase()[0]
                val char2 = name2.uppercase()[0]
                val isLetter1 = char1 in 'A'..'Z'
                val isLetter2 = char2 in 'A'..'Z'
                when {
                    isLetter1 && !isLetter2 -> -1
                    !isLetter1 && isLetter2 -> 1
                    else -> name1.compareTo(name2, ignoreCase = true)
                }
            }

        var lastHeader = ""
        for (contact in sortedList) {
            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()
            val header =
                if (firstChar != null && firstChar.isLetter()) firstChar.toString() else "#"
            if (header != lastHeader) {
                result.add(ContactListItem.Header(header))
                lastHeader = header
            }
            result.add(ContactListItem.Contact(contact))
        }
        return result
    }


    fun getAccountContactCounts(isMerge: Boolean): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        val resolver = context.contentResolver

        try {
            // ── Step 1: Build the "visible & listable" contact map ────────────
            // Only include contacts with phone numbers
            data class ContactInfo(val displayName: String, val hasPhone: Boolean)
            val contactInfo = mutableMapOf<Long, ContactInfo>()

            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                ),
                "${ContactsContract.Contacts.HAS_PHONE_NUMBER} > 0",
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: ""
                    val hasPhone = cursor.getInt(hasPhoneIdx) == 1
                    if (hasPhone) {
                        contactInfo[id] = ContactInfo(name, hasPhone)
                    }
                }
            }

            // ── Step 2: Device Only + per-Google-account + All Valid ──────────
            val deviceIdentifiers = mutableSetOf<String>()
            val googleAccountMap = mutableMapOf<String, MutableSet<String>>()
            val allValidIdentifiers = mutableSetOf<String>()

            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.CONTACT_ID,
                    ContactsContract.RawContacts.ACCOUNT_TYPE,
                    ContactsContract.RawContacts.ACCOUNT_NAME
                ),
                "${ContactsContract.RawContacts.DELETED} = 0",
                null,
                null
            )?.use { cursor ->
                val contactIdIdx = cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID)
                val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)

                while (cursor.moveToNext()) {
                    if (cursor.isNull(contactIdIdx)) continue
                    val contactId = cursor.getLong(contactIdIdx)

                    // Skip contacts that would be filtered out (no phone number)
                    val info = contactInfo[contactId] ?: continue

                    val type = cursor.getString(typeIdx) ?: ""
                    val accountName = cursor.getString(nameIdx) ?: ""

                    // isMerge: same display name → one slot
                    val identifier = if (isMerge) info.displayName.ifEmpty { contactId.toString() }
                    else contactId.toString()

                    val isGoogle = type == "com.google"
                    val isWhatsApp = type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                    val isTelegram = type == "org.telegram.messenger" || type.contains("telegram", ignoreCase = true)
                    val isEmail = accountName.contains("@") && type.contains("exchange", ignoreCase = true)

                    // Only count if NOT WhatsApp and NOT Telegram
                    if (!isWhatsApp && !isTelegram) {
                        allValidIdentifiers.add(identifier)

                        if (!isGoogle && !isEmail) {
                            deviceIdentifiers.add(identifier)
                        }

                        if (isGoogle && accountName.isNotEmpty()) {
                            googleAccountMap.getOrPut(accountName) { mutableSetOf() }.add(identifier)
                        }
                    }
                }
            }

            counts["All Accounts"] = allValidIdentifiers.size
            counts["Device Only"] = deviceIdentifiers.size
            googleAccountMap.forEach { (email, set) -> counts[email] = set.size }

        } catch (e: Exception) {
            e.printStackTrace()
            counts.putIfAbsent("All Accounts", 0)
            counts.putIfAbsent("Device Only", 0)
        }

        return counts
    }
}