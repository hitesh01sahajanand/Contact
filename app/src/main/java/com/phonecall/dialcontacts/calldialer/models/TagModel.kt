package com.phonecall.dialcontacts.calldialer.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagModel(
    @PrimaryKey
    val phoneNumber: String,
    val tagName: String
)
