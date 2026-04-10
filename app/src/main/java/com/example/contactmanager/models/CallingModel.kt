package com.example.contactmanager.models

import android.graphics.Bitmap
import android.telecom.Call

data class CallingModel(
    var call: Call? = null,
    var callDetails: Call.Details? = null,
    var childCallModel: MutableList<CallingModel> = mutableListOf(),
    var isConferenceCall: Boolean = false,
    var isPartOfConferenceCall: Boolean = false,
    var isUserImgFetched: Boolean = false,
    var name: String? = null,
    var phnNumber: String? = null,
    var sim: String? = null,
    var userImg: Bitmap? = null
)
