package com.example.contactmanager.models

data class AvailableAccountModel(
    val accountName: String,
    val accountType: String,
    val displayName: String,
    var count: Int = 0,
    var isSelected: Boolean = false
)
