package com.example.contactmanager.models

import android.graphics.Bitmap

data class NotificationViewModel(
    var callModel: CallingModel? = null,
    var imageOfUserCall: Bitmap? = null,
    var nameFromCall: String? = null,
    var phoneNumberOfCall: String? = null
)
