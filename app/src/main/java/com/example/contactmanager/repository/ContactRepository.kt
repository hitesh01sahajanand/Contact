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

    /*fun getSortedContactsWithHeaders(): List<ContactListItem> {

        val contactList = mutableListOf<ContactModel>()
        val uniqueNumbers = mutableSetOf<String>()
        val result = mutableListOf<ContactListItem>()

        val phoneUtil = PhoneNumberUtil.getInstance()

        try {
            val cursor = context.contentResolver.query(
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
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                    ) ?: ""

                    var number = it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                    ) ?: ""

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                        )
                    )

                    val isFavorite = it.getInt(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.STARRED
                        )
                    ) == 1

                    // 🔥 GLOBAL NORMALIZATION (libphonenumber)
                    var normalizedNumber: String? = null

                    try {
                        val parsedNumber = phoneUtil.parse(number, null)
                        normalizedNumber = phoneUtil.format(
                            parsedNumber,
                            PhoneNumberUtil.PhoneNumberFormat.E164
                        )
                    } catch (e: Exception) {
                        // fallback (basic cleanup)
                        number = number.replace("\\s".toRegex(), "")
                            .replace("-", "")
                            .replace("(", "")
                            .replace(")", "")

                        normalizedNumber = number
                    }

                    // 🔥 Duplicate Remove
                    if (normalizedNumber.isNullOrEmpty()) continue
                    if (!uniqueNumbers.add(normalizedNumber)) continue

                    contactList.add(
                        ContactModel(
                            id = contactId,
                            name = name,
                            number = normalizedNumber,
                            isFavorite = isFavorite,
                            isRecent = false,
                            isMissed = false,
                            lastCallDateTime = null,
                            photoUri = photoUri
                        )
                    )
                }
            }

            // 🔥 SORT
            val sortedList = contactList.sortedBy {
                it.name.trim().lowercase()
            }

            var lastHeader = ""

            for (contact in sortedList) {

                val firstChar = contact.name.firstOrNull()?.uppercaseChar()

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

        } catch (e: Exception) {
            Log.e("TAG", "getSortedContactsWithHeaders: ${e.message}")
        }

        return result
    }*/


    /*fun getContactsByAccountWithHeaders(accountName: String): List<ContactListItem> {

        val contactMap = LinkedHashMap<String, ContactModel>() // 🔥 key = contactId
        val result = mutableListOf<ContactListItem>()

        try {

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                """
            ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?
            """.trimIndent(),
                arrayOf(accountName, "com.google"),
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
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        )
                    ) ?: ""

                    var number = it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )
                    ) ?: ""

                    val photoUri = it.getString(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                        )
                    )

                    // 🔥 normalize
                    number = number.replace("\\D".toRegex(), "")
                    if (number.length > 10) number = number.takeLast(10)

                    val key = "$contactId-$number"

                    if (contactMap.containsKey(key)) continue

                    val isFavorite = it.getInt(
                        it.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.STARRED
                        )
                    ) == 1

                    contactMap[key] = ContactModel(
                        id = contactId,
                        name = name,
                        number = number,
                        isFavorite = isFavorite,
                        isRecent = false,
                        isMissed = false,
                        lastCallDateTime = null,
                        photoUri = photoUri
                    )
                }
            }

            val sortedList = contactMap.values.sortedBy {
                it.name.trim().lowercase()
            }

            var lastHeader = ""

            for (contact in sortedList) {

                val firstChar = contact.name.firstOrNull()?.uppercaseChar()

                val header = if (firstChar != null && firstChar.isLetter()) {
                    firstChar.toString()
                } else "#"

                if (header != lastHeader) {
                    result.add(ContactListItem.Header(header))
                    lastHeader = header
                }

                result.add(ContactListItem.Contact(contact))
            }

        } catch (e: Exception) {
            Log.e("TAG", "getContactsByAccount: ${e.message}")
        }

        return result
    }*/


    fun getContactsByAccountWithHeaders(): MutableList<ContactListItem> {

        val contactMap = LinkedHashMap<String, ContactModel>()
        val result = mutableListOf<ContactListItem>()

        try {
            val resolver = context.contentResolver

            // 🔹 1. Main Contacts
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

        // 🔹 Sort
        val sortedList = contactMap.values.sortedWith { c1, c2 ->
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

            // 🔹 1. Main Contacts (Filtered by Google Account)
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                """
            ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?
            """.trimIndent(),
                arrayOf(accountEmail, "com.google"),
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
                """
            mimetype = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?
            """.trimIndent(),
                arrayOf("vnd.android.cursor.item/organization", accountEmail, "com.google"),
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
                """
            mimetype = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?
            """.trimIndent(),
                arrayOf("vnd.android.cursor.item/note", accountEmail, "com.google"),
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

            // 🔹 4. Structured Name (First, Middle, Last)
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf("contact_id", "data2", "data5", "data3"),
                """
            mimetype = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_NAME} = ? AND 
            ${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?
            """.trimIndent(),
                arrayOf("vnd.android.cursor.item/name", accountEmail, "com.google"),
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

        // 🔹 5. Sorting
        val sortedList = contactMap.values.sortedWith { c1, c2 ->

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