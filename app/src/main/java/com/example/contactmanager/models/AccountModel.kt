package com.example.contactmanager.models

import android.graphics.Bitmap

data class AccountModel(
    val name: String,
    val email: String,
    val isAll: Boolean = false,
    val avtar: Bitmap? = null,
    val count: Int = 0
)