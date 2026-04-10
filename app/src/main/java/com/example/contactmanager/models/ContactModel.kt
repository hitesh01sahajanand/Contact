package com.example.contactmanager.models

import android.graphics.Bitmap

/*
data class ContactModel(
    val id: String,
    val name: String,
    val number: String,
    var isFavorite: Boolean,
    val isRecent: Boolean,
    val isMissed: Boolean,
    val lastCallDateTime: Long?,
    val photoUri: String?,
    var avatar: Bitmap? = null
)*/

import kotlin.math.abs
import androidx.core.graphics.toColorInt

data class ContactModel(
    var groupId: Long = 0,
    var groupTitle: String? = null,
    var note: String? = null,
    var _id: String? = null,
    var company: String? = null,
    var displayName: String? = null,
    var email: String? = null,
    var firstName: String? = null,
    var middleName: String? = null,
    var surname: String? = null,
    var number: String? = null,
    var userThumbnail: String? = null,
    var contactsCount: Int = 0,
    var isSelected: Boolean = false,
    var isFavourite: Int = 0,
    var lookupKey: String? = null
) {

    companion object {
        val COLOR_CODES = arrayOf("#2173C2", "#FFB950", "#AEB33C", "#8456E8", "#60CB6B")
    }

    var contactId: String? = null
        set(value) {
            field = value
            setColorIndexBasedOnId(value)
        }

    var color: Int = -1

    var colorIndex: Int = -1
        set(value) {
            val index = value.coerceIn(0, COLOR_CODES.size - 1)
            field = index
            try {
                color = COLOR_CODES[index].toColorInt()
            } catch (e: IllegalArgumentException) {
                color = -1
            }
        }

    fun hasThumbnail(): Boolean = userThumbnail != null

    fun getFormattedName(startWithSurname: Boolean): String {
        if (!startWithSurname) {
            val nameToUse = when {
                !displayName.isNullOrEmpty() -> displayName!!
                !firstName.isNullOrEmpty() -> firstName!!
                else -> ""
            }

            if (nameToUse.isNotEmpty()) {
                val parts = nameToUse.trim().split("\\s+".toRegex())
                if (parts.size == 2 && parts[0].equals(parts[1], true)) {
                    return parts[0]
                }
                return nameToUse
            }
            return ""
        }

        if (firstName.isNullOrEmpty()) {
            return displayName ?: ""
        }

        return buildString {
            if (!surname.isNullOrEmpty()) {
                append(surname).append(" ").append(firstName)
                if (!middleName.isNullOrEmpty()) {
                    append(" ").append(middleName)
                }
            } else {
                append(firstName)
                if (!middleName.isNullOrEmpty()) {
                    append(" ").append(middleName)
                }
                if (!surname.isNullOrEmpty() &&
                    !firstName!!.lowercase().contains(surname!!.lowercase())
                ) {
                    append(" ").append(surname)
                }
            }
        }.trim()
    }

    private fun setColorIndexBasedOnId(id: String?) {
        if (id != null) {
            colorIndex = abs(id.hashCode()) % COLOR_CODES.size
        }
    }
}
