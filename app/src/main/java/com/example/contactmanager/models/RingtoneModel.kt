package com.example.contactmanager.models

import android.net.Uri

data class RingtoneModel(
    val name: String,
    val uri: Uri,
    var isPlaying: Boolean = false,
    var isSelected: Boolean = false
)
