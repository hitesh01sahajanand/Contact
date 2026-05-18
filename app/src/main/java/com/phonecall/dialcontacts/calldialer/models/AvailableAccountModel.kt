package com.phonecall.dialcontacts.calldialer.models

data class AvailableAccountModel(
    val accountName: String,
    val accountType: String,
    val displayName: String,
    var count: Int = 0,
    var isSelected: Boolean = false
)
