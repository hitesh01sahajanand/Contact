package com.example.contactmanager.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_numbers")
data class BlockModel(
    @PrimaryKey
    val phoneNumber: String,
    val blockedAt: Long = System.currentTimeMillis()
)
