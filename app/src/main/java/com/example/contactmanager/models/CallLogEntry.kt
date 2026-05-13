package com.example.contactmanager.models

import java.util.Date
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
    var callCount: Int = 1,
    var simId: Int = -1
)
{

    fun resetCallIds(id: Long) {
        callIds.clear()
        callIds.add(id)
    }
}
