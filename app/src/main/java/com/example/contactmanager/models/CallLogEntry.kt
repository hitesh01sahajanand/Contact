package com.example.contactmanager.models

import java.util.Date

/*data class CallLogModel(
    val name: String?,
    val number: String,
    var type: Int,
    var date: Long,
    var duration: Long,
    var isMissed: Boolean,
    var isIncoming: Boolean,
    var isOutgoing: Boolean,
    val photoUri: String?,
    var callCount: Int = 1,
    var avatar: Bitmap? = null,
    var isRejected: Boolean,
)*/

data class CallLogEntry(
    var stringNumber: String? = null,
    var stringType: String? = null,
    var dateData: Date? = null,
    var stringDuration: String? = null,
    var stringCallName: String? = null,
    var stringDateCategory: String? = null,
    var stringPhotoUri: String? = null,
    var contactId: String? = null,
    var isBlocked: Boolean = false,
    var intType: Int = 0,
    var intColor: Int = 0,
    var intIconColor: Int = 0,
    var callIds: MutableList<Long> = mutableListOf(),
    var callCount: Int = 1
)
{

    fun resetCallIds(id: Long) {
        callIds.clear()
        callIds.add(id)
    }

    fun getFormattedName(showSurname: Boolean): String? {
        var nameToReturn = stringCallName

        if (!nameToReturn.isNullOrEmpty()) {
            val parts = nameToReturn.trim().split("\\s+".toRegex())
            if (parts.size == 2 && parts[0].equals(parts[1], ignoreCase = true)) {
                nameToReturn = parts[0]
            }
        }

        if (showSurname || nameToReturn.isNullOrEmpty()) {
            return nameToReturn
        } else {
            val split = nameToReturn.trim().split("\\s+".toRegex())
            if (split.size > 1) {
                return split.dropLast(1).joinToString(" ").trim()
            }
            return nameToReturn
        }
    }
}
