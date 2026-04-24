package com.example.contactmanager.repository

import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.example.contactmanager.models.ContactListItem
import com.example.contactmanager.models.ContactModel
import com.google.i18n.phonenumbers.PhoneNumberUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ContactRepository @Inject constructor(@param:ApplicationContext private val context: Context) {

    fun getContactsByAccountWithHeaders(): MutableList<ContactListItem> {

        val contactMap = LinkedHashMap<String, ContactModel>()
        val result = mutableListOf<ContactListItem>()

        try {
            val resolver = context.contentResolver

            // 🔹 1. Get ALL Contacts (Guarantees no missing contacts of any kind)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                "display_name COLLATE NOCASE ASC"
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    if (contactId != null) {
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
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    val phoneNumber = cursor.getString(numberIndex)

                    contactMap[contactId]?.let {
                        if (it.number.isNullOrEmpty() && !phoneNumber.isNullOrEmpty()) {
                            it.number = phoneNumber
                        }
                    }
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
                val companyIndex = cursor.getColumnIndex("data1")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    val company = cursor.getString(companyIndex)

                    contactMap[contactId]?.let {
                        if (!company.isNullOrEmpty()) {
                            it.company = company
                        }
                    }
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
                    val contactId = cursor.getString(idIndex)
                    val note = cursor.getString(noteIndex)

                    contactMap[contactId]?.let {
                        if (!note.isNullOrEmpty()) {
                            it.note = note
                        }
                    }
                }
            }

            // 🔹 4. Structured Name
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("contact_id", "data2", "data5", "data3"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)

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

        // 🔹 Sort
        val sortedList = contactMap.values
            .filter { !it.displayName.isNullOrBlank() || !it.number.isNullOrBlank() || !it.userThumbnail.isNullOrBlank() }
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

        // 🔹 Headers + Contacts mapping
        var lastHeader = ""

        for (contact in sortedList) {

            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()

            val header = if (firstChar != null && firstChar.isLetter()) {
                firstChar.toString()
            } else {
                "#"
            }

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

            // 🔹 0. Get all contact_id's belonging to this Google Account
            val validContactIds = mutableSetOf<String>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                "${ContactsContract.RawContacts.DELETED} = 0 AND ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                arrayOf(accountEmail, "com.google"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    if (id != null) validContactIds.add(id)
                }
            }

            // 🔹 1. Get Base Contacts (Filtered by valid contact_ids)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                "display_name COLLATE NOCASE ASC"
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    
                    if (contactId != null && contactId in validContactIds) {
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
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    val phoneNumber = cursor.getString(numberIndex)

                    contactMap[contactId]?.let {
                        if (it.number.isNullOrEmpty() && !phoneNumber.isNullOrEmpty()) {
                            it.number = phoneNumber
                        }
                    }
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
                val companyIndex = cursor.getColumnIndex("data1")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    
                    contactMap[contactId]?.let {
                        val company = cursor.getString(companyIndex)
                        if (!company.isNullOrEmpty()) {
                            it.company = company
                        }
                    }
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
                    val contactId = cursor.getString(idIndex)
                    
                    contactMap[contactId]?.let {
                        val note = cursor.getString(noteIndex)
                        if (!note.isNullOrEmpty()) {
                            it.note = note
                        }
                    }
                }
            }

            // 🔹 4. Structured Name (First, Middle, Last)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("contact_id", "data2", "data5", "data3"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)

                    contactMap[contactId]?.let {
                        val firstName = cursor.getString(firstIndex)
                        val middleName = cursor.getString(middleIndex)
                        val surname = cursor.getString(lastIndex)

                        if (!firstName.isNullOrEmpty()) it.firstName = firstName
                        if (!middleName.isNullOrEmpty()) it.middleName = middleName
                        if (!surname.isNullOrEmpty()) it.surname = surname
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🔹 5. Sorting
        val sortedList = contactMap.values
            .filter { !it.displayName.isNullOrBlank() || !it.number.isNullOrBlank() || !it.userThumbnail.isNullOrBlank() }
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

        // 🔹 6. Headers (A-Z, #)
        var lastHeader = ""

        for (contact in sortedList) {

            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()

            val header = if (firstChar != null && firstChar.isLetter()) {
                firstChar.toString()
            } else {
                "#"
            }

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

            // 🔹 0. Get all contact_id's belonging to the Device
            val validContactIds = mutableSetOf<String>()
            resolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.CONTACT_ID, 
                    ContactsContract.RawContacts.ACCOUNT_TYPE, 
                    ContactsContract.RawContacts.ACCOUNT_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val type = cursor.getString(1) ?: ""
                    val name = cursor.getString(2) ?: ""

                    // If it's a known third-party/sync account, skip it. Otherwise, consider it Device.
                    val isGoogle = type == "com.google"
                    val isWhatsApp = type == "com.whatsapp" || type.contains("whatsapp", ignoreCase = true)
                    val isTelegram = type == "org.telegram.messenger" || type.contains("telegram", ignoreCase = true)
                    val isEmail = name.contains("@") && type.contains("exchange", ignoreCase = true)

                    if (!isGoogle && !isWhatsApp && !isTelegram && !isEmail) {
                        validContactIds.add(id)
                    }
                }
            }

            // 🔹 1. Get Base Contacts (Filtered by valid contact_ids)
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                "display_name COLLATE NOCASE ASC"
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    
                    if (contactId != null && contactId in validContactIds) {
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
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    val phoneNumber = cursor.getString(numberIndex)

                    contactMap[contactId]?.let {
                        if (it.number.isNullOrEmpty() && !phoneNumber.isNullOrEmpty()) {
                            it.number = phoneNumber
                        }
                    }
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
                val companyIndex = cursor.getColumnIndex("data1")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    
                    contactMap[contactId]?.let {
                        val company = cursor.getString(companyIndex)
                        if (!company.isNullOrEmpty()) {
                            it.company = company
                        }
                    }
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
                    val contactId = cursor.getString(idIndex)
                    
                    contactMap[contactId]?.let {
                        val note = cursor.getString(noteIndex)
                        if (!note.isNullOrEmpty()) {
                            it.note = note
                        }
                    }
                }
            }

            // 🔹 4. Structured Name (First, Middle, Last)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("contact_id", "data2", "data5", "data3"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)

                    contactMap[contactId]?.let {
                        val firstName = cursor.getString(firstIndex)
                        val middleName = cursor.getString(middleIndex)
                        val surname = cursor.getString(lastIndex)

                        if (!firstName.isNullOrEmpty()) it.firstName = firstName
                        if (!middleName.isNullOrEmpty()) it.middleName = middleName
                        if (!surname.isNullOrEmpty()) it.surname = surname
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 🔹 5. Sorting
        val sortedList = contactMap.values
            .filter { !it.displayName.isNullOrBlank() || !it.number.isNullOrBlank() || !it.userThumbnail.isNullOrBlank() }
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

        // 🔹 6. Headers (A-Z, #)
        var lastHeader = ""

        for (contact in sortedList) {

            val firstChar = contact.displayName?.firstOrNull()?.uppercaseChar()

            val header = if (firstChar != null && firstChar.isLetter()) {
                firstChar.toString()
            } else {
                "#"
            }

            if (header != lastHeader) {
                result.add(ContactListItem.Header(header))
                lastHeader = header
            }

            result.add(ContactListItem.Contact(contact))
        }

        return result
    }


    fun loadAllContacts(): List<ContactModel> {

        val contactMap = LinkedHashMap<String, ContactModel>()

        try {
            val resolver = context.contentResolver

            // 🔹 1. Main Contacts (Phone)
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                "display_name COLLATE NOCASE ASC"
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex("contact_id")
                val nameIndex = cursor.getColumnIndex("display_name")
                val numberIndex = cursor.getColumnIndex("data1")
                val photoIndex = cursor.getColumnIndex("photo_uri")
                val starredIndex = cursor.getColumnIndex("starred")

                while (cursor.moveToNext()) {

                    val contactId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex) ?: ""
                    val phoneNumber = cursor.getString(numberIndex)
                    val photoUri = cursor.getString(photoIndex)
                    val starred = cursor.getInt(starredIndex)

                    if (!contactMap.containsKey(contactId)) {

                        val contact = ContactModel().apply {
                            this.contactId = contactId
                            this.displayName = displayName
                            this.number = phoneNumber
                            this.userThumbnail = photoUri
                            this.isFavourite = starred
                            this.firstName = displayName
                        }

                        contactMap[contactId] = contact
                    }
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
                val companyIndex = cursor.getColumnIndex("data1")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)
                    val company = cursor.getString(companyIndex)

                    contactMap[contactId]?.let {
                        if (!company.isNullOrEmpty()) {
                            it.company = company
                        }
                    }
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
                    val contactId = cursor.getString(idIndex)
                    val note = cursor.getString(noteIndex)

                    contactMap[contactId]?.let {
                        if (!note.isNullOrEmpty()) {
                            it.note = note
                        }
                    }
                }
            }

            // 🔹 4. Structured Name
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("contact_id", "data2", "data5", "data3"),
                "mimetype = ?",
                arrayOf("vnd.android.cursor.item/name"),
                null
            )?.use { cursor ->

                val idIndex = cursor.getColumnIndex("contact_id")
                val firstIndex = cursor.getColumnIndex("data2")
                val middleIndex = cursor.getColumnIndex("data5")
                val lastIndex = cursor.getColumnIndex("data3")

                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(idIndex)

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

        val list = ArrayList(contactMap.values)

        // 🔹 5. Custom Sort
        list.sortWith { c1, c2 ->

            val name1 = c1.displayName?.trim() ?: ""
            val name2 = c2.displayName?.trim() ?: ""

            if (name1.isEmpty() && name2.isEmpty()) return@sortWith 0
            if (name1.isEmpty()) return@sortWith 1
            if (name2.isEmpty()) return@sortWith -1

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

        return list
    }

}