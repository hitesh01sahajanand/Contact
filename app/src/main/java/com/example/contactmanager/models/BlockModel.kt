package com.example.contactmanager.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockModel(
    @PrimaryKey
    val phoneNumber: String,
    val name: String? = null,
    val photoUri: String? = null,
    val blockedAt: Long = System.currentTimeMillis()
)
