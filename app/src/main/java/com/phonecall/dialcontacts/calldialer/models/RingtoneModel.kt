package com.phonecall.dialcontacts.calldialer.models

import android.net.Uri

data class RingtoneModel(
    val name: String,
    val uri: Uri,
    var isPlaying: Boolean = false,
    var isSelected: Boolean = false
)
