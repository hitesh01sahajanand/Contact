package com.example.contactmanager.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagModel(
    @PrimaryKey
    val phoneNumber: String,
    val tagName: String
)
